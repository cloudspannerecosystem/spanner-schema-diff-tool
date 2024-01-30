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

import com.google.cloud.solutions.spannerddl.diff.ASTTreeUtils;

/** Abstract Syntax Tree parser object for "column_length" token */
public class ASTcolumn_length extends SimpleNode {

  public ASTcolumn_length(int id) {
    super(id);
  }

  public ASTcolumn_length(DdlParser p, int id) {
    super(p, id);
  }

  @Override
  public String toString() {
    return ASTTreeUtils.tokensToString(this);
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }
}
