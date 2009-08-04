package org.python.antlr.adapter;

import java.util.ArrayList;
import java.util.List;

import org.python.antlr.ast.comprehension;
import org.python.core.Py;
import org.python.core.PyObject;

public class ComprehensionAdapter implements AstAdapter {

    public Object py2ast(PyObject o) {
        if (o instanceof comprehension) {
            return o;
        }
        return null;
    }

    public PyObject ast2py(Object o) {
        if (o == null) {
            return Py.None;
        }
        return (PyObject)o;
    }

    public List iter2ast(PyObject iter) {
        List<comprehension> comprehensions = new ArrayList<comprehension>();
        if (iter != Py.None) {
            for(Object o : (Iterable)iter) {
                comprehensions.add((comprehension)py2ast((PyObject)o));
            }
        }
        return comprehensions;
    }
}
