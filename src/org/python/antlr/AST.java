package org.python.antlr;

import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedType;

@ExposedType(name = "_ast.AST", base = PyObject.class)
public class AST extends PyObject {
    public static final PyType TYPE = PyType.fromClass(AST.class);
    
    public AST() {
    }

    public AST(PyType objtype) {
        super(objtype);
    }

}
