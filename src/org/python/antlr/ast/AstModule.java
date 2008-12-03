package org.python.antlr.ast;

import org.python.core.ClassDictInit;
import org.python.core.imp;
import org.python.core.Py;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTableCode;
import org.python.core.PyType;

import org.python.antlr.AST;
import org.python.core.exceptions;

public class AstModule implements ClassDictInit {

    private AstModule() {}

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__doc__", Py.None);
        dict.__setitem__("__name__", new PyString("_ast"));
        dict.__setitem__("__version__", new PyString("62047"));
        dict.__setitem__("PyCF_ONLY_AST", new PyInteger(PyTableCode.PyCF_ONLY_AST));

        dict.__setitem__("AST", AST.TYPE);
        dict.__setitem__("Module", Module.TYPE);
        dict.__setitem__("Assert", Assert.TYPE);
        dict.__setitem__("Assign", Assign.TYPE);
        dict.__setitem__("Attribute", AugAssign.TYPE);
        dict.__setitem__("AugAssign", AugAssign.TYPE);
        dict.__setitem__("BinOp", BinOp.TYPE);
        dict.__setitem__("BoolOp", BoolOp.TYPE);
        dict.__setitem__("Break", Break.TYPE);
        dict.__setitem__("Call", Call.TYPE);
        dict.__setitem__("ClassDef", ClassDef.TYPE);
        dict.__setitem__("Compare", Compare.TYPE);
        dict.__setitem__("Continue", Continue.TYPE);
        dict.__setitem__("Delete", Delete.TYPE);
        dict.__setitem__("Dict", Dict.TYPE);
        dict.__setitem__("Ellipsis", Ellipsis.TYPE);
//        dict.__setitem__("ErrorExpr", ErrorExpr.TYPE);
//        dict.__setitem__("ErrorMod", ErrorMod.TYPE);
//        dict.__setitem__("ErrorSlice", ErrorSlice.TYPE);
//        dict.__setitem__("ErrorStmt", ErrorStmt.TYPE);
        dict.__setitem__("ExceptHandler", ExceptHandler.TYPE);
        dict.__setitem__("Exec", Exec.TYPE);
        dict.__setitem__("Expr", Expr.TYPE);
        dict.__setitem__("Expression", Expression.TYPE);
        dict.__setitem__("ExtSlice", ExtSlice.TYPE);
        dict.__setitem__("For", For.TYPE);
        dict.__setitem__("FunctionDef", FunctionDef.TYPE);
        dict.__setitem__("GeneratorExp", GeneratorExp.TYPE);
        dict.__setitem__("Global", Global.TYPE);
        dict.__setitem__("If", If.TYPE);
        dict.__setitem__("IfExp", IfExp.TYPE);
        dict.__setitem__("Import", Import.TYPE);
        dict.__setitem__("ImportFrom", ImportFrom.TYPE);
        dict.__setitem__("Index", Index.TYPE);
        dict.__setitem__("Interactive", Interactive.TYPE);
        dict.__setitem__("Lambda", Lambda.TYPE);
        dict.__setitem__("List", List.TYPE);
        dict.__setitem__("ListComp", ListComp.TYPE);
        dict.__setitem__("Module", Module.TYPE);
        dict.__setitem__("Name", Name.TYPE);
        dict.__setitem__("Num", Num.TYPE);
        dict.__setitem__("Pass", Pass.TYPE);
        dict.__setitem__("Print", Print.TYPE);
        dict.__setitem__("Raise", Raise.TYPE);
        dict.__setitem__("Repr", Repr.TYPE);
        dict.__setitem__("Return", Return.TYPE);
        dict.__setitem__("Slice", Slice.TYPE);
        dict.__setitem__("Str", Str.TYPE);
        dict.__setitem__("Subscript", Subscript.TYPE);
        dict.__setitem__("Suite", Suite.TYPE);
        dict.__setitem__("TryExcept", TryExcept.TYPE);
        dict.__setitem__("TryFinally", TryFinally.TYPE);
        dict.__setitem__("Tuple", Tuple.TYPE);
        dict.__setitem__("UnaryOp", UnaryOp.TYPE);
        dict.__setitem__("While", While.TYPE);
        dict.__setitem__("With", With.TYPE);
        dict.__setitem__("Yield", Yield.TYPE);
        dict.__setitem__("alias", aliasType.TYPE);
        dict.__setitem__("arguments", argumentsType.TYPE);
        dict.__setitem__("comprehension", comprehensionType.TYPE);
        dict.__setitem__("excepthandler", excepthandlerType.TYPE);
        dict.__setitem__("expr", exprType.TYPE);
        dict.__setitem__("keyword", keywordType.TYPE);
        dict.__setitem__("mod", modType.TYPE);
        dict.__setitem__("slice", sliceType.TYPE);
        dict.__setitem__("stmt", stmtType.TYPE);
        
        dict.__setitem__("operator", imp.importName("org.python.antlr.ast.operatorType", false));
        dict.__setitem__("Add", Add.TYPE);
        dict.__setitem__("Sub", Sub.TYPE);
        dict.__setitem__("Mult", Mult.TYPE);
        dict.__setitem__("Div", Div.TYPE);
        dict.__setitem__("FloorDiv", FloorDiv.TYPE);
        dict.__setitem__("Mod", Mod.TYPE);
        dict.__setitem__("LShift", LShift.TYPE);
        dict.__setitem__("RShift", RShift.TYPE);
        dict.__setitem__("BitOr", BitOr.TYPE);
        dict.__setitem__("BitAnd", BitAnd.TYPE);
        dict.__setitem__("BitXor", BitXor.TYPE);
        dict.__setitem__("Pow", Pow.TYPE);
       
        dict.__setitem__("boolop", imp.importName("org.python.antlr.ast.boolopType", false));
        dict.__setitem__("And", And.TYPE);
        dict.__setitem__("Or", Or.TYPE);
      
        dict.__setitem__("cmpop", imp.importName("org.python.antlr.ast.cmpopType", false));
        dict.__setitem__("Eq", Eq.TYPE);
        dict.__setitem__("Gt", Gt.TYPE);
        dict.__setitem__("GtE", GtE.TYPE);
        dict.__setitem__("In", In.TYPE);
        dict.__setitem__("Is", Is.TYPE);
        dict.__setitem__("IsNot", IsNot.TYPE);
        dict.__setitem__("Lt", Lt.TYPE);
        dict.__setitem__("LtE", LtE.TYPE);
        dict.__setitem__("NotEq", NotEq.TYPE);
        dict.__setitem__("NotIn", NotIn.TYPE);
       
        dict.__setitem__("expr_context", imp.importName("org.python.antlr.ast.expr_contextType", false));
        dict.__setitem__("Load", Load.TYPE);
        dict.__setitem__("Store", Store.TYPE);
        dict.__setitem__("Del", Del.TYPE);
        dict.__setitem__("AugLoad", AugLoad.TYPE);
        dict.__setitem__("AugStore", AugStore.TYPE);
        dict.__setitem__("Param", Param.TYPE);
       
        dict.__setitem__("unaryop", imp.importName("org.python.antlr.ast.unaryopType", false));
        dict.__setitem__("Invert", Invert.TYPE);
        dict.__setitem__("Not", Not.TYPE);
        dict.__setitem__("UAdd", UAdd.TYPE);
        dict.__setitem__("USub", USub.TYPE);

        dict.__setitem__("classDictInit", null);
    }
}
