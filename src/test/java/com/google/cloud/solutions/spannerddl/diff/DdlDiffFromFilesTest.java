package com.google.cloud.solutions.spannerddl.diff;

import static com.google.cloud.solutions.spannerddl.diff.DdlDiff.ALLOW_DROP_STATEMENTS_OPT;
import static com.google.cloud.solutions.spannerddl.diff.DdlDiff.ALLOW_RECREATE_CONSTRAINTS_OPT;
import static com.google.cloud.solutions.spannerddl.diff.DdlDiff.ALLOW_RECREATE_INDEXES_OPT;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.fail;

import com.google.cloud.solutions.spannerddl.testUtils.ReadTestDatafile;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DdlDiffFromFilesTest {
  private static final Logger LOG = LoggerFactory.getLogger(DdlDiffFromFilesTest.class);

  @Test
  public void compareDddTextFiles() throws IOException {
    // Uses 3 files: 2 containing DDL segments to run diffs on, 1 with the expected results
    // if allowRecreateIndexes and allowDropStatements are set.

    Map<String, String> originalSegments =
        ReadTestDatafile.readDdlSegmentsFromFile("originalDdl.txt");
    Map<String, String> newSegments = ReadTestDatafile.readDdlSegmentsFromFile("newDdl.txt");
    Map<String, String> expectedOutputs =
        ReadTestDatafile.readDdlSegmentsFromFile("expectedDdlDiff.txt");

    Iterator<Map.Entry<String, String>> originalSegmentIt = originalSegments.entrySet().iterator();
    Iterator<Map.Entry<String, String>> newSegmentIt = newSegments.entrySet().iterator();
    Iterator<Map.Entry<String, String>> expectedOutputIt = expectedOutputs.entrySet().iterator();

    String segmentName = null;
    try {
      while (originalSegmentIt.hasNext() && newSegmentIt.hasNext() && expectedOutputIt.hasNext()) {
        Map.Entry<String, String> originalSegment = originalSegmentIt.next();
        segmentName = originalSegment.getKey();
        Map.Entry<String, String> newSegment = newSegmentIt.next();
        Map.Entry<String, String> expectedOutput = expectedOutputIt.next();

        // verify segment name order for sanity.
        assertWithMessage("section name in newDdl.txt differs from originalDdl.txt")
            .that(segmentName)
            .isEqualTo(newSegment.getKey());
        assertWithMessage("section name in expectedDdlDiff.txt differs from originalDdl.txt")
            .that(segmentName)
            .isEqualTo(expectedOutput.getKey());

        List<String> expectedDiff =
            expectedOutput.getValue() != null
                ? Arrays.asList(expectedOutput.getValue().split("\n"))
                : Collections.emptyList();

        LOG.info("Processing segment (with drops): " + segmentName);
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

        // build an expectedResults without any drops.
        List<String> expectedDiffNoDrops =
            expectedDiff.stream()
                .filter(
                    statement ->
                        !statement.matches(
                            ".*DROP (SCHEMA|TABLE|COLUMN|CHANGE STREAM|SEARCH INDEX|VIEW).*"))
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

        LOG.info("Processing segment (without drops): " + segmentName);
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
    } catch (DdlDiffException e) {
      fail("DdlDiffException when processing segment:\n'" + segmentName + "''\n" + e.getMessage());
    } catch (Exception e) {
      throw new Error(
          "Unexpected exception when processing segment \n'" + segmentName + "'\n" + e.getMessage(),
          e);
    }

    if (originalSegmentIt.hasNext()) {
      throw new Error(
          "Mismatched number of segments: Others have finished, but Original still has "
              + originalSegmentIt.next().getKey());
    }
    if (newSegmentIt.hasNext()) {
      throw new Error(
          "Mismatched number of segments: Others have finished, but New still has "
              + newSegmentIt.next().getKey());
    }
    if (expectedOutputIt.hasNext()) {
      throw new Error(
          "Mismatched number of segments: Others have finished, but Expected still has "
              + expectedOutputIt.next().getKey());
    }
  }
}
