package org.python.antlr.adapter;

import org.python.core.Py;
import org.python.core.PyJavaInstance;
import org.python.core.PyObject;

import org.python.antlr.ast.stmtType;

import java.util.ArrayList;
import java.util.List;

public class StmtAdapter implements AstAdapter {

    public Object py2ast(PyObject o) {
        if (o == null) {
            return o;
        }
        if (o instanceof stmtType) {
            return o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to stmt node");
    }

    public PyObject ast2py(Object o) {
        return (PyObject)o;
    }

    public List iter2ast(PyObject iter) {
        List<stmtType> stmts = new ArrayList<stmtType>();
        for(Object o : (Iterable)iter) {
            stmts.add((stmtType)py2ast((PyObject)o));
        }
        return stmts;
    }
}
