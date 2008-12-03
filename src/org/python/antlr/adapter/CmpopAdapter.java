package org.python.antlr.adapter;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyJavaInstance;

import org.python.antlr.ast.cmpopType;
import org.python.antlr.ast.Eq;
import org.python.antlr.ast.NotEq;
import org.python.antlr.ast.Lt;
import org.python.antlr.ast.LtE;
import org.python.antlr.ast.Gt;
import org.python.antlr.ast.GtE;
import org.python.antlr.ast.Is;
import org.python.antlr.ast.IsNot;
import org.python.antlr.ast.In;
import org.python.antlr.ast.NotIn;

import java.util.ArrayList;
import java.util.List;

public class CmpopAdapter implements AstAdapter {

    public Object py2ast(PyObject o) {
        if (o == null) {
            return o;
        }
        return o;
        //FIXME: investigate the right exception
        //throw Py.TypeError("Can't convert " + o.getClass().getName() + " to cmpop node");
    }

    public PyObject ast2py(Object o) {
        switch ((cmpopType)o) {
            case Eq:
                return new Eq();
            case NotEq: 
                return new NotEq();
            case Lt: 
                return new Lt();
            case LtE: 
                return new LtE();
            case Gt: 
                return new Gt();
            case GtE: 
                return new GtE();
            case Is: 
                return new Is();
            case IsNot: 
                return new IsNot();
            case In: 
                return new In();
            case NotIn: 
                return new NotIn();
        }
        return Py.None;
    }

    public List iter2ast(PyObject iter) {
        List<cmpopType> cmpops = new ArrayList<cmpopType>();
        for(Object o : (Iterable)iter) {
            cmpops.add((cmpopType)py2ast((PyObject)o));
        }
        return cmpops;
    }
}
