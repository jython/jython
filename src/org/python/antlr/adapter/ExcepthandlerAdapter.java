package org.python.antlr.adapter;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyJavaInstance;

import org.python.antlr.ast.ExceptHandler;
import org.python.antlr.ast.Num;

import java.util.ArrayList;
import java.util.List;

public class ExcepthandlerAdapter implements AstAdapter {

    public Object py2ast(PyObject o) {
        if (o == null) {
            return o;
        }
        if (o instanceof ExceptHandler) {
            return o;
        }

        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to excepthandler node");
    }

    public PyObject ast2py(Object o) {
        return (PyObject)o;
    }

    public List iter2ast(PyObject iter) {
        List<ExceptHandler> excepthandlers = new ArrayList<ExceptHandler>();
        for(Object o : (Iterable)iter) {
            excepthandlers.add((ExceptHandler)py2ast((PyObject)o));
        }
        return excepthandlers;
    }

}
