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

public class ASTgeneration_clause extends SimpleNode {

  public ASTgeneration_clause(int id) {
    super(id);
  }

  public ASTgeneration_clause(DdlParser p, int id) {
    super(p, id);
  }

  public boolean isStored() {
    return children.length > 1 && children[1].getClass() == ASTstored.class;
  }

  @Override
  public String toString() {
    final ASTexpression exp = (ASTexpression) children[0];
    final String storedOpt = isStored() ? " STORED" : "";
    return "AS ( " + exp.toString() + " )" + storedOpt;
  }

  @Override
  public int hashCode() {
    return this.toString().hashCode();
  }
}
