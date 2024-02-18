package org.python.antlr;

import org.antlr.runtime.IntStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.python.core.Py;
import org.python.core.PyObject;

public class ParseException extends RuntimeException {

    public transient IntStream input;
    public int index;
    public Token token;
    public Object node;
    public int c;
    public int line;
    public int charPositionInLine;
    public boolean approximateLineInfo;
    public boolean definite;

    private PyObject type = Py.SyntaxError;

    public ParseException() {
        super();
    }

    public ParseException(String message, int lin, int charPos) {
        super(message);
        this.line = lin;
        this.charPositionInLine = charPos;
    }

    public ParseException(String message) {
        this(message, 0, 0);
    }

    /**
     * Construct a {@code ParseException} specifying whether to treat as a final decision. When we
     * use this constructor in the REPL, and {@code definite=true}, Jython will take it that the
     * problem is a definite syntax or semantic error, not that we just haven't finished a
     * multi-line construct that will be valid eventually.
     *
     * @param message text to include.
     * @param n not {@code null}, root of the {@link PythonTree} provoking the error.
     * @param definite true if we cannot recover with more input.
     */
    public ParseException(String message, PythonTree n, boolean definite) {
        this(message, n.getLineno(), n.getCol_offset());
        this.node = n;
        this.token = n.getToken();
        this.definite = definite;
    }

    /**
     * Construct a {@code ParseException} specifying message and tree. When we use this constructor
     * in the REPL, Jython will assume the problem is an incomplete input (as when you wrap a line
     * inside parentheses or are inside another nested construct), and a "... " prompt is likely to
     * be produced.
     *
     * @param message text to include.
     * @param n not {@code null}, root of the {@link PythonTree} provoking the error.
     */
    public ParseException(String message, PythonTree n) {
        this(message, n, false);
    }

    public ParseException(String message, RecognitionException r) {
        super(message);
        this.input = r.input;
        this.index = r.index;
        this.token = r.token;
        this.node = r.node;
        this.c = r.c;
        this.line = r.line;
        this.charPositionInLine = r.charPositionInLine;
        this.approximateLineInfo = r.approximateLineInfo;
    }

    public void setType(PyObject t) {
        this.type = t;
    }

    public PyObject getType() {
        return this.type;
    }

}
