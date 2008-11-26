package org.python.antlr.adapter;

import org.python.core.Py;
import org.python.core.PyJavaInstance;

import org.python.antlr.ast.stmtType;

import java.util.ArrayList;
import java.util.List;

public class StmtAdapter implements AstAdapter {

    public Object adapt(Object o) {
        if (o == null) {
            return o;
        }
        if (o instanceof PyJavaInstance) {
            o = ((PyJavaInstance)o).__tojava__(stmtType.class);
        }
        if (o instanceof stmtType) {
            return o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to stmt node");
    }

    public Object adaptIter(Object iter) {
        List<stmtType> stmts = new ArrayList<stmtType>();
        for(Object o : (Iterable)iter) {
            stmts.add((stmtType)adapt(o));
        }
        return stmts;
    }
}
