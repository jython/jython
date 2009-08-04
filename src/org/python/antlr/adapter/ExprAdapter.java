package org.python.antlr.adapter;

import java.util.ArrayList;
import java.util.List;

import org.python.antlr.ast.Num;
import org.python.antlr.ast.Str;
import org.python.antlr.base.expr;
import org.python.core.Py;
import org.python.core.PyComplex;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyUnicode;

public class ExprAdapter implements AstAdapter {

    public Object py2ast(PyObject o) {
        if (o instanceof expr) {
            return o;
        }
        if (o instanceof PyInteger || o instanceof PyLong || o instanceof PyFloat || o instanceof PyComplex) {
            return new Num(o);
        }
        if (o instanceof PyString || o instanceof PyUnicode) {
            return new Str(o);
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
        List<expr> exprs = new ArrayList<expr>();
        if (iter != Py.None) {
            for(Object o : (Iterable)iter) {
                exprs.add((expr)py2ast((PyObject)o));
            }
        }
        return exprs;
    }
}
