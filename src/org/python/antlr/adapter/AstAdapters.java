package org.python.antlr.adapter;

import org.python.antlr.ast.*;
import org.python.antlr.base.*;
import org.python.antlr.op.*;
import org.python.core.*;

/**
 * AstAdapter turns Python and Java objects into ast nodes.
 */
@SuppressWarnings("unchecked")
public class AstAdapters {
    public final static AliasAdapter aliasAdapter = new AliasAdapter();
    public final static CmpopAdapter cmpopAdapter = new CmpopAdapter();
    public final static ComprehensionAdapter comprehensionAdapter = new ComprehensionAdapter();
    public final static ExcepthandlerAdapter excepthandlerAdapter = new ExcepthandlerAdapter();
    public final static ExprAdapter exprAdapter = new ExprAdapter();
    public final static IdentifierAdapter identifierAdapter = new IdentifierAdapter();
    public final static KeywordAdapter keywordAdapter = new KeywordAdapter();
    public final static SliceAdapter sliceAdapter = new SliceAdapter();
    public final static StmtAdapter stmtAdapter = new StmtAdapter();

    public static java.util.List<alias> py2aliasList(PyObject o) {
        return (java.util.List<alias>)aliasAdapter.iter2ast(o);
    }

    public static java.util.List<cmpopType> py2cmpopList(PyObject o) {
        return (java.util.List<cmpopType>)cmpopAdapter.iter2ast(o);
    }

    public static java.util.List<comprehension> py2comprehensionList(PyObject o) {
        return (java.util.List<comprehension>)comprehensionAdapter.iter2ast(o);
    }

    public static java.util.List<excepthandler> py2excepthandlerList(PyObject o) {
        return (java.util.List<excepthandler>)excepthandlerAdapter.iter2ast(o);
    }

    public static java.util.List<expr> py2exprList(PyObject o) {
        return (java.util.List<expr>)exprAdapter.iter2ast(o);
    }

    public static java.util.List<String> py2identifierList(PyObject o) {
        return (java.util.List<String>)identifierAdapter.iter2ast(o);
    }

    public static java.util.List<keyword> py2keywordList(PyObject o) {
        return (java.util.List<keyword>)keywordAdapter.iter2ast(o);
    }

    public static java.util.List<slice> py2sliceList(PyObject o) {
        return (java.util.List<slice>)sliceAdapter.iter2ast(o);
    }

    public static java.util.List<stmt> py2stmtList(PyObject o) {
        return (java.util.List<stmt>)stmtAdapter.iter2ast(o);
    }

    public static expr py2expr(PyObject o) {
        return (expr)exprAdapter.py2ast(o);
    }

    public static Integer py2int(Object o) {
        if (o == null || o instanceof Integer) {
            return (Integer) o;
        } else if (o instanceof PyInteger) {
            return ((PyInteger) o).getValue();
        }
        return null;
    }

    public static String py2identifier(PyObject o) {
        return (String)identifierAdapter.py2ast(o);
    }

    public static expr_contextType py2expr_context(Object o) {
        if (o == null || o instanceof expr_contextType) {
            return (expr_contextType)o;
        }
        if (o instanceof PyObject && o != Py.None) {
            switch (((PyObject)o).asInt()) {
                case 1:
                    return expr_contextType.Load;
                case 2:
                    return expr_contextType.Store;
                case 3:
                    return expr_contextType.Del;
                case 4:
                    return expr_contextType.AugLoad;
                case 5:
                    return expr_contextType.AugStore;
                case 6:
                    return expr_contextType.Param;
                default:
                    return expr_contextType.UNDEFINED;
            }
        }
        return expr_contextType.UNDEFINED;
    }

    public static slice py2slice(PyObject o) {
        return (slice)sliceAdapter.py2ast(o);
    }

    public static stmt py2stmt(PyObject o) {
        return (stmt)stmtAdapter.py2ast(o);
    }

    //XXX: Unnecessary but needs to be fixed in the code generation of asdl_antlr.py
    public static Object py2string(Object o) {
        if (o instanceof PyString) {
            return o;
        }
        return null;
    }

    public static operatorType py2operator(Object o) {
        if (o == null || o instanceof operatorType) {
            return (operatorType)o;
        } else if (o instanceof PyObject && o != Py.None) {
            switch (((PyObject)o).asInt()) {
                case 1:
                    return operatorType.Add;
                case 2:
                    return operatorType.Sub;
                case 3:
                    return operatorType.Mult;
                case 4:
                    return operatorType.Div;
                case 5:
                    return operatorType.Mod;
                case 6:
                    return operatorType.Pow;
                case 7:
                    return operatorType.LShift;
                case 8:
                    return operatorType.RShift;
                case 9:
                    return operatorType.BitOr;
                case 10:
                    return operatorType.BitXor;
                case 11:
                    return operatorType.BitAnd;
                case 12:
                    return operatorType.FloorDiv;
                default:
                    return operatorType.UNDEFINED;
            }
        }
        return operatorType.UNDEFINED;
    }

    public static PyObject operator2py(operatorType o) {
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
            default:
                return Py.None;
        }
    }

    public static PyObject boolop2py(boolopType o) {
        switch (o) {
            case And:
                return new And();
            case Or: 
                return new Or();
            default:
                return Py.None;
        }
    }

    public static PyObject cmpop2py(cmpopType o) {
        switch (o) {
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
            default:
                return Py.None;
        }
    }

    public static PyObject unaryop2py(unaryopType o) {
        switch (o) {
            case Invert:
                return new Invert();
            case Not: 
                return new Not();
            case UAdd: 
                return new UAdd();
            case USub: 
                return new USub();
            default:
                return Py.None;
        }
    }


    public static PyObject expr_context2py(expr_contextType o) {
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
            default:
                return Py.None;
        }
    }

    public static boolopType py2boolop(Object o) {
        if (o == null || o instanceof boolopType) {
            return (boolopType)o;
        }
        if (o instanceof PyObject && o != Py.None) {
            switch (((PyObject)o).asInt()) {
                case 1:
                    return boolopType.And;
                case 2:
                    return boolopType.Or;
                default:
                    return boolopType.UNDEFINED;
            }
        }
        return boolopType.UNDEFINED;
    }

    public static arguments py2arguments(Object o) {
        if (o instanceof arguments) {
            return (arguments)o;
        }
        return null;
    }

    //XXX: clearly this isn't necessary -- need to adjust the code generation.
    public static Object py2object(Object o) {
        return o;
    }

    public static Boolean py2bool(Object o) {
        if (o == null || o instanceof Boolean) {
            return (Boolean)o;
        } else if (o instanceof PyBoolean) {
            return ((PyBoolean) o).getBooleanValue();
        }
        return null;
    }

    public static unaryopType py2unaryop(Object o) {
        if (o == null || o instanceof unaryopType) {
            return (unaryopType)o;
        }
        if (o instanceof PyObject && o != Py.None) {
            switch (((PyObject)o).asInt()) {
                case 1:
                    return unaryopType.Invert;
                case 2:
                    return unaryopType.Not;
                case 3:
                    return unaryopType.UAdd;
                case 4:
                    return unaryopType.USub;
                default:
                    return unaryopType.UNDEFINED;
            }
        }
        return unaryopType.UNDEFINED;
    }
}
