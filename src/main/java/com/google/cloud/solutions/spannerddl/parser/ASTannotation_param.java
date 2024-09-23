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

public class ASTannotation_param extends SimpleNode {
  public ASTannotation_param(int id) {
    super(id);
  }

  public ASTannotation_param(DdlParser p, int id) {
    super(p, id);
  }

  public String getKey() {
    ASTparam_key key = AstTreeUtils.getChildByType(this, ASTparam_key.class);
    return AstTreeUtils.tokensToString(key);
  }

  public String getValue() {
    ASTparam_val val = AstTreeUtils.getOptionalChildByType(this, ASTparam_val.class);
    if (val != null) {
      return AstTreeUtils.tokensToString(val);
    } else {
      return null;
    }
  }
}
