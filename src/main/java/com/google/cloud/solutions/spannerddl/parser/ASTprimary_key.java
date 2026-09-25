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

import com.google.cloud.solutions.spannerddl.diff.AstTreeUtils;
import com.google.common.base.Joiner;
import java.util.List;

/** Abstract Syntax Tree parser object for "primary_Key" token */
public class ASTprimary_key extends SimpleNode {

  public ASTprimary_key(int id) {
    super(id);
  }

  public ASTprimary_key(DdlParser p, int id) {
    super(p, id);
  }

  /** Compares key columns without treating optional identifier quoting as a key change. */
  public boolean hasSameColumns(ASTprimary_key other) {
    List<ASTkey_part> keyparts = AstTreeUtils.getChildrenAssertType(children, ASTkey_part.class);
    List<ASTkey_part> otherKeyparts =
        AstTreeUtils.getChildrenAssertType(other.children, ASTkey_part.class);
    if (keyparts.size() != otherKeyparts.size()) {
      return false;
    }
    for (int i = 0; i < keyparts.size(); i++) {
      ASTkey_part keypart = keyparts.get(i);
      ASTkey_part otherKeypart = otherKeyparts.get(i);
      if (!AstTreeUtils.unquoteIdentifier(keypart.getKeyPath())
              .equals(AstTreeUtils.unquoteIdentifier(otherKeypart.getKeyPath()))
          || !keypart
              .toString()
              .substring(keypart.getKeyPath().length())
              .equals(otherKeypart.toString().substring(otherKeypart.getKeyPath().length()))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    List<ASTkey_part> keyparts = AstTreeUtils.getChildrenAssertType(children, ASTkey_part.class);
    if (keyparts.size() > 0) {
      return "PRIMARY KEY (" + Joiner.on(", ").join(keyparts) + ")";
    } else {
      return "PRIMARY KEY";
    }
  }
}
