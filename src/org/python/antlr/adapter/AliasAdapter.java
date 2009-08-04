package org.python.antlr.adapter;

import java.util.ArrayList;
import java.util.List;

import org.python.antlr.ast.alias;
import org.python.core.Py;
import org.python.core.PyObject;

public class AliasAdapter implements AstAdapter {

    public Object py2ast(PyObject o) {
        if (o instanceof alias) {
            return o;
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
        List<alias> aliases = new ArrayList<alias>();
        if (iter != Py.None) {
            for(Object o : (Iterable)iter) {
                aliases.add((alias)py2ast((PyObject)o));
            }
        }
        return aliases;
    }
}
