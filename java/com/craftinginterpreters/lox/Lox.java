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
  /**
   * Check if there is error or not
   */
  static boolean hadError = false;

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

    // For now, just print the tokens.
    for (Token token : tokens) {
      System.out.println(token);
    }
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
  private static void report(int line, String where,
      String message) {
    System.err.println("[line " + line + "] Error" + where + ": " + message);
    hadError = true;
  }

}
