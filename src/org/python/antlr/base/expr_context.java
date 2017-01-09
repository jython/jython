// Hand copied from stmt.
// XXX: autogenerate this.
package org.python.antlr.base;

import org.antlr.runtime.Token;
import org.python.antlr.AST;
import org.python.antlr.PythonTree;
import org.python.core.PyString;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedType;

@ExposedType(name = "_ast.expr_context", base = AST.class)
public abstract class expr_context extends PythonTree {

    public static final PyType TYPE = PyType.fromClass(expr_context.class);
    private final static PyString[] fields = new PyString[0];
    @ExposedGet(name = "_fields")
    public PyString[] get_fields() { return fields; }

    private final static PyString[] attributes =
    new PyString[] {new PyString("lineno"), new PyString("col_offset")};
    @ExposedGet(name = "_attributes")
    public PyString[] get_attributes() { return attributes; }

    public expr_context() {
    }

    public expr_context(PyType subType) {
    }

    public expr_context(int ttype, Token token) {
        super(ttype, token);
    }

    public expr_context(Token token) {
        super(token);
    }

    public expr_context(PythonTree node) {
        super(node);
    }

}
