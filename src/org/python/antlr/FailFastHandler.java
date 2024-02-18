package org.python.antlr;

import org.antlr.runtime.BaseRecognizer;
import org.antlr.runtime.BitSet;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.Lexer;
import org.antlr.runtime.MismatchedTokenException;
import org.antlr.runtime.RecognitionException;
import org.python.antlr.base.expr;
import org.python.antlr.base.mod;
import org.python.antlr.base.slice;
import org.python.antlr.base.stmt;

public class FailFastHandler implements ErrorHandler {

    @Override
    public void reportError(BaseRecognizer br, RecognitionException re) {
        throw new ParseException(message(br,re), re);
    }

    @Override
    public void recover(Lexer lex, RecognitionException re) {
        throw new ParseException(message(lex,re), re);
    }

    @Override
    public void recover(BaseRecognizer br, IntStream input, RecognitionException re) {
        throw new ParseException(message(br,re), re);
    }

    @Override
    public boolean mismatch(BaseRecognizer br, IntStream input, int ttype, BitSet follow)
        throws RecognitionException {

        throw new MismatchedTokenException(ttype, input);
    }

    @Override
    public Object recoverFromMismatchedToken(BaseRecognizer br, IntStream input, int ttype,
            BitSet follow) throws RecognitionException {
        throw new MismatchedTokenException(ttype, input);
    }

    @Override
    public expr errorExpr(PythonTree t) {
        throw new ParseException("Bad Expr Node", t);
    }

    @Override
    public mod errorMod(PythonTree t) {
        throw new ParseException("Bad Mod Node", t);
    }

    @Override
    public slice errorSlice(PythonTree t) {
        throw new ParseException("Bad Slice Node", t);
    }

    @Override
    public stmt errorStmt(PythonTree t) {
        throw new ParseException("Bad Stmt Node", t);
    }

    @Override
    public void error(String message, PythonTree t) {
        throw new ParseException(message, t);
    }

    @Override
    public void error(String message, PythonTree t, boolean definite) {
        throw new ParseException(message, t, definite);
    }

    private String message(BaseRecognizer br, RecognitionException re) {
        return br.getErrorMessage(re, br.getTokenNames());
    }

}
