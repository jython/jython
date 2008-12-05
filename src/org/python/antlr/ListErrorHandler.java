package org.python.antlr;

import org.antlr.runtime.BaseRecognizer;
import org.antlr.runtime.BitSet;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.Lexer;
import org.antlr.runtime.RecognitionException;
import org.python.antlr.ast.ErrorMod;
import org.python.antlr.ast.ErrorExpr;
import org.python.antlr.ast.ErrorSlice;
import org.python.antlr.ast.ErrorStmt;
import org.python.antlr.base.expr;
import org.python.antlr.base.mod;
import org.python.antlr.base.slice;
import org.python.antlr.base.stmt;

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

    public boolean mismatch(BaseRecognizer br, IntStream input, int ttype, BitSet follow) {
        return true;
    }

    public Object recoverFromMismatchedToken(BaseRecognizer br, IntStream input, int ttype, BitSet follow) {
        return null;
    }

    public expr errorExpr(PythonTree t) {
        return new ErrorExpr(t);
    }

    public mod errorMod(PythonTree t) {
        return new ErrorMod(t);
    }

    public slice errorSlice(PythonTree t) {
        return new ErrorSlice(t);
    }

    public stmt errorStmt(PythonTree t) {
        return new ErrorStmt(t);
    }

    public void error(String message, PythonTree t) {
        System.err.println(message);
    }

}
