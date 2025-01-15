/*
 * Copyright 2024 Google LLC
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

import static com.google.cloud.solutions.spannerddl.diff.AstTreeUtils.getChildByType;
import static com.google.cloud.solutions.spannerddl.diff.AstTreeUtils.getOptionalChildByType;

import com.google.cloud.solutions.spannerddl.diff.AstTreeUtils;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

public class ASTcreate_schema_statement extends SimpleNode {
  public ASTcreate_schema_statement(int id) {
    super(id);
  }

  public ASTcreate_schema_statement(DdlParser p, int id) {
    super(p, id);
  }

  private void validateChildren() {
    AstTreeUtils.validateChildrenClasses(
        children, ImmutableSet.of(ASTif_not_exists.class, ASTname.class, ASToptions_clause.class));
  }

  @Override
  public String toString() {
    return toStringOptionalExistClause(true);
  }

  public String getName() {
    return getChildByType(children, ASTname.class).toString();
  }

  /** Create string version, optionally including the IF NOT EXISTS clause */
  public String toStringOptionalExistClause(boolean includeExists) {
    validateChildren();
    return Joiner.on(" ")
        .skipNulls()
        .join(
            "CREATE",
            "SCHEMA",
            (includeExists ? getOptionalChildByType(children, ASTif_not_exists.class) : null),
            getName(),
            AstTreeUtils.getOptionalChildByType(children, ASToptions_clause.class));
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof ASTcreate_schema_statement) && toString().equals(obj.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }
}
