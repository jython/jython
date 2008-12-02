package org.python.antlr.adapter;

import org.python.core.Py;
import org.python.core.PyJavaInstance;
import org.python.core.PyObject;

import org.python.antlr.ast.Num;

import java.util.ArrayList;
import java.util.List;

public class IdentifierAdapter implements AstAdapter {

    public Object py2ast(PyObject o) {
        return o.toString();
    }

    public PyObject ast2py(Object o) {
        return (PyObject)o;
    }

    public List iter2ast(PyObject iter) {
        List<String> identifiers = new ArrayList<String>();
        for(Object o : (Iterable)iter) {
            identifiers.add((String)py2ast((PyObject)o));
        }
        return identifiers;
    }

}
