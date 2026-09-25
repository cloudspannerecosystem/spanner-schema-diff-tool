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
import org.jspecify.annotations.Nullable;

/** Abstract Syntax Tree parser object for "table_interleave_clause" token */
public class ASTtable_interleave_clause extends SimpleNode {
  public ASTtable_interleave_clause(int id) {
    super(id);
  }

  public ASTtable_interleave_clause(DdlParser p, int id) {
    super(p, id);
  }

  public String getInterleaveTargetClause() {
    return "INTERLEAVE IN " + (isParentInterleave() ? "PARENT " : "") + getInterleaveTableName();
  }

  public String getInterleaveTableName() {
    return AstTreeUtils.tokensToString(
        AstTreeUtils.getChildByType(children, ASTinterleave_in.class));
  }

  public boolean isParentInterleave() {
    return AstTreeUtils.getOptionalChildByType(children, ASTparent.class) != null;
  }

  public @Nullable String getOnDelete() {
    validate();
    ASTon_delete_clause ondelete =
        AstTreeUtils.getOptionalChildByType(children, ASTon_delete_clause.class);
    if (!isParentInterleave()) {
      return null;
    }
    if (ondelete == null) {
      return ASTon_delete_clause.ON_DELETE_NO_ACTION;
    } else {
      return ondelete.toString();
    }
  }

  public void validate() {
    if (!isParentInterleave()
        && AstTreeUtils.getOptionalChildByType(children, ASTon_delete_clause.class) != null) {
      throw new IllegalArgumentException(
          "ON DELETE is only valid for INTERLEAVE IN PARENT clauses");
    }
  }

  @Override
  public String toString() {
    return Joiner.on(" ").skipNulls().join(getInterleaveTargetClause(), getOnDelete());
  }
}
