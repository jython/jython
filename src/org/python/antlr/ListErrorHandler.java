package org.python.antlr;

import org.antlr.runtime.BaseRecognizer;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.Lexer;
import org.antlr.runtime.RecognitionException;
import org.python.antlr.ast.ErrorMod;
import org.python.antlr.ast.exprType;
import org.python.antlr.ast.ErrorExpr;
import org.python.antlr.ast.ErrorSlice;
import org.python.antlr.ast.ErrorStmt;
import org.python.antlr.ast.modType;
import org.python.antlr.ast.sliceType;
import org.python.antlr.ast.stmtType;

public class ListErrorHandler implements ErrorHandler {

    public void reportError(BaseRecognizer br, RecognitionException re) {
        br.reportError(re);
    }

    public void recover(Lexer lex, RecognitionException re) {
        lex.recover(re);
    }

    public void recover(BaseRecognizer br, IntStream input, RecognitionException re) {
        br.recover(input, re);
    }

    public boolean isRecoverable() {
        return true;
    }

    public exprType errorExpr(PythonTree t) {
        return new ErrorExpr(t);
    }

    public modType errorMod(PythonTree t) {
        return new ErrorMod(t);
    }

    public sliceType errorSlice(PythonTree t) {
        return new ErrorSlice(t);
    }

    public stmtType errorStmt(PythonTree t) {
        return new ErrorStmt(t);
    }

}
