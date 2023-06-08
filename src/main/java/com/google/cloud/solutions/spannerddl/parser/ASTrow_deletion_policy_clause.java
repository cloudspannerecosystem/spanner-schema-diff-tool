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

/**
 * This defines the row deletion policy used in: Create Table, Alter Table add ROW DELETION POLICY
 * etc.
 */
public class ASTrow_deletion_policy_clause extends SimpleNode {
  public ASTrow_deletion_policy_clause(int id) {
    super(id);
  }

  public ASTrow_deletion_policy_clause(DdlParser p, int id) {
    super(p, id);
  }

  @Override
  public String toString() {
    ASTrow_deletion_policy_expression expression =
        ASTTreeUtils.getChildByType(children, ASTrow_deletion_policy_expression.class);
    return "ROW DELETION POLICY ("
        + ASTTreeUtils.tokensToString(expression.firstToken, expression.lastToken)
        + ")";
  }

  @Override
  public boolean equals(Object other) {
    // use text comparison
    return (other instanceof ASTrow_deletion_policy_clause
        && this.toString().equals(other.toString()));
  }
}
