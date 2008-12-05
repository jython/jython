package org.python.antlr.adapter;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyJavaInstance;

import org.python.antlr.ast.alias;

import java.util.ArrayList;
import java.util.List;

public class AliasAdapter implements AstAdapter {

    public Object py2ast(PyObject o) {
        if (o == null) {
            return o;
        }
        if (o instanceof alias) {
            return o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to alias node");
    }

    public PyObject ast2py(Object o) {
        return (PyObject)o;
    }

    public List iter2ast(PyObject iter) {
        List<alias> aliases = new ArrayList<alias>();
        for(Object o : (Iterable)iter) {
            aliases.add((alias)py2ast((PyObject)(PyObject)o));
        }
        return aliases;
    }
}
