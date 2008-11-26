package org.python.antlr.adapter;

import org.python.core.Py;
import org.python.core.PyJavaInstance;

import org.python.antlr.ast.ExceptHandler;
import org.python.antlr.ast.Num;

import java.util.ArrayList;
import java.util.List;

public class ExcepthandlerAdapter implements AstAdapter {

    public Object adapt(Object o) {
        if (o == null) {
            return o;
        }
        if (o instanceof PyJavaInstance) {
            o = ((PyJavaInstance)o).__tojava__(ExceptHandler.class);
        }
        if (o instanceof ExceptHandler) {
            return o;
        }

        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to excepthandler node");
    }

    public Object adaptIter(Object iter) {
        List<ExceptHandler> excepthandlers = new ArrayList<ExceptHandler>();
        for(Object o : (Iterable)iter) {
            excepthandlers.add((ExceptHandler)adapt(o));
        }
        return excepthandlers;
    }

}
