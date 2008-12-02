package org.python.antlr.adapter;

import org.python.core.Py;
import org.python.core.PyJavaInstance;
import org.python.core.PyObject;

import org.python.antlr.ast.sliceType;
import org.python.antlr.ast.Num;

import java.util.ArrayList;
import java.util.List;

public class SliceAdapter implements AstAdapter {

    public Object py2ast(PyObject o) {
        if (o == null) {
            return o;
        }
        if (o instanceof sliceType) {
            return o;
        }

        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to slice node");
    }

    public PyObject ast2py(Object o) {
        return (PyObject)o;
    }

    public List iter2ast(PyObject iter) {
        List<sliceType> slices = new ArrayList<sliceType>();
        for(Object o : (Iterable)iter) {
            slices.add((sliceType)py2ast((PyObject)o));
        }
        return slices;
    }

}
