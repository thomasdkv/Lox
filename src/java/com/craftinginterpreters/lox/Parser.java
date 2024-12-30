package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

class Parser {
  private static class ParseError extends RuntimeException {
  }

  private final List<Token> tokens;
  private int current = 0;
  private int loopLevel = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
      statements.add(declaration());
    }

    return statements;
  }

  private Expr expression() {
    return assignment();
  }

  private Stmt declaration() {
    try {
      if (match(VAR))
        return varDeclaration();

      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }

  private Stmt statement() {
    /**
     * if (match(BREAK)) return breakStatement();
     */
    if (match(FOR))
      return forStatement();
    if (match(IF))
      return ifStatement();
    if (match(PRINT))
      return printStatement();
    if (match(WHILE))
      return whileStatement();
    if (match(BREAK))
      return breakStatement();
    if (match(LEFT_BRACE))
      return new Stmt.Block(block());

    return expressionStatement();
  }

  /*
   * private Void breakStatement() { consume(SEMICOLON,
   * "Expect ';' after 'break'."); return null; }
   */

  private Stmt forStatement() {
    try {
      loopLevel += 1;

      consume(LEFT_PAREN, "Expect '(' after 'for'.");

      Stmt initializer;
      if (match(SEMICOLON)) {
        initializer = null;
      } else if (match(VAR)) {
        initializer = varDeclaration();
      } else {
        initializer = expressionStatement();
      }

      Expr condition = null;
      if (!check(SEMICOLON)) {
        condition = expression();
      }
      consume(SEMICOLON, "Expect ';' after loop condition.");

      Expr increment = null;
      if (!check(RIGHT_PAREN)) {
        increment = expression();
      }
      consume(RIGHT_PAREN, "Expect ')' after for clauses.");

      Stmt body = statement();

      if (increment != null) {
        body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
      }

      if (condition == null)
        condition = new Expr.Literal(true);
      body = new Stmt.While(condition, body);

      if (initializer != null) {
        body = new Stmt.Block(Arrays.asList(initializer, body));
      }
      return body;

    } finally {
      loopLevel -= 1;
    }
  }

  private Stmt ifStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'if'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after if condition.");

    Stmt thenBranch = statement();
    Stmt elseBranch = null;
    if (match(ELSE)) {
      elseBranch = statement();
    }

    return new Stmt.If(condition, thenBranch, elseBranch);
  }

  private Stmt printStatement() {
    Expr value = expression();
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Print(value);
  }

  private Stmt varDeclaration() {
    Token name = consume(IDENTIFIER, "Expect variable name.");

    Expr initializer = null;
    if (match(EQUAL)) {
      initializer = expression();
    }

    consume(SEMICOLON, "Expect ';' after variable declaration.");
    return new Stmt.Var(name, initializer);
  }

  private Stmt whileStatement() {
    try {
      loopLevel += 1;

      consume(LEFT_PAREN, "Expect '(' after 'while'.");
      Expr condition = expression();
      consume(RIGHT_PAREN, "Expect ')' after condition.");
      Stmt body = statement();

      return new Stmt.While(condition, body);
    } finally {
      loopLevel -= 1;
    }
  }

  private Stmt breakStatement() {
    if (loopLevel <= 0) {
      throw error(previous(), "break can only be used inside a loop.");
    }
    consume(SEMICOLON, "Expect ';' after break.");

    return new Stmt.Break();
  }

  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(SEMICOLON, "Expect ';' after expression.");
    return new Stmt.Expression(expr);
  }

  /**
   * We create an empty list and then parse statements and add them to the list
   * until we reach the end of the block, marked by the closing }. Note that the
   * loop also has an explicit check for {@link #isAtEnd()}. We have to be careful
   * to avoid infinite loops, even when parsing invalid code. If the user forgets
   * a closing }, the parser needs to not get stuck. <br />
   * <br />
   * 
   * That’s it for syntax.
   * 
   * 
   * @return
   */
  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();

    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }

    consume(RIGHT_BRACE, "Expect '}' after block.");
    return statements;
  }

  private Expr assignment() {
    Expr expr = or();

    if (match(EQUAL)) {
      Token equals = previous();
      Expr value = assignment();

      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable) expr).name;
        return new Expr.Assign(name, value);
      }

      error(equals, "Invalid assignment target.");
    }

    return expr;
  }

  private Expr or() {
    Expr expr = and();

    while (match(OR)) {
      Token operator = previous();
      Expr right = and();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  private Expr and() {
    Expr expr = equality();

    while (match(AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  /**
   * equality -> comparison ( ( "!=" | "==" ) comparison )* ; <br />
   *
   * Let’s step through it. The first comparison nonterminal in the body
   * translates to the first call to comparison() in the method. We take that
   * result and store it in a local variable. <br />
   *
   * Then, the ( ... )* loop in the rule maps to a while loop. We need to know
   * when to exit that loop. We can see that inside the rule, we must first find
   * either a != or == token. So, if we don’t see one of those, we must be done
   * with the sequence of equality operators.
   * 
   * @return
   */
  private Expr equality() {
    Expr expr = comparison();

    while (match(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  /**
   * comparison -> term ( ( ">" \| ">=" | "<" | "<=" ) term )* ; <br />
   *
   * The grammar rule is virtually identical to {@link #equality() equality} and
   * so is the corresponding code. The only differences are the token types for
   * the operators we match, and the method we call for the operands—now term()
   * instead of comparison(). The remaining two binary operator rules follow the
   * same pattern.
   * 
   * @return
   */
  private Expr comparison() {
    Expr expr = term();

    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr term() {
    Expr expr = factor();

    while (match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr factor() {
    Expr expr = unary();

    while (match(SLASH, STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

    return primary();
  }

  private Expr primary() {
    if (match(FALSE))
      return new Expr.Literal(false);
    if (match(TRUE))
      return new Expr.Literal(true);
    if (match(NIL))
      return new Expr.Literal(null);

    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }

    if (match(IDENTIFIER)) {
      return new Expr.Variable(previous());
    }

    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }

    throw error(peek(), "Expect expression.");
  }

  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }

    return false;
  }

  private Token consume(TokenType type, String message) {
    if (check(type))
      return advance();

    throw error(peek(), message);
  }

  private boolean check(TokenType type) {
    if (isAtEnd())
      return false;
    return peek().type == type;
  }

  private Token advance() {
    if (!isAtEnd())
      current++;
    return previous();
  }

  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }

  private void synchronize() {
    advance();

    while (!isAtEnd()) {
      if (previous().type == SEMICOLON)
        return;

      switch (peek().type) {
      case CLASS:
      case FUN:
      case VAR:
      case FOR:
      case IF:
      case WHILE:
      case PRINT:
      case RETURN:
        return;
      }

      advance();
    }
  }
}
