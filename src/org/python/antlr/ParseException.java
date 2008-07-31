package org.python.antlr;

import org.python.antlr.PythonTree;
import org.antlr.runtime.*;

public class ParseException extends RuntimeException {
	public transient IntStream input;
	public int index;
	public Token token;
	public Object node;
	public int c;
	public int line;
	public int charPositionInLine;
	public boolean approximateLineInfo;

    public ParseException() {
        super();
    }

    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, PythonTree node) {
        super(message);
    }

    public ParseException(String message, RecognitionException r) {
        super(message);
        input = r.input;
        index = r.index;
        token = r.token;
        node = r.node;
        c = r.c;
        line = r.line;
        charPositionInLine = r.charPositionInLine;
        approximateLineInfo = r.approximateLineInfo;
    }

}
