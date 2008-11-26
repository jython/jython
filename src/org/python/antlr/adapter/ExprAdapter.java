package org.python.antlr.adapter;

import org.python.core.Py;
import org.python.core.PyInteger;
import org.python.core.PyJavaInstance;

import org.python.antlr.ast.exprType;
import org.python.antlr.ast.Num;

import java.util.ArrayList;
import java.util.List;

public class ExprAdapter implements AstAdapter {

    public Object adapt(Object o) {
        if (o == null) {
            return o;
        }
        if (o instanceof PyJavaInstance) {
            o = ((PyJavaInstance)o).__tojava__(exprType.class);
        }
        if (o instanceof exprType) {
            return o;
        } else if (o instanceof Integer) {
            return new Num(new PyInteger((Integer)o));
        }

        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to expr node");
    }

    public Object adaptIter(Object iter) {
        List<exprType> exprs = new ArrayList<exprType>();
        for(Object o : (Iterable)iter) {
            exprs.add((exprType)adapt(o));
        }
        return exprs;
    }

}
