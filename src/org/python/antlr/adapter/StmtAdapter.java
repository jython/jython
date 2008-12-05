package org.python.antlr.adapter;

import org.python.core.Py;
import org.python.core.PyJavaInstance;
import org.python.core.PyObject;

import org.python.antlr.base.stmt;

import java.util.ArrayList;
import java.util.List;

public class StmtAdapter implements AstAdapter {

    public Object py2ast(PyObject o) {
        if (o == null) {
            return o;
        }
        if (o instanceof stmt) {
            return o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to stmt node");
    }

    public PyObject ast2py(Object o) {
        return (PyObject)o;
    }

    public List iter2ast(PyObject iter) {
        List<stmt> stmts = new ArrayList<stmt>();
        for(Object o : (Iterable)iter) {
            stmts.add((stmt)py2ast((PyObject)o));
        }
        return stmts;
    }
}
