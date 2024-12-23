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
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ASTcreate_index_statement extends SimpleNode
    implements Comparable<ASTcreate_index_statement> {

  public ASTcreate_index_statement(int id) {
    super(id);
  }

  public ASTcreate_index_statement(DdlParser p, int id) {
    super(p, id);
  }

  public String getIndexName() {
    return AstTreeUtils.tokensToString(AstTreeUtils.getChildByType(children, ASTname.class), false);
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

  /**
   * Gets the Index definition without the Stored column clause (and any EXISTS clause).
   *
   * <p>Used for comparing indexes for compatible changes.
   */
  public String getDefinitionWithoutStoring() {
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
            getIndexName(),
            "ON",
            getChildByType(children, ASTtable.class),
            getChildByType(children, ASTcolumns.class),
            (interleave != null ? "," : null),
            interleave);
  }

  private void validateChildren() {
    AstTreeUtils.validateChildrenClasses(
        children,
        ImmutableSet.of(
            ASTunique_index.class,
            ASTnull_filtered.class,
            ASTcolumns.class,
            ASTstored_column_list.class,
            ASTif_not_exists.class,
            ASTname.class,
            ASTtable.class,
            ASTindex_interleave_clause.class));
  }

  public List<String> getStoredColumnNames() {
    ASTstored_column_list cols = getOptionalChildByType(children, ASTstored_column_list.class);
    if (cols == null) {
      return Collections.emptyList();
    }
    return cols.getStoredColumns().stream().map(Object::toString).collect(Collectors.toList());
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
