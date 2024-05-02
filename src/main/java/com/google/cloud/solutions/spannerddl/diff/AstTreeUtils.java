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

package com.google.cloud.solutions.spannerddl.diff;

import com.google.cloud.solutions.spannerddl.parser.DdlParserConstants;
import com.google.cloud.solutions.spannerddl.parser.Node;
import com.google.cloud.solutions.spannerddl.parser.SimpleNode;
import com.google.cloud.solutions.spannerddl.parser.Token;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/** Utility functions for getting and casting Nodes in the parsed AST. */
public class AstTreeUtils {

  /** Gets (and casts) the first found child of a specific node type. */
  public static <T> T getOptionalChildByType(Node[] children, Class<T> type) {
    for (Node child : children) {
      if (type.isInstance(child)) {
        return type.cast(child);
      }
    }
    return null;
  }

  /**
   * Gets (and casts) the first found child of a specific node type, throwing an exception if it
   * does not exist.
   */
  public static <T> T getChildByType(Node[] children, Class<T> type) {
    T child = getOptionalChildByType(children, type);
    if (child == null) {
      throw new IllegalArgumentException(
          "Cannot find child of type " + type.getName() + " in " + Arrays.asList(children));
    }
    return child;
  }

  private static final Set<String> reservedWords =
      Arrays.stream(DdlParserConstants.tokenImage)
          .filter(s -> s.charAt(0) == '"')
          .map(s -> s.substring(1, s.length() - 1))
          .collect(Collectors.toSet());

  /** Checks if the word is a reserved word/known token. */
  public static boolean isReservedWord(String word) {
    return reservedWords.contains(word);
  }

  /**
   * Ensures that the passed array of Nodes is of a specific node type, and returns a List with the
   * specific type.
   */
  public static <T> List<T> getChildrenAssertType(Node[] children, Class<T> type) {
    return StreamSupport.stream(Arrays.spliterator(children), false)
        .map(type::cast)
        .collect(Collectors.toList());
  }

  private AstTreeUtils() {}

  /**
   * Generate the original parsed text between the 2 specified tokens, normalizing the text with
   * spacing and capitalization of tokens.
   */
  public static String tokensToString(Token firstToken, Token lastToken) {
    return tokensToString(firstToken, lastToken, true);
  }

  /**
   * Generate the original parsed text between the 2 specified tokens, normalizing the text with
   * spacing and optional capitalization of reserved words.
   */
  public static String tokensToString(
      Token firstToken, Token lastToken, boolean upperCaseReserved) {
    StringBuilder sb = new StringBuilder();
    Token t = firstToken;
    while (t != lastToken) {
      String tok = t.toString();
      sb.append(isReservedWord(tok) && upperCaseReserved ? tok.toUpperCase(Locale.ROOT) : tok);

      if (t.next != null
          && !t.next.toString().equals(",")
          && !t.next.toString().equals(".")
          && !tok.equals(".")) {
        sb.append(" ");
      }
      t = t.next;
    }
    // append last token
    String tok = t.toString();
    sb.append(isReservedWord(tok) && upperCaseReserved ? tok.toUpperCase(Locale.ROOT) : tok);
    return sb.toString();
  }

  /**
   * Generate the original parsed text of the node, normalizing the text with spacing and
   * capitalization of reserved words.
   */
  public static String tokensToString(SimpleNode node) {
    return tokensToString(node, true);
  }

  /**
   * Generate the original parsed text of the node, normalizing the text with spacing and optional
   * capitalization of reserved words.
   */
  public static String tokensToString(SimpleNode node, boolean upperCaseReserved) {
    return tokensToString(node.jjtGetFirstToken(), node.jjtGetLastToken(), upperCaseReserved);
  }

  /** Verifies that each child is one of the specified classes. */
  public static void validateChildrenClasses(
      Node[] children, Set<Class<? extends SimpleNode>> validChildClasses) {
    for (Node child : children) {
      if (!validChildClasses.contains(child.getClass())) {
        throw new IllegalArgumentException(
            "Unexpected child node "
                + child.getClass().getSimpleName()
                + " in parent "
                + child.jjtGetParent().getClass().getSimpleName());
      }
    }
  }
}
