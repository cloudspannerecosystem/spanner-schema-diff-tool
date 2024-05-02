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

package com.google.cloud.solutions.spannerddl.diff;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.List;

/** Simple container class to hold a set of drop, modify and create statements. */
@AutoValue
public abstract class SchemaUpdateStatements {

  /** Creates an instance storing the specified statements. */
  public static SchemaUpdateStatements create(
      List<String> dropStatements, List<String> modifyStatements, List<String> createStatements) {
    return new AutoValue_SchemaUpdateStatements(
        ImmutableList.copyOf(dropStatements),
        ImmutableList.copyOf(modifyStatements),
        ImmutableList.copyOf(createStatements));
  }

  public abstract ImmutableList<String> dropStatements();

  public abstract ImmutableList<String> modifyStatements();

  public abstract ImmutableList<String> createStatements();
}
