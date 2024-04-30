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

/** Abstract Syntax Tree parser object for "alter_table_statement" token */
public class ASTalter_table_statement extends SimpleNode {

  public ASTalter_table_statement(int id) {
    super(id);
  }

  public ASTalter_table_statement(DdlParser p, int id) {
    super(p, id);
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof ASTalter_table_statement) {
      // lazy: compare text rendering.
      return this.toString().equals(other.toString());
    }
    return false;
  }

  @Override
  public String toString() {
    // perform validation. Supported Alter Table statements are:
    // ADD (FOREIGN KEY|CHECK CONSTRAINT|ROW DELETION POLICY)
    StringBuilder ret = new StringBuilder();
    ret.append("ALTER TABLE ");
    ret.append(jjtGetChild(0)); // tablename
    ret.append(" ADD ");
    final Node alterTableAction = jjtGetChild(1);
    if (alterTableAction instanceof ASTforeign_key) {
      ret.append(alterTableAction);
    } else if (alterTableAction instanceof ASTcheck_constraint) {
      ret.append(alterTableAction);
    } else if (alterTableAction instanceof ASTadd_row_deletion_policy) {
      ret.append(alterTableAction.jjtGetChild(0));
    } else {
      throw new IllegalArgumentException(
          "Unrecognised Alter Table action : "
              + alterTableAction.getClass()
              + " in: "
              + AstTreeUtils.tokensToString(this));
    }
    return ret.toString();
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }
}
