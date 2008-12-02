package org.python.antlr.adapter;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyJavaInstance;

import org.python.antlr.ast.cmpopType;

import java.util.ArrayList;
import java.util.List;

public class CmpopAdapter implements AstAdapter {

    public Object py2ast(PyObject o) {
        if (o == null) {
            return o;
        }
        return o;
        //FIXME: investigate the right exception
        //throw Py.TypeError("Can't convert " + o.getClass().getName() + " to cmpop node");
    }

    public PyObject ast2py(Object o) {
        return (PyObject)o;
    }

    public List iter2ast(PyObject iter) {
        List<cmpopType> cmpops = new ArrayList<cmpopType>();
        for(Object o : (Iterable)iter) {
            cmpops.add((cmpopType)py2ast((PyObject)o));
        }
        return cmpops;
    }
}
