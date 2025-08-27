/*
 * Copyright 2026 Google LLC
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
import java.util.Objects;

public class ASTcreate_locality_group_statement extends SimpleNode {
  public ASTcreate_locality_group_statement(int id) {
    super(id);
  }

  public ASTcreate_locality_group_statement(DdlParser p, int id) {
    super(p, id);
  }

  public String getNameOrDefault() {
    ASTname name = AstTreeUtils.getOptionalChildByType(children, ASTname.class);
    if (name == null) {
      throw new IllegalArgumentException("Cannot create DEFAULT locality group");
    }
    return name.toString();
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public String toString() {
    AstTreeUtils.validateChildrenClasses(
        children,
        ImmutableSet.of(
            ASTname.class, ASTdefaultt.class, ASTif_not_exists.class, ASToptions_clause.class));

    return Joiner.on(" ")
        .skipNulls()
        .join(
            "CREATE LOCALITY GROUP",
            Objects.toString(
                AstTreeUtils.getOptionalChildByType(children, ASTif_not_exists.class), null),
            getNameOrDefault(),
            getOptionsClause());
  }

  public ASToptions_clause getOptionsClause() {
    return AstTreeUtils.getOptionalChildByType(children, ASToptions_clause.class);
  }
}
