package smc.lexer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {
  private final TokenCollector collector;
  private int lineNumber;
  private int position;

  public Lexer(TokenCollector collector) {
    this.collector = collector;
  }

  public void lex(String s) {
    lineNumber = 1;
    String[] lines = s.split("\n");
    for (String line : lines) {
      lexLine(line);
      lineNumber++;
    }
  }

  private void lexLine(String line) {
    for (position = 0; position < line.length(); )
      lexToken(line);
  }

  private void lexToken(String line) {
    if (!findToken(line)) {
      collector.error(lineNumber, position + 1);
      position += 1;
    }
  }

  private boolean findToken(String line) {
    return
      findWhiteSpace(line) ||
        findSingleCharacterToken(line) ||
        findName(line);
  }

  private static final Pattern whitePattern = Pattern.compile("^\\s+");
  private static final Pattern commentPattern = Pattern.compile("^//.*$");
  private static final Pattern[] whitePatterns = new Pattern[] {whitePattern, commentPattern};

  private boolean findWhiteSpace(String line) {
    for (Pattern pattern : whitePatterns) {
      Matcher matcher = pattern.matcher(line.substring(position));
      if (matcher.find()) {
        position += matcher.end();
        return true;
      }
    }

    return false;
  }

  private boolean findSingleCharacterToken(String line) {
    switch (line.substring(position, position + 1)) {
      case "{" -> collector.openBrace(lineNumber, position);
      case "}" -> collector.closedBrace(lineNumber, position);
      case "(" -> collector.openParen(lineNumber, position);
      case ")" -> collector.closedParen(lineNumber, position);
      case "<" -> collector.openAngle(lineNumber, position);
      case ">" -> collector.closedAngle(lineNumber, position);
      case "-", "*" -> collector.dash(lineNumber, position);
      case ":" -> collector.colon(lineNumber, position);
      default -> {
        return false;
      }
    }
    ++position;
    return true;
  }

  private static final Pattern namePattern = Pattern.compile("^\\w+");

  private boolean findName(String line) {
    Matcher nameMatcher = namePattern.matcher(line.substring(position));
    if (nameMatcher.find()) {
      collector.name(nameMatcher.group(0), lineNumber, position);
      position += nameMatcher.end();
      return true;
    }
    return false;
  }
}
