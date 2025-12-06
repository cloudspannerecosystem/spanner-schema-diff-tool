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

/** Abstract Syntax Tree parser object for "table_interleave_clause" token */
public class ASTtable_interleave_clause extends SimpleNode {
  public ASTtable_interleave_clause(int id) {
    super(id);
  }

  public ASTtable_interleave_clause(DdlParser p, int id) {
    super(p, id);
  }

  public String getParentTableName() {
    for (Node child : children) {
      if (child instanceof ASTinterleave_in) {
        return AstTreeUtils.tokensToString((ASTinterleave_in) child);
      }
    }
    return null;
  }

  public boolean hasParentKeyword() {
    for (Node child : children) {
      if (child instanceof ASTparent) {
        return true;
      }
    }
    return false;
  }

  public boolean hasOnDeleteClause() {
    for (Node child : children) {
      if (child instanceof ASTon_delete_clause) {
        return true;
      }
    }
    return false;
  }

  public String getOnDelete() {
    for (Node child : children) {
      if (child instanceof ASTon_delete_clause) {
        if (!hasParentKeyword()) {
          throw new IllegalArgumentException(
              "ON DELETE clause requires INTERLEAVE IN PARENT syntax");
        }
        return child.toString();
      }
    }
    if (hasParentKeyword()) {
      return ASTon_delete_clause.ON_DELETE_NO_ACTION;
    }
    return null;
  }

  @Override
  public String toString() {
    return Joiner.on(" ")
        .skipNulls()
        .join(
            "INTERLEAVE IN",
            ((hasParentKeyword() || hasOnDeleteClause()) ? "PARENT" : null),
            getParentTableName(),
            getOnDelete());
  }
}
