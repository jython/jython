// Copyright © Corporation for National Research Initiatives
package org.python.core;

public class PySyntaxError extends PyException {
    int lineno, column;
    String text;
    String filename;
    public boolean forceNewline;

    public PySyntaxError(String s, int line, int column,
                         String text, String filename)
    {
        this(s, line, column, text, filename, false);
    }

    public PySyntaxError(String s, int line, int column, String text,
                         String filename, boolean forceNewline)
    {
        super(Py.SyntaxError);
        PyObject[] tmp = new PyObject[] {
            new PyString(filename), new PyInteger(line),
            new PyInteger(column), new PyString(text)
        };

        this.value = new PyTuple(new PyObject[] {
            new PyString(s), new PyTuple(tmp)
        });

        this.lineno = line;
        this.column = column;
        this.text = text;
        this.filename = filename;
        this.forceNewline = forceNewline;
    }
}
