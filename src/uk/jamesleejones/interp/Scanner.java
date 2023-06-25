package uk.jamesleejones.interp;

import static uk.jamesleejones.interp.TokenType.AND;
import static uk.jamesleejones.interp.TokenType.BANG;
import static uk.jamesleejones.interp.TokenType.BANG_EQUAL;
import static uk.jamesleejones.interp.TokenType.CLASS;
import static uk.jamesleejones.interp.TokenType.COMMA;
import static uk.jamesleejones.interp.TokenType.DOT;
import static uk.jamesleejones.interp.TokenType.ELSE;
import static uk.jamesleejones.interp.TokenType.EOF;
import static uk.jamesleejones.interp.TokenType.EQUAL;
import static uk.jamesleejones.interp.TokenType.EQUAL_EQUAL;
import static uk.jamesleejones.interp.TokenType.FALSE;
import static uk.jamesleejones.interp.TokenType.FOR;
import static uk.jamesleejones.interp.TokenType.FUN;
import static uk.jamesleejones.interp.TokenType.GREATER;
import static uk.jamesleejones.interp.TokenType.GREATER_EQUALS;
import static uk.jamesleejones.interp.TokenType.IDENTIFIER;
import static uk.jamesleejones.interp.TokenType.IF;
import static uk.jamesleejones.interp.TokenType.LEFT_BRACE;
import static uk.jamesleejones.interp.TokenType.LEFT_PAREN;
import static uk.jamesleejones.interp.TokenType.LESS;
import static uk.jamesleejones.interp.TokenType.LESS_EQUAL;
import static uk.jamesleejones.interp.TokenType.MINUS;
import static uk.jamesleejones.interp.TokenType.NIL;
import static uk.jamesleejones.interp.TokenType.NUMBER;
import static uk.jamesleejones.interp.TokenType.OR;
import static uk.jamesleejones.interp.TokenType.PLUS;
import static uk.jamesleejones.interp.TokenType.PRINT;
import static uk.jamesleejones.interp.TokenType.RETURN;
import static uk.jamesleejones.interp.TokenType.RIGHT_BRACE;
import static uk.jamesleejones.interp.TokenType.RIGHT_PAREN;
import static uk.jamesleejones.interp.TokenType.SEMICOLON;
import static uk.jamesleejones.interp.TokenType.SLASH;
import static uk.jamesleejones.interp.TokenType.STAR;
import static uk.jamesleejones.interp.TokenType.STRING;
import static uk.jamesleejones.interp.TokenType.SUPER;
import static uk.jamesleejones.interp.TokenType.THIS;
import static uk.jamesleejones.interp.TokenType.TRUE;
import static uk.jamesleejones.interp.TokenType.VAR;
import static uk.jamesleejones.interp.TokenType.WHILE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner {
  private final String source;
  private final List<Token> tokens = new ArrayList<>();

  private int start = 0;
  private int current = 0;
  private int line = 1;

  private static final Map<String, TokenType> keywords;

  static {
    keywords = new HashMap<>();
    keywords.put("and", AND);
    keywords.put("class", CLASS);
    keywords.put("else", ELSE);
    keywords.put("false", FALSE);
    keywords.put("for", FOR);
    keywords.put("fun", FUN);
    keywords.put("if", IF);
    keywords.put("nil", NIL);
    keywords.put("or", OR);
    keywords.put("print", PRINT);
    keywords.put("return", RETURN);
    keywords.put("super", SUPER);
    keywords.put("this", THIS);
    keywords.put("true", TRUE);
    keywords.put("var", VAR);
    keywords.put("while", WHILE);
  }

  Scanner(String source) {
    this.source = source;
  }

  List<Token> scanTokens() {
    while (!isAtEnd()) {
      start = current;
      scanToken();
    }

    tokens.add(new Token(EOF, "", null, line));
    return tokens;
  }

  private void scanToken() {
    char c = advance();
    switch (c) {
      // Single character tokens
      case '(' -> addToken(LEFT_PAREN);
      case ')' -> addToken(RIGHT_PAREN);
      case '{' -> addToken(LEFT_BRACE);
      case '}' -> addToken(RIGHT_BRACE);
      case ',' -> addToken(COMMA);
      case '.' -> addToken(DOT);
      case '-' -> addToken(MINUS);
      case '+' -> addToken(PLUS);
      case ';' -> addToken(SEMICOLON);
      case '*' -> addToken(STAR);

      // One or two character tokens
      case '!' -> addToken(match('=') ? BANG_EQUAL : BANG);
      case '=' -> addToken(match('=') ? EQUAL_EQUAL : EQUAL);
      case '>' -> addToken(match('=') ? GREATER_EQUALS : GREATER);
      case '<' -> addToken(match('=') ? LESS_EQUAL : LESS);

      // Comments
      case '/' -> {
        // Then it's a comment.
        if (match('/')) {
          while (peek() != '\n' && !isAtEnd())
            advance();
        } else {
          addToken(SLASH);
        }
      }

      // Blank space
      case ' ', '\t', '\r' -> {}
      case '\n' -> line++;

      // Literals
      case '"' -> string();

      // If it's none of the above, it's an error.
      default -> {
        if (isDigit(c)) {
          number();
        } else if (isAlpha(c)) {
          identifier();
        } else {
          Interp.error(line, "Unexpected character.");
        }
      }
    }
  }

  private boolean isAtEnd() {
    return current >= source.length();
  }

  private char advance() {
    return source.charAt(current++);
  }

  private char peek() {
    if (isAtEnd()) return '\0';
    return source.charAt(current);
  }

  private char peekNext() {
    if (current + 1 >= source.length()) return '\0';
    return source.charAt(current + 1);
  }

  private void addToken(TokenType type) {
    addToken(type, null);
  }

  private void addToken(TokenType type, Object literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }

  private boolean match(char expected) {
    if (isAtEnd()) return false;
    if (source.charAt(current) != expected) return false;

    current++;
    return true;
  }

  private void string() {
    StringBuilder sb = new StringBuilder();

    while (peek() != '"' && !isAtEnd()) {
      sb.append(peek());
      if (peek() == '\n') line++;
      advance();
    }

    if (isAtEnd()) {
      Interp.error(line, "Unterminated string.");
      return;
    }

    advance(); // Capture the closing ".

    // TODO: Possibly support escape sequences by unescaping them

    addToken(STRING, sb.toString());
  }

  // Java's standard library isDigit allows characters we don't want
  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private void number() {
    StringBuilder sb = new StringBuilder();

    while (isDigit(peek()) && !isAtEnd()) {
      sb.append(peek());
      advance();
    }

    if (peek() == '.' && isDigit(peekNext())) {
      sb.append('.');
      advance(); // Skip decimal

      while (isDigit(peek())) {
        sb.append(peek());
        advance();
      }
    }

    addToken(NUMBER, Double.parseDouble(sb.toString()));
  }

  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') ||
           (c >= 'A' && c <= 'Z') ||
           c == '_';
  }

  private boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  private void identifier() {
    StringBuilder sb = new StringBuilder();
    while (isAlphaNumeric(peek())) {
      sb.append(peek());
      advance();
    }

    // See if the identifier is a keyword.
    TokenType type = keywords.get(sb.toString());
    if (type == null) type = IDENTIFIER;
    addToken(type);
  }
}
