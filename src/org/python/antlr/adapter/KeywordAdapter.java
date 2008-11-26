package org.python.antlr.adapter;

import org.python.core.Py;
import org.python.core.PyJavaInstance;

import org.python.antlr.ast.keywordType;

import java.util.ArrayList;
import java.util.List;

public class KeywordAdapter implements AstAdapter {

    public Object adapt(Object o) {
        if (o == null) {
            return o;
        }
        if (o instanceof PyJavaInstance) {
            o = ((PyJavaInstance)o).__tojava__(keywordType.class);
        }
        if (o instanceof keywordType) {
            return o;
        }

        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to keyword node");
    }

    public Object adaptIter(Object iter) {
        List<keywordType> keywords = new ArrayList<keywordType>();
        for(Object o : (Iterable)iter) {
            keywords.add((keywordType)adapt(o));
        }
        return keywords;
    }
}
