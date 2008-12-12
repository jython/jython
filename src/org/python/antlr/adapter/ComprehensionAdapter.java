package org.python.antlr.adapter;

import java.util.ArrayList;
import java.util.List;

import org.python.antlr.ast.comprehension;
import org.python.core.Py;
import org.python.core.PyObject;

public class ComprehensionAdapter implements AstAdapter {

    public Object py2ast(PyObject o) {
        if (o == null) {
            return o;
        }
        if (o instanceof comprehension) {
            return o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to comprehension node");
    }

    public PyObject ast2py(Object o) {
        return (PyObject)o;
    }

    public List iter2ast(PyObject iter) {
        List<comprehension> comprehensions = new ArrayList<comprehension>();
        for(Object o : (Iterable)iter) {
            comprehensions.add((comprehension)py2ast((PyObject)o));
        }
        return comprehensions;
    }
}
