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

import com.google.cloud.solutions.spannerddl.diff.ASTTreeUtils;
import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Abstract Syntax Tree parser object for "create_table_statement" token
 */
public class ASTcreate_table_statement extends SimpleNode {

  public ASTcreate_table_statement(int id) {
    super(id);
  }

  public ASTcreate_table_statement(DdlParser p, int id) {
    super(p, id);
  }

  public String getTableName() {
    return children[0].toString();
  }

  public TreeMap<String, ASTcolumn_def> getColumns() {
    TreeMap<String, ASTcolumn_def> columns = new TreeMap<>();
    for (Node child : children) {
      if (child instanceof ASTcolumn_def) {
        ASTcolumn_def column = (ASTcolumn_def) child;
        columns.put(column.getColumnName(), column);
      }
    }
    return columns;
  }

  public TreeMap<String, ASTforeign_key> getForeignKeys() {
    TreeMap<String, ASTforeign_key> foreignKeys = new TreeMap<>();
    for (Node child : children) {
      if (child instanceof ASTforeign_key) {
        ASTforeign_key foreignKey = (ASTforeign_key) child;
        foreignKeys.put(foreignKey.getName(), foreignKey);
      }
    }
    return foreignKeys;
  }


  public synchronized ASTprimary_key getPrimaryKey() {
    return ASTTreeUtils.getChildByType(children, ASTprimary_key.class);
  }

  public synchronized Optional<ASTtable_interleave_clause> getInterleaveClause() {
    try {
      return Optional.of(ASTTreeUtils.getChildByType(children, ASTtable_interleave_clause.class));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  @Override
  public String toString() {
    StringBuilder ret = new StringBuilder("CREATE TABLE");
    ret.append(" ");
    // child 0 is name
    ret.append(getTableName());
    ret.append(" (");
    // append column and FK definitions.
    List<SimpleNode> tableElements = new ArrayList<>();
    tableElements.addAll(getColumns().values());
    tableElements.addAll(getForeignKeys().values());
    ret.append(Joiner.on(", ").join(tableElements));
    ret.append(") ");
    ret.append(getPrimaryKey());

    Optional<ASTtable_interleave_clause> interleaveClause = getInterleaveClause();
    if (interleaveClause.isPresent()) {
      ret.append(", ");
      ret.append(interleaveClause.get()); // interleave optional
    }
    return ret.toString();
  }

  public static Comparator<ASTcreate_table_statement> COMPARE_BY_NAME =
      Comparator.comparing(ASTcreate_table_statement::getTableName);

  @Override
  public boolean equals(Object other) {
    if (other instanceof ASTcreate_table_statement) {
      // lazy: compare text rendering.
      return this.toString().equals(other.toString());
    }
    return false;
  }
}
