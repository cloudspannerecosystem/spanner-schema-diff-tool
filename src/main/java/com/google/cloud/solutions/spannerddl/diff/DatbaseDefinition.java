/*
 * Copyright 2024 Google LLC
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

import com.google.auto.value.AutoValue;
import com.google.cloud.solutions.spannerddl.parser.ASTadd_row_deletion_policy;
import com.google.cloud.solutions.spannerddl.parser.ASTalter_database_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTalter_table_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTcheck_constraint;
import com.google.cloud.solutions.spannerddl.parser.ASTcreate_change_stream_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTcreate_index_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTcreate_search_index_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTcreate_table_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTddl_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTforeign_key;
import com.google.cloud.solutions.spannerddl.parser.ASTrow_deletion_policy_clause;
import com.google.cloud.solutions.spannerddl.parser.DdlParserTreeConstants;
import com.google.cloud.solutions.spannerddl.parser.SimpleNode;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/**
 * Separarates the different DDL creation statements into separate maps.
 *
 * <p>Constraints which were created inline with their table are separated into a map with any other
 * ALTER statements which adds constraints.
 *
 * <p>This allows the diff tool to handle these objects which are created inline with the table in
 * the same way as if they were created separately with ALTER statements.
 */
@AutoValue
abstract class DatbaseDefinition {
  static DatbaseDefinition create(List<ASTddl_statement> statements) {
    // Use LinkedHashMap to preserve creation order in original DDL.
    LinkedHashMap<String, ASTcreate_table_statement> tablesInCreationOrder = new LinkedHashMap<>();
    LinkedHashMap<String, ASTcreate_index_statement> indexes = new LinkedHashMap<>();
    LinkedHashMap<String, ASTcreate_search_index_statement> searchIndexes = new LinkedHashMap<>();
    LinkedHashMap<String, ConstraintWrapper> constraints = new LinkedHashMap<>();
    LinkedHashMap<String, ASTrow_deletion_policy_clause> ttls = new LinkedHashMap<>();
    LinkedHashMap<String, ASTcreate_change_stream_statement> changeStreams = new LinkedHashMap<>();
    LinkedHashMap<String, String> alterDatabaseOptions = new LinkedHashMap<>();

    for (ASTddl_statement ddlStatement : statements) {
      final SimpleNode statement = (SimpleNode) ddlStatement.jjtGetChild(0);

      switch (statement.getId()) {
        case DdlParserTreeConstants.JJTCREATE_TABLE_STATEMENT:
          ASTcreate_table_statement createTable = (ASTcreate_table_statement) statement;
          // Remove embedded constraint statements from the CreateTable node
          // as they are taken into account via `constraints`
          tablesInCreationOrder.put(createTable.getTableName(), createTable.clearConstraints());

          // convert embedded constraint statements into wrapper object with table name
          // use a single map for all foreign keys, constraints and row deletion polcies whether
          // created in table or externally
          createTable.getConstraints().values().stream()
              .map(c -> ConstraintWrapper.create(createTable.getTableName(), c))
              .forEach(c -> constraints.put(c.getName(), c));

          // Move embedded Row Deletion Policies
          final Optional<ASTrow_deletion_policy_clause> rowDeletionPolicyClause =
              createTable.getRowDeletionPolicyClause();
          rowDeletionPolicyClause.ifPresent(rdp -> ttls.put(createTable.getTableName(), rdp));
          break;
        case DdlParserTreeConstants.JJTCREATE_SEARCH_INDEX_STATEMENT:
          searchIndexes.put(
              ((ASTcreate_search_index_statement) statement).getName(),
              (ASTcreate_search_index_statement) statement);
          break;
        case DdlParserTreeConstants.JJTCREATE_INDEX_STATEMENT:
          indexes.put(
              ((ASTcreate_index_statement) statement).getIndexName(),
              (ASTcreate_index_statement) statement);
          break;
        case DdlParserTreeConstants.JJTALTER_TABLE_STATEMENT:
          // Alter table can be adding Index, Constraint or Row Deletion Policy
          ASTalter_table_statement alterTable = (ASTalter_table_statement) statement;
          final String tableName = alterTable.jjtGetChild(0).toString();

          if (alterTable.jjtGetChild(1) instanceof ASTforeign_key
              || alterTable.jjtGetChild(1) instanceof ASTcheck_constraint) {
            ConstraintWrapper constraint =
                ConstraintWrapper.create(tableName, (SimpleNode) alterTable.jjtGetChild(1));
            constraints.put(constraint.getName(), constraint);

          } else if (statement.jjtGetChild(1) instanceof ASTadd_row_deletion_policy) {
            ttls.put(
                tableName,
                (ASTrow_deletion_policy_clause) alterTable.jjtGetChild(1).jjtGetChild(0));
          } else {
            // other ALTER statements are not supported.
            throw new IllegalArgumentException(
                "Unsupported ALTER TABLE statement: " + AstTreeUtils.tokensToString(ddlStatement));
          }
          break;
        case DdlParserTreeConstants.JJTALTER_DATABASE_STATEMENT:
          alterDatabaseOptions.putAll(
              ((ASTalter_database_statement) statement).getOptionsClause().getKeyValueMap());
          break;
        case DdlParserTreeConstants.JJTCREATE_CHANGE_STREAM_STATEMENT:
          changeStreams.put(
              ((ASTcreate_change_stream_statement) statement).getName(),
              (ASTcreate_change_stream_statement) statement);
          break;
        default:
          throw new IllegalArgumentException(
              "Unsupported statement: " + AstTreeUtils.tokensToString(ddlStatement));
      }
    }
    return new AutoValue_DatbaseDefinition(
        ImmutableMap.copyOf(tablesInCreationOrder),
        ImmutableMap.copyOf(searchIndexes),
        ImmutableMap.copyOf(indexes),
        ImmutableMap.copyOf(constraints),
        ImmutableMap.copyOf(ttls),
        ImmutableMap.copyOf(changeStreams),
        ImmutableMap.copyOf(alterDatabaseOptions));
  }

  abstract ImmutableMap<String, ASTcreate_table_statement> tablesInCreationOrder();

  abstract ImmutableMap<String, ASTcreate_search_index_statement> searchIndexes();

  abstract ImmutableMap<String, ASTcreate_index_statement> indexes();

  abstract ImmutableMap<String, ConstraintWrapper> constraints();

  abstract ImmutableMap<String, ASTrow_deletion_policy_clause> ttls();

  abstract ImmutableMap<String, ASTcreate_change_stream_statement> changeStreams();

  abstract ImmutableMap<String, String> alterDatabaseOptions();
}
