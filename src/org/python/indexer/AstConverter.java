/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer;

import org.python.antlr.PythonTree;
import org.python.antlr.Visitor;
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
import org.python.antlr.ast.ExceptHandler;
import org.python.antlr.ast.Exec;
import org.python.antlr.ast.Expr;
import org.python.antlr.ast.For;
import org.python.antlr.ast.FunctionDef;
import org.python.antlr.ast.GeneratorExp;
import org.python.antlr.ast.Global;
import org.python.antlr.ast.If;
import org.python.antlr.ast.IfExp;
import org.python.antlr.ast.Import;
import org.python.antlr.ast.ImportFrom;
import org.python.antlr.ast.Index;
import org.python.antlr.ast.Lambda;
import org.python.antlr.ast.ListComp;
import org.python.antlr.ast.Module;
import org.python.antlr.ast.Name;
import org.python.antlr.ast.Num;
import org.python.antlr.ast.Pass;
import org.python.antlr.ast.Print;
import org.python.antlr.ast.Raise;
import org.python.antlr.ast.Repr;
import org.python.antlr.ast.Return;
import org.python.antlr.ast.Slice;
import org.python.antlr.ast.Str;
import org.python.antlr.ast.Subscript;
import org.python.antlr.ast.TryExcept;
import org.python.antlr.ast.TryFinally;
import org.python.antlr.ast.Tuple;
import org.python.antlr.ast.UnaryOp;
import org.python.antlr.ast.While;
import org.python.antlr.ast.With;
import org.python.antlr.ast.Yield;
import org.python.antlr.ast.alias;
import org.python.antlr.ast.arguments;
import org.python.antlr.ast.boolopType;
import org.python.antlr.ast.cmpopType;
import org.python.antlr.ast.comprehension;
import org.python.antlr.ast.keyword;
import org.python.antlr.ast.operatorType;
import org.python.antlr.ast.unaryopType;
import org.python.antlr.base.excepthandler;
import org.python.antlr.base.expr;
import org.python.antlr.base.stmt;
import org.python.indexer.ast.NAlias;
import org.python.indexer.ast.NAssert;
import org.python.indexer.ast.NAssign;
import org.python.indexer.ast.NAttribute;
import org.python.indexer.ast.NAugAssign;
import org.python.indexer.ast.NBinOp;
import org.python.indexer.ast.NBlock;
import org.python.indexer.ast.NBoolOp;
import org.python.indexer.ast.NBreak;
import org.python.indexer.ast.NCall;
import org.python.indexer.ast.NClassDef;
import org.python.indexer.ast.NCompare;
import org.python.indexer.ast.NComprehension;
import org.python.indexer.ast.NContinue;
import org.python.indexer.ast.NDelete;
import org.python.indexer.ast.NDict;
import org.python.indexer.ast.NEllipsis;
import org.python.indexer.ast.NExceptHandler;
import org.python.indexer.ast.NExec;
import org.python.indexer.ast.NFor;
import org.python.indexer.ast.NFunctionDef;
import org.python.indexer.ast.NGeneratorExp;
import org.python.indexer.ast.NGlobal;
import org.python.indexer.ast.NIf;
import org.python.indexer.ast.NIfExp;
import org.python.indexer.ast.NImport;
import org.python.indexer.ast.NImportFrom;
import org.python.indexer.ast.NIndex;
import org.python.indexer.ast.NKeyword;
import org.python.indexer.ast.NLambda;
import org.python.indexer.ast.NList;
import org.python.indexer.ast.NListComp;
import org.python.indexer.ast.NModule;
import org.python.indexer.ast.NName;
import org.python.indexer.ast.NNode;
import org.python.indexer.ast.NNum;
import org.python.indexer.ast.NPass;
import org.python.indexer.ast.NPrint;
import org.python.indexer.ast.NQname;
import org.python.indexer.ast.NRaise;
import org.python.indexer.ast.NRepr;
import org.python.indexer.ast.NReturn;
import org.python.indexer.ast.NExprStmt;
import org.python.indexer.ast.NSlice;
import org.python.indexer.ast.NStr;
import org.python.indexer.ast.NSubscript;
import org.python.indexer.ast.NTryExcept;
import org.python.indexer.ast.NTryFinally;
import org.python.indexer.ast.NTuple;
import org.python.indexer.ast.NUnaryOp;
import org.python.indexer.ast.NWhile;
import org.python.indexer.ast.NWith;
import org.python.indexer.ast.NYield;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts the antlr AST into the indexer's AST format.
 */
public class AstConverter extends Visitor {

    public String convOp(Object t) {
        if (t instanceof operatorType) {
            switch((operatorType)t) {
                case Add:
                    return "+";
                case Sub:
                    return "-";
                case Mult:
                    return "*";
                case Div:
                    return "/";
                case Mod:
                    return "%";
                case Pow:
                    return "**";
                case LShift:
                    return "<<";
                case RShift:
                    return ">>";
                case BitOr:
                    return "|";
                case BitXor:
                    return "^";
                case BitAnd:
                    return "&";
                case FloorDiv:
                    return "//";
                default:
                    return null;
            }
        }
        if (t instanceof boolopType) {
            switch ((boolopType)t) {
                case And:
                    return "and";
                case Or:
                    return "or";
                default:
                    return null;
            }
        }
        if (t instanceof unaryopType) {
            switch ((unaryopType)t) {
                case Invert:
                    return "~";
                case Not:
                    return "not";
                case USub:
                    return "-";
                case UAdd:
                    return "+";
                default:
                    return null;
            }
        }
        if (t instanceof cmpopType) {
            switch ((cmpopType)t) {
                case Eq:
                    return "==";
                case NotEq:
                    return "!=";
                case Gt:
                    return ">";
                case GtE:
                    return ">=";
                case Lt:
                    return "<";
                case LtE:
                    return "<=";
                case In:
                    return "in";
                case NotIn:
                    return "not in";
                case Is:
                    return "is";
                case IsNot:
                    return "is not";
                default:
                    return null;
            }
        }
        return null;
    }

     // Helpers for converting lists of things

    private List<NExceptHandler> convertListExceptHandler(List<excepthandler> in) throws Exception {
        List<NExceptHandler> out = new ArrayList<NExceptHandler>(in == null ? 0 : in.size());
        if (in != null) {
            for (excepthandler e : in) {
                @SuppressWarnings("unchecked")
                NExceptHandler nxh = (NExceptHandler)e.accept(this);
                if (nxh != null) {
                    out.add(nxh);
                }
            }
        }
        return out;
    }

    private List<NNode> convertListExpr(List<expr> in) throws Exception {
        List<NNode> out = new ArrayList<NNode>(in == null ? 0 : in.size());
        if (in != null) {
            for (expr e : in) {
                @SuppressWarnings("unchecked")
                NNode nx = (NNode)e.accept(this);
                if (nx != null) {
                    out.add(nx);
                }
            }
        }
        return out;
    }

    private List<NName> convertListName(List<Name> in) throws Exception {
        List<NName> out = new ArrayList<NName>(in == null ? 0 : in.size());
        if (in != null) {
            for (expr e : in) {
                @SuppressWarnings("unchecked")
                NName nn = (NName)e.accept(this);
                if (nn != null) {
                    out.add(nn);
                }
            }
        }
        return out;
    }

    private NQname convertQname(List<Name> in) throws Exception {
        if (in == null) {
            return null;
        }
        // This would be less ugly if we generated Qname nodes in the antlr ast.
        NQname out = null;
        int end = -1;
        for (int i = in.size() - 1; i >= 0; i--) {
            Name n = in.get(i);
            if (end == -1) {
                end = n.getCharStopIndex();
            }
            @SuppressWarnings("unchecked")
            NName nn = (NName)n.accept(this);
            out = new NQname(out, nn, n.getCharStartIndex(), end);
        }
        return out;
    }

    private List<NKeyword> convertListKeyword(List<keyword> in) throws Exception {
        List<NKeyword> out = new ArrayList<NKeyword>(in == null ? 0 : in.size());
        if (in != null) {
            for (keyword e : in) {
                NKeyword nkw = new NKeyword(e.getInternalArg(), convExpr(e.getInternalValue()));
                if (nkw != null) {
                    out.add(nkw);
                }
            }
        }
        return out;
    }

    private NBlock convertListStmt(List<stmt> in) throws Exception {
        List<NNode> out = new ArrayList<NNode>(in == null ? 0 : in.size());
        if (in != null) {
            for (stmt e : in) {
                @SuppressWarnings("unchecked")
                NNode nx = (NNode)e.accept(this);
                if (nx != null) {
                    out.add(nx);
                }
            }
        }
        return new NBlock(out, 0, 0);
    }

    private NNode convExpr(PythonTree e) throws Exception {
        if (e == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Object o = e.accept(this);
        if (o instanceof NNode) {
            return (NNode)o;
        }
        return null;
    }

    private int start(PythonTree tree) {
        return tree.getCharStartIndex();
    }

    private int stop(PythonTree tree) {
        return tree.getCharStopIndex();
    }

    @Override
    public Object visitAssert(Assert n) throws Exception {
        return new NAssert(convExpr(n.getInternalTest()),
                           convExpr(n.getInternalMsg()),
                           start(n), stop(n));
    }

    @Override
    public Object visitAssign(Assign n) throws Exception {
        return new NAssign(convertListExpr(n.getInternalTargets()),
                           convExpr(n.getInternalValue()),
                           start(n), stop(n));
    }

    @Override
    public Object visitAttribute(Attribute n) throws Exception {
        return new NAttribute(convExpr(n.getInternalValue()),
                              (NName)convExpr(n.getInternalAttrName()),
                              start(n), stop(n));
    }

    @Override
    public Object visitAugAssign(AugAssign n) throws Exception {
        return new NAugAssign(convExpr(n.getInternalTarget()),
                              convExpr(n.getInternalValue()),
                              convOp(n.getInternalOp()),
                              start(n), stop(n));
    }

    @Override
    public Object visitBinOp(BinOp n) throws Exception {
        return new NBinOp(convExpr(n.getInternalLeft()),
                          convExpr(n.getInternalRight()),
                          convOp(n.getInternalOp()),
                          start(n), stop(n));
    }

    @Override
    public Object visitBoolOp(BoolOp n) throws Exception {
        NBoolOp.OpType op;
        switch (n.getInternalOp()) {
            case And:
                op = NBoolOp.OpType.AND;
                break;
            case Or:
                op = NBoolOp.OpType.OR;
                break;
            default:
                op = NBoolOp.OpType.UNDEFINED;
                break;
        }
        return new NBoolOp(op, convertListExpr(n.getInternalValues()), start(n), stop(n));
    }

    @Override
    public Object visitBreak(Break n) throws Exception {
        return new NBreak(start(n), stop(n));
    }

    @Override
    public Object visitCall(Call n) throws Exception {
        return new NCall(convExpr(n.getInternalFunc()),
                         convertListExpr(n.getInternalArgs()),
                         convertListKeyword(n.getInternalKeywords()),
                         convExpr(n.getInternalKwargs()),
                         convExpr(n.getInternalStarargs()),
                         start(n), stop(n));
    }

    @Override
    public Object visitClassDef(ClassDef n) throws Exception {
        return new NClassDef((NName)convExpr(n.getInternalNameNode()),
                             convertListExpr(n.getInternalBases()),
                             convertListStmt(n.getInternalBody()),
                             start(n), stop(n));
    }

    @Override
    public Object visitCompare(Compare n) throws Exception {
        return new NCompare(convExpr(n.getInternalLeft()),
                            null,  // XXX:  why null?
                            convertListExpr(n.getInternalComparators()),
                            start(n), stop(n));
    }

    @Override
    public Object visitContinue(Continue n) throws Exception {
        return new NContinue(start(n), stop(n));
    }

    @Override
    public Object visitDelete(Delete n) throws Exception {
        return new NDelete(convertListExpr(n.getInternalTargets()), start(n), stop(n));
    }

    @Override
    public Object visitDict(Dict n) throws Exception {
        return new NDict(convertListExpr(n.getInternalKeys()),
                         convertListExpr(n.getInternalValues()),
                         start(n), stop(n));
    }

    @Override
    public Object visitEllipsis(Ellipsis n) throws Exception {
        return new NEllipsis(start(n), stop(n));
    }

    @Override
    public Object visitExceptHandler(ExceptHandler n) throws Exception {
        return new NExceptHandler(convExpr(n.getInternalName()),
                                  convExpr(n.getInternalType()),
                                  convertListStmt(n.getInternalBody()),
                                  start(n), stop(n));
    }

    @Override
    public Object visitExec(Exec n) throws Exception {
        return new NExec(convExpr(n.getInternalBody()),
                         convExpr(n.getInternalGlobals()),
                         convExpr(n.getInternalLocals()),
                         start(n), stop(n));
    }

    @Override
    public Object visitExpr(Expr n) throws Exception {
        return new NExprStmt(convExpr(n.getInternalValue()), start(n), stop(n));
    }

    @Override
    public Object visitFor(For n) throws Exception {
        return new NFor(convExpr(n.getInternalTarget()),
                        convExpr(n.getInternalIter()),
                        convertListStmt(n.getInternalBody()),
                        convertListStmt(n.getInternalOrelse()),
                        start(n), stop(n));
    }

    @Override
    public Object visitFunctionDef(FunctionDef n) throws Exception {
        arguments args = n.getInternalArgs();
        NFunctionDef fn = new NFunctionDef((NName)convExpr(n.getInternalNameNode()),
                                           convertListExpr(args.getInternalArgs()),
                                           convertListStmt(n.getInternalBody()),
                                           convertListExpr(args.getInternalDefaults()),
                                           (NName)convExpr(args.getInternalVarargName()),
                                           (NName)convExpr(args.getInternalKwargName()),
                                           start(n), stop(n));
        fn.setDecoratorList(convertListExpr(n.getInternalDecorator_list()));
        return fn;
    }

    @Override
    public Object visitGeneratorExp(GeneratorExp n) throws Exception {
        List<NComprehension> generators =
                new ArrayList<NComprehension>(n.getInternalGenerators().size());
        for (comprehension c : n.getInternalGenerators()) {
            generators.add(new NComprehension(convExpr(c.getInternalTarget()),
                                              convExpr(c.getInternalIter()),
                                              convertListExpr(c.getInternalIfs()),
                                              start(c), stop(c)));
        }
        return new NGeneratorExp(convExpr(n.getInternalElt()), generators, start(n), stop(n));
    }

    @Override
    public Object visitGlobal(Global n) throws Exception {
        return new NGlobal(convertListName(n.getInternalNameNodes()),
                           start(n), stop(n));
    }

    @Override
    public Object visitIf(If n) throws Exception {
        return new NIf(convExpr(n.getInternalTest()),
                       convertListStmt(n.getInternalBody()),
                       convertListStmt(n.getInternalOrelse()),
                       start(n), stop(n));
    }

    @Override
    public Object visitIfExp(IfExp n) throws Exception {
        return new NIfExp(convExpr(n.getInternalTest()),
                          convExpr(n.getInternalBody()),
                          convExpr(n.getInternalOrelse()),
                          start(n), stop(n));
    }

    @Override
    public Object visitImport(Import n) throws Exception {
        List<NAlias> aliases = new ArrayList<NAlias>(n.getInternalNames().size());
        for (alias e : n.getInternalNames()) {
            aliases.add(new NAlias(e.getInternalName(),
                                   convertQname(e.getInternalNameNodes()),
                                   (NName)convExpr(e.getInternalAsnameNode()),
                                   start(e), stop(e)));
        }
        return new NImport(aliases, start(n), stop(n));
    }

    @Override
    public Object visitImportFrom(ImportFrom n) throws Exception {
        List<NAlias> aliases = new ArrayList<NAlias>(n.getInternalNames().size());
        for (alias e : n.getInternalNames()) {
            aliases.add(new NAlias(e.getInternalName(),
                                   convertQname(e.getInternalNameNodes()),
                                   (NName)convExpr(e.getInternalAsnameNode()),
                                   start(e), stop(e)));
        }
        return new NImportFrom(n.getInternalModule(),
                               convertQname(n.getInternalModuleNames()),
                               aliases, start(n), stop(n));
    }

    @Override
    public Object visitIndex(Index n) throws Exception {
        return new NIndex(convExpr(n.getInternalValue()), start(n), stop(n));
    }

    @Override
    public Object visitLambda(Lambda n) throws Exception {
        arguments args = n.getInternalArgs();
        return new NLambda(convertListExpr(args.getInternalArgs()),
                           convExpr(n.getInternalBody()),
                           convertListExpr(args.getInternalDefaults()),
                           (NName)convExpr(args.getInternalVarargName()),
                           (NName)convExpr(args.getInternalKwargName()),
                           start(n), stop(n));
    }

    @Override
    public Object visitList(org.python.antlr.ast.List n) throws Exception {
        return new NList(convertListExpr(n.getInternalElts()), start(n), stop(n));
    }

    // This is more complex than it should be, but let's wait until Jython add
    // visitors to comprehensions
    @Override
    public Object visitListComp(ListComp n) throws Exception {
        List<NComprehension> generators =
                new ArrayList<NComprehension>(n.getInternalGenerators().size());
        for (comprehension c : n.getInternalGenerators()) {
            generators.add(new NComprehension(convExpr(c.getInternalTarget()),
                                              convExpr(c.getInternalIter()),
                                              convertListExpr(c.getInternalIfs()),
                                              start(c), stop(c)));
        }
        return new NListComp(convExpr(n.getInternalElt()), generators, start(n), stop(n));
    }

    @Override
    public Object visitModule(Module n) throws Exception {
        return new NModule(convertListStmt(n.getInternalBody()), start(n), stop(n));
    }

    @Override
    public Object visitName(Name n) throws Exception {
        return new NName(n.getInternalId(), start(n), stop(n));
    }

    @Override
    public Object visitNum(Num n) throws Exception {
        return new NNum(n.getInternalN(), start(n), stop(n));
    }

    @Override
    public Object visitPass(Pass n) throws Exception {
        return new NPass(start(n), stop(n));
    }

    @Override
    public Object visitPrint(Print n) throws Exception {
        return new NPrint(convExpr(n.getInternalDest()),
                          convertListExpr(n.getInternalValues()),
                          start(n), stop(n));
    }

    @Override
    public Object visitRaise(Raise n) throws Exception {
        return new NRaise(convExpr(n.getInternalType()),
                          convExpr(n.getInternalInst()),
                          convExpr(n.getInternalTback()),
                          start(n), stop(n));
    }

    @Override
    public Object visitRepr(Repr n) throws Exception {
        return new NRepr(convExpr(n.getInternalValue()), start(n), stop(n));
    }

    @Override
    public Object visitReturn(Return n) throws Exception {
        return new NReturn(convExpr(n.getInternalValue()), start(n), stop(n));
    }

    @Override
    public Object visitSlice(Slice n) throws Exception {
        return new NSlice(convExpr(n.getInternalLower()),
                          convExpr(n.getInternalStep()),
                          convExpr(n.getInternalUpper()),
                          start(n), stop(n));
    }

    @Override
    public Object visitStr(Str n) throws Exception {
        return new NStr(n.getInternalS(), start(n), stop(n));
    }

    @Override
    public Object visitSubscript(Subscript n) throws Exception {
        return new NSubscript(convExpr(n.getInternalValue()),
                              convExpr(n.getInternalSlice()),
                              start(n), stop(n));
    }

    @Override
    public Object visitTryExcept(TryExcept n) throws Exception {
        return new NTryExcept(convertListExceptHandler(n.getInternalHandlers()),
                              convertListStmt(n.getInternalBody()),
                              convertListStmt(n.getInternalOrelse()),
                              start(n), stop(n));
    }

    @Override
    public Object visitTryFinally(TryFinally n) throws Exception {
        return new NTryFinally(convertListStmt(n.getInternalBody()),
                               convertListStmt(n.getInternalFinalbody()),
                               start(n), stop(n));
    }

    @Override
    public Object visitTuple(Tuple n) throws Exception {
        return new NTuple(convertListExpr(n.getInternalElts()), start(n), stop(n));
    }

    @Override
    public Object visitUnaryOp(UnaryOp n) throws Exception {
        return new NUnaryOp(null,  // XXX:  why null for operator?
                            convExpr(n.getInternalOperand()),
                            start(n), stop(n));
    }

    @Override
    public Object visitWhile(While n) throws Exception {
        return new NWhile(convExpr(n.getInternalTest()),
                          convertListStmt(n.getInternalBody()),
                          convertListStmt(n.getInternalOrelse()),
                          start(n), stop(n));
    }

    @Override
    public Object visitWith(With n) throws Exception {
        return new NWith(convExpr(n.getInternalOptional_vars()),
                         convExpr(n.getInternalContext_expr()),
                         convertListStmt(n.getInternalBody()),
                         start(n), stop(n));
    }

    @Override
    public Object visitYield(Yield n) throws Exception {
        return new NYield(convExpr(n.getInternalValue()), start(n), stop(n));
    }
}
