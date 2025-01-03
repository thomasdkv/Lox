package com.craftinginterpreters.lox;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.io.BufferedReader;
//import java.util.Scanner;

public class Lox {
  private static final Interpreter interpreter = new Interpreter();
  static boolean hadError = false;
  static boolean hadRuntimeError = false;

  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  /**
   * If you start jlox from the command line and give it a path to a file, it
   * reads the file and executes it
   * 
   * @param path
   * @throws IOException
   */
  private static void runFile(String path) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(path));
    run(new String(bytes, Charset.defaultCharset()));
    if (hadError)
      System.exit(65);
    if (hadRuntimeError)
      System.exit(70);
  }

  /**
   * If you want a more intimate conversation with your interpreter, you can also
   * run it interactively. Fire up jlox without any arguments, and it drops you
   * into a prompt where you can enter and execute code one line at a time.
   * 
   * @throws IOException
   */
  private static void runPrompt() throws IOException {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    for (;;) {
      System.out.print("> ");
      String line = reader.readLine();
      if (line == null)
        break;
      run(line);
      hadError = false;
    }
  }

  /**
   * @param source
   */
  private static void run(String source) {

    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();

    Parser parser = new Parser(tokens);
    List<Stmt> statements = parser.parse();

    // Stop if there was a syntax error.
    if (hadError)
      return;

    Resolver resolver = new Resolver(interpreter);
    resolver.resolve(statements);

    // Stop if there was a resolution error.
    if (hadError)
      return;

    interpreter.interpret(statements);
  }

  /**
   * @param line    Which line in the code the error is on
   * @param message Error message
   */
  static void error(int line, String message) {
    report(line, "", message);
  }

  /**
   * @param line
   * @param where
   * @param message
   */
  private static void report(int line, String where, String message) {
    System.err.println("[line " + line + "] Error" + where + ": " + message);
    hadError = true;
  }

  static void error(Token token, String message) {
    if (token.type == TokenType.EOF) {
      report(token.line, " at end", message);
    } else {
      report(token.line, " at '" + token.lexeme + "'", message);
    }
  }

  static void runtimeError(RuntimeError error) {
    System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
    hadRuntimeError = true;
  }

}
