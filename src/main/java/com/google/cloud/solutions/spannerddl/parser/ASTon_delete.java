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

public class ASTon_delete extends SimpleNode {
  public ASTon_delete(int id) {
    super(id);
  }

  public ASTon_delete(DdlParser p, int id) {
    super(p, id);
  }

  @Override
  public String toString() {
    if (children[0] instanceof ASTreferential_action) {
      return "ON DELETE " + children[0];
    } else {
      throw new UnsupportedOperationException("Not Implemented");
    }
  }

  @Override
  public boolean equals(Object other) {
    return (other instanceof ASTon_delete && this.toString().equals(other.toString()));
  }
}
