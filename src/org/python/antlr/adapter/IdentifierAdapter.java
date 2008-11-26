package org.python.antlr.adapter;

import org.python.core.Py;
import org.python.core.PyJavaInstance;

import org.python.antlr.ast.Num;

import java.util.ArrayList;
import java.util.List;

public class IdentifierAdapter implements AstAdapter {

    public Object adapt(Object o) {
        if (o == null) {
            return o;
        }
        if (o instanceof PyJavaInstance) {
            o = ((PyJavaInstance)o).__tojava__(String.class);
        }
        if (o instanceof String) {
            return o;
        }

        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to identifier node");
    }

    public Object adaptIter(Object iter) {
        List<String> identifiers = new ArrayList<String>();
        for(Object o : (Iterable)iter) {
            identifiers.add((String)adapt(o));
        }
        return identifiers;
    }

}
