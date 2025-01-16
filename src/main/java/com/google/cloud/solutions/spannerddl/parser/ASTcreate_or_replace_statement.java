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

import static com.google.cloud.solutions.spannerddl.diff.AstTreeUtils.getOptionalChildByType;

import java.util.Objects;
import java.util.stream.Stream;

public class ASTcreate_or_replace_statement extends SimpleNode {
  public ASTcreate_or_replace_statement(int id) {
    super(id);
  }

  public ASTcreate_or_replace_statement(DdlParser p, int id) {
    super(p, id);
  }

  public SimpleNode getSchemaObject() {
    return Stream.of(
            getOptionalChildByType(children, ASTcreate_view_statement.class),
            getOptionalChildByType(children, ASTcreate_model_statement.class),
            getOptionalChildByType(children, ASTcreate_schema_statement.class))
        .filter(Objects::nonNull)
        .findFirst()
        .get();
  }

  @Override
  public String toString() {
    return getSchemaObject().toString();
  }

  @Override
  public int hashCode() {
    return getSchemaObject().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ASTcreate_or_replace_statement
        && getSchemaObject().equals(((ASTcreate_or_replace_statement) obj).getSchemaObject());
  }
}
