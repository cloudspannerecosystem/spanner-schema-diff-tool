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

public class ASTforeign_key extends SimpleNode {

  public ASTforeign_key(int id) {
    super(id);
  }

  public ASTforeign_key(DdlParser p, int id) {
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

  public List<String> getConstrainedColumnNames() {
    return identifierListToStringList(
        (ASTidentifier_list) ((ASTreferencing_columns) children[1]).children[0]);
  }

  private List<String> identifierListToStringList(ASTidentifier_list idList) {
    return Arrays.stream(idList.children)
        .map(o -> ASTTreeUtils.tokensToString((ASTidentifier) o, false))
        .collect(Collectors.toList());
  }

  public String getReferencedTableName() {
    int child = 1;
    if (children[0] instanceof ASTconstraint_name) {
      token = token.next;
    }
    return ASTTreeUtils.tokensToString((ASTreferenced_table) children[child], false);
  }

  public List<String> getReferencedColumnNames() {
    int child = 2;
    if (children[0] instanceof ASTconstraint_name) {
      child++;
    }
    return identifierListToStringList(
        (ASTidentifier_list) ((ASTreferenced_columns) children[child]).children[0]);
  }

  public String getDeleteOption() {
    ASTon_delete deleteOption = ASTTreeUtils.getOptionalChildByType(children, ASTon_delete.class);
    if (deleteOption != null) {
      return " " + deleteOption;
    } else {
      return "";
    }
  }

  public String toString() {
    return "CONSTRAINT "
        + getName()
        + " FOREIGN KEY ("
        + Joiner.on(", ").join(getConstrainedColumnNames())
        + ") REFERENCES "
        + getReferencedTableName()
        + " ("
        + Joiner.on(", ").join(getReferencedColumnNames())
        + ")"
        + getDeleteOption();
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof ASTforeign_key) {
      return this.toString().equals(other.toString());
    }
    return false;
  }
}
