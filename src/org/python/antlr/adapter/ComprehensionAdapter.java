package org.python.antlr.adapter;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyJavaInstance;

import org.python.antlr.ast.comprehensionType;

import java.util.ArrayList;
import java.util.List;

public class ComprehensionAdapter implements AstAdapter {

    public Object py2ast(PyObject o) {
        if (o == null) {
            return o;
        }
        if (o instanceof comprehensionType) {
            return o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to comprehension node");
    }

    public PyObject ast2py(Object o) {
        return (PyObject)o;
    }

    public List iter2ast(PyObject iter) {
        List<comprehensionType> comprehensions = new ArrayList<comprehensionType>();
        for(Object o : (Iterable)iter) {
            comprehensions.add((comprehensionType)py2ast((PyObject)o));
        }
        return comprehensions;
    }
}
