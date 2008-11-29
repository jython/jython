package org.python.antlr;

import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedType;

@ExposedType(name = "_ast.AST", base = PyObject.class)
public abstract class AST extends PyObject {
    public static final PyType TYPE = PyType.fromClass(AST.class);
    public static String[] emptyStringArray = new String[0];

}
