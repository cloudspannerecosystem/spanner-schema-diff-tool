package com.google.cloud.solutions.spannerddl.parser;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.fail;

import com.google.cloud.solutions.spannerddl.testUtils.ReadTestDatafile;
import java.io.IOException;
import java.io.StringReader;
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
        DdlParser parser = new DdlParser(new StringReader(test.getValue()));
        parser.ddl_statement();
        Node tableStatement = parser.jjtree.rootNode().jjtGetChild(0);

        // get all annotations
        List<String> annotations = new ArrayList<>();
        for (int i = 0, count = tableStatement.jjtGetNumChildren(); i < count; i++) {
          Node child = tableStatement.jjtGetChild(i);
          if (child instanceof ASTannotation) {
            annotations.add(((ASTannotation) child).getAnnotation());
          }
        }

        List<String> expectedList =
            expected != null ? Arrays.asList(expected.split("\n")) : Collections.emptyList();

        assertWithMessage("Mismatch for section " + segmentName)
            .that(annotations)
            .isEqualTo(expectedList);
      } catch (ParseException e) {
        fail("Failed to parse section: '" + segmentName + "': " + e);
      }
    }
  }
}
