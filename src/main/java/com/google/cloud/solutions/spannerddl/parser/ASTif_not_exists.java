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

public class ASTif_not_exists extends SimpleNode {
  public ASTif_not_exists(int id) {
    super(id);
  }

  public ASTif_not_exists(DdlParser p, int id) {
    super(p, id);
  }

  @Override
  public String toString() {
    return "IF NOT EXISTS";
  }
}
