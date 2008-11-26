package org.python.antlr.adapter;

import org.python.core.Py;
import org.python.core.PyJavaInstance;

import org.python.antlr.ast.cmpopType;

import java.util.ArrayList;
import java.util.List;

public class CmpopAdapter implements AstAdapter {

    public Object adapt(Object o) {
        if (o == null) {
            return o;
        }
        if (o instanceof PyJavaInstance) {
            o = ((PyJavaInstance)o).__tojava__(cmpopType.class);
        }
        if (o instanceof cmpopType) {
            return o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to cmpop node");
    }

    public Object adaptIter(Object iter) {
        List<cmpopType> cmpops = new ArrayList<cmpopType>();
        for(Object o : (Iterable)iter) {
            cmpops.add((cmpopType)adapt(o));
        }
        return cmpops;
    }
}
