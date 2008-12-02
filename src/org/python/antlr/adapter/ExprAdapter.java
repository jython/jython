package org.python.antlr.adapter;

import org.python.core.Py;
import org.python.core.PyComplex;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyJavaInstance;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyUnicode;

import org.python.antlr.ast.exprType;
import org.python.antlr.ast.Num;
import org.python.antlr.ast.Str;

import java.util.ArrayList;
import java.util.List;

public class ExprAdapter implements AstAdapter {
        
    public Object py2ast(PyObject o) {
        if (o == null) {
            return o;
        }
        if (o instanceof exprType) {
            return o;
        } else if (o instanceof PyInteger || o instanceof PyLong || o instanceof PyFloat || o instanceof PyComplex) {
            return new Num(o);
        } else if (o instanceof PyString || o instanceof PyUnicode) {
            return new Str(o);
        }

        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to expr node");
    }

    public PyObject ast2py(Object o) {
        return (PyObject)o;
    }

    public List iter2ast(PyObject iter) {
        List<exprType> exprs = new ArrayList<exprType>();
        for(Object o : (Iterable)iter) {
            exprs.add((exprType)py2ast((PyObject)o));
        }
        return exprs;
    }

}
