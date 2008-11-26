package org.python.antlr.adapter;

import org.python.core.Py;
import org.python.core.PyJavaInstance;

import org.python.antlr.ast.sliceType;
import org.python.antlr.ast.Num;

import java.util.ArrayList;
import java.util.List;

public class SliceAdapter implements AstAdapter {

    public Object adapt(Object o) {
        if (o == null) {
            return o;
        }
        if (o instanceof PyJavaInstance) {
            o = ((PyJavaInstance)o).__tojava__(sliceType.class);
        }
        if (o instanceof sliceType) {
            return o;
        }

        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to slice node");
    }

    public Object adaptIter(Object iter) {
        List<sliceType> slices = new ArrayList<sliceType>();
        for(Object o : (Iterable)iter) {
            slices.add((sliceType)adapt(o));
        }
        return slices;
    }

}
