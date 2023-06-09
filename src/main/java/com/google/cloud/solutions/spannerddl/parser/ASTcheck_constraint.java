/*
 * Copyright 2023 Google LLC
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

public class ASTcheck_constraint extends SimpleNode {

  public ASTcheck_constraint(int id) {
    super(id);
  }

  public ASTcheck_constraint(DdlParser p, int id) {
    super(p, id);
  }

  public String getName() {
    // name is optional
    if (children[0] instanceof ASTconstraint_name) {
      return ASTTreeUtils.tokensToString((ASTconstraint_name) children[0]);
    } else {
      return ASTcreate_table_statement.ANONYMOUS_NAME;
    }
  }

  public String getExpression() {
    int child = 0;
    if (children[0] instanceof ASTconstraint_name) {
      child++;
    }
    return (ASTTreeUtils.tokensToString((ASTcheck_constraint_expression) children[child]));
  }

  public String toString() {
    return "CONSTRAINT " + getName() + " CHECK (" + getExpression() + ")";
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof ASTcolumn_def) {
      return this.toString().equals(other.toString());
    }
    return false;
  }
}
