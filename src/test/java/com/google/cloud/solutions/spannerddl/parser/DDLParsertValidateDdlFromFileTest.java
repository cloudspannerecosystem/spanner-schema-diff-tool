package com.google.cloud.solutions.spannerddl.parser;

import static com.google.common.truth.Truth.assertWithMessage;

import com.google.cloud.solutions.spannerddl.testUtils.ReadTestDatafile;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DDLParsertValidateDdlFromFileTest {

  @Parameter(0)
  public String segmentName;

  @Parameter(1)
  public String ddlStatement;

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() throws IOException {
    Map<String, String> tests = ReadTestDatafile.readDdlSegmentsFromFile("ddlParserValidation.txt");
    return tests.entrySet().stream()
        .map(
            entry ->
                new Object[] {
                  entry.getKey(),
                  // remove newlines, indentation and shrink all whitespace to a single space.
                  entry.getValue().replaceAll("\\s+", " ").trim()
                })
        .collect(Collectors.toList());
  }

  @Test
  public void validateDDL() {
    try (StringReader in = new StringReader(ddlStatement)) {
      DdlParser parser = new DdlParser(in);
      parser.ddl_statement();
      ASTddl_statement parsedStatement = (ASTddl_statement) parser.jjtree.rootNode();
      assertWithMessage("Mismatch for section %s:", segmentName)
          .that(parsedStatement.toString())
          .isEqualTo(ddlStatement);
    } catch (ParseException | UnsupportedOperationException | IllegalArgumentException e) {
      System.err.println("Exception in section " + segmentName);
      e.printStackTrace(System.err);
      assertWithMessage(
              "Exception when parsing section: %s:\n%s\nStatement: %s",
              segmentName, e, ddlStatement)
          .fail();
    }
  }
}
