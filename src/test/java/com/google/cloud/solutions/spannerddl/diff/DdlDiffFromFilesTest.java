package com.google.cloud.solutions.spannerddl.diff;

import static com.google.cloud.solutions.spannerddl.diff.DdlDiff.ALLOW_DROP_STATEMENTS_OPT;
import static com.google.cloud.solutions.spannerddl.diff.DdlDiff.ALLOW_RECREATE_CONSTRAINTS_OPT;
import static com.google.cloud.solutions.spannerddl.diff.DdlDiff.ALLOW_RECREATE_INDEXES_OPT;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.cloud.solutions.spannerddl.testUtils.ReadTestDatafile;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DdlDiffFromFilesTest {

  @Parameter(0)
  public String segmentName;

  @Parameter(1)
  public String originalSegment;

  @Parameter(2)
  public String newSegment;

  @Parameter(3)
  public String expectedOutput;

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() throws IOException {
    // Uses 3 files: 2 containing DDL segments to run diffs on, 1 with the expected results
    // if allowRecreateIndexes and allowDropStatements are set.

    Map<String, String> originalSegments =
        ReadTestDatafile.readDdlSegmentsFromFile("originalDdl.txt");
    Map<String, String> newSegments = ReadTestDatafile.readDdlSegmentsFromFile("newDdl.txt");
    Map<String, String> expectedOutputs =
        ReadTestDatafile.readDdlSegmentsFromFile("expectedDdlDiff.txt");

    // Validate that all the segments are in all files
    ArrayList<String> errors = new ArrayList<>();
    MapDifference<String, String> diff = Maps.difference(originalSegments, newSegments);
    if (!diff.entriesOnlyOnLeft().isEmpty()) {
      errors.add(
          "Mismatch in segments: newDdl.txt is missing segments: "
              + diff.entriesOnlyOnLeft().keySet());
    }
    if (!diff.entriesOnlyOnRight().isEmpty()) {
      errors.add(
          "Mismatch in segments: newDdl.txt has additional segments: "
              + diff.entriesOnlyOnRight().keySet());
    }
    diff = Maps.difference(originalSegments, expectedOutputs);
    if (!diff.entriesOnlyOnLeft().isEmpty()) {
      errors.add(
          "Mismatch in segments: expectedDdlDiff.txt is missing segments: "
              + diff.entriesOnlyOnLeft().keySet());
    }
    if (!diff.entriesOnlyOnRight().isEmpty()) {
      errors.add(
          "Mismatch in segments: expectedDdlDiff.txt has additional segments: "
              + diff.entriesOnlyOnRight().keySet());
    }

    if (!errors.isEmpty()) {
      assertWithMessage(
              "Mismatching segments in test data:\n   %s", Joiner.on("\n   ").join(errors))
          .fail();
    }
    return originalSegments.entrySet().stream()
        .map(
            (e) ->
                new Object[] {
                  e.getKey(),
                  e.getValue(),
                  newSegments.get(e.getKey()),
                  expectedOutputs.get(e.getKey())
                })
        .collect(Collectors.toList());
  }

  @Test
  public void compareDddWithDrops() throws DdlDiffException {

    List<String> expectedDiff =
        expectedOutput == null ? List.of() : Arrays.asList(expectedOutput.split("\n"));

    DdlDiff ddlDiff =
        DdlDiff.build(
            originalSegment, newSegment, ImmutableMap.of(DdlDiff.IGNORE_PROTO_BUNDLES_OPT, false));
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
    assertWithMessage("Mismatch for section %s", segmentName).that(diff).isEqualTo(expectedDiff);
  }

  @Test
  public void compareDddNoDrops() throws DdlDiffException {
    // build an expectedResults without any drops.
    List<String> expectedDiff =
        expectedOutput == null ? List.of() : Arrays.asList(expectedOutput.split("\n"));
    List<String> expectedDiffNoDrops =
        expectedDiff.stream()
            .filter(
                statement ->
                    !statement.matches(".*DROP (SCHEMA|TABLE|COLUMN|CHANGE STREAM|SEARCH INDEX).*"))
            .collect(Collectors.toList());

    // remove any drop indexes from the expectedResults if they do not have an equivalent
    // CREATE statement. This is because we are allowing recreation of indexes, but not allowing
    // dropping of removed indexes.
    for (String statement : expectedDiff) {
      if (statement.startsWith("DROP INDEX ")) {
        String indexName = Iterables.get(Splitter.on(' ').split(statement), 2);
        // see if there is a matching create statement
        Pattern p = Pattern.compile("CREATE .*INDEX " + indexName + " ");
        if (expectedDiffNoDrops.stream().noneMatch(s -> p.matcher(s).find())) {
          expectedDiffNoDrops.remove(statement);
        }
      }
    }
    DdlDiff ddlDiff =
        DdlDiff.build(
            originalSegment, newSegment, ImmutableMap.of(DdlDiff.IGNORE_PROTO_BUNDLES_OPT, false));
    List<String> diff =
        ddlDiff.generateDifferenceStatements(
            ImmutableMap.of(
                ALLOW_RECREATE_INDEXES_OPT,
                true,
                ALLOW_DROP_STATEMENTS_OPT,
                false,
                ALLOW_RECREATE_CONSTRAINTS_OPT,
                true));
    // check expected results.
    assertWithMessage("Mismatch for section (noDrops)%s", segmentName)
        .that(diff)
        .isEqualTo(expectedDiffNoDrops);
  }
}
