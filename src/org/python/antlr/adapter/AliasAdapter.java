package org.python.antlr.adapter;

import org.python.core.Py;
import org.python.core.PyJavaInstance;

import org.python.antlr.ast.aliasType;

import java.util.ArrayList;
import java.util.List;

public class AliasAdapter implements AstAdapter {

    public Object adapt(Object o) {
        if (o == null) {
            return o;
        }
        if (o instanceof PyJavaInstance) {
            o = ((PyJavaInstance)o).__tojava__(aliasType.class);
        }
        if (o instanceof aliasType) {
            return o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to alias node");
    }

    public Object adaptIter(Object iter) {
        List<aliasType> aliases = new ArrayList<aliasType>();
        for(Object o : (Iterable)iter) {
            aliases.add((aliasType)adapt(o));
        }
        return aliases;
    }
}
