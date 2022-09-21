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
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.fail;

import com.google.cloud.solutions.spannerddl.parser.ASTcreate_table_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTddl_statement;
import com.google.common.collect.ImmutableMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.Test;

public class DdlDiffTest {

  @Test
  public void parseDDL() throws DdlDiffException {
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

    List<ASTddl_statement> result = DdlDiff.parseDDL(DDL);

    assertThat(result).hasSize(4);

    assertThat(result.get(0).toString())
        .isEqualTo("CREATE TABLE test1 (col1 INT64) PRIMARY KEY (col1 ASC)");
    assertThat(result.get(1).toString())
        .isEqualTo("CREATE TABLE test2 (col2 FLOAT64) PRIMARY KEY (col2 ASC)");
    assertThat(result.get(2).toString())
        .isEqualTo(
            "CREATE TABLE test3 (col3 STRING(MAX)) PRIMARY KEY (col3 ASC), "
                + "INTERLEAVE IN PARENT testparent ON DELETE NO ACTION");
    assertThat(result.get(3).toString()).isEqualTo("CREATE INDEX index1 ON table1 (col1 ASC)");
  }

  @Test
  public void parseDDLCreateTableSyntaxError() {
    parseDdlCheckDdlDiffException(
        "Create table test1 ( col1 int64 )", "Was expecting:\n\n\"primary\" ...");
  }

  @Test
  public void parseDDLCreateIndexSyntaxError() {
    parseDdlCheckDdlDiffException("Create index index1 on test1", "Was expecting:\n\n\"(\" ...");
  }

  private void parseDdlCheckDdlDiffException(String DDL, String exceptionContains) {
    try {
      DdlDiff.parseDDL(DDL);
      fail("Expected DdlDiffException not thrown.");
    } catch (DdlDiffException e) {
      assertThat(e.getMessage()).contains(exceptionContains);
    }
  }

  @Test
  public void parseCreateTable_anonForeignKey() throws DdlDiffException {
    try {
      DdlDiff.parseDDL(
          "create table test ("
              + "intcol int64 not null, "
              + "FOREIGN KEY(col1, col2) REFERENCES other_table(other_col1, other_col2)"
              + ")"
              + "primary key (intcol ASC)");
      fail("Expected exception not thrown");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).containsMatch("anonymous FOREIGN KEY constraints");
    }
  }

  @Test
  public void parseCreateTable_anonCheckConstraint() throws DdlDiffException {
    try {
      DdlDiff.parseDDL(
          "create table test ("
              + "intcol int64 not null, "
              + "check (intcol>1)"
              + ")"
              + "primary key (intcol ASC)");
      fail("Expected exception not thrown");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).containsMatch("anonymous CHECK constraints");
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseDDLNoAlterTableAlterColumn() throws DdlDiffException {
    String DDL = "alter table test1 alter column col1 int64 not null";
    DdlDiff.parseDDL(DDL);
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseDDLNoAlterTableAddColumn() throws DdlDiffException {
    String DDL = "alter table test1 add column col1 int64 not null";
    DdlDiff.parseDDL(DDL);
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseDDLNoAlterTableDropColumn() throws DdlDiffException {
    String DDL = "alter table test1 drop column col1";
    DdlDiff.parseDDL(DDL);
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseDDLNoAlterTableDropConstraint() throws DdlDiffException {
    String DDL = "alter table test1 drop constraint xxx";
    DdlDiff.parseDDL(DDL);
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseDDLNoAlterTableSetOnDelete() throws DdlDiffException {
    String DDL = "alter table test1 set on delete cascade";
    DdlDiff.parseDDL(DDL);
  }

  public void parseDDLAlterTableAddConstraint() throws DdlDiffException {
    String DDL = "alter table test1 add constraint XXX FOREIGN KEY (yyy) references zzz(xxx)";
    DdlDiff.parseDDL(DDL);
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseDDLNoAlterTableAddAnonConstraint() throws DdlDiffException {
    String DDL = "alter table test1 add FOREIGN KEY (yyy) references zzz(xxx)";
    DdlDiff.parseDDL(DDL);
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseDDLNoDropTable() throws DdlDiffException {
    String DDL = "drop table test1";
    DdlDiff.parseDDL(DDL);
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseDDLNoDropIndex() throws DdlDiffException {
    String DDL = "drop index test1";
    DdlDiff.parseDDL(DDL);
  }

  @Test
  public void generateAlterTable_AddColumn() throws DdlDiffException {
    // Add single row.
    assertThat(
            getTableDiff(
                "create table test1 (col1 int64) primary key (col1);",
                "create table test1 (col1 int64, col2 int64 not null) primary key (col1);",
                true))
        .containsExactly("ALTER TABLE test1 ADD COLUMN col2 INT64 NOT NULL");

    // Add multiple rows.
    assertThat(
            getTableDiff(
                "create table test1 (col1 int64) primary key (col1);",
                "create table test1 (col1 int64, col2 String(MAX), col3 Array<Bytes(100)> not null) "
                    + "primary key (col1);",
                true))
        .containsExactly(
            "ALTER TABLE test1 ADD COLUMN col2 STRING(MAX)",
            "ALTER TABLE test1 ADD COLUMN col3 ARRAY<BYTES(100)> NOT NULL");
  }

  @Test
  public void generateAlterTable_DropColumn() throws DdlDiffException {
    // Add single row.
    assertThat(
            getTableDiff(
                "create table test1 (col1 int64, col2 int64 not null) primary key (col1);",
                "create table test1 (col1 int64) primary key (col1);",
                true))
        .containsExactly("ALTER TABLE test1 DROP COLUMN col2");
    assertThat(
            getTableDiff(
                "create table test1 (col1 int64, col2 int64 not null) primary key (col1);",
                "create table test1 (col1 int64) primary key (col1);",
                false))
        .isEmpty();

    // Add multiple rows.
    assertThat(
            getTableDiff(
                "create table test1 (col1 int64, col2 String(MAX), col3 Array<Bytes(100)> not null) "
                    + "primary key (col1);",
                "create table test1 (col1 int64) primary key (col1);",
                true))
        .containsExactly(
            "ALTER TABLE test1 DROP COLUMN col2", "ALTER TABLE test1 DROP COLUMN col3");
    assertThat(
            getTableDiff(
                "create table test1 (col1 int64, col2 String(MAX), col3 Array<Bytes(100)> not null) "
                    + "primary key (col1);",
                "create table test1 (col1 int64) primary key (col1);",
                false))
        .isEmpty();
  }

  @Test
  public void generateAlterTable_AlterColumnAttrs() throws DdlDiffException {
    // Not null added and removed
    assertThat(
            getTableDiff(
                "create table test1 (col1 int64, col2 int64 not null) primary key (col1);",
                "create table test1 (col1 int64 not null, col2 int64) primary key (col1);",
                true))
        .containsExactly(
            "ALTER TABLE test1 ALTER COLUMN col1 INT64 NOT NULL",
            "ALTER TABLE test1 ALTER COLUMN col2 INT64");

    // Options added and removed
    assertThat(
            getTableDiff(
                "create table test1 (col1 timestamp options(allow_commit_timestamp=true), col2 timestamp) "
                    + "primary key (col1);",
                "create table test1 (col1 timestamp, col2 timestamp options(allow_commit_timestamp=true)) "
                    + "primary key (col1);",
                true))
        .containsExactly(
            "ALTER TABLE test1 ALTER COLUMN col1 SET OPTIONS (allow_commit_timestamp=NULL)",
            "ALTER TABLE test1 ALTER COLUMN col2 SET OPTIONS (allow_commit_timestamp=TRUE)");
  }

  @Test
  public void generateAlterTable_AlterColumnSize() throws DdlDiffException {
    // changes string()  size and array<bytes()> size
    assertThat(
            getTableDiff(
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
    getTableDiffCheckDdlDiffException(
        "create table test1 (col1 ARRAY<BYTES(100)>) primary key (col1);",
        "create table test1 (col1 ARRAY<ARRAY<BYTES(100)>>) primary key (col1);",
        true,
        "Cannot change type of table test1 column col1");

    // change Array sized type
    getTableDiffCheckDdlDiffException(
        "create table test1 (col1 ARRAY<BYTES(100)>) primary key (col1);",
        "create table test1 (col1 ARRAY<STRING(100)>) primary key (col1);",
        true,
        "Cannot change type of table test1 column col1");

    // change Array type
    getTableDiffCheckDdlDiffException(
        "create table test1 (col1 ARRAY<int64>) primary key (col1);",
        "create table test1 (col1 ARRAY<float64>) primary key (col1);",
        true,
        "Cannot change type of table test1 column col1");

    // change sized type
    getTableDiffCheckDdlDiffException(
        "create table test1 (col1 BYTES(100)) primary key (col1);",
        "create table test1 (col1 STRING(100)) primary key (col1);",
        true,
        "Cannot change type of table test1 column col1");

    // change type
    getTableDiffCheckDdlDiffException(
        "create table test1 (col1 int64) primary key (col1);",
        "create table test1 (col1 float64) primary key (col1);",
        true,
        "Cannot change type of table test1 column col1");
  }

  @Test
  public void generateAlterTable_changeKey() {
    // change key col
    getTableDiffCheckDdlDiffException(
        "create table test1 (col1 int64, col2 int64) primary key (col1);",
        "create table test1 (col1 int64, col2 int64) primary key (col2);",
        true,
        "Cannot change primary key of table test1");

    // add key col
    getTableDiffCheckDdlDiffException(
        "create table test1 (col1 int64, col2 int64) primary key (col1);",
        "create table test1 (col1 int64, col2 int64) primary key (col1, col2);",
        true,
        "Cannot change primary key of table test1");

    // remove key col
    getTableDiffCheckDdlDiffException(
        "create table test1 (col1 int64, col2 int64) primary key (col1, col2);",
        "create table test1 (col1 int64, col2 int64) primary key (col1);",
        true,
        "Cannot change primary key of table test1");

    // change order
    getTableDiffCheckDdlDiffException(
        "create table test1 (col1 int64, col2 int64) primary key (col1);",
        "create table test1 (col1 int64, col2 int64) primary key (col1 DESC);",
        true,
        "Cannot change primary key of table test1");
  }

  @Test
  public void generateAlterTable_changeInterleaving() throws DdlDiffException {
    // remove interleave
    getTableDiffCheckDdlDiffException(
        "create table test1 (col1 int64, col2 int64) "
            + "primary key (col1), interleave in parent testparent;",
        "create table test1 (col1 int64, col2 int64) primary key (col1)",
        true,
        "Cannot change interleaving on table test1");

    // remove different parent
    getTableDiffCheckDdlDiffException(
        "create table test1 (col1 int64, col2 int64) "
            + "primary key (col1), interleave in parent testparent;",
        "create table test1 (col1 int64, col2 int64) "
            + "primary key (col1), interleave in parent otherparent",
        true,
        "Cannot change interleaved parent of table test1");

    // change on delete
    assertThat(
            getTableDiff(
                "create table test1 (col1 int64, col2 int64) "
                    + "primary key (col1), interleave in parent testparent on delete cascade;",
                "create table test1 (col1 int64, col2 int64) "
                    + "primary key (col1), interleave in parent testparent",
                true))
        .containsExactly("ALTER TABLE test1 SET ON DELETE NO ACTION");
    // change on delete
    assertThat(
            getTableDiff(
                "create table test1 (col1 int64, col2 int64) "
                    + "primary key (col1), interleave in parent testparent on delete NO ACTION;",
                "create table test1 (col1 int64, col2 int64) "
                    + "primary key (col1), interleave in parent testparent on delete cascade",
                true))
        .containsExactly("ALTER TABLE test1 SET ON DELETE CASCADE");
  }

  @Test
  public void generateAlterTable_changeGenerationClause() throws DdlDiffException {
    // remove interleave
    getTableDiffCheckDdlDiffException(
        "create table test1 (col1 int64, col2 int64, col3 int64 as ( col1*col2 ) stored) primary key (col1)",
        "create table test1 (col1 int64, col2 int64, col3 int64 as ( col1/col2 ) stored) primary key (col1)",
        true,
        "Cannot change generation clause of table test1 column col3 from  AS ");

    // add generation
    getTableDiffCheckDdlDiffException(
        "create table test1 (col1 int64, col2 int64, col3 int64) primary key (col1)",
        "create table test1 (col1 int64, col2 int64, col3 int64 as ( col1*col2 ) stored) primary key (col1)",
        true,
        "Cannot change generation clause of table test1 column col3 from null ");

    // remove generation
    getTableDiffCheckDdlDiffException(
        "create table test1 (col1 int64, col2 int64, col3 int64 as ( col1*col2 ) stored) primary key (col1)",
        "create table test1 (col1 int64, col2 int64, col3 int64) primary key (col1)",
        true,
        "Cannot change generation clause of table test1 column col3 from  AS");
  }

  @Test
  public void generateAlterTable_alterStatementOrdering() throws DdlDiffException {
    assertThat(
            getTableDiff(
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
            "ALTER TABLE test1 SET ON DELETE CASCADE",
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

  private static void getTableDiffCheckDdlDiffException(
      String originalDdl, String newDdl, boolean allowDropStatements, String exceptionContains) {
    try {
      getTableDiff(originalDdl, newDdl, allowDropStatements);
      fail("Expected DdlDiffException not thrown.");
    } catch (DdlDiffException e) {
      assertThat(e.getMessage()).contains(exceptionContains);
    }
  }

  private static List<String> getTableDiff(
      String originalDdl, String newDdl, boolean allowDropStatements) throws DdlDiffException {
    List<ASTddl_statement> ddl1 = DdlDiff.parseDDL(originalDdl);
    List<ASTddl_statement> ddl2 = DdlDiff.parseDDL(newDdl);
    assertThat(ddl1).hasSize(1);
    assertThat(ddl2).hasSize(1);
    return DdlDiff.generateAlterTableStatements(
        (ASTcreate_table_statement) ddl1.get(0).jjtGetChild(0),
        (ASTcreate_table_statement) ddl2.get(0).jjtGetChild(0),
        ImmutableMap.of(
            ALLOW_RECREATE_CONSTRAINTS_OPT, true, ALLOW_DROP_STATEMENTS_OPT, allowDropStatements));
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
                "CREATE UNIQUE NULL_FILTERED INDEX index1 ON table1 (col1 ASC)"));
    assertThat(
            DdlDiff.build(
                    "Create unique null_filtered index index1 on table1 (col1 desc)",
                    "Create unique null_filtered index index1 on table1 (col2 desc)")
                .generateDifferenceStatements(options))
        .isEqualTo(
            Arrays.asList(
                "DROP INDEX index1",
                "CREATE UNIQUE NULL_FILTERED INDEX index1 ON table1 (col2 DESC)"));
    assertThat(
            DdlDiff.build(
                    "Create unique null_filtered index index1 on table1 (col1 desc)",
                    "Create index index1 on table1 (col1 desc)")
                .generateDifferenceStatements(options))
        .isEqualTo(Arrays.asList("DROP INDEX index1", "CREATE INDEX index1 ON table1 (col1 DESC)"));
  }

  @Test
  public void compareDddTextFiles() throws IOException {
    // Uses 3 files: 2 containing DDL segments to run diffs on, 1 with the expected results
    // if allowRecreateIndexes and allowDropStatements are set.

    LinkedHashMap<String, String> originalSegments = readDdlSegmentsFromFile("originalDdl.txt");
    LinkedHashMap<String, String> newSegments = readDdlSegmentsFromFile("newDdl.txt");
    LinkedHashMap<String, String> expectedOutputs = readDdlSegmentsFromFile("expectedDdlDiff.txt");

    Iterator<Map.Entry<String, String>> originalSegmentIt = originalSegments.entrySet().iterator();
    Iterator<Map.Entry<String, String>> newSegmentIt = newSegments.entrySet().iterator();
    Iterator<Map.Entry<String, String>> expectedOutputIt = expectedOutputs.entrySet().iterator();

    String segmentName = null;
    try {
      while (originalSegmentIt.hasNext()) {
        Map.Entry<String, String> originalSegment = originalSegmentIt.next();
        segmentName = originalSegment.getKey();
        Map.Entry<String, String> newSegment = newSegmentIt.next();
        Map.Entry<String, String> expectedOutput = expectedOutputIt.next();

        // verify segment name order for sanity.
        assertWithMessage("mismatched section names in newDdl.txt")
            .that(newSegment.getKey())
            .isEqualTo(segmentName);
        assertWithMessage("mismatched section names in expectedDdlDiff.txt")
            .that(expectedOutput.getKey())
            .isEqualTo(segmentName);
        List<String> expectedDiff = Arrays.asList(expectedOutput.getValue().split("\n"));

        DdlDiff ddlDiff = DdlDiff.build(originalSegment.getValue(), newSegment.getValue());
        // Run diff with allowRecreateIndexes and allowDropStatements
        List<String> diff =
            ddlDiff.generateDifferenceStatements(
                ImmutableMap.of(
                    ALLOW_RECREATE_INDEXES_OPT,
                    true,
                    ALLOW_DROP_STATEMENTS_OPT,
                    true,
                    ALLOW_RECREATE_CONSTRAINTS_OPT,
                    true));
        // check expected results.
        assertWithMessage("Mismatch for section " + segmentName).that(diff).isEqualTo(expectedDiff);

        // TEST PART 2: with allowDropStatements=false

        // build an expectedResults without any column or table drops.
        List<String> expectedDiffNoDrops =
            expectedDiff.stream()
                .filter(statement -> !statement.matches(".*DROP (TABLE|COLUMN).*"))
                .collect(Collectors.toCollection(LinkedList::new));

        // remove any drop indexes from the expectedResults if they do not have an equivalent
        // CREATE statement. This is because we are allowing recreation of indexes, but not allowing
        // dropping of removed indexes.
        for (String statement : expectedDiff) {
          if (statement.startsWith("DROP INDEX ")) {
            String indexName = statement.split(" ")[2];
            // see if there is a matching create statement
            Pattern p = Pattern.compile("CREATE .*INDEX " + indexName + " ");
            if (expectedDiffNoDrops.stream().noneMatch(s -> p.matcher(s).find())) {
              expectedDiffNoDrops.remove(statement);
            }
          }
        }

        diff =
            ddlDiff.generateDifferenceStatements(
                ImmutableMap.of(
                    ALLOW_RECREATE_INDEXES_OPT,
                    true,
                    ALLOW_DROP_STATEMENTS_OPT,
                    false,
                    ALLOW_RECREATE_CONSTRAINTS_OPT,
                    true));
        // check expected results.
        assertWithMessage("Mismatch for section (noDrops)" + segmentName)
            .that(diff)
            .isEqualTo(expectedDiffNoDrops);
      }
    } catch (Throwable e) {
      e.printStackTrace(System.err);
      fail("Unexpected exception when processing segment " + segmentName + ": " + e);
    }
  }

  private LinkedHashMap<String, String> readDdlSegmentsFromFile(String filename)
      throws IOException {
    File file = new File("src/test/resources/" + filename).getAbsoluteFile();
    LinkedHashMap<String, String> output = new LinkedHashMap<>();

    try (BufferedReader in = new BufferedReader(new FileReader(file))) {

      String sectionName = null;
      StringBuilder section = new StringBuilder();
      String line;
      while (null != (line = in.readLine())) {
        line = line.replaceAll("#.*", "").trim();
        if (line.isEmpty()) {
          continue;
        }
        if (line.startsWith("==")) {
          // new section
          if (sectionName != null) {
            // add closed section.
            output.put(sectionName, section.toString());
          }
          sectionName = line;
          section = new StringBuilder();
          continue;
        } else if (sectionName == null) {
          throw new IOException("no section name before first statement");
        }
        section.append(line).append('\n');
      }
      if (section.length() > 0) {
        output.put(sectionName, section.toString());
      }
      return output;
    }
  }
}
