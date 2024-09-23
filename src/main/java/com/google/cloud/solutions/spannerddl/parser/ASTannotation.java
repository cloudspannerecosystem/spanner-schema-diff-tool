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

import static com.google.cloud.solutions.spannerddl.diff.AstTreeUtils.getChildByType;

import com.google.cloud.solutions.spannerddl.diff.AstTreeUtils;
import java.util.ArrayList;
import java.util.List;

/** Abstract Syntax Tree parser object for "annotation" token */
public class ASTannotation extends SimpleNode {
  public ASTannotation(int id) {
    super(id);
  }

  public ASTannotation(DdlParser p, int id) {
    super(p, id);
  }

  public String getName() {
    return AstTreeUtils.tokensToString(getChildByType(children, ASTname.class));
  }

  public List<ASTannotation_param> getParams() {
    List<ASTannotation_param> params = new ArrayList<>();
    for (Node child : children) {
      if (child instanceof ASTannotation_param) {
        params.add((ASTannotation_param) child);
      }
    }
    return params;
  }

  public String getAnnotation() {
    return AstTreeUtils.tokensToString(this, false).replaceAll(" ", "");
  }
}
