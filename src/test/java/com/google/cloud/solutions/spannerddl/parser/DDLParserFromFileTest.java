package com.google.cloud.solutions.spannerddl.parser;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.fail;

import com.google.cloud.solutions.spannerddl.testUtils.ReadTestDatafile;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.Test;

public class DDLParserFromFileTest {

  @Test
  public void validateDDLfromFile() throws IOException {

    LinkedHashMap<String, String> tests =
        ReadTestDatafile.readDdlSegmentsFromFile("ddlParserValidation.txt");

    Iterator<Map.Entry<String, String>> testIt = tests.entrySet().iterator();

    String segmentName = "unread";
    while (testIt.hasNext()) {
      Entry<String, String> test = testIt.next();
      segmentName = test.getKey();
      // remove newlines, indentation and shrink all whitespace to a single space.
      String ddlStatement = test.getValue().replaceAll("\\s+", " ").trim();

      try (StringReader in = new StringReader(ddlStatement)) {
        DdlParser parser = new DdlParser(in);
        parser.ddl_statement();
        ASTddl_statement parsedStatement = (ASTddl_statement) parser.jjtree.rootNode();

        assertWithMessage("Mismatch for section " + segmentName)
            .that(parsedStatement.toString())
            .isEqualTo(ddlStatement);
      } catch (ParseException e) {
        fail(
            "Failed to parse section: '"
                + segmentName
                + "': "
                + e
                + "\nStatement: "
                + ddlStatement);
      }
    }
    System.out.println("validateDDLfromFile - tests completed : " + tests.size());
  }

  @Test
  public void validateUnsupportedDDLfromFile() throws Exception {

    LinkedHashMap<String, String> tests =
        ReadTestDatafile.readDdlSegmentsFromFile("ddlParserUnsupported.txt");

    Iterator<Map.Entry<String, String>> testIt = tests.entrySet().iterator();

    String segmentName = "unread";
    String ddlStatement = "unread";
    while (testIt.hasNext()) {
      Entry<String, String> test = testIt.next();
      segmentName = test.getKey();
      // remove newlines, indentation and shrink all whitespace to a single space.
      ddlStatement = test.getValue().replaceAll("\\s+", " ").trim();

      try (StringReader in = new StringReader(ddlStatement)) {
        DdlParser parser = new DdlParser(in);
        parser.ddl_statement();

        fail(
            "UnsupportedOperationException not thrown for section '"
                + segmentName
                + "'\nStatement: "
                + ddlStatement);
      } catch (UnsupportedOperationException e) {
        /* expected */ ;
      } catch (ParseException e) {
        fail(
            "Failed to parse section: '"
                + segmentName
                + "': "
                + e
                + "\nStatement: "
                + ddlStatement);
      }
    }
    System.out.println("validateUnsupportedDDLfromFile - tests completed : " + tests.size());
  }
}
