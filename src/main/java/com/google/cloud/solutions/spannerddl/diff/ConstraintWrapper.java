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
import com.google.cloud.solutions.spannerddl.parser.ASTcheck_constraint;
import com.google.cloud.solutions.spannerddl.parser.ASTforeign_key;
import com.google.cloud.solutions.spannerddl.parser.SimpleNode;

/**
 * Wrapper class for Check and Foreign Key constraints to include the table name for when they are
 * separated from their create table/alter table statements in
 * separateTablesIndexesConstraintsTtls().
 */
@AutoValue
abstract class ConstraintWrapper {

  static ConstraintWrapper create(String tableName, SimpleNode constraint) {
    if (!(constraint instanceof ASTforeign_key) && !(constraint instanceof ASTcheck_constraint)) {
      throw new IllegalArgumentException("not a valid constraint type : " + constraint.toString());
    }
    return new AutoValue_ConstraintWrapper(tableName, constraint);
  }

  abstract String tableName();

  abstract SimpleNode constraint();

  String getName() {
    if (constraint() instanceof ASTcheck_constraint) {
      return ((ASTcheck_constraint) constraint()).getName();
    }
    if (constraint() instanceof ASTforeign_key) {
      return ((ASTforeign_key) constraint()).getName();
    }
    throw new IllegalArgumentException("not a valid constraint type : " + constraint().toString());
  }
}
