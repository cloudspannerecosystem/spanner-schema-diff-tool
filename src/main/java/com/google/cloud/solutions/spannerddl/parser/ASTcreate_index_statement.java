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

import static com.google.cloud.solutions.spannerddl.diff.AstTreeUtils.getChildByType;
import static com.google.cloud.solutions.spannerddl.diff.AstTreeUtils.getOptionalChildByType;

import com.google.cloud.solutions.spannerddl.diff.AstTreeUtils;
import com.google.common.base.Joiner;

public class ASTcreate_index_statement extends SimpleNode
    implements Comparable<ASTcreate_index_statement> {

  public ASTcreate_index_statement(int id) {
    super(id);
  }

  public ASTcreate_index_statement(DdlParser p, int id) {
    super(p, id);
  }

  public String getIndexName() {
    return AstTreeUtils.getChildByType(children, ASTname.class).toString();
  }

  @Override
  public String toString() {
    return toStringOptionalExistClause(true);
  }

  /** Create string version, optionally including the IF NOT EXISTS clause */
  public String toStringOptionalExistClause(boolean includeExists) {
    validateChildren();
    ASTindex_interleave_clause interleave =
        getOptionalChildByType(children, ASTindex_interleave_clause.class);

    return Joiner.on(" ")
        .skipNulls()
        .join(
            "CREATE",
            getOptionalChildByType(children, ASTunique_index.class),
            getOptionalChildByType(children, ASTnull_filtered.class),
            "INDEX",
            (includeExists ? getOptionalChildByType(children, ASTif_not_exists.class) : null),
            getIndexName(),
            "ON",
            getChildByType(children, ASTtable.class),
            getChildByType(children, ASTcolumns.class),
            getOptionalChildByType(children, ASTstored_column_list.class),
            (interleave != null ? "," : null),
            interleave);
  }

  private void validateChildren() {
    for (Node child : children) {
      switch (child.getId()) {
        case DdlParserTreeConstants.JJTUNIQUE_INDEX:
        case DdlParserTreeConstants.JJTNULL_FILTERED:
        case DdlParserTreeConstants.JJTCOLUMNS:
        case DdlParserTreeConstants.JJTSTORED_COLUMN_LIST:
        case DdlParserTreeConstants.JJTIF_NOT_EXISTS:
        case DdlParserTreeConstants.JJTNAME:
        case DdlParserTreeConstants.JJTTABLE:
        case DdlParserTreeConstants.JJTINDEX_INTERLEAVE_CLAUSE:
          break;
        default:
          throw new IllegalArgumentException("Unknown child node " + child.getClass());
      }
    }
  }

  @Override
  public int compareTo(ASTcreate_index_statement other) {
    return this.getIndexName().compareTo(other.getIndexName());
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof ASTcreate_index_statement) {
      // lazy: compare text rendering, but don't take into account IF NOT EXISTS statements
      return this.toStringOptionalExistClause(false)
          .equals(((ASTcreate_index_statement) other).toStringOptionalExistClause(false));
    }
    return false;
  }

  @Override
  public int hashCode() {
    return toStringOptionalExistClause(false).hashCode();
  }
}
