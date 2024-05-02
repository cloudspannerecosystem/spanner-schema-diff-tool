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
import static org.junit.Assert.assertThrows;

import java.io.StringReader;
import org.junit.Test;

public class DDLParserTest {

  @Test
  public void parseCreateTable() throws ParseException {

    ASTcreate_table_statement statement =
        (ASTcreate_table_statement)
            parse(
                    "create table test.test (boolcol bool, intcol int64 not null, "
                        + " float32col float32, floatcol float64,"
                        + " `sizedstring` string(55), maxstring string(max) NOT NULL DEFAULT"
                        + " (\"prefix\" || sizedstring || \"suffix\"), sizedbytes bytes(55),"
                        + " maxbytes bytes(max), datecol date, timestampcol timestamp options"
                        + " (allow_commit_timestamp = true), intarray array<int64>, numericcol"
                        + " numeric,jsoncol json, pgcolumn pg.something, generatedcol string(max)"
                        + " as (sizedstring+"
                        + " strstr(maxstring,strpos(maxstring,'xxx'),length(maxstring)) +2.0)"
                        + " STORED, constraint fk_col_remote FOREIGN KEY(col1, col2) REFERENCES"
                        + " test.other_table(other_col1, other_col2) on delete cascade, constraint"
                        + " fk_col_remote2 FOREIGN KEY(col1) REFERENCES"
                        + " test.other_table(other_col1) on delete no action, constraint"
                        + " check_some_value CHECK ((length(sizedstring)>100 or sizedstring="
                        + " \"xxx\") AND boolcol= true and intcol > -123.4 and numericcol < 1.5))"
                        + " primary key (intcol ASC, floatcol desc, boolcol), interleave in parent"
                        + " `other_table` on delete cascade,row deletion policy"
                        + " (OLDER_THAN(timestampcol, INTERVAL 10 DAY))")
                .jjtGetChild(0);

    assertThat(statement.toString())
        .isEqualTo(
            ("CREATE TABLE test.test \n"
                    + "(\n"
                    + "  boolcol BOOL, \n"
                    + "  intcol INT64 NOT NULL, \n"
                    + "  float32col FLOAT32, \n"
                    + "  floatcol FLOAT64, \n"
                    + "  `sizedstring` STRING(55), \n"
                    + "  maxstring STRING(MAX) NOT NULL DEFAULT (\"prefix\" || sizedstring ||\n"
                    + "     \"suffix\"), \n"
                    + "  sizedbytes BYTES(55), \n"
                    + "  maxbytes BYTES(MAX), \n"
                    + "  datecol DATE, \n"
                    + "  timestampcol TIMESTAMP OPTIONS (allow_commit_timestamp=TRUE), \n"
                    + "  intarray ARRAY<INT64>, \n"
                    + "  numericcol NUMERIC, \n"
                    + "  jsoncol JSON, \n"
                    + "  pgcolumn PG.SOMETHING, \n"
                    + "  generatedcol STRING(MAX) AS ( sizedstring + strstr ( maxstring, strpos (\n"
                    + "     maxstring, 'xxx' ), length ( maxstring ) ) + 2.0 ) STORED, \n"
                    + "  CONSTRAINT fk_col_remote FOREIGN KEY ( col1, col2 ) REFERENCES \n"
                    + "     test.other_table ( other_col1, other_col2 ) ON DELETE CASCADE, \n"
                    + "  CONSTRAINT fk_col_remote2 FOREIGN KEY ( col1 ) REFERENCES test.other_table\n"
                    + "     ( other_col1 ) ON DELETE NO ACTION, \n"
                    + "  CONSTRAINT check_some_value CHECK (( length ( sizedstring ) > 100 or \n"
                    + "     sizedstring = \"xxx\" ) AND boolcol = true and intcol > -123.4 and \n"
                    + "     numericcol < 1.5)\n"
                    + ")\n"
                    + "PRIMARY KEY (intcol ASC, floatcol DESC, boolcol ASC), \n"
                    + "INTERLEAVE IN PARENT `other_table` ON DELETE CASCADE, \n"
                    + "ROW DELETION POLICY (OLDER_THAN ( timestampcol, INTERVAL 10 DAY ))")
                // remove extra spaces left in expected output for readability
                .replaceAll("\\s+", " "));

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
                + "( col1 ASC, col2 DESC, col3 ASC ) "
                + "STORING ( col4, col5, col6 ) , "
                + "INTERLEAVE IN other_table");

    // Test re-parse of toString output.
    ASTcreate_index_statement statement2 =
        (ASTcreate_index_statement) parseAndVerifyToString(statement.toString()).jjtGetChild(0);
    assertThat(statement).isEqualTo(statement2);
  }

  @Test
  public void parseDDLCreateTableSyntaxError() {
    parseCheckingParseException(
        "Create table test1 ( col1 int64 )", "Was expecting:\n\n\"primary\" ...");
  }

  @Test
  public void parseDDLCreateIndexSyntaxError() {
    parseCheckingParseException(
        "Create index index1 on test1", "Was expecting one of:\n\n\"(\" ...");
  }

  @Test
  public void parseDdlOnDeleteDefaultNoAction() throws ParseException {
    ASTcreate_table_statement statement =
        (ASTcreate_table_statement)
            parse(
                    "CREATE TABLE test_table \n"
                        + "(intcol INT64, \n"
                        + "CONSTRAINT fk_other1 FOREIGN KEY ( intcol ) REFERENCES othertable ("
                        + " othercol )) \n"
                        + "PRIMARY KEY (intcol ASC)")
                .jjtGetChild(0);
    assertThat(statement.toString())
        .isEqualTo(
            ("CREATE TABLE test_table (\n"
                    + "intcol INT64, \n"
                    + "CONSTRAINT fk_other1 FOREIGN KEY ( intcol ) REFERENCES othertable ("
                    + " othercol ) ON DELETE NO ACTION\n"
                    + ") PRIMARY KEY (intcol ASC)")
                .replaceAll("\\s+", " "));
  }

  @Test
  public void doNotNormalizeSQLExpressions() throws ParseException {

    ASTcreate_table_statement statement =
        (ASTcreate_table_statement)
            parse(
                    "create table myTest (\n"
                        + "key int64,\n"
                        + "value int64 AS ( key *100 ) STORED,\n"
                        + "constraint ck_key CHECK (key > 0))\n"
                        + "primary key (key)")
                .jjtGetChild(0);

    assertThat(statement.toString())
        .isEqualTo(
            "CREATE TABLE myTest ( key INT64, value INT64 AS ( key * 100 ) STORED, "
                + "CONSTRAINT ck_key CHECK (key > 0) ) PRIMARY KEY (key ASC)");

    // Test re-parse of toString output.
    ASTcreate_table_statement statement2 =
        (ASTcreate_table_statement) parseAndVerifyToString(statement.toString()).jjtGetChild(0);
    assertThat(statement).isEqualTo(statement2);
  }

  private static void parseCheckingParseException(String ddlStatement, String exceptionContains) {
    ParseException e =
        assertThrows(ParseException.class, () -> parseAndVerifyToString(ddlStatement));
    assertThat(e.getMessage()).contains(exceptionContains);
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
