package org.python.antlr.adapter;

import java.util.ArrayList;
import java.util.List;

import org.python.antlr.ast.keyword;
import org.python.core.Py;
import org.python.core.PyObject;

public class KeywordAdapter implements AstAdapter {

    public Object py2ast(PyObject o) {
        if (o instanceof keyword) {
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
        List<keyword> keywords = new ArrayList<keyword>();
        if (iter != Py.None) {
            for(Object o : (Iterable)iter) {
                keywords.add((keyword)py2ast((PyObject)o));
            }
        }
        return keywords;
    }
}
