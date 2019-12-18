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

/** Abstract Syntax Tree parser object for "table_interleave_clause" token */
import com.google.common.base.Joiner;

public class ASTtable_interleave_clause extends SimpleNode {

  public ASTtable_interleave_clause(int id) {
    super(id);
  }

  public ASTtable_interleave_clause(DdlParser p, int id) {
    super(p, id);
  }

  public String getParentTableName() {
    return children[0].toString();
  }

  public String getOnDelete() {
    if (children.length == 2) {
      // verify child type
      return children[1].toString();
    } else {
      return ASTon_delete_clause.ON_DELETE_NO_ACTION;
    }
  }

  @Override
  public String toString() {
    return Joiner.on(" ").join("INTERLEAVE IN PARENT", getParentTableName(), getOnDelete());
  }
}
