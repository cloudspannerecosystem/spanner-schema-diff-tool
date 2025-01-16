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

import com.google.cloud.solutions.spannerddl.parser.ASTadd_row_deletion_policy;
import com.google.cloud.solutions.spannerddl.parser.ASTalter_database_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTalter_table_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTcheck_constraint;
import com.google.cloud.solutions.spannerddl.parser.ASTcolumn_def;
import com.google.cloud.solutions.spannerddl.parser.ASTcolumn_default_clause;
import com.google.cloud.solutions.spannerddl.parser.ASTcolumn_type;
import com.google.cloud.solutions.spannerddl.parser.ASTcreate_change_stream_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTcreate_index_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTcreate_or_replace_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTcreate_schema_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTcreate_search_index_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTcreate_table_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTddl_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTforeign_key;
import com.google.cloud.solutions.spannerddl.parser.ASToptions_clause;
import com.google.cloud.solutions.spannerddl.parser.ASTrow_deletion_policy_clause;
import com.google.cloud.solutions.spannerddl.parser.DdlParser;
import com.google.cloud.solutions.spannerddl.parser.DdlParserTreeConstants;
import com.google.cloud.solutions.spannerddl.parser.ParseException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MapDifference;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
 * <p>or execute the {@link #main(String[]) main()} function with the {@link
 * DdlDiffOptions#buildOptions() appropriate command line options}.
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

  private final DatabaseDefinition originalDb;
  private final DatabaseDefinition newDb;
  private final MapDifference<String, ASTcreate_index_statement> indexDifferences;
  private final MapDifference<String, ASTcreate_table_statement> tableDifferences;
  private final MapDifference<String, ConstraintWrapper> constraintDifferences;
  private final MapDifference<String, ASTrow_deletion_policy_clause> ttlDifferences;
  private final MapDifference<String, String> alterDatabaseOptionsDifferences;
  private final MapDifference<String, ASTcreate_change_stream_statement> changeStreamDifferences;
  private final MapDifference<String, ASTcreate_search_index_statement> searchIndexDifferences;
  private final String databaseName; // for alter Database
  private final MapDifference<String, ASTcreate_schema_statement> schemaDifferences;

  private DdlDiff(DatabaseDefinition originalDb, DatabaseDefinition newDb, String databaseName)
      throws DdlDiffException {
    this.originalDb = originalDb;
    this.newDb = newDb;
    this.databaseName = databaseName;

    this.tableDifferences =
        Maps.difference(originalDb.tablesInCreationOrder(), newDb.tablesInCreationOrder());
    this.indexDifferences = Maps.difference(originalDb.indexes(), newDb.indexes());
    this.constraintDifferences = Maps.difference(originalDb.constraints(), newDb.constraints());
    this.ttlDifferences = Maps.difference(originalDb.ttls(), newDb.ttls());
    this.alterDatabaseOptionsDifferences =
        Maps.difference(originalDb.alterDatabaseOptions(), newDb.alterDatabaseOptions());
    this.changeStreamDifferences =
        Maps.difference(originalDb.changeStreams(), newDb.changeStreams());
    this.searchIndexDifferences =
        Maps.difference(originalDb.searchIndexes(), newDb.searchIndexes());
    this.schemaDifferences = Maps.difference(originalDb.schemas(), newDb.schemas());

    if (!alterDatabaseOptionsDifferences.areEqual() && Strings.isNullOrEmpty(databaseName)) {
      // should never happen, but...
      throw new DdlDiffException("No database ID defined - required for Alter Database statements");
    }
  }

  /** Generate statements to convert the original to the new DB DDL. */
  public List<String> generateDifferenceStatements(Map<String, Boolean> options)
      throws DdlDiffException {
    ImmutableList.Builder<String> output = ImmutableList.builder();

    if (!indexDifferences.entriesDiffering().isEmpty()
        && !options.get(ALLOW_RECREATE_INDEXES_OPT)) {

      long numChangedIndexes =
          indexDifferences.entriesDiffering().values().stream()
              // Check if the indexes only difference is the STORING clause
              .map((diff) -> DdlDiff.checkIndexDiffOnlyStoring(diff))
              // Filter out those who have only changed Storing clauses
              .filter((v) -> !v)
              .count();

      if (numChangedIndexes > 0) {
        throw new DdlDiffException(
            "At least one Index differs, and "
                + ALLOW_RECREATE_INDEXES_OPT
                + " is not set.\n"
                + "Indexes: "
                + Joiner.on(", ").join(indexDifferences.entriesDiffering().keySet()));
      }
    }

    if (!constraintDifferences.entriesDiffering().isEmpty()
        && !options.get(ALLOW_RECREATE_CONSTRAINTS_OPT)) {
      throw new DdlDiffException(
          "At least one constraint differs, and "
              + ALLOW_RECREATE_CONSTRAINTS_OPT
              + " is not set.\n"
              + Joiner.on(", ").join(constraintDifferences.entriesDiffering().keySet()));
    }

    if (!schemaDifferences.entriesDiffering().isEmpty()) {
      throw new DdlDiffException(
          "At least one schema differs but ALTER SCHEMA is not supported"
              + Joiner.on(", ").join(schemaDifferences.entriesDiffering().keySet()));
    }

    // check for modified Alter Database statements
    if (!alterDatabaseOptionsDifferences.areEqual()) {
      String optionsUpdates = generateOptionsUpdates(alterDatabaseOptionsDifferences);
      if (!Strings.isNullOrEmpty(optionsUpdates)) {
        LOG.info("Updating database options");
        output.add("ALTER DATABASE " + databaseName + " SET OPTIONS (" + optionsUpdates + ")");
      }
    }

    // Drop deleted indexes.
    if (options.get(ALLOW_DROP_STATEMENTS_OPT)) {
      // Drop deleted indexes.
      for (String indexName : indexDifferences.entriesOnlyOnLeft().keySet()) {
        LOG.info("Dropping deleted index: {}", indexName);
        output.add("DROP INDEX " + indexName);
      }
    }

    // Drop deleted change streams.
    if (options.get(ALLOW_DROP_STATEMENTS_OPT)) {
      // Drop deleted indexes.
      for (String changeStreamName : changeStreamDifferences.entriesOnlyOnLeft().keySet()) {
        LOG.info("Dropping deleted change stream: {}", changeStreamName);
        output.add("DROP CHANGE STREAM " + changeStreamName);
      }
    }

    // drop deleted search indexes.
    if (options.get(ALLOW_DROP_STATEMENTS_OPT)) {
      for (String searchIndexName : searchIndexDifferences.entriesOnlyOnLeft().keySet()) {
        LOG.info("Dropping deleted search index: {}", searchIndexName);
        output.add("DROP SEARCH INDEX " + searchIndexName);
      }
    }

    // Drop modified indexes that need to be re-created...
    for (ValueDifference<ASTcreate_index_statement> difference :
        indexDifferences.entriesDiffering().values()) {
      if (!checkIndexDiffOnlyStoring(difference)) {
        LOG.info(
            "Dropping changed index for re-creation: {}", difference.leftValue().getIndexName());
        output.add("DROP INDEX " + difference.leftValue().getIndexName());
      }
    }

    // Drop deleted constraints
    for (ConstraintWrapper fk : constraintDifferences.entriesOnlyOnLeft().values()) {
      LOG.info("Dropping constraint: {}", fk.getName());
      output.add("ALTER TABLE " + fk.tableName() + " DROP CONSTRAINT " + fk.getName());
    }

    // Drop modified constraints that need to be re-created...
    for (ValueDifference<ConstraintWrapper> fkDiff :
        constraintDifferences.entriesDiffering().values()) {
      LOG.info("Dropping changed constraint for re-creation: {}", fkDiff.leftValue().getName());
      output.add(
          "ALTER TABLE "
              + fkDiff.leftValue().tableName()
              + " DROP CONSTRAINT "
              + fkDiff.leftValue().getName());
    }

    // Drop deleted TTLs
    for (String tableName : ttlDifferences.entriesOnlyOnLeft().keySet()) {
      LOG.info("Dropping row deletion policy for : {}", tableName);
      output.add("ALTER TABLE " + tableName + " DROP ROW DELETION POLICY");
    }

    // For each changed search index, apply the drop column statements
    SchemaUpdateStatements searchIndexUpdateStatements =
        ASTcreate_search_index_statement.generateAlterStatementsFor(
            searchIndexDifferences.entriesDiffering(), options.get(ALLOW_DROP_STATEMENTS_OPT));
    output.addAll(searchIndexUpdateStatements.dropStatements());

    if (options.get(ALLOW_DROP_STATEMENTS_OPT)) {
      // Drop tables that have been deleted -- need to do it in reverse creation order.
      List<String> reverseOrderedTableNames =
          new ArrayList<>(originalDb.tablesInCreationOrder().keySet());
      Collections.reverse(reverseOrderedTableNames);
      for (String tableName : reverseOrderedTableNames) {
        if (tableDifferences.entriesOnlyOnLeft().containsKey(tableName)) {
          LOG.info("Dropping deleted table: {}", tableName);
          output.add("DROP TABLE " + tableName);
        }
      }
    }

    // Drop schemas
    if (options.get(ALLOW_DROP_STATEMENTS_OPT)) {
      for (ASTcreate_schema_statement schema : schemaDifferences.entriesOnlyOnLeft().values()) {
        LOG.info("Dropping schema: {}", schema.getName());
        output.add("DROP SCHEMA " + schema.getName());
      }
    }

    // Alter existing tables, or error if not possible.
    for (ValueDifference<ASTcreate_table_statement> difference :
        tableDifferences.entriesDiffering().values()) {
      LOG.info("Altering modified table: {}", difference.leftValue().getTableName());
      output.addAll(
          generateAlterTableStatements(difference.leftValue(), difference.rightValue(), options));
    }

    // create schemas
    for (ASTcreate_schema_statement schema : schemaDifferences.entriesOnlyOnRight().values()) {
      LOG.info("creating schema: {}", schema.getName());
      output.add(schema.toString());
    }

    // Create new tables. Must be done in the order of creation in the new DDL.
    for (Map.Entry<String, ASTcreate_table_statement> newTableEntry :
        newDb.tablesInCreationOrder().entrySet()) {
      if (tableDifferences.entriesOnlyOnRight().containsKey(newTableEntry.getKey())) {
        LOG.info("Creating new table: {}", newTableEntry.getKey());
        output.add(newTableEntry.getValue().toStringOptionalExistClause(false));
      }
    }

    // Create new TTLs
    for (Map.Entry<String, ASTrow_deletion_policy_clause> newTtl :
        ttlDifferences.entriesOnlyOnRight().entrySet()) {
      LOG.info("Adding new row deletion policy for : {}", newTtl.getKey());
      output.add("ALTER TABLE " + newTtl.getKey() + " ADD " + newTtl.getValue());
    }

    // update existing TTLs
    for (Entry<String, ValueDifference<ASTrow_deletion_policy_clause>> differentTtl :
        ttlDifferences.entriesDiffering().entrySet()) {
      LOG.info("Updating row deletion policy for : {}", differentTtl.getKey());
      output.add(
          "ALTER TABLE "
              + differentTtl.getKey()
              + " REPLACE "
              + differentTtl.getValue().rightValue());
    }

    // Create new indexes
    for (ASTcreate_index_statement index : indexDifferences.entriesOnlyOnRight().values()) {
      LOG.info("Creating new index: {}", index.getIndexName());
      output.add(index.toStringOptionalExistClause(false));
    }

    // Re-create modified indexes...
    for (ValueDifference<ASTcreate_index_statement> difference :
        indexDifferences.entriesDiffering().values()) {

      if (checkIndexDiffOnlyStoring(difference)) {
        LOG.info("Updating STORING clause on index: {}", difference.leftValue().getIndexName());
        Map<String, String> originalStoredCols =
            difference.leftValue().getStoredColumnNames().stream()
                .collect(Collectors.toMap(Function.identity(), Function.identity()));
        Map<String, String> newStoredCols =
            difference.rightValue().getStoredColumnNames().stream()
                .collect(Collectors.toMap(Function.identity(), Function.identity()));

        MapDifference<String, String> colDiff = Maps.difference(originalStoredCols, newStoredCols);

        for (String deletedCol : colDiff.entriesOnlyOnLeft().values()) {
          output.add(
              "ALTER INDEX "
                  + difference.leftValue().getIndexName()
                  + " DROP STORED COLUMN "
                  + deletedCol);
        }
        for (String deletedCol : colDiff.entriesOnlyOnRight().values()) {
          output.add(
              "ALTER INDEX "
                  + difference.leftValue().getIndexName()
                  + " ADD STORED COLUMN "
                  + deletedCol);
        }
      } else {
        LOG.info("Re-creating changed index: {}", difference.leftValue().getIndexName());
        output.add(difference.rightValue().toStringOptionalExistClause(false));
      }
    }

    // Create new constraints.
    for (ConstraintWrapper fk : constraintDifferences.entriesOnlyOnRight().values()) {
      LOG.info("Creating new constraint: {}", fk.getName());
      output.add("ALTER TABLE " + fk.tableName() + " ADD " + fk.constraint());
    }

    // Re-create modified constraints.
    for (ValueDifference<ConstraintWrapper> constraintDiff :
        constraintDifferences.entriesDiffering().values()) {
      LOG.info("Re-creating changed constraint: {}", constraintDiff.rightValue().getName());
      output.add(
          "ALTER TABLE "
              + constraintDiff.rightValue().tableName()
              + " ADD "
              + constraintDiff.rightValue().constraint().toString());
    }

    // Create new change streams
    for (ASTcreate_change_stream_statement newChangeStream :
        changeStreamDifferences.entriesOnlyOnRight().values()) {
      LOG.info("Creating new change stream: {}", newChangeStream.getName());
      output.add(newChangeStream.toString());
    }

    // Alter existing change streams
    for (ValueDifference<ASTcreate_change_stream_statement> changedChangeStream :
        changeStreamDifferences.entriesDiffering().values()) {
      LOG.info("Updating change stream: {}", changedChangeStream.rightValue().getName());
      String oldForClause = changedChangeStream.leftValue().getForClause().toString();
      String newForClause = changedChangeStream.rightValue().getForClause().toString();

      String oldOptions = Objects.toString(changedChangeStream.leftValue().getOptionsClause(), "");
      String newOptions = Objects.toString(changedChangeStream.rightValue().getOptionsClause(), "");

      if (!oldForClause.equals(newForClause)) {
        output.add(
            "ALTER CHANGE STREAM "
                + changedChangeStream.rightValue().getName()
                + " SET "
                + newForClause);
      }
      if (!oldOptions.equals(newOptions)) {

        // need to look at old and new options values individually

        Map<String, String> oldOptionsKv =
            changedChangeStream.leftValue().getOptionsClause() == null
                ? Map.of()
                : changedChangeStream.leftValue().getOptionsClause().getKeyValueMap();

        Map<String, String> newOptionsKv =
            changedChangeStream.rightValue().getOptionsClause() == null
                ? Map.of()
                : changedChangeStream.rightValue().getOptionsClause().getKeyValueMap();

        String optionsDiff = generateOptionsUpdates(Maps.difference(oldOptionsKv, newOptionsKv));

        if (optionsDiff != null) {
          output.add(
              "ALTER CHANGE STREAM "
                  + changedChangeStream.rightValue().getName()
                  + " SET OPTIONS ("
                  + optionsDiff
                  + ")");
        }
      }
    }

    for (ASTcreate_search_index_statement searchIndex :
        searchIndexDifferences.entriesOnlyOnRight().values()) {
      LOG.info("Creating new search index: {}", searchIndex.getName());
      output.add(searchIndex.toString());
    }

    // For each changed search index, apply the add column statements
    output.addAll(searchIndexUpdateStatements.createStatements());

    // Add all new search indexes

    return output.build();
  }

  /** Verify that different indexes are only different in STORING clause. */
  private static boolean checkIndexDiffOnlyStoring(
      ValueDifference<ASTcreate_index_statement> indexDifference) {

    return indexDifference
        .leftValue()
        .getDefinitionWithoutStoring()
        .equals(indexDifference.rightValue().getDefinitionWithoutStoring());
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
        && !left.getInterleaveClause()
            .get()
            .getParentTableName()
            .equals(right.getInterleaveClause().get().getParentTableName())) {
      throw new DdlDiffException(
          "Cannot change interleaved parent of table " + left.getTableName());
    }

    // Check Key is same
    if (!left.getPrimaryKey().toString().equals(right.getPrimaryKey().toString())) {
      throw new DdlDiffException("Cannot change primary key of table " + left.getTableName());
    }

    // On delete changed
    if (left.getInterleaveClause().isPresent()
        && !left.getInterleaveClause()
            .get()
            .getOnDelete()
            .equals(right.getInterleaveClause().get().getOnDelete())) {
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
      String tableName, List<String> alterStatements, ValueDifference<ASTcolumn_def> columnDiff)
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
    ASToptions_clause rightOptionsClause = columnDiff.rightValue().getOptionsClause();
    Map<String, String> leftOptions =
        leftOptionsClause == null ? Collections.emptyMap() : leftOptionsClause.getKeyValueMap();
    Map<String, String> rightOptions =
        rightOptionsClause == null ? Collections.emptyMap() : rightOptionsClause.getKeyValueMap();
    MapDifference<String, String> optionsDiff = Maps.difference(leftOptions, rightOptions);

    String updateText = generateOptionsUpdates(optionsDiff);

    if (!Strings.isNullOrEmpty(updateText)) {
      alterStatements.add(
          "ALTER TABLE "
              + tableName
              + " ALTER COLUMN "
              + columnDiff.rightValue().getColumnName()
              + " SET OPTIONS ("
              + updateText
              + ")");
    }

    // Update default values

    final ASTcolumn_default_clause oldDefaultValue =
        columnDiff.leftValue().getColumnDefaultClause();
    final ASTcolumn_default_clause newDefaultValue =
        columnDiff.rightValue().getColumnDefaultClause();
    if (!Objects.equals(oldDefaultValue, newDefaultValue)) {
      if (newDefaultValue == null) {
        alterStatements.add(
            "ALTER TABLE "
                + tableName
                + " ALTER COLUMN "
                + columnDiff.rightValue().getColumnName()
                + " DROP DEFAULT");
      } else {
        // add or change default value
        alterStatements.add(
            "ALTER TABLE "
                + tableName
                + " ALTER COLUMN "
                + columnDiff.rightValue().getColumnName()
                + " SET "
                + newDefaultValue);
      }
    }
  }

  private static String generateOptionsUpdates(MapDifference<String, String> optionsDiff) {

    if (optionsDiff.areEqual()) {
      return null;
    } else {
      TreeMap<String, String> optionsToUpdate = new TreeMap<>();

      // remove options only in left by setting value to null
      optionsDiff.entriesOnlyOnLeft().keySet().forEach(k -> optionsToUpdate.put(k, "NULL"));
      // add all modified to update
      optionsDiff
          .entriesDiffering()
          .forEach((key, value) -> optionsToUpdate.put(key, value.rightValue()));
      // add all new
      optionsToUpdate.putAll(optionsDiff.entriesOnlyOnRight());
      return Joiner.on(",")
          .join(
              optionsToUpdate.entrySet().stream()
                  .map(e -> e.getKey() + "=" + e.getValue())
                  .iterator());
    }
  }

  /**
   * Build a DdlDiff instance that can compares two Cloud Spanner Schema (DDL) strings.
   * generateDifferenceStatements can be invoked to generate the ALTER statements
   *
   * @param originalDdl Original DDL
   * @param newDdl New DDL
   * @return DdlDiff instance
   * @throws DdlDiffException if there is an error in paring the DDL
   */
  public static DdlDiff build(String originalDdl, String newDdl) throws DdlDiffException {
    List<ASTddl_statement> originalStatements;
    List<ASTddl_statement> newStatements;
    try {
      originalStatements = parseDdl(Strings.nullToEmpty(originalDdl));
    } catch (DdlDiffException e) {
      throw new DdlDiffException("Failed parsing ORIGINAL DDL: " + e.getMessage(), e);
    }
    try {
      newStatements = parseDdl(Strings.nullToEmpty(newDdl));
    } catch (DdlDiffException e) {
      throw new DdlDiffException("Failed parsing NEW DDL: " + e.getMessage(), e);
    }

    DatabaseDefinition originalDb = DatabaseDefinition.create(originalStatements);
    DatabaseDefinition newDb = DatabaseDefinition.create(newStatements);

    return new DdlDiff(
        originalDb, newDb, getDatabaseNameFromAlterDatabase(originalStatements, newStatements));
  }

  private static String getDatabaseNameFromAlterDatabase(
      List<ASTddl_statement> originalStatements, List<ASTddl_statement> newStatements)
      throws DdlDiffException {
    String originalName = getDatabaseNameFromAlterDatabase(originalStatements);
    String newName = getDatabaseNameFromAlterDatabase(newStatements);

    if (originalName == null) {
      return newName;
    }
    if (newName == null) {
      return originalName;
    }
    if (!originalName.equals(newName)) {
      throw new DdlDiffException(
          "Database IDs differ in old and new DDL ALTER DATABASE statements");
    }
    return newName;
  }

  private static String getDatabaseNameFromAlterDatabase(List<ASTddl_statement> statements)
      throws DdlDiffException {
    Set<String> names =
        statements.stream()
            .filter(s -> s.jjtGetChild(0) instanceof ASTalter_database_statement)
            .map(s -> ((ASTalter_database_statement) s.jjtGetChild(0)).getDbName())
            .collect(Collectors.toSet());
    if (names.size() > 1) {
      throw new DdlDiffException(
          "Multiple database IDs defined in ALTER DATABASE statements in DDL");
    } else if (names.size() == 0) {
      return null;
    } else {
      return names.iterator().next();
    }
  }

  /**
   * Parses the Cloud Spanner Schema (DDL) string to a list of AST DDL statements.
   *
   * @param original DDL to parse
   * @return List of parsed DDL statements
   * @throws DdlDiffException if there is an error in parsing the DDL
   */
  public static List<ASTddl_statement> parseDdl(String original) throws DdlDiffException {
    return parseDdl(original, false);
  }

  /**
   * Parses the Cloud Spanner Schema (DDL) string to a list of AST DDL statements.
   *
   * @param original DDL to parse
   * @param parseAnnotationInComments If true then the annotations that appear as comments
   *     "-- @ANNOTATION annotation" will be parsed
   * @return List of parsed DDL statements
   */
  public static List<ASTddl_statement> parseDdl(String original, boolean parseAnnotationInComments)
      throws DdlDiffException {
    // the annotations are prefixed with "--" so that SQL file remains valid.
    // strip the comment prefix before so that annotations can be parsed.
    // otherwise they will be ignored as comment lines
    if (parseAnnotationInComments) {
      original =
          Pattern.compile("^\\s*--\\s+@", Pattern.MULTILINE).matcher(original).replaceAll("@");
    }

    // Remove "--" comments and split by ";"
    List<String> statements = Splitter.on(';').splitToList(original.replaceAll("--.*(\n|$)", ""));
    ArrayList<ASTddl_statement> ddlStatements = new ArrayList<>(statements.size());

    for (String statement : statements) {
      statement = statement.trim();
      if (statement.isEmpty()) {
        continue;
      }
      try {
        ASTddl_statement ddlStatement = DdlParser.parseDdlStatement(statement);
        int statementType = ddlStatement.jjtGetChild(0).getId();

        switch (statementType) {
          case DdlParserTreeConstants.JJTALTER_TABLE_STATEMENT:
            ASTalter_table_statement alterTableStatement =
                (ASTalter_table_statement) ddlStatement.jjtGetChild(0);
            // child 0 = table name
            // child 1 = alter statement. Only ASTforeign_key is supported
            if (!(alterTableStatement.jjtGetChild(1) instanceof ASTforeign_key)
                && !(alterTableStatement.jjtGetChild(1) instanceof ASTcheck_constraint)
                && !(alterTableStatement.jjtGetChild(1) instanceof ASTadd_row_deletion_policy)) {
              throw new IllegalArgumentException(
                  "Unsupported statement:\n"
                      + statement
                      + "\n"
                      + "ALTER TABLE statements only support 'ADD [constraint|row deletion"
                      + " policy]'");
            }
            if (alterTableStatement.jjtGetChild(1) instanceof ASTforeign_key
                && ((ASTforeign_key) alterTableStatement.jjtGetChild(1))
                    .getName()
                    .equals(ASTcreate_table_statement.ANONYMOUS_NAME)) {
              throw new IllegalArgumentException(
                  "Unsupported statement:\n"
                      + statement
                      + "\nCan not create diffs when anonymous constraints are used.");
            }
            if (alterTableStatement.jjtGetChild(1) instanceof ASTcheck_constraint
                && ((ASTcheck_constraint) alterTableStatement.jjtGetChild(1))
                    .getName()
                    .equals(ASTcreate_table_statement.ANONYMOUS_NAME)) {
              throw new IllegalArgumentException(
                  "Unsupported statement:\n"
                      + statement
                      + "\nCan not create diffs when anonymous constraints are used.");
            }
            break;
          case DdlParserTreeConstants.JJTCREATE_TABLE_STATEMENT:
            if (((ASTcreate_table_statement) ddlStatement.jjtGetChild(0))
                .getConstraints()
                .containsKey(ASTcreate_table_statement.ANONYMOUS_NAME)) {
              throw new IllegalArgumentException(
                  "Unsupported statement:\n"
                      + statement
                      + "\nCan not create diffs when anonymous constraints are used.");
            }
            break;
          case DdlParserTreeConstants.JJTCREATE_INDEX_STATEMENT:
          case DdlParserTreeConstants.JJTALTER_DATABASE_STATEMENT:
          case DdlParserTreeConstants.JJTCREATE_CHANGE_STREAM_STATEMENT:
          case DdlParserTreeConstants.JJTCREATE_SEARCH_INDEX_STATEMENT:
            // no-op - allowed
            break;
          case DdlParserTreeConstants.JJTCREATE_OR_REPLACE_STATEMENT:
            // can be one of several types.
            switch (((ASTcreate_or_replace_statement) ddlStatement.jjtGetChild(0))
                .getSchemaObject()
                .getId()) {
              case DdlParserTreeConstants.JJTCREATE_SCHEMA_STATEMENT:
                // no-op - allowed
                break;
              default:
                throw new IllegalArgumentException(
                    "Unsupported statement for creating diffs:\n" + statement);
            }
            break;
          default:
            throw new IllegalArgumentException(
                "Unsupported statement for creating diffs:\n" + statement);
        }
        ddlStatements.add(ddlStatement);
      } catch (ParseException e) {
        throw new DdlDiffException(
            String.format(
                "Unable to parse statement:\n'%s'\nFailure: %s", statement, e.getMessage()),
            e);
      }
    }
    return ddlStatements;
  }

  /**
   * Main entrypoint for this tool.
   *
   * @see DdlDiffOptions for command line options.
   */
  public static void main(String[] args) {
    DdlDiffOptions options = DdlDiffOptions.parseCommandLine(args);

    try {
      DdlDiff ddlDiff =
          DdlDiff.build(
              new String(Files.readAllBytes(options.originalDdlPath()), UTF_8),
              new String(Files.readAllBytes(options.newDdlPath()), UTF_8));

      List<String> alterStatements = ddlDiff.generateDifferenceStatements(options.args());

      StringBuilder output = new StringBuilder();
      for (String statement : alterStatements) {
        output.append(statement);
        output.append(";\n\n");
      }

      Files.write(options.outputDdlPath(), output.toString().getBytes(UTF_8));

      System.exit(0);
    } catch (IOException e) {
      System.err.println("Cannot read DDL file: " + e);
      System.exit(1);
    } catch (DdlDiffException e) {
      System.err.println("Failed to generate a diff: " + e.getMessage());
      System.exit(1);
    }
  }
}
