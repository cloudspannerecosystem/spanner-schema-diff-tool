/*
 * Copyright 2024 Google LLC
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

/**
 * Abstract Syntax Tree parser object for "annotation" token
 *
 * <p>Column Annotations are NOT a feature of Cloud Spanner.
 *
 * <p>This is an additional feature of the Cloud Spanner Schema parser exclusively in this tool so
 * that users of this tool can add metadata to colums, and have that metadata represented in the
 * parsed schema.
 *
 * <p>To use Annotations, they should be added to a CREATE TABLE statement as follows:
 *
 * <pre>
 *  CREATE TABLE Albums (
 *   -- @ANNOTATION SOMETEXT,
 *    id STRING(36),
 *  ) PRIMARY KEY (id)
 * </pre>
 *
 * Annotations need to be on their own line, and terminate with a comma. (This is because the '-- '
 * prefix is removed before using the JJT parser).
 *
 * <p>As they are comments, they are ignored by the diff generator and by Spanner itself.
 */
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
