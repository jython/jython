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
        dict.__setitem__("Add", imp.importName("org.python.antlr.ast.operatorType.Add", false));
        dict.__setitem__("Sub", imp.importName("org.python.antlr.ast.operatorType.Sub", false));
        dict.__setitem__("Mult", imp.importName("org.python.antlr.ast.operatorType.Mult", false));
        dict.__setitem__("Div", imp.importName("org.python.antlr.ast.operatorType.Div", false));
        dict.__setitem__("FloorDiv", imp.importName("org.python.antlr.ast.operatorType.FloorDiv", false));
        dict.__setitem__("Mod", imp.importName("org.python.antlr.ast.operatorType.Mod", false));
        dict.__setitem__("LShift", imp.importName("org.python.antlr.ast.operatorType.LShift", false));
        dict.__setitem__("RShift", imp.importName("org.python.antlr.ast.operatorType.RShift", false));
        dict.__setitem__("BitOr", imp.importName("org.python.antlr.ast.operatorType.BitOr", false));
        dict.__setitem__("BitAnd", imp.importName("org.python.antlr.ast.operatorType.BitAnd", false));
        dict.__setitem__("BitXor", imp.importName("org.python.antlr.ast.operatorType.BitXor", false));
        dict.__setitem__("Pow", imp.importName("org.python.antlr.ast.operatorType.Pow", false));
       
        dict.__setitem__("boolop", imp.importName("org.python.antlr.ast.boolopType", false));
        dict.__setitem__("And", imp.importName("org.python.antlr.ast.boolopType.And", false));
        dict.__setitem__("Or", imp.importName("org.python.antlr.ast.boolopType.Or", false));
      
        dict.__setitem__("cmpop", imp.importName("org.python.antlr.ast.cmpopType", false));
        dict.__setitem__("Eq", imp.importName("org.python.antlr.ast.cmpopType.Eq", false));
        dict.__setitem__("Gt", imp.importName("org.python.antlr.ast.cmpopType.Gt", false));
        dict.__setitem__("GtE", imp.importName("org.python.antlr.ast.cmpopType.GtE", false));
        dict.__setitem__("In", imp.importName("org.python.antlr.ast.cmpopType.In", false));
        dict.__setitem__("Is", imp.importName("org.python.antlr.ast.cmpopType.Is", false));
        dict.__setitem__("IsNot", imp.importName("org.python.antlr.ast.cmpopType.IsNot", false));
        dict.__setitem__("Lt", imp.importName("org.python.antlr.ast.cmpopType.Lt", false));
        dict.__setitem__("LtE", imp.importName("org.python.antlr.ast.cmpopType.LtE", false));
        dict.__setitem__("NotEq", imp.importName("org.python.antlr.ast.cmpopType.NotEq", false));
        dict.__setitem__("NotIn", imp.importName("org.python.antlr.ast.cmpopType.NotIn", false));
       
        dict.__setitem__("expr_context", imp.importName("org.python.antlr.ast.expr_contextType", false));
        dict.__setitem__("Load", imp.importName("org.python.antlr.ast.expr_contextType.Load", false));
        dict.__setitem__("Store", imp.importName("org.python.antlr.ast.expr_contextType.Store", false));
        dict.__setitem__("Del", imp.importName("org.python.antlr.ast.expr_contextType.Del", false));
        dict.__setitem__("AugLoad", imp.importName("org.python.antlr.ast.expr_contextType.AugLoad", false));
        dict.__setitem__("AugStore", imp.importName("org.python.antlr.ast.expr_contextType.AugStore", false));
        dict.__setitem__("Param", imp.importName("org.python.antlr.ast.expr_contextType.Param", false));
       
        dict.__setitem__("unaryop", imp.importName("org.python.antlr.ast.unaryopType", false));
        dict.__setitem__("Invert", imp.importName("org.python.antlr.ast.unaryopType.Invert", false));
        dict.__setitem__("Not", imp.importName("org.python.antlr.ast.unaryopType.Not", false));
        dict.__setitem__("UAdd", imp.importName("org.python.antlr.ast.unaryopType.UAdd", false));
        dict.__setitem__("USub", imp.importName("org.python.antlr.ast.unaryopType.USub", false));

        dict.__setitem__("classDictInit", null);
    }
}
