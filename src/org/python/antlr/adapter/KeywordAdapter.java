package org.python.antlr.adapter;

import org.python.core.Py;
import org.python.core.PyJavaInstance;
import org.python.core.PyObject;

import org.python.antlr.ast.keywordType;

import java.util.ArrayList;
import java.util.List;

public class KeywordAdapter implements AstAdapter {

    public Object py2ast(PyObject o) {
        if (o == null) {
            return o;
        }
        if (o instanceof keywordType) {
            return o;
        }

        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to keyword node");
    }

    public PyObject ast2py(Object o) {
        return (PyObject)o;
    }

    public List iter2ast(PyObject iter) {
        List<keywordType> keywords = new ArrayList<keywordType>();
        for(Object o : (Iterable)iter) {
            keywords.add((keywordType)py2ast((PyObject)o));
        }
        return keywords;
    }
}
