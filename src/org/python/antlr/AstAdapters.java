package org.python.antlr;

import org.python.antlr.ast.*;
import org.python.core.*;

/**
 * AstAdapter turns Python and Java objects into ast nodes.
 */
public class AstAdapters {

    public static java.util.List<aliasType> to_aliasList(Object o) {
        return null;
    }

    public static java.util.List<cmpopType> to_cmpopList(Object o) {
        return null;
    }

    public static java.util.List<comprehensionType> to_comprehensionList(Object o) {
        return null;
    }

    public static java.util.List<excepthandlerType> to_excepthandlerList(Object o) {
        return null;
    }

    public static java.util.List<exprType> to_exprList(Object o) {
        return null;
    }

    public static java.util.List<String> to_identifierList(Object o) {
        return null;
    }

    public static java.util.List<keywordType> to_keywordList(Object o) {
        return null;
    }

    public static java.util.List<sliceType> to_sliceList(Object o) {
        return null;
    }

    public static java.util.List<stmtType> to_stmtList(Object o) {
        return null;
    }

    public static exprType to_expr(Object o) {
        if (o == null || o instanceof exprType) {
            return (exprType)o;
        } else if (o instanceof Integer) {
            return new Num(new PyInteger((Integer)o));
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to expr node");
    }

    public static int to_int(Object o) {
        if (o == null || o instanceof Integer) {
            return (Integer)o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to int node");
    }

    public static String to_identifier(Object o) {
        if (o == null || o instanceof String) {
            return (String)o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to identifier node");
    }

    public static expr_contextType to_expr_context(Object o) {
        if (o == null || o instanceof expr_contextType) {
            return (expr_contextType)o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to expr_context node");
    }

    public static sliceType to_slice(Object o) {
        if (o == null || o instanceof sliceType) {
            return (sliceType)o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to slice node");
    }

    public static stmtType to_stmt(Object o) {
        if (o instanceof PyJavaInstance) {
            o = ((PyJavaInstance)o).__tojava__(stmtType.class);
        }
        if (o == null || o instanceof stmtType) {
            return (stmtType)o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to stmt node");
    }

    public static String to_string(Object o) {
        if (o == null || o instanceof String) {
            return (String)o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to string node");
    }

    public static operatorType to_operator(Object o) {
        if (o == null || o instanceof operatorType) {
            return (operatorType)o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to operator node");
    }

    public static boolopType to_boolop(Object o) {
        if (o == null || o instanceof boolopType) {
            return (boolopType)o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to boolop node");
    }

    public static argumentsType to_arguments(Object o) {
        if (o == null || o instanceof argumentsType) {
            return (argumentsType)o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to arguments node");
    }

    public static Object to_object(Object o) {
        if (o == null || o instanceof Object) {
            return (Object)o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to object node");
    }

    public static Boolean to_bool(Object o) {
        if (o == null || o instanceof Boolean) {
            return (Boolean)o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to Boolean node");
    }

    public static unaryopType to_unaryop(Object o) {
        if (o == null || o instanceof unaryopType) {
            return (unaryopType)o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to unaryop node");
    }

}
