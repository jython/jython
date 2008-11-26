package org.python.antlr.adapter;

import org.python.core.Py;
import org.python.core.PyJavaInstance;

import org.python.antlr.ast.excepthandlerType;
import org.python.antlr.ast.Num;

import java.util.ArrayList;
import java.util.List;

public class ExcepthandlerAdapter implements AstAdapter {

    public Object adapt(Object o) {
        if (o == null) {
            return o;
        }
        if (o instanceof PyJavaInstance) {
            o = ((PyJavaInstance)o).__tojava__(excepthandlerType.class);
        }
        if (o instanceof excepthandlerType) {
            return o;
        }

        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to excepthandler node");
    }

    public Object adaptIter(Object iter) {
        List<excepthandlerType> excepthandlers = new ArrayList<excepthandlerType>();
        for(Object o : (Iterable)iter) {
            excepthandlers.add((excepthandlerType)adapt(o));
        }
        return excepthandlers;
    }

}
