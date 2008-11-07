package org.python.antlr;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.Token;

public class BaseParser {

    protected CharStream charStream;
    protected boolean partial;
    protected String filename;
    protected String encoding;
    protected ErrorHandler errorHandler = new FailFastHandler();

    public void setAntlrErrorHandler(ErrorHandler eh) {
        this.errorHandler = eh;
    }

    public static class PyLexer extends PythonLexer {
        public PyLexer(CharStream lexer) {
            super(lexer);
        }

        public Token nextToken() {
            startPos = getCharPositionInLine();
            return super.nextToken();
        }
    }
}
