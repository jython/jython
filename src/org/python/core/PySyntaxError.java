// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

/**
 * A convenience class for creating Syntax errors. Note that the
 * syntax error is still taken from Py.SyntaxError.
 * <p>
 * Generally subclassing from PyException is not the right way
 * of creating new exception classes.
 */

public class PySyntaxError extends PyException {
    int lineno;
    int column;
    String text;
    String filename;


    public PySyntaxError(String s, int line, int column, String text, String filename) {
        super(Py.SyntaxError);
        // XXX: null text causes Java error, though I bet I'm not supposed to get null text.
        PyString pyText = text == null ? Py.EmptyString : Py.newString(text);
        PyObject[] tmp = new PyObject[] {Py.fileSystemEncode(filename), new PyInteger(line),
                new PyInteger(column), pyText};

        this.value = new PyTuple(Py.newString(s), new PyTuple(tmp));

        this.lineno = line;
        this.column = column;
        this.text = text;
        this.filename = filename;
    }
}
