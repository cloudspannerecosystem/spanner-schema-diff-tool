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

/** Abstract Syntax Tree parser object for "create_index_statement" token */
import com.google.cloud.solutions.spannerddl.diff.ASTTreeUtils;

public class ASTcreate_index_statement extends SimpleNode
    implements Comparable<ASTcreate_index_statement> {

  public ASTcreate_index_statement(int id) {
    super(id);
  }

  public ASTcreate_index_statement(DdlParser p, int id) {
    super(p, id);
  }

  public String getIndexName() {
    return ASTTreeUtils.getChildByType(children, ASTname.class).toString();
  }

  @Override
  public String toString() {
    StringBuilder ret = new StringBuilder("CREATE");
    for (Node child : children) {
      switch (child.getId()) {
        case DdlParserTreeConstants.JJTUNIQUE_INDEX:
        case DdlParserTreeConstants.JJTNULL_FILTERED:
        case DdlParserTreeConstants.JJTCOLUMNS:
        case DdlParserTreeConstants.JJTSTORED_COLUMN_LIST:
          ret.append(" ");
          ret.append(child);
          break;
        case DdlParserTreeConstants.JJTNAME:
          ret.append(" INDEX ");
          ret.append(child);
          break;
        case DdlParserTreeConstants.JJTTABLE:
          ret.append(" ON ");
          ret.append(child);
          break;
        case DdlParserTreeConstants.JJTINDEX_INTERLEAVE_CLAUSE:
          ret.append(", ");
          ret.append(child);
          break;
        default:
          throw new IllegalArgumentException("Unknown node ID " + child.getClass());
      }
    }
    return ret.toString();
  }

  @Override
  public int compareTo(ASTcreate_index_statement other) {
    return this.getIndexName().compareTo(other.getIndexName());
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof ASTcreate_index_statement) {
      // lazy: compare text rendering.
      return this.toString().equals(other.toString());
    }
    return false;
  }
}
