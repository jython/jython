package org.python.antlr.adapter;

import java.util.ArrayList;
import java.util.List;

import org.python.antlr.ast.ExceptHandler;
import org.python.core.Py;
import org.python.core.PyObject;

public class ExcepthandlerAdapter implements AstAdapter {

    public Object py2ast(PyObject o) {
        if (o instanceof ExceptHandler) {
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
        List<ExceptHandler> excepthandlers = new ArrayList<ExceptHandler>();
        if (iter != Py.None) {
            for(Object o : (Iterable)iter) {
                excepthandlers.add((ExceptHandler)py2ast((PyObject)o));
            }
        }
        return excepthandlers;
    }
}
