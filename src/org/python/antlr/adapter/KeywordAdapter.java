package org.python.antlr.adapter;

import java.util.ArrayList;
import java.util.List;

import org.python.antlr.ast.keyword;
import org.python.core.Py;
import org.python.core.PyObject;

public class KeywordAdapter implements AstAdapter {

    public Object py2ast(PyObject o) {
        if (o == null) {
            return o;
        }
        if (o instanceof keyword) {
            return o;
        }

        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to keyword node");
    }

    public PyObject ast2py(Object o) {
        return (PyObject)o;
    }

    public List iter2ast(PyObject iter) {
        List<keyword> keywords = new ArrayList<keyword>();
        for(Object o : (Iterable)iter) {
            keywords.add((keyword)py2ast((PyObject)o));
        }
        return keywords;
    }
}
