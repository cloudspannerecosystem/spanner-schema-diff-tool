/*
 * Copyright 2020 Google LLC
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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

public class DdlDiffValidationTest {

  @Test
  public void validateForeignKey_missingReferencedTable() {
    String ddl =
        "CREATE TABLE test1 ( col1 INT64, col2 STRING(100), CONSTRAINT fk_in_table FOREIGN KEY (col2) REFERENCES othertable (othercol) ) PRIMARY KEY (col1);";
    try {
      DdlDiff.validateDdl(ddl, ImmutableMap.of(DdlDiff.IGNORE_PROTO_BUNDLES_OPT, false));
      fail("Expected DdlDiffException");
    } catch (DdlDiffException e) {
      assertThat(e.getMessage())
          .contains(
              "Table 'test1' contains foreign key 'fk_in_table' which references table 'othertable' which does not exist.");
    }
  }

  @Test
  public void validateForeignKey_existingTable() {
    String ddl =
        "CREATE TABLE test1 ( col1 INT64, col2 STRING(100), CONSTRAINT fk_in_table FOREIGN KEY (col2) REFERENCES othertable (othercol) ) PRIMARY KEY (col1);"
            + "CREATE table othertable ( othercol INT64, col4 STRING(100) ) PRIMARY KEY (othercol);";
    try {
      DdlDiff.validateDdl(ddl, ImmutableMap.of(DdlDiff.IGNORE_PROTO_BUNDLES_OPT, false));
    } catch (DdlDiffException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void validateForeignKey_missingReferencedColumn() {
    String ddl =
        "CREATE TABLE othertable (othercol STRING(100)) PRIMARY KEY (othercol); "
            + "CREATE TABLE test1 ( col1 INT64, col2 STRING(100), CONSTRAINT fk_in_table FOREIGN KEY (col2) REFERENCES othertable (missing_col) ) PRIMARY KEY (col1);";
    try {
      DdlDiff.validateDdl(ddl, ImmutableMap.of(DdlDiff.IGNORE_PROTO_BUNDLES_OPT, false));
      fail("Expected DdlDiffException");
    } catch (DdlDiffException e) {
      assertThat(e.getMessage())
          .contains(
              "Table 'test1' contains foreign key 'fk_in_table' which references column 'missing_col' which does not exist in table 'othertable'.");
    }
  }

  @Test
  public void validateForeignKey_existingReferencedColumn() {
    String ddl =
        "CREATE TABLE othertable ( othercol STRING(100), existing_col INT64 ) PRIMARY KEY (othercol); "
            + "CREATE TABLE test1 ( col1 INT64, col2 STRING(100), CONSTRAINT fk_in_table FOREIGN KEY (col2) REFERENCES othertable (existing_col) ) PRIMARY KEY (col1);";
    try {
      DdlDiff.validateDdl(ddl, ImmutableMap.of(DdlDiff.IGNORE_PROTO_BUNDLES_OPT, false));
    } catch (DdlDiffException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void validateIndex_missingTable() {
    String ddl = "CREATE INDEX myindex ON mytable (col1);";
    try {
      DdlDiff.validateDdl(ddl, ImmutableMap.of(DdlDiff.IGNORE_PROTO_BUNDLES_OPT, false));
      fail("Expected DdlDiffException");
    } catch (DdlDiffException e) {
      assertThat(e.getMessage())
          .contains("Index 'myindex' is on table 'mytable' which does not exist.");
    }
  }

  @Test
  public void validateIndex_existingTable() {
    String ddl =
        "CREATE TABLE mytable ( col1 INT64, col2 STRING(100) ) PRIMARY KEY (col1); "
            + "CREATE INDEX myindex ON mytable (col1);";
    try {
      DdlDiff.validateDdl(ddl, ImmutableMap.of(DdlDiff.IGNORE_PROTO_BUNDLES_OPT, false));
    } catch (DdlDiffException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void validateIndex_missingColumn() {
    String ddl =
        "CREATE TABLE mytable (col1 INT64) PRIMARY KEY (col1); CREATE INDEX myindex ON mytable (missing_col);";
    try {
      DdlDiff.validateDdl(ddl, ImmutableMap.of(DdlDiff.IGNORE_PROTO_BUNDLES_OPT, false));
      fail("Expected DdlDiffException");
    } catch (DdlDiffException e) {
      assertThat(e.getMessage())
          .contains(
              "Index 'myindex' on table 'mytable' includes column 'missing_col' which does not exist in the table.");
    }
  }

  @Test
  public void validateIndex_existingColumn() {
    String ddl =
        "CREATE TABLE mytable (col1 INT64, existing_col INT64 ) PRIMARY KEY (col1); CREATE INDEX myindex ON mytable (existing_col);";
    try {
      DdlDiff.validateDdl(ddl, ImmutableMap.of(DdlDiff.IGNORE_PROTO_BUNDLES_OPT, false));
    } catch (DdlDiffException e) {
      throw new RuntimeException(e);
    }
  }
}
