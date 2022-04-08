/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.solutions.spannerddl.diff;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.cloud.solutions.spannerddl.parser.ASTalter_table_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTcheck_constraint;
import com.google.cloud.solutions.spannerddl.parser.ASTcolumn_def;
import com.google.cloud.solutions.spannerddl.parser.ASTcolumn_type;
import com.google.cloud.solutions.spannerddl.parser.ASTcreate_index_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTcreate_table_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTddl_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTforeign_key;
import com.google.cloud.solutions.spannerddl.parser.ASToptions_clause;
import com.google.cloud.solutions.spannerddl.parser.DdlParser;
import com.google.cloud.solutions.spannerddl.parser.DdlParserTreeConstants;
import com.google.cloud.solutions.spannerddl.parser.ParseException;
import com.google.cloud.solutions.spannerddl.parser.SimpleNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapDifference;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compares two Cloud Spanner Schema (DDL) files, and can generate the ALTER statements to convert
 * one to the other.
 *
 * <p>Example usage:
 *
 * <p>Pass the original and new DDL text to the {@link #build(String, String)} function, and call
 * {@link #generateDifferenceStatements(Map)} to generate the list of {@code ALTER} statements.
 *
 * <p>eg:
 *
 * <pre>
 * List&lt;String&gt; statements = DdlDiff.build(originalDDL, newDDL)
 *    .generateDifferenceStatements(true, true);
 * </pre>
 *
 * or execute the {@link #main(String[]) main()} function with the {@link #buildOptions()
 * appropriate command line options}.
 */
public class DdlDiff {

  private static final Logger LOG = LoggerFactory.getLogger(DdlDiff.class);
  public static final String ORIGINAL_DDL_FILE_OPT = "originalDdlFile";
  public static final String NEW_DDL_FILE_OPT = "newDdlFile";
  public static final String OUTPUT_DDL_FILE_OPT = "outputDdlFile";
  public static final String ALLOW_RECREATE_INDEXES_OPT = "allowRecreateIndexes";
  public static final String ALLOW_RECREATE_CONSTRAINTS_OPT = "allowRecreateConstraints";
  public static final String ALLOW_DROP_STATEMENTS_OPT = "allowDropStatements";
  public static final String HELP_OPT = "help";

  private final MapDifference<String, ASTcreate_index_statement> indexDifferences;
  private final MapDifference<String, ASTcreate_table_statement> tableDifferences;
  private final MapDifference<String, ConstraintWrapper> constraintDifferences;
  private final Map<String, ASTcreate_table_statement> newTablesCreationOrder;
  private final Map<String, ASTcreate_table_statement> originalTablesCreationOrder;

  private static class ConstraintWrapper {

    private final String tableName;
    private final SimpleNode constraint;

    private ConstraintWrapper(String tableName, SimpleNode constraint) {
      this.tableName = tableName;
      this.constraint = constraint;
      if (!(constraint instanceof ASTforeign_key) && !(constraint instanceof ASTcheck_constraint)) {
        throw new IllegalArgumentException(
            "not a valid constraint type : " + constraint.toString());
      }
    }

    private String getName() {
      if (constraint instanceof ASTcheck_constraint) {
        return ((ASTcheck_constraint) constraint).getName();
      }
      if (constraint instanceof ASTforeign_key) {
        return ((ASTforeign_key) constraint).getName();
      }
      throw new IllegalArgumentException("not a valid constraint type : " + constraint.toString());
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof ConstraintWrapper) {
        return this.constraint.equals(((ConstraintWrapper) other).constraint);
      }
      return false;
    }
  }

  private DdlDiff(
      MapDifference<String, ASTcreate_table_statement> tableDifferences,
      Map<String, ASTcreate_table_statement> originalTablesCreationOrder,
      Map<String, ASTcreate_table_statement> newTablesCreationOrder,
      MapDifference<String, ASTcreate_index_statement> indexDifferences,
      MapDifference<String, ConstraintWrapper> constraintDifferences) {
    this.tableDifferences = tableDifferences;
    this.originalTablesCreationOrder = originalTablesCreationOrder;
    this.newTablesCreationOrder = newTablesCreationOrder;
    this.indexDifferences = indexDifferences;
    this.constraintDifferences = constraintDifferences;
  }

  public List<String> generateDifferenceStatements(Map<String, Boolean> options)
      throws DdlDiffException {
    ImmutableList.Builder<String> output = ImmutableList.builder();

    boolean allowDropStatements = options.get(ALLOW_DROP_STATEMENTS_OPT);

    if (!indexDifferences.entriesDiffering().isEmpty()
        && !options.get(ALLOW_RECREATE_INDEXES_OPT)) {
      throw new DdlDiffException(
          "At least one Index differs, and allowRecreateIndexes is not set.\n"
              + "Indexes: "
              + Joiner.on(", ").join(indexDifferences.entriesDiffering().keySet()));
    }

    if (!constraintDifferences.entriesDiffering().isEmpty()
        && !options.get(ALLOW_RECREATE_CONSTRAINTS_OPT)) {
      throw new DdlDiffException(
          "At least one FOREIGN KEY constraint differs, and allowRecreateForeignKeys is not set.\n"
              + Joiner.on(", ").join(constraintDifferences.entriesDiffering().keySet()));
    }

    // Drop deleted indexes.
    if (allowDropStatements) {
      // Drop deleted indexes.
      for (String indexName : indexDifferences.entriesOnlyOnLeft().keySet()) {
        LOG.info("Dropping deleted index: {}", indexName);
        output.add("DROP INDEX " + indexName);
      }
    }

    // Drop modified indexes that need to be re-created...
    for (String indexName : indexDifferences.entriesDiffering().keySet()) {
      LOG.info("Dropping changed index for re-creation: {}", indexName);
      output.add("DROP INDEX " + indexName);
    }

    // Drop deleted foreign keys
    for (ConstraintWrapper fk : constraintDifferences.entriesOnlyOnLeft().values()) {
      output.add("ALTER TABLE " + fk.tableName + " DROP CONSTRAINT " + fk.getName());
    }

    // Drop modified foreign keys that need to be re-created...
    for (ValueDifference<ConstraintWrapper> fkDiff :
        constraintDifferences.entriesDiffering().values()) {
      output.add(
          "ALTER TABLE "
              + fkDiff.leftValue().tableName
              + " DROP CONSTRAINT "
              + fkDiff.leftValue().getName());
    }

    if (allowDropStatements) {
      // Drop tables that have been deleted -- need to do it in reverse creation order.
      List<String> reverseOrderedTableNames = new ArrayList<>(originalTablesCreationOrder.keySet());
      Collections.reverse(reverseOrderedTableNames);
      for (String tableName : reverseOrderedTableNames) {
        if (tableDifferences.entriesOnlyOnLeft().containsKey(tableName)) {
          LOG.info("Dropping deleted table: {}", tableName);
          output.add("DROP TABLE " + tableName);
        }
      }
    }

    // Alter existing tables, or error if not possible.
    for (ValueDifference<ASTcreate_table_statement> difference :
        tableDifferences.entriesDiffering().values()) {
      LOG.info("Altering modified table: {}", difference.leftValue().getTableName());
      output.addAll(
          generateAlterTableStatements(difference.leftValue(), difference.rightValue(), options));
    }

    // Create new tables. Must be done in the order of creation in the new DDL.
    for (Map.Entry<String, ASTcreate_table_statement> newTableEntry :
        newTablesCreationOrder.entrySet()) {
      if (tableDifferences.entriesOnlyOnRight().containsKey(newTableEntry.getKey())) {
        LOG.info("Creating new table: {}", newTableEntry.getKey());
        output.add(newTableEntry.getValue().toString());
      }
    }

    // Create new indexes
    for (ASTcreate_index_statement index : indexDifferences.entriesOnlyOnRight().values()) {
      LOG.info("Creating new index: {}", index.getIndexName());
      output.add(index.toString());
    }

    // Re-create modified indexes...
    for (ValueDifference<ASTcreate_index_statement> difference :
        indexDifferences.entriesDiffering().values()) {
      LOG.info("Re-creating changed index: {}", difference.leftValue().getIndexName());
      output.add(difference.rightValue().toString());
    }

    // Create new constrants.
    for (ConstraintWrapper fk : constraintDifferences.entriesOnlyOnRight().values()) {
      output.add("ALTER TABLE " + fk.tableName + " ADD " + fk.constraint.toString());
    }

    // Re-create modified Foreign Keys.
    for (ValueDifference<ConstraintWrapper> fkDiff :
        constraintDifferences.entriesDiffering().values()) {
      output.add(
          "ALTER TABLE "
              + fkDiff.rightValue().tableName
              + " ADD "
              + fkDiff.rightValue().constraint.toString());
    }
    return output.build();
  }

  @VisibleForTesting
  static List<String> generateAlterTableStatements(
      ASTcreate_table_statement left, ASTcreate_table_statement right, Map<String, Boolean> options)
      throws DdlDiffException {
    ArrayList<String> alterStatements = new ArrayList<>();

    // Alter Table can:
    //   - Add constraints
    //   - drop constraints
    //   - Add cols
    //   - drop cols (if enabled)
    //   - change on-delete action for interleaved
    // ALTER TABLE ALTER COLUMN can:
    //   - change options on column
    //   - change not null on column.
    // note that constraints need to be dropped before columns, and created after columns.

    // Check interleaving has not changed.
    if (left.getInterleaveClause().isPresent() != right.getInterleaveClause().isPresent()) {
      throw new DdlDiffException("Cannot change interleaving on table " + left.getTableName());
    }

    if (left.getInterleaveClause().isPresent()
        && !(left.getInterleaveClause()
            .get()
            .getParentTableName()
            .equals(right.getInterleaveClause().get().getParentTableName()))) {
      throw new DdlDiffException(
          "Cannot change interleaved parent of table " + left.getTableName());
    }

    // Check Key is same
    if (!left.getPrimaryKey().toString().equals(right.getPrimaryKey().toString())) {
      throw new DdlDiffException("Cannot change primary key of table " + left.getTableName());
    }

    // On delete changed
    if (left.getInterleaveClause().isPresent()
        && !(left.getInterleaveClause()
            .get()
            .getOnDelete()
            .equals(right.getInterleaveClause().get().getOnDelete()))) {
      alterStatements.add(
          "ALTER TABLE "
              + left.getTableName()
              + " SET "
              + right.getInterleaveClause().get().getOnDelete());
    }

    // compare columns.
    MapDifference<String, ASTcolumn_def> columnDifferences =
        Maps.difference(left.getColumns(), right.getColumns());

    if (options.get(ALLOW_DROP_STATEMENTS_OPT)) {
      for (String columnName : columnDifferences.entriesOnlyOnLeft().keySet()) {
        alterStatements.add("ALTER TABLE " + left.getTableName() + " DROP COLUMN " + columnName);
      }
    }

    for (ASTcolumn_def column : columnDifferences.entriesOnlyOnRight().values()) {
      alterStatements.add(
          "ALTER TABLE " + left.getTableName() + " ADD COLUMN " + column.toString());
    }

    for (ValueDifference<ASTcolumn_def> columnDiff :
        columnDifferences.entriesDiffering().values()) {
      addColumnDiffs(left.getTableName(), alterStatements, columnDiff);
    }

    return alterStatements;
  }

  private static void addColumnDiffs(
      String tableName,
      ArrayList<String> alterStatements,
      ValueDifference<ASTcolumn_def> columnDiff)
      throws DdlDiffException {

    // check for compatible type changes.
    if (!columnDiff
        .leftValue()
        .getColumnTypeString()
        .equals(columnDiff.rightValue().getColumnTypeString())) {

      // check for changing lengths of Strings or Arrays - for arrays we need the 'root' type and
      // the depth.
      ASTcolumn_type leftRootType = columnDiff.leftValue().getColumnType();
      int leftArrayDepth = 0;
      while (leftRootType.isArray()) {
        leftRootType = leftRootType.getArraySubType();
        leftArrayDepth++;
      }
      ASTcolumn_type rightRootType = columnDiff.rightValue().getColumnType();
      int rightArrayDepth = 0;
      while (rightRootType.isArray()) {
        rightRootType = rightRootType.getArraySubType();
        rightArrayDepth++;
      }

      if (leftArrayDepth != rightArrayDepth
          || !leftRootType.getTypeName().equals(rightRootType.getTypeName())
          || (!leftRootType.getTypeName().equals("STRING")
              && !leftRootType.getTypeName().equals("BYTES"))) {
        throw new DdlDiffException(
            "Cannot change type of table "
                + tableName
                + " column "
                + columnDiff.leftValue().getColumnName()
                + " from "
                + columnDiff.leftValue().getColumnTypeString()
                + " to "
                + columnDiff.rightValue().getColumnTypeString());
      }
    }

    // check generated column diffs
    // check for compatible type changes.
    if (!Objects.equals(
        Objects.toString(columnDiff.leftValue().getGenerationClause()),
        Objects.toString(columnDiff.rightValue().getGenerationClause()))) {
      throw new DdlDiffException(
          "Cannot change generation clause of table "
              + tableName
              + " column "
              + columnDiff.leftValue().getColumnName()
              + " from "
              + columnDiff.leftValue().getGenerationClause()
              + " to "
              + columnDiff.rightValue().getGenerationClause());
    }

    // Not null or type length limit change.
    if (columnDiff.leftValue().isNotNull() != columnDiff.rightValue().isNotNull()
        || !columnDiff
            .leftValue()
            .getColumnTypeString()
            .equals(columnDiff.rightValue().getColumnTypeString())) {
      alterStatements.add(
          Joiner.on(" ")
              .skipNulls()
              .join(
                  "ALTER TABLE",
                  tableName,
                  "ALTER COLUMN",
                  columnDiff.rightValue().getColumnName(),
                  columnDiff.rightValue().getColumnTypeString(),
                  (columnDiff.rightValue().isNotNull() ? "NOT NULL" : null)));
    }

    // Update options.
    ASToptions_clause leftOptionsClause = columnDiff.leftValue().getOptionsClause();
    Map<String, String> leftOptions =
        leftOptionsClause == null ? Collections.emptyMap() : leftOptionsClause.getKeyValueMap();

    TreeMap<String, String> optionsToUpdate = new TreeMap<>();
    // add all in right, then remove those that are the same as left
    ASToptions_clause rightOptionsClause = columnDiff.rightValue().getOptionsClause();
    if (rightOptionsClause != null) {
      optionsToUpdate.putAll(rightOptionsClause.getKeyValueMap());
    }

    for (Map.Entry<String, String> left : leftOptions.entrySet()) {
      if (optionsToUpdate.containsKey(left.getKey())) {
        if (optionsToUpdate.get(left.getKey()).equals(left.getValue())) {
          // same value - remove from to update list.
          optionsToUpdate.remove(left.getKey());
        }
        // else new value will remain.
      } else {
        // not present in right, add as 'null'
        optionsToUpdate.putIfAbsent(left.getKey(), "NULL");
      }
    }

    for (Map.Entry<String, String> optionToUpdate : optionsToUpdate.entrySet()) {
      alterStatements.add(
          "ALTER TABLE "
              + tableName
              + " ALTER COLUMN "
              + columnDiff.rightValue().getColumnName()
              + " SET OPTIONS ("
              + optionToUpdate.getKey()
              + "="
              + optionToUpdate.getValue()
              + ")");
    }
  }

  static DdlDiff build(String originalDDL, String newDDL) throws DdlDiffException {
    List<ASTddl_statement> originalStatements = parseDDL(originalDDL);
    List<ASTddl_statement> newStatements = parseDDL(newDDL);

    Map<String, ASTcreate_table_statement> originalTablesCreationOrder = new LinkedHashMap<>();
    Map<String, ASTcreate_index_statement> originalIndexes = new TreeMap<>();
    Map<String, ConstraintWrapper> originalConstraints = new TreeMap<>();

    separateTablesIndexesConstraints(
        originalStatements, originalTablesCreationOrder, originalIndexes, originalConstraints);
    Map<String, ASTcreate_table_statement> originalTablesNameOrder =
        new TreeMap<>(originalTablesCreationOrder);

    Map<String, ASTcreate_table_statement> newTablesCreationOrder = new LinkedHashMap<>();
    Map<String, ASTcreate_index_statement> newIndexes = new TreeMap<>();
    Map<String, ConstraintWrapper> newConstraints = new TreeMap<>();

    separateTablesIndexesConstraints(
        newStatements, newTablesCreationOrder, newIndexes, newConstraints);
    Map<String, ASTcreate_table_statement> newTablesNameOrder =
        new TreeMap<>(newTablesCreationOrder);

    return new DdlDiff(
        Maps.difference(originalTablesNameOrder, newTablesNameOrder),
        originalTablesCreationOrder,
        newTablesCreationOrder,
        Maps.difference(originalIndexes, newIndexes),
        Maps.difference(originalConstraints, newConstraints));
  }

  private static void separateTablesIndexesConstraints(
      List<ASTddl_statement> statements,
      Map<String, ASTcreate_table_statement> tables,
      Map<String, ASTcreate_index_statement> indexes,
      Map<String, ConstraintWrapper> constraints) {
    for (ASTddl_statement statement : statements) {
      if (statement.jjtGetChild(0) instanceof ASTcreate_table_statement) {
        ASTcreate_table_statement createTable =
            (ASTcreate_table_statement) statement.jjtGetChild(0);
        tables.put(createTable.getTableName(), createTable);
        // convert embedded constraint statements into wrapper object with table name
        // use a single map for all foreign keys, whether created in table or externally
        createTable.getConstraints().values().stream()
            .map(c -> new ConstraintWrapper(createTable.getTableName(), c))
            .forEach(c -> constraints.put(c.getName(), c));
      } else if (statement.jjtGetChild(0) instanceof ASTcreate_index_statement) {
        ASTcreate_index_statement createIndex =
            (ASTcreate_index_statement) statement.jjtGetChild(0);
        indexes.put(createIndex.getIndexName(), createIndex);

      } else if (statement.jjtGetChild(0) instanceof ASTalter_table_statement
          &&
          // use a single map for all foreign keys, whether created in table or externally
          (statement.jjtGetChild(0).jjtGetChild(1) instanceof ASTforeign_key
              || statement.jjtGetChild(0).jjtGetChild(1) instanceof ASTcheck_constraint)) {
        ASTalter_table_statement alterTable = (ASTalter_table_statement) statement.jjtGetChild(0);
        ConstraintWrapper constraint =
            new ConstraintWrapper(
                alterTable.jjtGetChild(0).toString(), (SimpleNode) alterTable.jjtGetChild(1));
        constraints.put(constraint.getName(), constraint);
      } else {
        throw new IllegalArgumentException(
            "Unsupported statement type: "
                + DdlParserTreeConstants.jjtNodeName[statement.jjtGetChild(0).getId()]);
      }
    }
  }

  @VisibleForTesting
  static List<ASTddl_statement> parseDDL(String original) throws DdlDiffException {
    String[] statements = original.replaceAll("--.*(\n|$)", "").split(";");
    ArrayList<ASTddl_statement> ddlStatements = new ArrayList<>(statements.length);

    for (String statement : statements) {
      statement = statement.trim();
      if (statement.isEmpty()) {
        continue;
      }
      try {
        ASTddl_statement ddlStatement = DdlParser.parseDdlStatement(statement);
        int statementType = ddlStatement.jjtGetChild(0).getId();

        if (statementType == DdlParserTreeConstants.JJTDROP_STATEMENT) {
          throw new IllegalArgumentException(
              "Unsupported statement:\n"
                  + statement
                  + "\nCan only create diffs from 'CREATE TABLE, CREATE INDEX, and "
                  + "'ALTER TABLE table_name ADD CONSTRAINT' DDL statements");
        }

        if (statementType == DdlParserTreeConstants.JJTALTER_TABLE_STATEMENT) {
          ASTalter_table_statement alterTableStatement =
              (ASTalter_table_statement) ddlStatement.jjtGetChild(0);
          // child 0 = table name
          // child 1 = alter statement. Only ASTforeign_key is supported
          if (!(alterTableStatement.jjtGetChild(1) instanceof ASTforeign_key)
              && !(alterTableStatement.jjtGetChild(1) instanceof ASTcheck_constraint)) {
            throw new IllegalArgumentException(
                "Unsupported statement:\n"
                    + statement
                    + "\nCan only create diffs from 'CREATE TABLE, CREATE INDEX, and "
                    + "'ALTER TABLE table_name ADD CONSTRAINT' DDL statements");
          }
          // only foreign key statements here:
          if (alterTableStatement.jjtGetChild(1) instanceof ASTforeign_key
              && ((ASTforeign_key) alterTableStatement.jjtGetChild(1))
                  .getName()
                  .equals(ASTforeign_key.ANONYMOUS_NAME)) {
            throw new IllegalArgumentException(
                "Unsupported statement:\n"
                    + statement
                    + "\nCan not create diffs when anonymous FOREIGN KEY constraints are used.");
          }
          if (alterTableStatement.jjtGetChild(1) instanceof ASTcheck_constraint
              && ((ASTcheck_constraint) alterTableStatement.jjtGetChild(1))
                  .getName()
                  .equals(ASTcheck_constraint.ANONYMOUS_NAME)) {
            throw new IllegalArgumentException(
                "Unsupported statement:\n"
                    + statement
                    + "\nCan not create diffs when anonymous CHECK constraints are used.");
          }
        }
        if (statementType == DdlParserTreeConstants.JJTCREATE_TABLE_STATEMENT) {
          if (((ASTcreate_table_statement) ddlStatement.jjtGetChild(0))
              .getConstraints()
              .containsKey(ASTforeign_key.ANONYMOUS_NAME)) {
            throw new IllegalArgumentException(
                "Unsupported statement:\n"
                    + statement
                    + "\nCan not create diffs when anonymous FOREIGN KEY constraints are used.");
          }
        }
        if (statementType == DdlParserTreeConstants.JJTCREATE_TABLE_STATEMENT) {
          if (((ASTcreate_table_statement) ddlStatement.jjtGetChild(0))
              .getConstraints()
              .containsKey(ASTcheck_constraint.ANONYMOUS_NAME)) {
            throw new IllegalArgumentException(
                "Unsupported statement:\n"
                    + statement
                    + "\nCan not create diffs when anonymous CHECK constraints are used.");
          }
        }
        ddlStatements.add(ddlStatement);
      } catch (ParseException e) {
        throw new DdlDiffException(
            String.format("Unable to parse statement:\n%s\n%s", statement, e.getMessage()), e);
      }
    }
    return ddlStatements;
  }

  public static void main(String[] args) {

    CommandLine commandLine;
    try {
      commandLine = new DefaultParser().parse(buildOptions(), args);
      if (commandLine.hasOption(HELP_OPT)) {
        printHelpAndExit(0);
      }
      Map<String, Boolean> options =
          ImmutableMap.of(
              ALLOW_RECREATE_INDEXES_OPT, commandLine.hasOption(ALLOW_RECREATE_INDEXES_OPT),
              ALLOW_DROP_STATEMENTS_OPT, commandLine.hasOption(ALLOW_DROP_STATEMENTS_OPT),
              ALLOW_RECREATE_CONSTRAINTS_OPT,
                  commandLine.hasOption(ALLOW_RECREATE_CONSTRAINTS_OPT));
      List<String> alterStatements =
          DdlDiff.build(
                  new String(
                      Files.readAllBytes(
                          new File(commandLine.getOptionValue(ORIGINAL_DDL_FILE_OPT)).toPath()),
                      UTF_8),
                  new String(
                      Files.readAllBytes(
                          new File(commandLine.getOptionValue(NEW_DDL_FILE_OPT)).toPath()),
                      UTF_8))
              .generateDifferenceStatements(options);

      StringBuilder output = new StringBuilder();
      for (String statement : alterStatements) {
        output.append(statement);
        output.append(";\n\n");
      }

      Files.write(
          new File(commandLine.getOptionValue(OUTPUT_DDL_FILE_OPT)).toPath(),
          output.toString().getBytes(UTF_8));

      System.exit(0);

    } catch (org.apache.commons.cli.ParseException e) {
      System.err.println("Failed parsing command line options: " + e.getMessage());
    } catch (InvalidPathException e) {
      System.err.println("Invalid file path: " + e.getInput() + "\n" + e.getReason());
    } catch (IOException e) {
      System.err.println("Cannot read DDL file: " + e.toString());
    } catch (DdlDiffException e) {
      e.printStackTrace();
    }
    System.err.println();
    printHelpAndExit(1);
  }

  @VisibleForTesting
  static Options buildOptions() {
    Options options = new Options();
    options.addOption(
        Option.builder()
            .longOpt(ORIGINAL_DDL_FILE_OPT)
            .desc("File path to the original DDL definition.")
            .hasArg()
            .argName("FILE")
            .type(File.class)
            .required()
            .build());
    options.addOption(
        Option.builder()
            .longOpt(NEW_DDL_FILE_OPT)
            .desc("File path to the new DDL definition.")
            .hasArg()
            .argName("FILE")
            .type(File.class)
            .required()
            .build());
    options.addOption(
        Option.builder()
            .longOpt(OUTPUT_DDL_FILE_OPT)
            .desc("File path to the output DDL to write.")
            .hasArg()
            .argName("FILE")
            .required()
            .build());
    options.addOption(
        Option.builder()
            .longOpt(ALLOW_RECREATE_INDEXES_OPT)
            .desc("Allows dropping and recreating secondary Indexes to apply changes.")
            .build());
    options.addOption(
        Option.builder()
            .longOpt(ALLOW_RECREATE_CONSTRAINTS_OPT)
            .desc(
                "Allows dropping and recreating Check and Foreign Keys constrainys (and their "
                    + "backing Indexes) to apply changes.")
            .build());
    options.addOption(
        Option.builder()
            .longOpt(ALLOW_DROP_STATEMENTS_OPT)
            .desc(
                "Enables output of DROP commands to delete columns, tables or indexes not used"
                    + " in the new DDL file.")
            .build());
    options.addOption(Option.builder().longOpt(HELP_OPT).desc("Show help").build());
    return options;
  }

  private static void printHelpAndExit(int exitStatus) {
    try (PrintWriter pw = new PrintWriter(System.err)) {
      new HelpFormatter()
          .printHelp(
              pw,
              132,
              "DdlDiff",
              "Compares original and new DDL files and creates a DDL file with DROP, CREATE and"
                  + " ALTER statements which convert the original Schema to the new Schema.\n\n"
                  + "Incompatible table changes (table hierarchy changes. column type changes) are"
                  + " not supported and will cause this tool to fail.\n\n"
                  + "To prevent accidental data loss, DROP statements are not generated for removed"
                  + " tables, columns and indexes. This can be overridden using the"
                  + " --"
                  + ALLOW_DROP_STATEMENTS_OPT
                  + " command line argument.\n\n"
                  + "By default, changes to indexes will also cause a failure. The"
                  + " --"
                  + ALLOW_RECREATE_INDEXES_OPT
                  + " command line option enables index changes by"
                  + " generating statements to drop and recreate the index.\n\n"
                  + "By default, changes to foreign key constraints will also cause a failure. The"
                  + " --"
                  + ALLOW_RECREATE_CONSTRAINTS_OPT
                  + " command line option enables constraint and foreign key changes by"
                  + " generating statements to drop and recreate the constraint.\n\n",
              buildOptions(),
              1,
              4,
              "",
              true);
    }
    System.exit(exitStatus);
  }
}
