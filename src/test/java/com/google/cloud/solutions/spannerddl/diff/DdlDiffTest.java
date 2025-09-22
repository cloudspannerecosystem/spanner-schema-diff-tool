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

import static com.google.cloud.solutions.spannerddl.diff.DdlDiff.ALLOW_DROP_STATEMENTS_OPT;
import static com.google.cloud.solutions.spannerddl.diff.DdlDiff.ALLOW_RECREATE_CONSTRAINTS_OPT;
import static com.google.cloud.solutions.spannerddl.diff.DdlDiff.ALLOW_RECREATE_INDEXES_OPT;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.cloud.solutions.spannerddl.parser.ASTddl_statement;
import com.google.cloud.solutions.spannerddl.parser.ParseException;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class DdlDiffTest {

  @Test
  public void parseMultiDdlStatements() throws DdlDiffException {
    String DDL =
        "create table test1 (col1 int64) primary key (col1);\n\n"
            + "-- comment ; with semicolon.\n"
            + "create table test2 (col2 float64) primary key (col2);\n\n"
            + "create table test3 (col3 STRING(max) -- inline comment\n"
            + ") primary key (col3), interleave in parent testparent \n"
            + "on delete no action;\n\n"
            + "create index index1 on table1 (col1);\n"
            + "-- more comment\n"
            + "; -- stray semicolon";

    List<ASTddl_statement> result = DdlDiff.parseDdl(DDL);

    assertThat(result).hasSize(4);

    assertThat(result.get(0).toString())
        .isEqualTo("CREATE TABLE test1 ( col1 INT64 ) PRIMARY KEY (col1 ASC)");
    assertThat(result.get(1).toString())
        .isEqualTo("CREATE TABLE test2 ( col2 FLOAT64 ) PRIMARY KEY (col2 ASC)");
    assertThat(result.get(2).toString())
        .isEqualTo(
            "CREATE TABLE test3 ( col3 STRING(MAX) ) PRIMARY KEY (col3 ASC), "
                + "INTERLEAVE IN PARENT testparent ON DELETE NO ACTION");
    assertThat(result.get(3).toString()).isEqualTo("CREATE INDEX index1 ON table1 ( col1 ASC )");
  }

  @Test
  public void parseCreateTable_anonForeignKey() throws DdlDiffException {
    try {
      DdlDiff.parseDdl(
          "create table test ("
              + "intcol int64 not null, "
              + "FOREIGN KEY(col1, col2) REFERENCES other_table(other_col1, other_col2)"
              + ")"
              + "primary key (intcol ASC)");
      fail("Expected exception not thrown");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage())
          .containsMatch("Can not create diffs when anonymous constraints are used.");
    }
  }

  @Test
  public void parseCreateTable_anonCheckConstraint() throws DdlDiffException {
    try {
      DdlDiff.parseDdl(
          "create table test ("
              + "intcol int64 not null, "
              + "check (intcol>1)"
              + ")"
              + "primary key (intcol ASC)");
      fail("Expected exception not thrown");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage())
          .containsMatch("Can not create diffs when anonymous constraints are used.");
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseDDLNoAlterTableAlterColumn() throws DdlDiffException {
    DdlDiff.parseDdl("alter table test1 alter column col1 int64 not null");
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseDDLNoAlterTableAddColumn() throws DdlDiffException {
    DdlDiff.parseDdl("alter table test1 add column col1 int64 not null");
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseDDLNoAlterTableDropColumn() throws DdlDiffException {
    DdlDiff.parseDdl("alter table test1 drop column col1");
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseDDLNoAlterTableDropConstraint() throws DdlDiffException {
    DdlDiff.parseDdl("alter table test1 drop constraint xxx");
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseDDLNoAlterTableSetOnDelete() throws DdlDiffException {
    DdlDiff.parseDdl("alter table test1 set on delete cascade");
  }

  @Test
  public void parseDDLAlterTableAddConstraint() throws DdlDiffException {
    DdlDiff.parseDdl("alter table test1 add constraint XXX FOREIGN KEY (yyy) references zzz(xxx)");
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseDDLNoAlterTableAddAnonConstraint() throws DdlDiffException {
    DdlDiff.parseDdl("alter table test1 add FOREIGN KEY (yyy) references zzz(xxx)");
  }

  @Test
  public void generateAlterTable_AddColumn() throws DdlDiffException {
    // Add single row.
    assertThat(
            getDiff(
                "create table test1 (col1 int64) primary key (col1);",
                "create table test1 (col1 int64, col2 int64 not null) primary key (col1);",
                true))
        .containsExactly("ALTER TABLE test1 ADD COLUMN col2 INT64 NOT NULL");

    // Add multiple rows.
    assertThat(
            getDiff(
                "create table test1 (col1 int64) primary key (col1);",
                "create table test1 (col1 int64, col2 String(MAX), col3 Array<Bytes(100)> not null)"
                    + " primary key (col1);",
                true))
        .containsExactly(
            "ALTER TABLE test1 ADD COLUMN col2 STRING(MAX)",
            "ALTER TABLE test1 ADD COLUMN col3 ARRAY<BYTES(100)> NOT NULL");
  }

  @Test
  public void generateAlterTable_DropColumn() throws DdlDiffException {
    // Add single row.
    assertThat(
            getDiff(
                "create table test1 (col1 int64, col2 int64 not null) primary key (col1);",
                "create table test1 (col1 int64) primary key (col1);",
                true))
        .containsExactly("ALTER TABLE test1 DROP COLUMN col2");
    assertThat(
            getDiff(
                "create table test1 (col1 int64, col2 int64 not null) primary key (col1);",
                "create table test1 (col1 int64) primary key (col1);",
                false))
        .isEmpty();

    // Add multiple rows.
    assertThat(
            getDiff(
                "create table test1 (col1 int64, col2 String(MAX), col3 Array<Bytes(100)> not null)"
                    + " primary key (col1);",
                "create table test1 (col1 int64) primary key (col1);",
                true))
        .containsExactly(
            "ALTER TABLE test1 DROP COLUMN col2", "ALTER TABLE test1 DROP COLUMN col3");
    assertThat(
            getDiff(
                "create table test1 (col1 int64, col2 String(MAX), col3 Array<Bytes(100)> not null)"
                    + " primary key (col1);",
                "create table test1 (col1 int64) primary key (col1);",
                false))
        .isEmpty();
  }

  @Test
  public void generateAlterTable_AlterColumnAttrs() throws DdlDiffException {
    // Not null added and removed
    assertThat(
            getDiff(
                "create table test1 (col1 int64, col2 int64 not null) primary key (col1);",
                "create table test1 (col1 int64 not null, col2 int64) primary key (col1);",
                true))
        .containsExactly(
            "ALTER TABLE test1 ALTER COLUMN col1 INT64 NOT NULL",
            "ALTER TABLE test1 ALTER COLUMN col2 INT64");

    // Options added and removed
    assertThat(
            getDiff(
                "create table test1 (col1 timestamp options(allow_commit_timestamp=true), col2"
                    + " timestamp) primary key (col1);",
                "create table test1 (col1 timestamp, col2 timestamp"
                    + " options(allow_commit_timestamp=true)) primary key (col1);",
                true))
        .containsExactly(
            "ALTER TABLE test1 ALTER COLUMN col1 SET OPTIONS (allow_commit_timestamp=NULL)",
            "ALTER TABLE test1 ALTER COLUMN col2 SET OPTIONS (allow_commit_timestamp=TRUE)");
  }

  @Test
  public void generateAlterTable_AlterColumnSize() throws DdlDiffException {
    // changes string()  size and array<bytes()> size
    assertThat(
            getDiff(
                "create table test1 (col1 String(100), col2 ARRAY<BYTES(100)>) primary key (col1);",
                "create table test1 (col1 String(200), col2 ARRAY<BYTES(MAX)>) primary key (col1);",
                true))
        .containsExactly(
            "ALTER TABLE test1 ALTER COLUMN col1 STRING(200)",
            "ALTER TABLE test1 ALTER COLUMN col2 ARRAY<BYTES(MAX)>");
  }

  @Test
  public void generateAlterTable_incompatibleTypeChange() {
    // Change array depth
    getDiffCheckDdlDiffException(
        "create table test1 (col1 ARRAY<BYTES(100)>) primary key (col1);",
        "create table test1 (col1 ARRAY<ARRAY<BYTES(100)>>) primary key (col1);",
        true,
        "Cannot change type of table test1 column col1");

    // change Array sized type
    getDiffCheckDdlDiffException(
        "create table test1 (col1 ARRAY<BYTES(100)>) primary key (col1);",
        "create table test1 (col1 ARRAY<STRING(100)>) primary key (col1);",
        true,
        "Cannot change type of table test1 column col1");

    // change Array type
    getDiffCheckDdlDiffException(
        "create table test1 (col1 ARRAY<int64>) primary key (col1);",
        "create table test1 (col1 ARRAY<float64>) primary key (col1);",
        true,
        "Cannot change type of table test1 column col1");

    // change sized type
    getDiffCheckDdlDiffException(
        "create table test1 (col1 BYTES(100)) primary key (col1);",
        "create table test1 (col1 STRING(100)) primary key (col1);",
        true,
        "Cannot change type of table test1 column col1");

    // change type
    getDiffCheckDdlDiffException(
        "create table test1 (col1 int64) primary key (col1);",
        "create table test1 (col1 float64) primary key (col1);",
        true,
        "Cannot change type of table test1 column col1");
  }

  @Test
  public void generateAlterTable_changeKey() {
    // change key col
    getDiffCheckDdlDiffException(
        "create table test1 (col1 int64, col2 int64) primary key (col1);",
        "create table test1 (col1 int64, col2 int64) primary key (col2);",
        true,
        "Cannot change primary key of table test1");

    // add key col
    getDiffCheckDdlDiffException(
        "create table test1 (col1 int64, col2 int64) primary key (col1);",
        "create table test1 (col1 int64, col2 int64) primary key (col1, col2);",
        true,
        "Cannot change primary key of table test1");

    // remove key col
    getDiffCheckDdlDiffException(
        "create table test1 (col1 int64, col2 int64) primary key (col1, col2);",
        "create table test1 (col1 int64, col2 int64) primary key (col1);",
        true,
        "Cannot change primary key of table test1");

    // change order
    getDiffCheckDdlDiffException(
        "create table test1 (col1 int64, col2 int64) primary key (col1);",
        "create table test1 (col1 int64, col2 int64) primary key (col1 DESC);",
        true,
        "Cannot change primary key of table test1");
  }

  @Test
  public void generateAlterTable_changeInterleaving() throws DdlDiffException {
    // remove interleave
    getDiffCheckDdlDiffException(
        "create table test1 (col1 int64, col2 int64) "
            + "primary key (col1), interleave in parent testparent;",
        "create table test1 (col1 int64, col2 int64) primary key (col1)",
        true,
        "Cannot change interleaving on table test1");

    // change parent should generate ALTER
    assertThat(
            getDiff(
                "create table test1 (col1 int64, col2 int64) "
                    + "primary key (col1), interleave in parent testparent;",
                "create table test1 (col1 int64, col2 int64) "
                    + "primary key (col1), interleave in parent otherparent",
                true))
        .containsExactly(
            "ALTER TABLE test1 SET INTERLEAVE IN PARENT otherparent ON DELETE NO ACTION");

    // change on delete
    assertThat(
            getDiff(
                "create table test1 (col1 int64, col2 int64) "
                    + "primary key (col1), interleave in parent testparent on delete cascade;",
                "create table test1 (col1 int64, col2 int64) "
                    + "primary key (col1), interleave in parent testparent",
                true))
        .containsExactly(
            "ALTER TABLE test1 SET INTERLEAVE IN PARENT testparent ON DELETE NO ACTION");
    // change on delete
    assertThat(
            getDiff(
                "create table test1 (col1 int64, col2 int64) "
                    + "primary key (col1), interleave in parent testparent on delete NO ACTION;",
                "create table test1 (col1 int64, col2 int64) "
                    + "primary key (col1), interleave in parent testparent on delete cascade",
                true))
        .containsExactly("ALTER TABLE test1 SET INTERLEAVE IN PARENT testparent ON DELETE CASCADE");
  }

  @Test
  public void generateAlterTable_changeGenerationClause() {
    // remove interleave
    getDiffCheckDdlDiffException(
        "create table test1 (col1 int64, col2 int64, col3 int64 as ( col1*col2 ) stored) primary"
            + " key (col1)",
        "create table test1 (col1 int64, col2 int64, col3 int64 as ( col1/col2 ) stored) primary"
            + " key (col1)",
        true,
        "Cannot change generation clause of table test1 column col3 from AS ");

    // add generation
    getDiffCheckDdlDiffException(
        "create table test1 (col1 int64, col2 int64, col3 int64) primary key (col1)",
        "create table test1 (col1 int64, col2 int64, col3 int64 as ( col1*col2 ) stored) primary"
            + " key (col1)",
        true,
        "Cannot change generation clause of table test1 column col3 from null ");

    // remove generation
    getDiffCheckDdlDiffException(
        "create table test1 (col1 int64, col2 int64, col3 int64 as ( col1*col2 ) stored) primary"
            + " key (col1)",
        "create table test1 (col1 int64, col2 int64, col3 int64) primary key (col1)",
        true,
        "Cannot change generation clause of table test1 column col3 from AS");
  }

  @Test
  public void generateAlterTable_alterStatementOrdering() throws DdlDiffException {
    assertThat(
            getDiff(
                "create table test1 ("
                    + "col1 int64, "
                    + "col2 int64, "
                    + "col3 int64 NOT NULL, "
                    + "col4 int64, "
                    + "col5 timestamp OPTIONS (allow_commit_timestamp=true), "
                    + "col6 timestamp, "
                    + "col7 STRING(100) ) "
                    + "primary key (col1), interleave in parent testparent",
                "create table test1 ("
                    + "col1 int64, "
                    // remove col2
                    + "col3 int64, " // remove not null
                    + "col4 int64 NOT NULL, " // add not null
                    + "col5 timestamp, " // remove options
                    + "col6 timestamp OPTIONS (allow_commit_timestamp=true)," // add options
                    + "col7 STRING(200), " // change size
                    + "col8 float64 not null " // add column
                    + " ) primary key (col1), "
                    + "interleave in parent testparent on delete cascade", // on delete rule.
                true)) // allow drop
        .containsExactly(
            // change table options.
            "ALTER TABLE test1 SET INTERLEAVE IN PARENT testparent ON DELETE CASCADE",
            // then drop cols
            "ALTER TABLE test1 DROP COLUMN col2",
            // then add cols
            "ALTER TABLE test1 ADD COLUMN col8 FLOAT64 NOT NULL",
            // then alter cols.
            "ALTER TABLE test1 ALTER COLUMN col3 INT64",
            "ALTER TABLE test1 ALTER COLUMN col4 INT64 NOT NULL",
            "ALTER TABLE test1 ALTER COLUMN col5 SET OPTIONS (allow_commit_timestamp=NULL)",
            "ALTER TABLE test1 ALTER COLUMN col6 SET OPTIONS (allow_commit_timestamp=TRUE)",
            "ALTER TABLE test1 ALTER COLUMN col7 STRING(200)");
  }

  @Test
  public void parseAlterDatabaseDifferentDbNames() throws ParseException {
    getDiffCheckDdlDiffException(
        "ALTER DATABASE dbname SET OPTIONS(hello='world');",
        "ALTER DATABASE otherdbname SET OPTIONS(hello='world');",
        true,
        "Database IDs differ");

    getDiffCheckDdlDiffException(
        "ALTER DATABASE dbname SET OPTIONS(hello='world');"
            + "ALTER DATABASE otherdbname SET OPTIONS(goodbye='world');",
        "",
        true,
        "Multiple database IDs defined");

    getDiffCheckDdlDiffException(
        "",
        "ALTER DATABASE dbname SET OPTIONS(hello='world');"
            + "ALTER DATABASE otherdbname SET OPTIONS(hello='world');",
        true,
        "Multiple database IDs defined");
  }

  @Test
  public void diffCreateTableDifferentExists() throws DdlDiffException {
    assertThat(
            getDiff(
                "create table test1 ( col1 int64 ) primary key (col1);",
                "create table if not exists test1 ( col1 int64 ) primary key (col1);",
                true))
        .isEmpty();
  }

  @Test
  public void diffCreateIndexDifferentExists() throws DdlDiffException {
    assertThat(
            getDiff(
                "CREATE INDEX myindex ON mytable ( col1 );",
                "CREATE INDEX IF NOT EXISTS myindex ON mytable ( col1 );",
                true))
        .isEmpty();
  }

  @Test
  public void diffCreateIndexOnlyStoring() throws DdlDiffException {
    assertThat(
            DdlDiff.build(
                    "CREATE INDEX myindex ON mytable ( col1 ) STORING (col2, col3);",
                    "CREATE INDEX myindex ON mytable ( col1 ) STORING (col3, col4);")
                .generateDifferenceStatements(
                    ImmutableMap.of(
                        ALLOW_RECREATE_INDEXES_OPT,
                        false,
                        ALLOW_DROP_STATEMENTS_OPT,
                        false,
                        ALLOW_RECREATE_CONSTRAINTS_OPT,
                        false)))
        .containsExactly(
            "ALTER INDEX myindex DROP STORED COLUMN col2",
            "ALTER INDEX myindex ADD STORED COLUMN col4");
  }

  @Test
  public void diffCreateIndexNotOnlyStoringRecreates() throws DdlDiffException {
    assertThat(
            DdlDiff.build(
                    "CREATE UNIQUE INDEX myindex ON mytable ( col1 ) STORING (col2, col3);",
                    "CREATE INDEX myindex ON mytable ( col1 ) STORING (col3, col4);")
                .generateDifferenceStatements(
                    ImmutableMap.of(
                        ALLOW_RECREATE_INDEXES_OPT,
                        true,
                        ALLOW_DROP_STATEMENTS_OPT,
                        false,
                        ALLOW_RECREATE_CONSTRAINTS_OPT,
                        false)))
        .containsExactly(
            "DROP INDEX myindex",
            "CREATE INDEX myindex ON mytable ( col1 ASC ) STORING ( col3, col4 )");
  }

  @Test
  public void diffCreateIndexNotOnlyStoringThrows() throws DdlDiffException {
    getDiffCheckDdlDiffException(
        "CREATE UNIQUE INDEX myindex ON mytable ( col1 ) STORING (col2, col3);",
        "CREATE INDEX myindex ON mytable ( col1 ) STORING (col3, col4);",
        false,
        "At least one Index differs, and allowRecreateIndexes is not set");
  }

  private static void getDiffCheckDdlDiffException(
      String originalDdl, String newDdl, boolean allowDropStatements, String exceptionContains) {
    try {
      getDiff(originalDdl, newDdl, allowDropStatements);
      fail("Expected DdlDiffException not thrown.");
    } catch (DdlDiffException e) {
      assertThat(e.getMessage()).contains(exceptionContains);
    }
  }

  private static List<String> getDiff(
      String originalDdl, String newDdl, boolean allowDropStatements) throws DdlDiffException {
    return DdlDiff.build(originalDdl, newDdl)
        .generateDifferenceStatements(
            ImmutableMap.of(
                ALLOW_RECREATE_CONSTRAINTS_OPT,
                true,
                ALLOW_DROP_STATEMENTS_OPT,
                allowDropStatements,
                ALLOW_RECREATE_INDEXES_OPT,
                false));
  }

  @Test
  public void generateDifferences_dropTables() throws DdlDiffException {
    DdlDiff diff =
        DdlDiff.build(
            "Create table table1 (col1 int64) primary key (col1);"
                + "Create table table2 (col2 int64) primary key (col2);",
            "Create table table1 (col1 int64) primary key (col1);");

    assertThat(diff.generateDifferenceStatements(ImmutableMap.of(ALLOW_DROP_STATEMENTS_OPT, true)))
        .containsExactly("DROP TABLE table2");
    assertThat(diff.generateDifferenceStatements(ImmutableMap.of(ALLOW_DROP_STATEMENTS_OPT, false)))
        .isEmpty();
  }

  @Test
  public void generateDifferences_dropIndexes() throws DdlDiffException {
    DdlDiff diff =
        DdlDiff.build(
            "Create index index1 on table1 (col1 desc);"
                + "Create index index2 on table2 (col2 desc);",
            "Create index index1 on table1 (col1 desc);");

    assertThat(diff.generateDifferenceStatements(ImmutableMap.of(ALLOW_DROP_STATEMENTS_OPT, true)))
        .containsExactly("DROP INDEX index2");
    assertThat(diff.generateDifferenceStatements(ImmutableMap.of(ALLOW_DROP_STATEMENTS_OPT, false)))
        .isEmpty();
  }

  @Test
  public void generateDifferences_createLocalityGroup() throws DdlDiffException {
    DdlDiff diff = DdlDiff.build("", "CREATE LOCALITY GROUP lg OPTIONS (x=TRUE)");
    assertThat(diff.generateDifferenceStatements(ImmutableMap.of(ALLOW_DROP_STATEMENTS_OPT, true)))
        .containsExactly("CREATE LOCALITY GROUP lg OPTIONS (x=TRUE)");
  }

  @Test
  public void generateDifferences_dropLocalityGroup() throws DdlDiffException {
    DdlDiff diff = DdlDiff.build("CREATE LOCALITY GROUP lg", "");
    assertThat(diff.generateDifferenceStatements(ImmutableMap.of(ALLOW_DROP_STATEMENTS_OPT, true)))
        .containsExactly("DROP LOCALITY GROUP lg");
    assertThat(diff.generateDifferenceStatements(ImmutableMap.of(ALLOW_DROP_STATEMENTS_OPT, false)))
        .isEmpty();
  }

  @Test
  public void generateDifferences_alterLocalityGroupOptions() throws DdlDiffException {
    DdlDiff diff =
        DdlDiff.build(
            "CREATE LOCALITY GROUP lg OPTIONS (x=TRUE,y=FALSE)",
            "CREATE LOCALITY GROUP lg OPTIONS (x=NULL,y=TRUE,z=123)");
    assertThat(diff.generateDifferenceStatements(ImmutableMap.of(ALLOW_DROP_STATEMENTS_OPT, true)))
        .containsExactly("ALTER LOCALITY GROUP lg SET OPTIONS (x=NULL,y=TRUE,z=123)");
  }

  @Test
  public void alterTable_interleaveOnDeleteChange_generatesAlter() throws DdlDiffException {
    String original =
        "CREATE TABLE c (k INT64) PRIMARY KEY (k), INTERLEAVE IN PARENT p ON DELETE NO ACTION;";
    String updated = "CREATE TABLE c (k INT64) PRIMARY KEY (k), INTERLEAVE IN p ON DELETE CASCADE;";
    assertThat(getDiff(original, updated, true))
        .containsExactly("ALTER TABLE c SET INTERLEAVE IN p ON DELETE CASCADE");
  }

  @Test
  public void alterTable_interleaveParentChange_generatesAlter() throws DdlDiffException {
    String original =
        "CREATE TABLE c (k INT64) PRIMARY KEY (k), INTERLEAVE IN PARENT p1 ON DELETE CASCADE;";
    String updated =
        "CREATE TABLE c (k INT64) PRIMARY KEY (k), INTERLEAVE IN p2 ON DELETE CASCADE;";
    assertThat(getDiff(original, updated, true))
        .containsExactly("ALTER TABLE c SET INTERLEAVE IN p2 ON DELETE CASCADE");
  }

  @Test
  public void alterTable_interleaveAdded_generatesAlter() throws DdlDiffException {
    String original = "CREATE TABLE c (k INT64) PRIMARY KEY (k);";
    String updated =
        "CREATE TABLE c (k INT64) PRIMARY KEY (k), INTERLEAVE IN p ON DELETE NO ACTION;";
    assertThat(getDiff(original, updated, true))
        .containsExactly("ALTER TABLE c SET INTERLEAVE IN p ON DELETE NO ACTION");
  }

  @Test
  public void differentIndexesWithNoRecreate() {
    Map<String, Boolean> options =
        ImmutableMap.of(ALLOW_DROP_STATEMENTS_OPT, false, ALLOW_RECREATE_INDEXES_OPT, false);
    try {
      DdlDiff.build(
              "Create unique null_filtered index index1 on table1 (col1 desc)",
              "Create unique null_filtered index index1 on table1 (col1 asc)")
          .generateDifferenceStatements(options);
      fail("Expected exception not thrown");
    } catch (DdlDiffException e) {
      assertThat(e.getMessage()).containsMatch("one Index differs");
    }
    try {
      DdlDiff.build(
              "Create unique null_filtered index index1 on table1 (col1 desc)",
              "Create unique null_filtered index index1 on table1 (col2 desc)")
          .generateDifferenceStatements(options);
      fail("Expected exception not thrown");
    } catch (DdlDiffException e) {
      assertThat(e.getMessage()).containsMatch("one Index differs");
    }
    try {
      DdlDiff.build(
              "Create unique null_filtered index index1 on table1 (col1 desc)",
              "Create index index1 on table1 (col1 desc)")
          .generateDifferenceStatements(options);
      fail("Expected exception not thrown");
    } catch (DdlDiffException e) {
      assertThat(e.getMessage()).containsMatch("one Index differs");
    }
  }

  @Test
  public void differentIndexesWithRecreate() throws DdlDiffException {
    Map<String, Boolean> options =
        ImmutableMap.of(ALLOW_DROP_STATEMENTS_OPT, true, ALLOW_RECREATE_INDEXES_OPT, true);
    assertThat(
            DdlDiff.build(
                    "Create unique null_filtered index index1 on table1 (col1 desc)",
                    "Create unique null_filtered index index1 on table1 (col1 asc)")
                .generateDifferenceStatements(options))
        .isEqualTo(
            Arrays.asList(
                "DROP INDEX index1",
                "CREATE UNIQUE NULL_FILTERED INDEX index1 ON table1 ( col1 ASC )"));
    assertThat(
            DdlDiff.build(
                    "Create unique null_filtered index index1 on table1 ( col1 desc )",
                    "Create unique null_filtered index index1 on table1 ( col2 desc )")
                .generateDifferenceStatements(options))
        .isEqualTo(
            Arrays.asList(
                "DROP INDEX index1",
                "CREATE UNIQUE NULL_FILTERED INDEX index1 ON table1 ( col2 DESC )"));
    assertThat(
            DdlDiff.build(
                    "Create unique null_filtered index index1 on table1 ( col1 desc )",
                    "Create index index1 on table1 ( col1 desc )")
                .generateDifferenceStatements(options))
        .isEqualTo(
            Arrays.asList("DROP INDEX index1", "CREATE INDEX index1 ON table1 ( col1 DESC )"));
  }
}
