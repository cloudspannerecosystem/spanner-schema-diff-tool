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

public class ASTreferential_action extends SimpleNode {
  public ASTreferential_action(int id) {
    super(id);
  }

  public ASTreferential_action(DdlParser p, int id) {
    super(p, id);
  }

  @Override
  public String toString() {
    return AstTreeUtils.tokensToString(this);
  }

  @Override
  public boolean equals(Object other) {
    return (other instanceof ASTreferential_action && this.toString().equals(other.toString()));
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }
}
