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

package com.google.cloud.solutions.spannerddl.diff;

import com.google.cloud.solutions.spannerddl.parser.Node;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/** Utility functions for getting and casting Nodes in the parsed AST. */
public class ASTTreeUtils {

  /** Gets (and casts) the first found child of a specific node type */
  public static <T> T getChildByType(Node[] children, Class<T> type) {
    for (Node child : children) {
      if (type.isInstance(child)) {
        return type.cast(child);
      }
    }
    throw new IllegalArgumentException(
        "Cannot find child of type " + type.getName() + " in " + Arrays.asList(children));
  }

  /**
   * Ensures that the passed array of Nodes is of a specific node type, and returns a List with the
   * specific type.
   */
  public static <T> List<T> getChildrenAssertType(Node[] children, Class<T> type) {
    return StreamSupport.stream(Arrays.spliterator(children), false)
        .map(type::cast)
        .collect(Collectors.toList());
  }

  private ASTTreeUtils() {
  }
}
