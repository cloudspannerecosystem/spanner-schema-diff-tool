package com.google.cloud.solutions.spannerddl.parser;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.fail;

import com.google.cloud.solutions.spannerddl.diff.DdlDiff;
import com.google.cloud.solutions.spannerddl.diff.DdlDiffException;
import com.google.cloud.solutions.spannerddl.testUtils.ReadTestDatafile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.Test;

public class DDLAnnotationTest {

  @Test
  public void validateAnnotations() throws IOException {
    Map<String, String> tests = ReadTestDatafile.readDdlSegmentsFromFile("annotations.txt");
    Map<String, String> expects =
        ReadTestDatafile.readDdlSegmentsFromFile("expectedAnnotations.txt");

    Iterator<Entry<String, String>> testIt = tests.entrySet().iterator();
    Iterator<Entry<String, String>> expectedIt = expects.entrySet().iterator();

    while (testIt.hasNext() && expectedIt.hasNext()) {
      Entry<String, String> test = testIt.next();
      String expected = expectedIt.next().getValue();
      String segmentName = test.getKey();

      try {
        // first get all the annotations without removing the comment prefix
        List<String> annotations = getTableAnnotations(test.getValue(), false);

        // annotations should be empty
        assertThat(annotations).isEmpty();

        // now get all the annotations after removing the comment prefix
        annotations = getTableAnnotations(test.getValue(), true);

        List<String> expectedList =
            expected != null ? Arrays.asList(expected.split("\n")) : Collections.emptyList();

        assertWithMessage("Mismatch for section %s", segmentName)
            .that(annotations)
            .isEqualTo(expectedList);
      } catch (DdlDiffException e) {
        fail("Failed to parse section: '" + segmentName + "': " + e);
      }
    }
  }

  private List<String> getTableAnnotations(String ddl, boolean parseAnnotations)
      throws DdlDiffException {
    List<String> annotations = new ArrayList<>();

    List<ASTddl_statement> statements = DdlDiff.parseDdl(ddl, parseAnnotations);
    for (ASTddl_statement statement : statements) {
      if (statement.jjtGetChild(0).getId() == DdlParserTreeConstants.JJTCREATE_TABLE_STATEMENT) {
        Node tableStatement = statement.jjtGetChild(0);
        for (int i = 0, count = tableStatement.jjtGetNumChildren(); i < count; i++) {
          Node child = tableStatement.jjtGetChild(i);
          if (child instanceof ASTannotation) {
            annotations.add(((ASTannotation) child).getAnnotation());
          }
        }
      }
    }
    return annotations;
  }
}
