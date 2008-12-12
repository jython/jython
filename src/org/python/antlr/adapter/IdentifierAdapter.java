package org.python.antlr.adapter;

import java.util.ArrayList;
import java.util.List;

import org.python.core.PyObject;
import org.python.core.PyString;

public class IdentifierAdapter implements AstAdapter {

    public Object py2ast(PyObject o) {
        return o.toString();
    }

    public PyObject ast2py(Object o) {
        return new PyString(o.toString());
    }

    public List iter2ast(PyObject iter) {
        List<String> identifiers = new ArrayList<String>();
        for(Object o : (Iterable)iter) {
            identifiers.add((String)py2ast((PyObject)o));
        }
        return identifiers;
    }

}
