package org.python.antlr;

import org.antlr.runtime.BaseRecognizer;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.Lexer;
import org.antlr.runtime.RecognitionException;
import org.python.antlr.ast.exprType;
import org.python.antlr.ast.modType;
import org.python.antlr.ast.sliceType;
import org.python.antlr.ast.stmtType;

interface ErrorHandler {
    void reportError(BaseRecognizer br, RecognitionException re);
    void recover(BaseRecognizer br, IntStream input, RecognitionException re);
    void recover(Lexer lex, RecognitionException re);
    boolean isRecoverable();
    //exprType, modType, sliceType, stmtType
    exprType errorExpr(PythonTree t);
    modType errorMod(PythonTree t);
    sliceType errorSlice(PythonTree t);
    stmtType errorStmt(PythonTree t);

    //exceptions
    void error(String message, PythonTree t);
}
