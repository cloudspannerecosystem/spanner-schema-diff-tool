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

/** Abstract Syntax Tree parser object for "column_def" token */
import java.util.Comparator;
import javax.annotation.Nullable;

public class ASTcolumn_def extends SimpleNode {

  public ASTcolumn_def(int id) {
    super(id);
  }

  public ASTcolumn_def(DdlParser p, int id) {
    super(p, id);
  }

  public String getColumnName() {
    return children[0].toString();
  }

  public String getColumnTypeString() {
    return getColumnType().toString().toUpperCase();
  }

  public ASTcolumn_type getColumnType() {
    return (ASTcolumn_type) children[1];
  }

  public boolean isNotNull() {
    return (children.length > 2 && children[2] instanceof ASTnot_null);
  }

  public @Nullable ASToptions_clause getOptionsClause() {
    int index = 2; // skip name and type
    if (index < children.length && children[index] instanceof ASTnot_null) {
      // skip NOT NULL
      index++;
    }
    // Options should be last, but check for any unknown children.
    if (index == (children.length - 1) && children[index] instanceof ASToptions_clause) {
      return (ASToptions_clause) children[index];
    }
    if (index == children.length) {
      return null;
    }
    // If we are here we have an unknown child.
    throw new IllegalArgumentException("Unknown child type " + children[index]);
  }

  @Override
  public String toString() {
    StringBuilder ret = new StringBuilder();

    // name
    ret.append(getColumnName());
    ret.append(" ");
    // type
    ret.append(getColumnTypeString());

    int index = 2;
    if (isNotNull()) {
      ret.append(" NOT NULL");
      index++;
    }
    // getOptions also checks for unknown children.
    if (getOptionsClause() != null) {
      ret.append(" ");
      ret.append(getOptionsClause().toString());
    }
    return ret.toString().trim();
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof ASTcolumn_def) {
      return this.toString().equalsIgnoreCase(other.toString());
    }
    return false;
  }

  public static Comparator<ASTcolumn_def> SORT_BY_NAME_COMPARATOR =
      Comparator.comparing(o -> o.children[0].toString());
}
