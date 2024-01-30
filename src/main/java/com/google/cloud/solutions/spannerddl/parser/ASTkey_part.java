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

import java.util.Locale;

/** Abstract Syntax Tree parser object for "key_part" token */
public class ASTkey_part extends SimpleNode {

  public ASTkey_part(int id) {
    super(id);
  }

  public ASTkey_part(DdlParser p, int id) {
    super(p, id);
  }

  @Override
  public String toString() {
    if (children == null) {
      return jjtGetFirstToken().toString();
    }
    if (children.length == 1) {

      return ((ASTpath) children[0]).toString() + " ASC"; // key name without direction ;
    } else {
      // key name and ASC/DESC
      return ((ASTpath) children[0]).toString()
          + " "
          + children[1].toString().toUpperCase(Locale.ROOT);
    }
  }
}
