package org.python.antlr;

import org.python.antlr.PythonTree;

public class ParseException extends Exception {

    public PythonTree currentToken = null;

    public ParseException() {
        super();
    }

    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, PythonTree node) {
        super(message);
        this.currentToken = node;
    }
}
