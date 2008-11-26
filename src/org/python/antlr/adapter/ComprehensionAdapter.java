package org.python.antlr.adapter;

import org.python.core.Py;
import org.python.core.PyJavaInstance;

import org.python.antlr.ast.comprehensionType;

import java.util.ArrayList;
import java.util.List;

public class ComprehensionAdapter implements AstAdapter {

    public Object adapt(Object o) {
        if (o == null) {
            return o;
        }
        if (o instanceof PyJavaInstance) {
            o = ((PyJavaInstance)o).__tojava__(comprehensionType.class);
        }
        if (o instanceof comprehensionType) {
            return o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to comprehension node");
    }

    public Object adaptIter(Object iter) {
        List<comprehensionType> comprehensions = new ArrayList<comprehensionType>();
        for(Object o : (Iterable)iter) {
            comprehensions.add((comprehensionType)adapt(o));
        }
        return comprehensions;
    }
}
