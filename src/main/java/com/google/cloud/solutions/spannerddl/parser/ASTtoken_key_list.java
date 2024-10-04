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

import com.google.cloud.solutions.spannerddl.diff.AstTreeUtils;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.stream.Collectors;

public class ASTtoken_key_list extends SimpleNode {
  public ASTtoken_key_list(int id) {
    super(id);
  }

  public ASTtoken_key_list(DdlParser p, int id) {
    super(p, id);
  }

  private void validateChildren() {
    AstTreeUtils.validateChildrenClasses(children, ImmutableSet.of(ASTkey_part.class));
  }

  @Override
  public String toString() {
    validateChildren();
    List<String> keyPaths =
        getKeyParts().stream().map(ASTkey_part::getKeyPath).collect(Collectors.toList());
    return "( " + Joiner.on(", ").join(keyPaths) + " )";
  }

  public List<ASTkey_part> getKeyParts() {
    return AstTreeUtils.getChildrenAssertType(children, ASTkey_part.class);
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }
}
