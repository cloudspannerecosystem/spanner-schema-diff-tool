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

/** Abstract Syntax Tree parser object for "on_delete_clause" token */
public class ASTon_delete_clause extends SimpleNode {

  public static final String ON_DELETE_CASCADE = "ON DELETE CASCADE";
  public static final String ON_DELETE_NO_ACTION = "ON DELETE NO ACTION";

  public ASTon_delete_clause(int id) {
    super(id);
  }

  public ASTon_delete_clause(DdlParser p, int id) {
    super(p, id);
  }

  @Override
  public String toString() {
    if (children[0] instanceof ASTcascade) {
      return ON_DELETE_CASCADE;
    }
    if (children[0] instanceof ASTno_action) {
      return ON_DELETE_NO_ACTION;
    }
    throw new IllegalArgumentException("Unrecognised ON DELETE type: " + children[0]);
  }
}
