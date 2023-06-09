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

/** Abstract Syntax Tree parser object for "option_key_val" token */
public class ASToption_key_val extends SimpleNode {

  public ASToption_key_val(int id) {
    super(id);
  }

  public ASToption_key_val(DdlParser p, int id) {
    super(p, id);
  }

  @Override
  public String toString() {
    return getKey() + "=" + getValue();
  }

  public String getKey() {
    return children[0].toString();
  }

  public String getValue() {
    return ASTTreeUtils.tokensToString((SimpleNode) children[1]);
  }
}
