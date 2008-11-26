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

    public static java.util.List<aliasType> to_aliasList(Object o) {
        return (java.util.List<aliasType>)aliasAdapter.adaptIter(o);
    }

    public static java.util.List<cmpopType> to_cmpopList(Object o) {
        return (java.util.List<cmpopType>)cmpopAdapter.adaptIter(o);
    }

    public static java.util.List<comprehensionType> to_comprehensionList(Object o) {
        return (java.util.List<comprehensionType>)comprehensionAdapter.adaptIter(o);
    }

    public static java.util.List<excepthandlerType> to_excepthandlerList(Object o) {
        return (java.util.List<excepthandlerType>)excepthandlerAdapter.adaptIter(o);
    }

    public static java.util.List<exprType> to_exprList(Object o) {
        return (java.util.List<exprType>)exprAdapter.adaptIter(o);
    }

    public static java.util.List<String> to_identifierList(Object o) {
        return (java.util.List<String>)identifierAdapter.adaptIter(o);
    }

    public static java.util.List<keywordType> to_keywordList(Object o) {
        return (java.util.List<keywordType>)keywordAdapter.adaptIter(o);
    }

    public static java.util.List<sliceType> to_sliceList(Object o) {
        return (java.util.List<sliceType>)sliceAdapter.adaptIter(o);
    }

    public static java.util.List<stmtType> to_stmtList(Object o) {
        return (java.util.List<stmtType>)stmtAdapter.adaptIter(o);
    }

    public static exprType to_expr(Object o) {
        return (exprType)exprAdapter.adapt(o);
    }

    public static int to_int(Object o) {
        if (o == null || o instanceof Integer) {
            return (Integer)o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to int node");
    }

    public static String to_identifier(Object o) {
        return (String)identifierAdapter.adapt(o);
    }

    public static expr_contextType to_expr_context(Object o) {
        if (o == null || o instanceof expr_contextType) {
            return (expr_contextType)o;
        }
        //FIXME: investigate the right exception
        throw Py.TypeError("Can't convert " + o.getClass().getName() + " to expr_context node");
    }

    public static sliceType to_slice(Object o) {
        return (sliceType)sliceAdapter.adapt(o);
    }

    public static stmtType to_stmt(Object o) {
        return (stmtType)stmtAdapter.adapt(o);
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

    //XXX: clearly this isn't necessary -- need to adjust the code generation.
    public static Object to_object(Object o) {
        return o;
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
