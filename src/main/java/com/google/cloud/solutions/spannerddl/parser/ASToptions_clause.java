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
import java.util.Map;
import java.util.stream.Collectors;

public class ASToptions_clause extends SimpleNode {

  public ASToptions_clause(int id) {
    super(id);
  }

  public ASToptions_clause(DdlParser p, int id) {
    super(p, id);
  }

  @Override
  public String toString() {
    return "OPTIONS ("
        + Joiner.on(",").join(AstTreeUtils.getChildrenAssertType(children, ASToption_key_val.class))
        + ")";
  }

  public Map<String, String> getKeyValueMap() {
    return AstTreeUtils.getChildrenAssertType(children, ASToption_key_val.class).stream()
        .collect(Collectors.toMap(ASToption_key_val::getKey, ASToption_key_val::getValue));
  }

  @Override
  public boolean equals(Object obj) {
    return toString().equals(obj.toString());
  }
}
