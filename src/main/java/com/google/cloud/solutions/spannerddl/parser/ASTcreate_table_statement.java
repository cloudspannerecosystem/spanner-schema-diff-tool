/*
 * Copyright 2019 Google LLC
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

package com.google.cloud.solutions.spannerddl.parser;

import static com.google.cloud.solutions.spannerddl.diff.AstTreeUtils.getOptionalChildByType;

import com.google.cloud.solutions.spannerddl.diff.AstTreeUtils;
import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Abstract Syntax Tree parser object for "create_table_statement" token */
public class ASTcreate_table_statement extends SimpleNode {

  public static final String ANONYMOUS_NAME = "ANONYMOUS_CONSTRAINT_NOT_SUPPORTED";
  private boolean withConstraints = true;

  public ASTcreate_table_statement(int id) {
    super(id);
  }

  public ASTcreate_table_statement(DdlParser p, int id) {
    super(p, id);
  }

  public String getTableName() {
    return AstTreeUtils.tokensToString(AstTreeUtils.getChildByType(children, ASTname.class));
  }

  public Map<String, ASTcolumn_def> getColumns() {
    LinkedHashMap<String, ASTcolumn_def> columns = new LinkedHashMap<>();
    for (Node child : children) {
      if (child instanceof ASTcolumn_def) {
        ASTcolumn_def column = (ASTcolumn_def) child;
        columns.put(column.getColumnName(), column);
      }
    }
    return columns;
  }

  public Map<String, SimpleNode> getConstraints() {
    LinkedHashMap<String, SimpleNode> constraints = new LinkedHashMap<>();
    for (Node child : children) {
      if (child instanceof ASTforeign_key) {
        ASTforeign_key foreignKey = (ASTforeign_key) child;
        constraints.put(foreignKey.getName(), foreignKey);
      }
      if (child instanceof ASTcheck_constraint) {
        ASTcheck_constraint checkConstraint = (ASTcheck_constraint) child;
        constraints.put(checkConstraint.getName(), checkConstraint);
      }
    }
    return constraints;
  }

  public synchronized ASTprimary_key getPrimaryKey() {
    return AstTreeUtils.getChildByType(children, ASTprimary_key.class);
  }

  public synchronized Optional<ASTtable_interleave_clause> getInterleaveClause() {
    return Optional.ofNullable(getOptionalChildByType(children, ASTtable_interleave_clause.class));
  }

  public synchronized Optional<ASTrow_deletion_policy_clause> getRowDeletionPolicyClause() {
    return Optional.ofNullable(
        getOptionalChildByType(children, ASTrow_deletion_policy_clause.class));
  }

  public ASTcreate_table_statement clearConstraints() {
    this.withConstraints = false;
    return this;
  }

  @Override
  public String toString() {
    return toStringOptionalExistClause(true);
  }

  /** Create string version, optionally including the IF NOT EXISTS clause */
  public String toStringOptionalExistClause(boolean includeExists) {
    verifyTableElements();

    List<String> tabledef = new ArrayList<>();
    tabledef.addAll(
        getColumns().values().stream().map(Object::toString).collect(Collectors.toList()));
    if (this.withConstraints) {
      tabledef.addAll(
          getConstraints().values().stream().map(Object::toString).collect(Collectors.toList()));
    }

    return Joiner.on(" ")
        .skipNulls()
        .join(
            "CREATE TABLE",
            (includeExists
                ? Objects.toString(getOptionalChildByType(children, ASTif_not_exists.class), null)
                : null),
            getTableName(),
            // add cols and constraints
            "( " + Joiner.on(", ").skipNulls().join(tabledef) + " )",
            // add table suffixes, separated by ","
            Joiner.on(", ")
                .skipNulls()
                .join(
                    getPrimaryKey(),
                    getOptionalChildByType(children, ASTtable_interleave_clause.class),
                    (withConstraints
                        ? getOptionalChildByType(children, ASTrow_deletion_policy_clause.class)
                        : null)));
  }

  private void verifyTableElements() {
    for (Node child : children) {
      if (!(child instanceof ASTcolumn_def)
          && !(child instanceof ASTforeign_key)
          && !(child instanceof ASTname)
          && !(child instanceof ASTif_not_exists)
          && !(child instanceof ASTcheck_constraint)
          && !(child instanceof ASTprimary_key)
          && !(child instanceof ASTtable_interleave_clause)
          && !(child instanceof ASTrow_deletion_policy_clause)) {
        throw new IllegalArgumentException(
            "Unknown child type " + child.getClass().getSimpleName() + " - " + child);
      }
    }
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof ASTcreate_table_statement) {
      // lazy: compare text rendering, but don't take into account IF NOT EXISTS statements
      return this.toStringOptionalExistClause(false)
          .equals(((ASTcreate_table_statement) other).toStringOptionalExistClause(false));
    }
    return false;
  }

  @Override
  public int hashCode() {
    return toStringOptionalExistClause(false).hashCode();
  }
}
