package org.python.antlr;

import org.antlr.runtime.Token;

import org.python.core.Py;
import org.python.core.PyComplex;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyString;
import org.python.core.PyUnicode;
import org.python.core.codecs;
import org.python.antlr.ast.alias;
import org.python.antlr.ast.arguments;
import org.python.antlr.ast.boolopType;
import org.python.antlr.ast.cmpopType;
import org.python.antlr.ast.expr_contextType;
import org.python.antlr.ast.operatorType;
import org.python.antlr.ast.unaryopType;
import org.python.antlr.ast.Context;
import org.python.antlr.ast.keyword;
import org.python.antlr.ast.Attribute;
import org.python.antlr.ast.BinOp;
import org.python.antlr.ast.BoolOp;
import org.python.antlr.ast.Call;
import org.python.antlr.ast.ExtSlice;
import org.python.antlr.ast.For;
import org.python.antlr.ast.FunctionDef;
import org.python.antlr.ast.GeneratorExp;
import org.python.antlr.ast.IfExp;
import org.python.antlr.ast.Index;
import org.python.antlr.ast.Lambda;
import org.python.antlr.ast.ListComp;
import org.python.antlr.ast.Name;
import org.python.antlr.ast.Num;
import org.python.antlr.ast.Slice;
import org.python.antlr.ast.TryExcept;
import org.python.antlr.ast.TryFinally;
import org.python.antlr.ast.Tuple;
import org.python.antlr.ast.Repr;
import org.python.antlr.ast.Str;
import org.python.antlr.ast.UnaryOp;
import org.python.antlr.ast.While;
import org.python.antlr.ast.Yield;
import org.python.antlr.base.excepthandler;
import org.python.antlr.base.expr;
import org.python.antlr.base.slice;
import org.python.antlr.base.stmt;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GrammarActions {
    private ErrorHandler errorHandler = null;
    public GrammarActions() {
    }

    public void setErrorHandler(ErrorHandler eh) {
        this.errorHandler = eh;
    }

    String makeFromText(List dots, List<Name> names) {
        StringBuilder d = new StringBuilder();
        d.append(PythonTree.dottedNameListToString(names));
        return d.toString();
    }

    List<Name> makeModuleNameNode(List dots, List<Name> names) {
        List<Name> result = new ArrayList<Name>();
        if (dots != null) {
            for (Object o : dots) {
                Token tok = (Token)o;
                result.add(new Name(tok, tok.getText(), expr_contextType.Load));
            }
        }
        if (null != names) {
        	result.addAll(names);
        }
        return result;
    }

    List<Name> makeDottedName(Token top, List<PythonTree> attrs) {
      List<Name> result = new ArrayList<Name>();
      result.add(new Name(top, top.getText(), expr_contextType.Load));
      if (attrs != null) {
        for (PythonTree attr : attrs) {
          Token token = attr.getToken();
          result.add(new Name(token, token.getText(), expr_contextType.Load));
        }
      }
      return result;
    }

    int makeLevel(List lev) {
        if (lev == null) {
            return 0;
        }
        return lev.size();
    }

    List<alias> makeStarAlias(Token t) {
        List<alias> result = new ArrayList<alias>();
        result.add(new alias(t, "*", null));
        return result;
    }

    List<alias> makeAliases(List<alias> atypes) {
        if (atypes == null) {
            return new ArrayList<alias>();
        }
        return atypes;
    }

    List<expr> makeBases(expr etype) {
        List<expr> result = new ArrayList<expr>();
        if (etype != null) {
            if (etype instanceof Tuple) {
                return ((Tuple)etype).getInternalElts();
            }
            result.add(etype);
        }
        return result;
    }

    List<String> makeNames(List names) {
        List<String> s = new ArrayList<String>();
        for(int i=0;i<names.size();i++) {
            s.add(((Token)names.get(i)).getText());
        }
        return s;
    }

    Name makeNameNode(Token t) {
        if (t == null) {
            return null;
        }
        return new Name(t, t.getText(), expr_contextType.Load);
    }

    List<Name> makeNameNodes(List<Token> names) {
        List<Name> s = new ArrayList<Name>();
        for (int i=0; i<names.size(); i++) {
            s.add(makeNameNode(names.get(i)));
        }
        return s;
    }

    void errorGenExpNotSoleArg(PythonTree t) {
        errorHandler.error("Generator expression must be parenthesized if not sole argument", t);
    }

    expr castExpr(Object o) {
        if (o instanceof expr) {
            return (expr)o;
        }
        if (o instanceof PythonTree) {
            return errorHandler.errorExpr((PythonTree)o);
        }
        return null;
    }


    List<expr> castExprs(List exprs) {
        return castExprs(exprs, 0);
    }

    List<expr> castExprs(List exprs, int start) {
        List<expr> result = new ArrayList<expr>();
        if (exprs != null) {
            for (int i=start; i<exprs.size(); i++) {
                Object o = exprs.get(i);
                if (o instanceof expr) {
                    result.add((expr)o);
                } else if (o instanceof PythonParser.test_return) {
                    result.add((expr)((PythonParser.test_return)o).tree);
                }
            }
        }
        return result;
    }
    
    List<stmt> makeElse(List elseSuite, PythonTree elif) {
        if (elseSuite != null) {
            return castStmts(elseSuite);
        } else if (elif == null) {
            return new ArrayList<stmt>();
        }
        List <stmt> s = new ArrayList<stmt>();
        s.add(castStmt(elif));
        return s;
    }

    stmt castStmt(Object o) {
        if (o instanceof stmt) {
            return (stmt)o;
        } else if (o instanceof PythonParser.stmt_return) {
            return (stmt)((PythonParser.stmt_return)o).tree;
        } else if (o instanceof PythonTree) {
            return errorHandler.errorStmt((PythonTree)o);
        }
        return null;
    }

    List<stmt> castStmts(PythonTree t) {
        stmt s = (stmt)t;
        List<stmt> stmts = new ArrayList<stmt>();
        stmts.add(s);
        return stmts;
    }

    List<stmt> castStmts(List stmts) {
        if (stmts != null) {
            List<stmt> result = new ArrayList<stmt>();
            for (Object o:stmts) {
                result.add(castStmt(o));
            }
            return result;
        }
        return new ArrayList<stmt>();
    }

    expr makeDottedAttr(Token nameToken, List attrs) {
        expr current = new Name(nameToken, nameToken.getText(), expr_contextType.Load);
        for (Object o: attrs) {
            Token t = (Token)o;
            current = new Attribute(t, current, cantBeNoneName(t),
                expr_contextType.Load);
        }
        return current;
    }

    stmt makeWhile(Token t, expr test, List body, List orelse) {
        if (test == null) {
            return errorHandler.errorStmt(new PythonTree(t));
        }
        List<stmt> o = castStmts(orelse);
        List<stmt> b = castStmts(body);
        return new While(t, test, b, o);
    }

    stmt makeFor(Token t, expr target, expr iter, List body, List orelse) {
        if (target == null || iter == null) {
            return errorHandler.errorStmt(new PythonTree(t));
        }
        cantBeNone(target);

        List<stmt> o = castStmts(orelse);
        List<stmt> b = castStmts(body);
        return new For(t, target, iter, b, o);
    }

    stmt makeTryExcept(Token t, List body, List<excepthandler> handlers, List orelse, List finBody) {
        List<stmt> b = castStmts(body);
        List<excepthandler> e = handlers;
        List<stmt> o = castStmts(orelse);
        stmt te = new TryExcept(t, b, e, o);
        if (finBody == null) {
            return te;
        }
        List<stmt> f = castStmts(finBody);
        List<stmt> mainBody = new ArrayList<stmt>();
        mainBody.add(te);
        return new TryFinally(t, mainBody, f);
    }

    TryFinally makeTryFinally(Token t,  List body, List finBody) {
        List<stmt> b = castStmts(body);
        List<stmt> f = castStmts(finBody);
        return new TryFinally(t, b, f);
    }

    stmt makeFuncdef(Token t, Token nameToken, arguments args, List funcStatements, List decorators) {
        if (nameToken == null) {
            return errorHandler.errorStmt(new PythonTree(t));
        }
        Name n = cantBeNoneName(nameToken);
        arguments a;
        if (args != null) {
            a = args;
        } else {
            a = new arguments(t, new ArrayList<expr>(), (Name)null, null, new ArrayList<expr>());
        }
        List<stmt> s = castStmts(funcStatements);
        List<expr> d = castExprs(decorators);
        return new FunctionDef(t, n, a, s, d);
    }

    List<expr> makeAssignTargets(expr lhs, List rhs) {
        List<expr> e = new ArrayList<expr>();
        checkAssign(lhs);
        e.add(lhs);
        for(int i=0;i<rhs.size() - 1;i++) {
            expr r = castExpr(rhs.get(i));
            checkAssign(r);
            e.add(r);
        }
        return e;
    }

    expr makeAssignValue(List rhs) {
        expr value = castExpr(rhs.get(rhs.size() -1));
        recurseSetContext(value, expr_contextType.Load);
        return value;
    }

    void recurseSetContext(PythonTree tree, expr_contextType context) {
        if (tree instanceof Context) {
            ((Context)tree).setContext(context);
        }
        if (tree instanceof GeneratorExp) {
            GeneratorExp g = (GeneratorExp)tree;
            recurseSetContext(g.getInternalElt(), context);
        } else if (tree instanceof ListComp) {
            ListComp lc = (ListComp)tree;
            recurseSetContext(lc.getInternalElt(), context);
        } else if (!(tree instanceof ListComp)) {
            for (int i=0; i<tree.getChildCount(); i++) {
                recurseSetContext(tree.getChild(i), context);
            }
        }
    }

    arguments makeArgumentsType(Token t, List params, Token snameToken,
        Token knameToken, List defaults) {

        List<expr> p = castExprs(params);
        List<expr> d = castExprs(defaults);
        Name s;
        Name k;
        if (snameToken == null) {
            s = null;
        } else {
            s = cantBeNoneName(snameToken);
        }
        if (knameToken == null) {
            k = null;
        } else {
            k = cantBeNoneName(knameToken);
        }
        return new arguments(t, p, s, k, d);
    }

    List<expr> extractArgs(List args) {
        return castExprs(args);
    }

    List<keyword> makeKeywords(List args) {
        List<keyword> keywords = new ArrayList<keyword>();
        if (args != null) {
            for (Object o : args) {
                List e = (List)o;
                Object k = e.get(0);
                Object v = e.get(1);
                checkAssign(castExpr(k));
                if (k instanceof Name) {
                    Name arg = (Name)k;
                    keywords.add(new keyword(arg, arg.getInternalId(), castExpr(v)));
                } else {
                    errorHandler.error("keyword must be a name", (PythonTree)k);
                }
            }
        }
        return keywords;
    }

    Object makeFloat(Token t) {
        return Py.newFloat(Double.valueOf(t.getText()));
    }

    Object makeComplex(Token t) {
        String s = t.getText();
        s = s.substring(0, s.length() - 1);
        return Py.newImaginary(Double.valueOf(s));
    }

    //XXX: needs to handle NumberFormatException (on input like 0b2) and needs
    //     a better long guard than ndigits > 11 (this is much to short for
    //     binary for example)
    Object makeInt(Token t) {
        String s = t.getText();
        int radix = 10;
        if (s.startsWith("0x") || s.startsWith("0X")) {
            radix = 16;
            s = s.substring(2, s.length());
        } else if (s.startsWith("0o") || s.startsWith("0O")) {
            radix = 8;
            s = s.substring(2, s.length());
        } else if (s.startsWith("0b") || s.startsWith("0B")) {
            radix = 2;
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

    PyString extractStrings(List s, String encoding, boolean unicodeLiterals) {
        boolean ustring = false;
        Token last = null;
        StringBuffer sb = new StringBuffer();
        Iterator iter = s.iterator();
        while (iter.hasNext()) {
            last = (Token)iter.next();
            StringPair sp = extractString(last, encoding, unicodeLiterals);
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

    StringPair extractString(Token t, String encoding, boolean unicodeLiterals) {
        String string = t.getText();
        char quoteChar = string.charAt(0);
        int start = 0;
        int end;
        boolean ustring = unicodeLiterals;

        if (quoteChar == 'u' || quoteChar == 'U') {
            ustring = true;
            start++;
        }
        if (quoteChar == 'b' || quoteChar == 'B') {
            // In 2.x this is just a str, and the parser prevents a 'u' and a
            // 'b' in the same identifier, so just advance start.
            ustring = false;
            start++;
        }
        quoteChar = string.charAt(start);
        boolean raw = false;
        if (quoteChar == 'r' || quoteChar == 'R') {
            raw = true;
            start++;
        }
        int quotes = 3;
        if (string.length() - start == 2) {
            quotes = 1;
        }
        if (string.charAt(start) != string.charAt(start+1)) {
            quotes = 1;
        }

        start = quotes + start;
        end = string.length() - quotes;
        // string is properly decoded according to the source encoding
        // XXX: No need to re-encode when the encoding is iso-8859-1, but ParserFacade
        // needs to normalize the encoding name
        if (!ustring && encoding != null) {
            // str with a specified encoding: first re-encode back out
            string = new PyUnicode(string.substring(start, end)).encode(encoding);
            if (!raw) {
                // Handle escapes in non-raw strs
                string = PyString.decode_UnicodeEscape(string, 0, string.length(), "strict",
                                                       ustring);
            }
        } else if (raw) {
            // Raw str without an encoding or raw unicode
            string = string.substring(start, end);
            if (ustring) {
                // Raw unicode: handle unicode escapes
                string = codecs.PyUnicode_DecodeRawUnicodeEscape(string, "strict");
            }
        } else {
            // Plain unicode: already decoded, just handle escapes
            string = PyString.decode_UnicodeEscape(string, start, end, "strict", ustring);
        }
        return new StringPair(string, ustring);
    }

    Token extractStringToken(List s) {
        return (Token)s.get(0);
        //return (Token)s.get(s.size() - 1);
    }

    expr makeCall(Token t, expr func) {
        return makeCall(t, func, null, null, null, null);
    }

    expr makeCall(Token t, expr func, List args, List keywords, expr starargs, expr kwargs) {
        if (func == null) {
            return errorHandler.errorExpr(new PythonTree(t));
        }
        List<keyword> k = makeKeywords(keywords);
        List<expr> a = castExprs(args);
        return new Call(t, func, a, k, starargs, kwargs);
    }

    expr negate(Token t, expr o) {
        return negate(new PythonTree(t), o);
    }

    expr negate(PythonTree t, expr o) {
        if (o instanceof Num) {
            Num num = (Num)o;
            if (num.getInternalN() instanceof PyInteger) {
                int v = ((PyInteger)num.getInternalN()).getValue();
                if (v >= 0) {
                    num.setN(new PyInteger(-v));
                    return num;
                }
            } else if (num.getInternalN() instanceof PyLong) {
                BigInteger v = ((PyLong)num.getInternalN()).getValue();
                if (v.compareTo(BigInteger.ZERO) == 1) {
                    num.setN(new PyLong(v.negate()));
                    return num;
                }
            } else if (num.getInternalN() instanceof PyFloat) {
                double v = ((PyFloat)num.getInternalN()).getValue();
                if (v >= 0) {
                    num.setN(new PyFloat(-v));
                    return num;
                }
            } else if (num.getInternalN() instanceof PyComplex) {
                double v = ((PyComplex)num.getInternalN()).imag;
                if (v >= 0) {
                    num.setN(new PyComplex(0,-v));
                    return num;
                }
            }
        }
        return new UnaryOp(t, unaryopType.USub, o);
    }

    String cantBeNone(Token t) {
        if (t == null || t.getText().equals("None")) {
            errorHandler.error("can't be None", new PythonTree(t));
        }
        return t.getText();
    }

    Name cantBeNoneName(Token t) {
        if (t == null || t.getText().equals("None")) {
            errorHandler.error("can't be None", new PythonTree(t));
        }
        return new Name(t, t.getText(), expr_contextType.Load);
    }

    void cantBeNone(PythonTree e) {
        if (e.getText().equals("None")) {
            errorHandler.error("can't be None", e);
        }
    }

    void checkAssign(expr e) {
        if (e instanceof Name && ((Name)e).getInternalId().equals("None")) {
            errorHandler.error("assignment to None", e);
        } else if (e instanceof GeneratorExp) {
            errorHandler.error("can't assign to generator expression", e);
        } else if (e instanceof Num) {
            errorHandler.error("can't assign to number", e);
        } else if (e instanceof Str) {
            errorHandler.error("can't assign to string", e);
        } else if (e instanceof Yield) {
            errorHandler.error("can't assign to yield expression", e);
        } else if (e instanceof BinOp) {
            errorHandler.error("can't assign to operator", e);
        } else if (e instanceof BoolOp) {
            errorHandler.error("can't assign to operator", e);
        } else if (e instanceof Lambda) {
            errorHandler.error("can't assign to lambda", e);
        } else if (e instanceof Call) {
            errorHandler.error("can't assign to function call", e);
        } else if (e instanceof Repr) {
            errorHandler.error("can't assign to repr", e);
        } else if (e instanceof IfExp) {
            errorHandler.error("can't assign to conditional expression", e);
        } else if (e instanceof ListComp) {
            errorHandler.error("can't assign to list comprehension", e);
        } else if (e instanceof Tuple) {
            //XXX: performance problem?  Any way to do this better?
            List<expr> elts = ((Tuple)e).getInternalElts();
            if (elts.size() == 0) {
                errorHandler.error("can't assign to ()", e);
            }
            for (int i=0;i<elts.size();i++) {
                checkAssign(elts.get(i));
            }
        } else if (e instanceof org.python.antlr.ast.List) {
            //XXX: performance problem?  Any way to do this better?
            List<expr> elts = ((org.python.antlr.ast.List)e).getInternalElts();
            for (int i=0;i<elts.size();i++) {
                checkAssign(elts.get(i));
            }
        }
    }

    List<expr> makeDeleteList(List deletes) {
        List<expr> exprs = castExprs(deletes);
        for(expr e : exprs) {
            checkDelete(e);
        }
        return exprs;
    }

    void checkDelete(expr e) {
        if (e instanceof Call) {
            errorHandler.error("can't delete function call", e);
        } else if (e instanceof Num) {
            errorHandler.error("can't delete number", e);
        } else if (e instanceof Str) {
            errorHandler.error("can't delete string", e);
        } else if (e instanceof Tuple) {
            //XXX: performance problem?  Any way to do this better?
            List<expr> elts = ((Tuple)e).getInternalElts();
            if (elts.size() == 0) {
                errorHandler.error("can't delete ()", e);
            }
            for (int i=0;i<elts.size();i++) {
                checkDelete(elts.get(i));
            }
        } else if (e instanceof org.python.antlr.ast.List) {
            //XXX: performance problem?  Any way to do this better?
            List<expr> elts = ((org.python.antlr.ast.List)e).getInternalElts();
            for (int i=0;i<elts.size();i++) {
                checkDelete(elts.get(i));
            }
        }
    }

    slice makeSubscript(PythonTree lower, Token colon, PythonTree upper, PythonTree sliceop) {
            boolean isSlice = false;
        expr s = null;
        expr e = null;
        expr o = null;
        if (lower != null) {
            s = castExpr(lower);
        }
        if (colon != null) {
            isSlice = true;
            if (upper != null) {
                e = castExpr(upper);
            }
        }
        if (sliceop != null) {
            isSlice = true;
            if (sliceop != null) {
                o = castExpr(sliceop);
            } else {
                o = new Name(sliceop, "None", expr_contextType.Load);
            }
        }

        PythonTree tok = lower;
        if (lower == null) {
            tok = new PythonTree(colon);
        }
        if (isSlice) {
           return new Slice(tok, s, e, o);
        }
        else {
           return new Index(tok, s);
        }
    }

    List<cmpopType> makeCmpOps(List cmps) {
        List<cmpopType> result = new ArrayList<cmpopType>();
        if (cmps != null) {
            for (Object o: cmps) {
                result.add((cmpopType)o);
            }
        }
        return result;
    }
    
    BoolOp makeBoolOp(Token t, PythonTree left, boolopType op, List right) {
        List values = new ArrayList();
        values.add(left);
        values.addAll(right);
        return new BoolOp(t, op, castExprs(values));
    }

    BinOp makeBinOp(Token t, PythonTree left, operatorType op, List rights) {
        BinOp current = new BinOp(t, castExpr(left), op, castExpr(rights.get(0)));
        for (int i = 1; i< rights.size(); i++) {
            expr right = castExpr(rights.get(i));
            current = new BinOp(left, current, op, right);
        }
        return current;
    }

    BinOp makeBinOp(Token t, PythonTree left, List ops, List rights, List toks) {
        BinOp current = new BinOp(t, castExpr(left), (operatorType)ops.get(0), castExpr(rights.get(0)));
        for (int i = 1; i< rights.size(); i++) {
            expr right = castExpr(rights.get(i));
            operatorType op = (operatorType)ops.get(i);
            current = new BinOp((Token)toks.get(i), current, op, right);
        }
        return current;
    }

    List<slice> castSlices(List slices) {
        List<slice> result = new ArrayList<slice>();
        if (slices != null) {
            for (Object o:slices) {
                result.add(castSlice(o));
            }
        }
        return result;
    }
 
    slice castSlice(Object o) {
        if (o instanceof slice) {
            return (slice)o;
        }
        return errorHandler.errorSlice((PythonTree)o);
    }

    slice makeSliceType(Token begin, Token c1, Token c2, List sltypes) {
        boolean isTuple = false;
        if (c1 != null || c2 != null) {
            isTuple = true;
        }
        slice s = null;
        boolean extslice = false;

        if (isTuple) {
            List<slice> st;
            List<expr> etypes = new ArrayList<expr>();
            for (Object o : sltypes) {
                if (o instanceof Index) {
                    Index i = (Index)o;
                    etypes.add(i.getInternalValue());
                } else {
                    extslice = true;
                    break;
                }
            }
            if (!extslice) {
                expr t = new Tuple(begin, etypes, expr_contextType.Load);
                s = new Index(begin, t);
            }
        } else if (sltypes.size() == 1) {
            s = castSlice(sltypes.get(0));
        } else if (sltypes.size() != 0) {
            extslice = true;
        }
        if (extslice) {
            List<slice> st = castSlices(sltypes);
            s = new ExtSlice(begin, st);
        }
        return s;
    }
}
