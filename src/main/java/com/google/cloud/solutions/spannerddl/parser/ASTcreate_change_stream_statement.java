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

public class ASTcreate_change_stream_statement extends SimpleNode {
  public ASTcreate_change_stream_statement(int id) {
    super(id);
  }

  public ASTcreate_change_stream_statement(DdlParser p, int id) {
    super(p, id);
  }

  public String getName() {
    return AstTreeUtils.getChildByType(children, ASTname.class).toString();
  }

  public ASTchange_stream_for_clause getForClause() {
    return AstTreeUtils.getOptionalChildByType(children, ASTchange_stream_for_clause.class);
  }

  public ASToptions_clause getOptionsClause() {
    return AstTreeUtils.getOptionalChildByType(children, ASToptions_clause.class);
  }

  @Override
  public String toString() {
    return Joiner.on(" ")
        .skipNulls()
        .join("CREATE CHANGE STREAM", getName(), getForClause(), getOptionsClause());
  }

  @Override
  public boolean equals(Object other) {
    return (other instanceof ASTcreate_change_stream_statement)
        && this.toString().equals(other.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }
}
