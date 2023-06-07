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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import java.io.StringReader;
import org.junit.Test;

public class DDLParserTest {

  @Test
  public void parseCreateTable() throws ParseException {

    ASTcreate_table_statement statement =
        (ASTcreate_table_statement)
            parse(
                    "create table test ("
                        + "boolcol bool, "
                        + "intcol int64 not null, "
                        + "floatcol float64, "
                        + "sizedstring string(55), "
                        + "maxstring string(max), "
                        + "sizedbytes bytes(55), "
                        + "maxbytes bytes(max), "
                        + "datecol date, "
                        + "timestampcol timestamp options (allow_commit_timestamp = true), "
                        + "intarray array<int64>, "
                        + "numericcol numeric,"
                        + "jsoncol json,"
                        + "generatedcol string(max) as (sizedstring+ strstr(maxstring,strpos(maxstring,'xxx'),length(maxstring)) +2.0) STORED, "
                        + "constraint fk_col_remote FOREIGN KEY(col1, col2) REFERENCES other_table(other_col1, other_col2), "
                        + "constraint check_some_value CHECK ((length(sizedstring)>100 or sizedstring= \"xxx\") AND boolcol= true and intcol > -123.4 and numericcol < 1.5)"
                        + ") "
                        + "primary key (intcol ASC, floatcol desc, boolcol), "
                        + "interleave in parent other_table on delete cascade,"
                        + "row deletion policy (OLDER_THAN(timestampcol, INTERVAL 10 DAY))")
                .jjtGetChild(0);

    assertThat(statement.toString())
        .isEqualTo(
            "CREATE TABLE test ("
                + "boolcol BOOL, "
                + "intcol INT64 NOT NULL, "
                + "floatcol FLOAT64, "
                + "sizedstring STRING(55), "
                + "maxstring STRING(MAX), "
                + "sizedbytes BYTES(55), "
                + "maxbytes BYTES(MAX), "
                + "datecol DATE, "
                + "timestampcol TIMESTAMP OPTIONS (allow_commit_timestamp=TRUE), "
                + "intarray ARRAY<INT64>, "
                + "numericcol NUMERIC, "
                + "jsoncol JSON, "
                + "generatedcol STRING(MAX)  AS ( sizedstring + strstr ( maxstring, strpos ( maxstring, 'xxx' ), length ( maxstring ) ) + 2.0 ) STORED, "
                + "CONSTRAINT fk_col_remote FOREIGN KEY (col1, col2) REFERENCES other_table (other_col1, other_col2), "
                + "CONSTRAINT check_some_value CHECK (( length ( sizedstring ) > 100 OR sizedstring = \"xxx\" ) AND boolcol = TRUE AND intcol > -123.4 AND numericcol < 1.5)"
                + ") PRIMARY KEY (intcol ASC, floatcol DESC, boolcol ASC), "
                + "INTERLEAVE IN PARENT other_table ON DELETE CASCADE, "
                + "ROW DELETION POLICY (OLDER_THAN ( timestampcol, INTERVAL 10 DAY ))");

    // Test re-parse of toString output.
    ASTcreate_table_statement statement2 =
        (ASTcreate_table_statement) parseAndVerifyToString(statement.toString()).jjtGetChild(0);
    assertThat(statement).isEqualTo(statement2);
  }

  @Test
  public void parseCreateIndex() throws ParseException {

    ASTcreate_index_statement statement =
        (ASTcreate_index_statement)
            parse(
                    "create unique null_filtered index testindex on testtable("
                        + "col1, "
                        + "col2 desc, "
                        + "col3 asc) "
                        + "storing ( "
                        + "col4, "
                        + "col5, "
                        + "col6), "
                        + "interleave in other_table")
                .jjtGetChild(0);

    assertThat(statement.toString())
        .isEqualTo(
            "CREATE UNIQUE NULL_FILTERED INDEX testindex ON testtable "
                + "(col1 ASC, col2 DESC, col3 ASC) "
                + "STORING (col4, col5, col6), "
                + "INTERLEAVE IN other_table");

    // Test re-parse of toString output.
    ASTcreate_index_statement statement2 =
        (ASTcreate_index_statement) parseAndVerifyToString(statement.toString()).jjtGetChild(0);
    assertThat(statement).isEqualTo(statement2);
  }

  @Test
  public void parseDDLCreateTableSyntaxError() {
    parseCheckingException(
        "Create table test1 ( col1 int64 )", "Was expecting:\n\n\"primary\" ...");
  }

  @Test
  public void parseDDLCreateIndexSyntaxError() {
    parseCheckingException("Create index index1 on test1", "Was expecting one of:\n\n\"(\" ...");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void parseDDLNoDropTable() throws ParseException {
    parseAndVerifyToString("drop table test1");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void parseDDLNoDropIndex() throws ParseException {
    parseAndVerifyToString("drop index test1");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void parseDDLNoDropChangeStream() throws ParseException {
    parseAndVerifyToString("drop change stream test1");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void parseDDLNoCreateChangeStream() throws ParseException {
    parseAndVerifyToString("Create change stream test1 for test2");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void parseDDLNoCreateView() throws ParseException {
    parseAndVerifyToString("CREATE  VIEW test1 SQL SECURITY INVOKER AS SELECT * from test2");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void parseDDLNoCreateorReplaceView() throws ParseException {
    parseAndVerifyToString(
        "CREATE OR REPLACE VIEW test1 SQL SECURITY INVOKER AS SELECT * from test2");
  }

  @Test
  public void parseDDLNoAlterTableRowDeletionPolicy() throws ParseException {
    parseAndVerifyToString(
        "ALTER TABLE Albums "
            + "ADD ROW DELETION POLICY (OLDER_THAN ( timestamp_column, INTERVAL 1 DAY ))");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void parseDDLNoAlterTableReplaceRowDeletionPolicy() throws ParseException {
    String DDL =
        "ALTER TABLE Albums "
            + "REPLACE ROW DELETION POLICY (OLDER_THAN(timestamp_column, INTERVAL 1 DAY))";
    parseAndVerifyToString(DDL);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void parseDDLNoDropRowDeletionPolicy() throws ParseException {
    parseAndVerifyToString("ALTER TABLE Albums DROP ROW DELETION POLICY;");
  }

  private static void parseCheckingException(String ddlStatement, String exceptionContains) {
    try {
      parseAndVerifyToString(ddlStatement);
      fail("Expected ParseException not thrown.");
    } catch (ParseException e) {
      assertThat(e.getMessage()).contains(exceptionContains);
    }
  }

  private static ASTddl_statement parse(String DDLStatement) throws ParseException {
    try (StringReader in = new StringReader(DDLStatement)) {
      DdlParser parser = new DdlParser(in);
      parser.ddl_statement();
      return (ASTddl_statement) parser.jjtree.rootNode();
    }
  }

  private static ASTddl_statement parseAndVerifyToString(String DDLStatement)
      throws ParseException {
    ASTddl_statement node = parse(DDLStatement);
    assertThat(node.toString()).isEqualTo(DDLStatement); // validates statement regeneration
    return node;
  }
}
