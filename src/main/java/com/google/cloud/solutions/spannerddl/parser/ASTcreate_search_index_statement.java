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
package com.google.cloud.solutions.spannerddl.parser;

import static com.google.cloud.solutions.spannerddl.diff.AstTreeUtils.getChildByType;
import static com.google.cloud.solutions.spannerddl.diff.AstTreeUtils.getOptionalChildByType;

import com.google.cloud.solutions.spannerddl.diff.AstTreeUtils;
import com.google.cloud.solutions.spannerddl.diff.DdlDiffException;
import com.google.cloud.solutions.spannerddl.diff.SchemaUpdateStatements;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapDifference;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ASTcreate_search_index_statement extends SimpleNode
    implements Comparable<ASTcreate_search_index_statement> {

  private static final Logger LOG = LoggerFactory.getLogger(ASTcreate_search_index_statement.class);

  public ASTcreate_search_index_statement(int id) {
    super(id);
  }

  public ASTcreate_search_index_statement(DdlParser p, int id) {
    super(p, id);
  }

  public String getName() {
    return AstTreeUtils.tokensToString(getChildByType(children, ASTname.class));
  }

  private void validateChildren() {
    AstTreeUtils.validateChildrenClasses(
        children,
        ImmutableSet.of(
            ASTname.class,
            ASTtable.class,
            ASTtoken_key_list.class,
            ASTstored_column_list.class,
            ASTpartition_key.class,
            ASTorder_by_key.class,
            ASTcreate_index_where_clause.class,
            ASTindex_interleave_clause.class,
            ASToptions_clause.class));
  }

  /** Create string version, optionally including the IF NOT EXISTS clause */
  @Override
  public String toString() {
    validateChildren();
    ASTindex_interleave_clause interleave =
        getOptionalChildByType(children, ASTindex_interleave_clause.class);

    return Joiner.on(" ")
        .skipNulls()
        .join(
            "CREATE SEARCH INDEX",
            getName(),
            "ON",
            getChildByType(children, ASTtable.class),
            getChildByType(children, ASTtoken_key_list.class),
            getOptionalChildByType(children, ASTstored_column_list.class),
            getOptionalChildByType(children, ASTpartition_key.class),
            getOptionalChildByType(children, ASTorder_by_key.class),
            getOptionalChildByType(children, ASTcreate_index_where_clause.class),
            (interleave != null ? "," : null),
            interleave,
            getOptionalChildByType(children, ASToptions_clause.class));
  }

  @Override
  public int compareTo(ASTcreate_search_index_statement other) {
    return this.getName().compareTo(other.getName());
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof ASTcreate_search_index_statement) {
      return this.toString().equals(other.toString());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  public static SchemaUpdateStatements generateAlterStatementsFor(
      Map<String, ValueDifference<ASTcreate_search_index_statement>> searchIndexDifferences,
      boolean allowDropColumnStatements)
      throws DdlDiffException {
    final ImmutableList.Builder<String> dropStatements = ImmutableList.builder();
    final ImmutableList.Builder<String> createStatements = ImmutableList.builder();

    for (ValueDifference<ASTcreate_search_index_statement> diff : searchIndexDifferences.values()) {
      diff.leftValue()
          .generateAlterStatementsFor(
              diff.rightValue(), dropStatements, createStatements, allowDropColumnStatements);
    }
    return SchemaUpdateStatements.create(
        dropStatements.build(), List.of(), createStatements.build());
  }

  private void generateAlterStatementsFor(
      ASTcreate_search_index_statement other,
      ImmutableList.Builder<String> dropStatements,
      ImmutableList.Builder<String> createStatements,
      boolean allowDropColumnStatements)
      throws DdlDiffException {
    // for simplicity/clarity
    final ASTcreate_search_index_statement original = this;

    // Validate possible diffs
    if (!original.getName().equals(other.getName())) {
      throw new DdlDiffException(
          "CREATE SEARCH INDEX name mismatch: " + original.getName() + " != " + other.getName());
    }

    if (!Objects.equal(
        getOptionalChildByType(original.children, ASTpartition_key.class),
        getOptionalChildByType(other.children, ASTpartition_key.class))) {
      throw new DdlDiffException(
          "Cannot generate diff for CREATE SEARCH INDEX: "
              + original.getName()
              + " PARTITION BY clause changed");
    }

    if (!Objects.equal(
        getOptionalChildByType(original.children, ASTorder_by_key.class),
        getOptionalChildByType(other.children, ASTorder_by_key.class))) {
      throw new DdlDiffException(
          "Cannot generate diff for CREATE SEARCH INDEX: "
              + original.getName()
              + " ORDER BY clause changed");
    }

    if (!Objects.equal(
        getOptionalChildByType(original.children, ASTcreate_index_where_clause.class),
        getOptionalChildByType(other.children, ASTcreate_index_where_clause.class))) {
      throw new DdlDiffException(
          "Cannot generate diff for CREATE SEARCH INDEX: "
              + original.getName()
              + " WHERE clause changed");
    }

    if (!Objects.equal(
        getOptionalChildByType(original.children, ASTindex_interleave_clause.class),
        getOptionalChildByType(other.children, ASTindex_interleave_clause.class))) {
      throw new DdlDiffException(
          "Cannot generate diff for CREATE SEARCH INDEX: "
              + original.getName()
              + " INTERLEAVE IN clause changed");
    }

    if (!Objects.equal(
        getOptionalChildByType(original.children, ASToptions_clause.class),
        getOptionalChildByType(other.children, ASToptions_clause.class))) {
      throw new DdlDiffException(
          "Cannot generate diff for CREATE SEARCH INDEX: "
              + original.getName()
              + " OPTIONS clause changed");
    }

    // Look for differences in tokenKeyList
    // Easiest is to use Maps.difference, but first we need some maps, and we need to preserve order
    // so convert the keyParts to String, and then add to a LinkedHashMap.
    Map<String, ASTkey_part> originalKeyParts =
        getChildByType(original.children, ASTtoken_key_list.class).getKeyParts().stream()
            .collect(
                Collectors.toMap(
                    ASTkey_part::toString, Function.identity(), (x, y) -> y, LinkedHashMap::new));
    Map<String, ASTkey_part> newKeyParts =
        getChildByType(other.children, ASTtoken_key_list.class).getKeyParts().stream()
            .collect(
                Collectors.toMap(
                    ASTkey_part::toString, Function.identity(), (x, y) -> y, LinkedHashMap::new));
    MapDifference<String, ASTkey_part> keyPartsDiff =
        Maps.difference(originalKeyParts, newKeyParts);

    // Look for differences in storedColumnList
    // Easiest is to use Maps.difference, but first we need some maps, and we need to preserve order
    // so convert the keyParts to String, and then add to a LinkedHashMap.
    Map<String, ASTstored_column> originalStoredColumns =
        getChildByType(original.children, ASTstored_column_list.class).getStoredColumns().stream()
            .collect(
                Collectors.toMap(
                    ASTstored_column::toString,
                    Function.identity(),
                    (x, y) -> y,
                    LinkedHashMap::new));
    Map<String, ASTstored_column> newStoredColumns =
        getChildByType(other.children, ASTstored_column_list.class).getStoredColumns().stream()
            .collect(
                Collectors.toMap(
                    ASTstored_column::toString,
                    Function.identity(),
                    (x, y) -> y,
                    LinkedHashMap::new));
    MapDifference<String, ASTstored_column> storedColDiff =
        Maps.difference(originalStoredColumns, newStoredColumns);

    if (allowDropColumnStatements) {
      for (ASTkey_part droppedToken : keyPartsDiff.entriesOnlyOnLeft().values()) {
        LOG.info(
            "Dropping token colum {} for search index: {}",
            droppedToken.getKeyPath(),
            original.getName());

        dropStatements.add(
            "ALTER SEARCH INDEX "
                + original.getName()
                + " DROP COLUMN "
                + droppedToken.getKeyPath());
      }
    }

    for (ASTstored_column droppedStoredCol : storedColDiff.entriesOnlyOnLeft().values()) {
      LOG.info(
          "Dropping stored colum {} for search index: {}",
          droppedStoredCol.toString(),
          original.getName());

      dropStatements.add(
          "ALTER SEARCH INDEX "
              + original.getName()
              + " DROP STORED COLUMN "
              + droppedStoredCol.toString());
    }

    for (ASTkey_part newToken : keyPartsDiff.entriesOnlyOnRight().values()) {
      LOG.info(
          "Adding token colum {} for search index: {}", newToken.getKeyPath(), original.getName());

      createStatements.add(
          "ALTER SEARCH INDEX " + original.getName() + " ADD COLUMN " + newToken.toString());
    }

    for (ASTstored_column newStoredCol : storedColDiff.entriesOnlyOnRight().values()) {
      LOG.info(
          "Adding stored colum {} for search index: {}",
          newStoredCol.toString(),
          original.getName());

      createStatements.add(
          "ALTER SEARCH INDEX "
              + original.getName()
              + " ADD STORED COLUMN "
              + newStoredCol.toString());
    }
  }
}
