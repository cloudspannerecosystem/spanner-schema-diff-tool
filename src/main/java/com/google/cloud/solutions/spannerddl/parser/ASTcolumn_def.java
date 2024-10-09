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

import com.google.cloud.solutions.spannerddl.diff.AstTreeUtils;
import com.google.common.base.Joiner;
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
    return getColumnType().toString();
  }

  public ASTcolumn_type getColumnType() {
    return (ASTcolumn_type) children[1];
  }

  public ASTgeneration_clause getGenerationClause() {
    return AstTreeUtils.getOptionalChildByType(children, ASTgeneration_clause.class);
  }

  public ASTcolumn_default_clause getColumnDefaultClause() {
    return AstTreeUtils.getOptionalChildByType(children, ASTcolumn_default_clause.class);
  }

  public boolean isNotNull() {
    return AstTreeUtils.getOptionalChildByType(children, ASTnot_null.class) != null;
  }

  public @Nullable ASToptions_clause getOptionsClause() {
    return AstTreeUtils.getOptionalChildByType(children, ASToptions_clause.class);
  }

  public boolean isHidden() {
    return AstTreeUtils.getOptionalChildByType(children, ASThidden.class) != null;
  }

  public boolean isStored() {
    return getGenerationClause() != null && getGenerationClause().isStored();
  }

  @Override
  public String toString() {
    // check for unknown/unsupported children
    validate();

    return Joiner.on(" ")
        .skipNulls()
        .join(
            getColumnName(),
            getColumnTypeString(),
            (isNotNull() ? "NOT NULL" : null),
            getGenerationClause(),
            getColumnDefaultClause(),
            (isHidden() ? "HIDDEN" : null),
            getOptionsClause());
  }

  private void validate() {

    /* Column definition is:
         name
         column_type
         not_null (optional)
         generation_clause(optional) OR column_default_clause(optional)
         options_clause(optional)

       Parser will handle most issues, but we should check if an unknown class has been added to the
       parser.
       Iterate through children checking for optional items in order.
    */

    int index = 2; // skip name and type
    if (index < children.length && children[index] instanceof ASTnot_null) {
      // NOT NULL
      index++;
    }
    if (index < children.length && children[index] instanceof ASTgeneration_clause) {
      // generated
      index++;
    }
    if (index < children.length && children[index] instanceof ASTcolumn_default_clause) {
      // default value
      index++;
    }
    if (index < children.length && children[index] instanceof ASThidden) {
      // default value
      index++;
    }
    if (index < children.length && children[index] instanceof ASToptions_clause) {
      // options
      index++;
    }

    if (index < children.length) {
      // we have an unknown child.
      throw new IllegalArgumentException("Unknown child type " + children[index]);
    }
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof ASTcolumn_def) {
      return this.toString().equals(other.toString());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }
}
