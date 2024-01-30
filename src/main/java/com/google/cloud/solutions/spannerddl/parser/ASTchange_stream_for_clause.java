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

import com.google.cloud.solutions.spannerddl.diff.ASTTreeUtils;

public class ASTchange_stream_for_clause extends SimpleNode {
  public ASTchange_stream_for_clause(int id) {
    super(id);
  }

  public ASTchange_stream_for_clause(DdlParser p, int id) {
    super(p, id);
  }

  @Override
  public String toString() {
    ASTchange_stream_tracked_tables tables =
        ASTTreeUtils.getOptionalChildByType(children, ASTchange_stream_tracked_tables.class);
    if (tables != null) {
      return "FOR " + ASTTreeUtils.tokensToString(tables, false);
    }
    return "FOR ALL";
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }
}
