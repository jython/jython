package org.python.antlr;

public class ParseException extends Exception {

  public ParseException() {
    super();
  }

  public ParseException(String message) {
    super(message);
  }

  public ParseException(String message, PythonTree node) {
    super(message);
  }

}
