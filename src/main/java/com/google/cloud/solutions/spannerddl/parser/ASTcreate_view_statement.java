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

import com.google.cloud.solutions.spannerddl.diff.AstTreeUtils;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

public class ASTcreate_view_statement extends SimpleNode {
  public ASTcreate_view_statement(int id) {
    super(id);
  }

  public ASTcreate_view_statement(DdlParser p, int id) {
    super(p, id);
  }

  public String toStringBase() {
    validateChildren();
    return Joiner.on(" ")
            .skipNulls()
            .join(
                    "VIEW",
                    getName(),
                    "SQL SECURITY",
                    AstTreeUtils.tokensToString(AstTreeUtils.getOptionalChildByType(children, ASTsql_security.class)),
                    "AS",
                    AstTreeUtils.tokensToString(AstTreeUtils.getOptionalChildByType(children, ASTview_definition.class))
                    );
  }

  private void validateChildren() {
    AstTreeUtils.validateChildrenClasses(
            children, ImmutableSet.of(ASTname.class, ASTsql_security.class, ASTview_definition.class));
  }

  public String getName() {
    return AstTreeUtils.tokensToString(AstTreeUtils.getChildByType(children, ASTname.class), false);
  }

  @Override
  public String toString() {
    return Joiner.on(" ")
            .skipNulls()
            .join(
                    "CREATE",
                    toStringBase()
                    );
  }


  @Override
  public boolean equals(Object obj) {
    return (obj instanceof ASTcreate_view_statement) && toString().equals(obj.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }
}
