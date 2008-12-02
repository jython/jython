package org.python.antlr.adapter;

import org.python.antlr.ast.*;
import org.python.core.*;

import java.util.ArrayList;
/**
 * AstAdapter turns Python and Java objects into ast nodes.
 */
public class AstAdapters {
    public static AliasAdapter aliasAdapter = new AliasAdapter();
    public static CmpopAdapter cmpopAdapter = new CmpopAdapter();
    public static ComprehensionAdapter comprehensionAdapter = new ComprehensionAdapter();
    public static ExcepthandlerAdapter excepthandlerAdapter = new ExcepthandlerAdapter();
    public static ExprAdapter exprAdapter = new ExprAdapter();
    public static IdentifierAdapter identifierAdapter = new IdentifierAdapter();
    public static KeywordAdapter keywordAdapter = new KeywordAdapter();
    public static SliceAdapter sliceAdapter = new SliceAdapter();
    public static StmtAdapter stmtAdapter = new StmtAdapter();

    public static java.util.List<aliasType> py2aliasList(PyObject o) {
        return (java.util.List<aliasType>)aliasAdapter.iter2ast(o);
    }

    public static java.util.List<cmpopType> py2cmpopList(PyObject o) {
        return (java.util.List<cmpopType>)cmpopAdapter.iter2ast(o);
    }

    public static java.util.List<comprehensionType> py2comprehensionList(PyObject o) {
        return (java.util.List<comprehensionType>)comprehensionAdapter.iter2ast(o);
    }

    public static java.util.List<excepthandlerType> py2excepthandlerList(PyObject o) {
        return (java.util.List<excepthandlerType>)excepthandlerAdapter.iter2ast(o);
    }

    public static java.util.List<exprType> py2exprList(PyObject o) {
        return (java.util.List<exprType>)exprAdapter.iter2ast(o);
    }

    public static java.util.List<String> py2identifierList(PyObject o) {
        return (java.util.List<String>)identifierAdapter.iter2ast(o);
    }

    public static java.util.List<keywordType> py2keywordList(PyObject o) {
        return (java.util.List<keywordType>)keywordAdapter.iter2ast(o);
    }

    public static java.util.List<sliceType> py2sliceList(PyObject o) {
        return (java.util.List<sliceType>)sliceAdapter.iter2ast(o);
    }

    public static java.util.List<stmtType> py2stmtList(PyObject o) {
        return (java.util.List<stmtType>)stmtAdapter.iter2ast(o);
    }

    public static exprType py2expr(PyObject o) {
        return (exprType)exprAdapter.py2ast(o);
    }

    public static int py2int(Object o) {
        if (o == null || o instanceof Integer) {
            return (Integer)o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to int node");
    }

    public static String py2identifier(PyObject o) {
        return (String)identifierAdapter.py2ast(o);
    }

    public static expr_contextType py2expr_context(Object o) {
        if (o == null || o instanceof expr_contextType) {
            return (expr_contextType)o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to expr_context node");
    }

    public static sliceType py2slice(PyObject o) {
        return (sliceType)sliceAdapter.py2ast(o);
    }

    public static stmtType py2stmt(PyObject o) {
        return (stmtType)stmtAdapter.py2ast(o);
    }

    public static String py2string(Object o) {
        if (o == null || o instanceof String) {
            return (String)o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to string node");
    }

    public static operatorType py2operator(Object o) {
        if (o == null || o instanceof operatorType) {
            return (operatorType)o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to operator node");
    }

    public static PyObject op2py(operatorType o) {
        switch (o) {
            case Add:
                return new Add();
            case Sub: 
                return new Sub();
            case Mult:
                return new Mult();
            case Div:
                return new Div();
            case Mod:
                return new Mod();
            case Pow:
                return new Pow();
            case LShift:
                return new LShift();
            case RShift:
                return new RShift();
            case BitOr:
                return new BitOr();
            case BitXor:
                return new BitXor();
            case BitAnd:
                return new BitAnd();
            case FloorDiv:
                return new FloorDiv();
        }
        return Py.None;
    }

    public static PyObject op2py(boolopType o) {
        switch (o) {
            case And:
                return new And();
            case Or: 
                return new Or();
        }
        return Py.None;
    }

    public static PyObject op2py(unaryopType o) {
        switch (o) {
            case Invert:
                return new Invert();
            case Not: 
                return new Not();
            case UAdd: 
                return new UAdd();
            case USub: 
                return new USub();
        }
        return Py.None;
    }


    public static PyObject ctx2py(expr_contextType o) {
        switch (o) {
            case Load:
                return new Load();
            case Store: 
                return new Store();
            case Del:
                return new Del();
            case AugLoad:
                return new AugLoad();
            case AugStore: 
                return new AugStore();
            case Param: 
                return new Param();
        }
        return Py.None;
    }

    public static boolopType py2boolop(Object o) {
        if (o == null || o instanceof boolopType) {
            return (boolopType)o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to boolop node");
    }

    public static argumentsType py2arguments(Object o) {
        if (o == null || o instanceof argumentsType) {
            return (argumentsType)o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to arguments node");
    }

    //XXX: clearly this isn't necessary -- need to adjust the code generation.
    public static Object py2object(Object o) {
        return o;
    }

    public static Boolean py2bool(Object o) {
        if (o == null || o instanceof Boolean) {
            return (Boolean)o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to Boolean node");
    }

    public static unaryopType py2unaryop(Object o) {
        if (o == null || o instanceof unaryopType) {
            return (unaryopType)o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to unaryop node");
    }

}
