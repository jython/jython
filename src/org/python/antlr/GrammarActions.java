package org.python.antlr;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyUnicode;
import org.python.antlr.ParseException;
import org.python.antlr.ast.aliasType;
import org.python.antlr.ast.argumentsType;
import org.python.antlr.ast.boolopType;
import org.python.antlr.ast.comprehensionType;
import org.python.antlr.ast.cmpopType;
import org.python.antlr.ast.excepthandlerType;
import org.python.antlr.ast.exprType;
import org.python.antlr.ast.expr_contextType;
import org.python.antlr.ast.keywordType;
import org.python.antlr.ast.modType;
import org.python.antlr.ast.operatorType;
import org.python.antlr.ast.sliceType;
import org.python.antlr.ast.stmtType;
import org.python.antlr.ast.unaryopType;
import org.python.antlr.ast.Assert;
import org.python.antlr.ast.Assign;
import org.python.antlr.ast.Attribute;
import org.python.antlr.ast.AugAssign;
import org.python.antlr.ast.BinOp;
import org.python.antlr.ast.BoolOp;
import org.python.antlr.ast.Break;
import org.python.antlr.ast.Call;
import org.python.antlr.ast.ClassDef;
import org.python.antlr.ast.Compare;
import org.python.antlr.ast.Continue;
import org.python.antlr.ast.Delete;
import org.python.antlr.ast.Dict;
import org.python.antlr.ast.Ellipsis;
import org.python.antlr.ast.ErrorStmt;
import org.python.antlr.ast.Exec;
import org.python.antlr.ast.Expr;
import org.python.antlr.ast.Expression;
import org.python.antlr.ast.ExtSlice;
import org.python.antlr.ast.For;
import org.python.antlr.ast.FunctionDef;
import org.python.antlr.ast.GeneratorExp;
import org.python.antlr.ast.Global;
import org.python.antlr.ast.If;
import org.python.antlr.ast.IfExp;
import org.python.antlr.ast.Index;
import org.python.antlr.ast.Import;
import org.python.antlr.ast.ImportFrom;
import org.python.antlr.ast.Interactive;
import org.python.antlr.ast.Lambda;
import org.python.antlr.ast.ListComp;
import org.python.antlr.ast.Module;
import org.python.antlr.ast.Name;
import org.python.antlr.ast.Num;
import org.python.antlr.ast.Slice;
import org.python.antlr.ast.Subscript;
import org.python.antlr.ast.TryExcept;
import org.python.antlr.ast.TryFinally;
import org.python.antlr.ast.Tuple;
import org.python.antlr.ast.Pass;
import org.python.antlr.ast.Print;
import org.python.antlr.ast.Raise;
import org.python.antlr.ast.Repr;
import org.python.antlr.ast.Return;
import org.python.antlr.ast.Str;
import org.python.antlr.ast.UnaryOp;
import org.python.antlr.ast.With;
import org.python.antlr.ast.While;
import org.python.antlr.ast.Yield;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class GrammarActions {
    private ErrorHandler errorHandler = null;

    public GrammarActions() {
    }

    public void setErrorHandler(ErrorHandler eh) {
        this.errorHandler = eh;
    }
    void debug(String x) {
        if (false) {
            System.out.println(x);
        }
    }

    void throwGenExpNotSoleArg(PythonTree t) {
        throw new ParseException("Generator expression must be parenthesized if not sole argument", t);
    }

    exprType[] makeExprs(List exprs) {
        return makeExprs(exprs, 0);
    }

    exprType[] makeExprs(List exprs, int start) {
        if (exprs != null) {
            List<exprType> result = new ArrayList<exprType>();
            for (int i=start; i<exprs.size(); i++) {
                exprType e = (exprType)exprs.get(i);
                result.add(e);
            }
            return (exprType[])result.toArray(new exprType[result.size()]);
        }
        return new exprType[0];
    }

    stmtType[] makeStmts(List stmts) {
        if (stmts != null) {
            List<stmtType> result = new ArrayList<stmtType>();
            for (int i=0; i<stmts.size(); i++) {
                result.add((stmtType)stmts.get(i));
            }
            return (stmtType[])result.toArray(new stmtType[result.size()]);
        }
        return new stmtType[0];
    }

    exprType makeDottedAttr(Token nameToken, List attrs) {
        exprType current = new Name(nameToken, nameToken.getText(), expr_contextType.Load);
        for (int i=attrs.size() - 1; i > -1; i--) {
            Token t = ((PythonTree)attrs.get(i)).token;
            current = new Attribute(t, current, t.getText(),
                expr_contextType.Load);
        }
        return current;
    }

    stmtType makeFunctionDef(PythonTree t, PythonTree nameToken, argumentsType args, List funcStatements, List decorators) {
        if (nameToken == null) {
            return errorHandler.errorStmt(t);
        }
        argumentsType a;
        debug("Matched FunctionDef");
        if (args != null) {
            a = args;
        } else {
            a = new argumentsType(t, new exprType[0], null, null, new exprType[0]); 
        }
        stmtType[] s = (stmtType[])funcStatements.toArray(new stmtType[funcStatements.size()]);
        exprType[] d;
        if (decorators != null) {
            d = (exprType[])decorators.toArray(new exprType[decorators.size()]);
        } else {
            d = new exprType[0];
        }
        return new FunctionDef(t, nameToken.getText(), a, s, d);
    }

    argumentsType makeArgumentsType(Token t, List params, Token snameToken,
        Token knameToken, List defaults) {
        debug("Matched Arguments");

        exprType[] p = (exprType[])params.toArray(new exprType[params.size()]);
        exprType[] d = (exprType[])defaults.toArray(new exprType[defaults.size()]);
        String s;
        String k;
        if (snameToken == null) {
            s = null;
        } else {
            s = snameToken.getText();
        }
        if (knameToken == null) {
            k = null;
        } else {
            k = knameToken.getText();
        }
        return new argumentsType(t, p, s, k, d);
    }



    Object makeFloat(Token t) {
        debug("makeFloat matched " + t.getText());
        return Py.newFloat(Double.valueOf(t.getText()));
    }

    Object makeComplex(Token t) {
        String s = t.getText();
        s = s.substring(0, s.length() - 1);
        return Py.newImaginary(Double.valueOf(s));
    }

    Object makeInt(Token t) {
        debug("Num matched " + t.getText());
        String s = t.getText();
        int radix = 10;
        if (s.startsWith("0x") || s.startsWith("0X")) {
            radix = 16;
            s = s.substring(2, s.length());
        } else if (s.startsWith("0")) {
            radix = 8;
        }
        if (s.endsWith("L") || s.endsWith("l")) {
            s = s.substring(0, s.length()-1);
            return Py.newLong(new BigInteger(s, radix));
        }
        int ndigits = s.length();
        int i=0;
        while (i < ndigits && s.charAt(i) == '0')
            i++;
        if ((ndigits - i) > 11) {
            return Py.newLong(new BigInteger(s, radix));
        }

        long l = Long.valueOf(s, radix).longValue();
        if (l > 0xffffffffl || (l > Integer.MAX_VALUE)) {
            return Py.newLong(new BigInteger(s, radix));
        }
        return Py.newInteger((int) l);
    }

    class StringPair {
        private String s;
        private boolean unicode;

        StringPair(String s, boolean unicode) {
            this.s = s;
            this.unicode = unicode;
        }
        String getString() {
            return s;
        }
        
        boolean isUnicode() {
            return unicode;
        }
    }

    PyString extractStrings(List s) {
        boolean ustring = false;
        Token last = null;
        StringBuffer sb = new StringBuffer();
        Iterator iter = s.iterator();
        while (iter.hasNext()) {
            last = (Token)iter.next();
            StringPair sp = extractString(last);
            if (sp.isUnicode()) {
                ustring = true;
            }
            sb.append(sp.getString());
        }
        if (ustring) {
            return new PyUnicode(sb.toString());
        }
        return new PyString(sb.toString());
    }

    StringPair extractString(Token t) {
        String s = t.getText();
        char quoteChar = s.charAt(0);
        int start=0;
        boolean ustring = false;
        if (quoteChar == 'u' || quoteChar == 'U') {
            ustring = true;
            start++;
        }
        quoteChar = s.charAt(start);
        boolean raw = false;
        if (quoteChar == 'r' || quoteChar == 'R') {
            raw = true;
            start++;
        }
        int quotes = 3;
        if (s.length() - start == 2) {
            quotes = 1;
        }
        if (s.charAt(start) != s.charAt(start+1)) {
            quotes = 1;
        }

        if (raw) {
            return new StringPair(s.substring(quotes+start, s.length()-quotes), ustring);
        } else {
            StringBuffer sb = new StringBuffer(s.length());
            char[] ca = s.toCharArray();
            int n = ca.length-quotes;
            int i=quotes+start;
            int last_i=i;
            return new StringPair(PyString.decode_UnicodeEscape(s, i, n, "strict", ustring), ustring);
            //return decode_UnicodeEscape(s, i, n, "strict", ustring);
        }
    }

    Token extractStringToken(List s) {
        return (Token)s.get(s.size() - 1);
    }

    //FROM Walker:
    modType makeMod(PythonTree t, List stmts) {
        stmtType[] s;
        if (stmts != null) {
            s = (stmtType[])stmts.toArray(new stmtType[stmts.size()]);
        } else {
            s = new stmtType[0];
        }
        return new Module(t, s);
    }

    modType makeExpression(PythonTree t, exprType e) {
        return new Expression(t, e);
    }

    modType makeInteractive(PythonTree t, List stmts) {
        stmtType[] s;
        if (stmts == null) {
            s = new stmtType[0];
        } else {
            s = (stmtType[])stmts.toArray(new stmtType[stmts.size()]);
        }
        return new Interactive(t, s);
    }

    stmtType makeClassDef(PythonTree t, PythonTree nameToken, List bases, List body) {
        if (nameToken == null) {
            return errorHandler.errorStmt(t);
        }
        exprType[] b = (exprType[])bases.toArray(new exprType[bases.size()]);
        stmtType[] s = (stmtType[])body.toArray(new stmtType[body.size()]);
        return new ClassDef(t, nameToken.getText(), b, s);
    }

    argumentsType makeArgumentsType(PythonTree t, List params, PythonTree snameToken,
        PythonTree knameToken, List defaults) {
        debug("Matched Arguments");

        exprType[] p = (exprType[])params.toArray(new exprType[params.size()]);
        exprType[] d = (exprType[])defaults.toArray(new exprType[defaults.size()]);
        String s;
        String k;
        if (snameToken == null) {
            s = null;
        } else {
            s = snameToken.getText();
        }
        if (knameToken == null) {
            k = null;
        } else {
            k = knameToken.getText();
        }
        return new argumentsType(t, p, s, k, d);
    }

    stmtType makeTryExcept(PythonTree t, List body, List handlers, List orelse, List finBody) {
        stmtType[] b = (stmtType[])body.toArray(new stmtType[body.size()]);
        excepthandlerType[] e = (excepthandlerType[])handlers.toArray(new excepthandlerType[handlers.size()]);
        stmtType[] o;
        if (orelse != null) {
            o = (stmtType[])orelse.toArray(new stmtType[orelse.size()]);
        } else {
            o = new stmtType[0];
        }
 
        stmtType te = new TryExcept(t, b, e, o);
        if (finBody == null) {
            return te;
        }
        stmtType[] f = (stmtType[])finBody.toArray(new stmtType[finBody.size()]);
        stmtType[] mainBody = new stmtType[]{te};
        return new TryFinally(t, mainBody, f);
    }

    TryFinally makeTryFinally(PythonTree t,  List body, List finBody) {
        stmtType[] b = (stmtType[])body.toArray(new stmtType[body.size()]);
        stmtType[] f = (stmtType[])finBody.toArray(new stmtType[finBody.size()]);
        return new TryFinally(t, b, f);
    }

    stmtType makeIf(PythonTree t, exprType test, List body, List orelse) {
        if (test == null) {
            return errorHandler.errorStmt(t);
        }
        stmtType[] o;
        if (orelse != null) {
            o = (stmtType[])orelse.toArray(new stmtType[orelse.size()]);
        } else {
            o = new stmtType[0];
        }
        stmtType[] b;
        if (body != null) {
            b = (stmtType[])body.toArray(new stmtType[body.size()]);
        } else {
            b = new stmtType[0];
        }
        return new If(t, test, b, o);
    }


    stmtType makeWhile(PythonTree t, exprType test, List body, List orelse) {
        if (test == null) {
            return errorHandler.errorStmt(t);
        }
        stmtType[] o;
        if (orelse != null) {
            o = (stmtType[])orelse.toArray(new stmtType[orelse.size()]);
        } else {
            o = new stmtType[0];
        }
        stmtType[] b = (stmtType[])body.toArray(new stmtType[body.size()]);
        return new While(t, test, b, o);
    }

    stmtType makeFor(PythonTree t, exprType target, exprType iter, List body, List orelse) {
        if (target == null || iter == null) {
            return errorHandler.errorStmt(t);
        }
        stmtType[] o;
        if (orelse != null) {
            o = (stmtType[])orelse.toArray(new stmtType[orelse.size()]);
        } else {
            o = new stmtType[0];
        }
        stmtType[] b = (stmtType[])body.toArray(new stmtType[body.size()]);
        return new For(t, target, iter, b, o);
    }
    
    exprType makeCall(PythonTree t, exprType func) {
        return makeCall(t, func, null, null, null, null);
    }

    exprType makeCall(PythonTree t, exprType func, List args, List keywords, exprType starargs, exprType kwargs) {
        if (func == null) {
            return errorHandler.errorExpr(t);
        }
        exprType[] a;
        keywordType[] k;
        if (args == null) {
            a = new exprType[0];
        } else {
            a = (exprType[])args.toArray(new exprType[args.size()]);
        }
        if (keywords == null) {
            k = new keywordType[0];
        } else {
            k = (keywordType[])keywords.toArray(new keywordType[keywords.size()]);
        }
        return new Call(t, func, a, k, starargs, kwargs);
    }

    //FIXME: just calling __neg__ for now - can be better.  Also does not parse expressions like
    //       --2 correctly (should give ^(USub -2) but gives 2).
    exprType negate(PythonTree t, exprType o) {
        if (o instanceof Num) {
            Num num = (Num)o;
            if (num.n instanceof PyObject) {
                num.n = ((PyObject)num.n).__neg__();
            }
            return num;
        }
        return new UnaryOp(t, unaryopType.USub, o);
    }

    void checkAssign(exprType e) {
        if (e instanceof Name && ((Name)e).id.equals("None")) {
            throw new ParseException("assignment to None", e);
        }
    }
}
