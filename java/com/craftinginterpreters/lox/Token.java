package com.craftinginterpreters.lox;

class Token {
  /**
   * Type of Token
   */
  final TokenType type;
  /**
   * Word (sequence of characters)
   */
  final String lexeme;
  final Object literal;
  final int line;

  /**
   * @param type    Type of the Token
   * @param lexeme  Word (Sequence of characters)
   * @param literal
   * @param line
   */
  Token(TokenType type, String lexeme, Object literal, int line) {
    this.type = type;
    this.lexeme = lexeme;
    this.literal = literal;
    this.line = line;
  }

  public String toString() {
    return type + " " + lexeme + " " + literal;
  }
}
