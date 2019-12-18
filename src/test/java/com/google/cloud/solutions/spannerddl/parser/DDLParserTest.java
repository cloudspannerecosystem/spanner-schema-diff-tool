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

import java.io.StringReader;
import org.junit.Test;

public class DDLParserTest {

  @Test
  public void parseCreateTable() throws ParseException {

    ASTcreate_table_statement statement = (ASTcreate_table_statement) parse(
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
            + "intarray array<int64>) "
            + "primary key (intcol ASC, floatcol desc, boolcol), "
            + "interleave in parent other_table on delete cascade ")
        .jjtGetChild(0);

    assertThat(statement.toString()).isEqualTo(
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
            + "intarray ARRAY<INT64>) "
            + "PRIMARY KEY (intcol ASC, floatcol DESC, boolcol ASC), "
            + "INTERLEAVE IN PARENT other_table ON DELETE CASCADE");

    // Test re-parse of toString output.
    ASTcreate_table_statement statement2 = (ASTcreate_table_statement) parse(statement.toString())
        .jjtGetChild(0);
    assertThat(statement).isEqualTo(statement2);
  }


  @Test
  public void parseCreateIndex() throws ParseException {

    ASTcreate_index_statement statement = (ASTcreate_index_statement) parse(
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

    assertThat(statement.toString()).isEqualTo(
        "CREATE UNIQUE NULL_FILTERED INDEX testindex ON testtable "
            + "(col1 ASC, col2 DESC, col3 ASC) "
            + "STORING (col4, col5, col6), "
            + "INTERLEAVE IN other_table");

    // Test re-parse of toString output.
    ASTcreate_index_statement statement2 = (ASTcreate_index_statement) parse(statement.toString())
        .jjtGetChild(0);
    assertThat(statement).isEqualTo(statement2);
  }

  private static ASTddl_statement parse(String DDLStatement) throws ParseException {
    try (StringReader in = new StringReader(DDLStatement)) {
      DdlParser parser = new DdlParser(in);
      parser.ddl_statement();
      return (ASTddl_statement) parser.jjtree.rootNode();
    }
  }


}