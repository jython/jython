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

@ExposedType(name = "_ast.cmpop", base = AST.class)
public abstract class cmpop extends PythonTree {

    public static final PyType TYPE = PyType.fromClass(cmpop.class);
    private final static PyString[] fields = new PyString[0];
    @ExposedGet(name = "_fields")
    public PyString[] get_fields() { return fields; }

    private final static PyString[] attributes =
    new PyString[] {new PyString("lineno"), new PyString("col_offset")};
    @ExposedGet(name = "_attributes")
    public PyString[] get_attributes() { return attributes; }

    public cmpop() {
    }

    public cmpop(PyType subType) {
    }

    public cmpop(int ttype, Token token) {
        super(ttype, token);
    }

    public cmpop(Token token) {
        super(token);
    }

    public cmpop(PythonTree node) {
        super(node);
    }

}
