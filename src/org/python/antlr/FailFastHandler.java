package org.python.antlr;

import org.antlr.runtime.BaseRecognizer;
import org.antlr.runtime.BitSet;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.Lexer;
import org.antlr.runtime.MismatchedTokenException;
import org.antlr.runtime.RecognitionException;
import org.python.antlr.ast.exprType;
import org.python.antlr.ast.modType;
import org.python.antlr.ast.sliceType;
import org.python.antlr.ast.stmtType;

public class FailFastHandler implements ErrorHandler {

    public void reportError(BaseRecognizer br, RecognitionException re) {
        throw new ParseException(message(br,re), re);
    }

    public void recover(Lexer lex, RecognitionException re) {
        throw new ParseException(message(lex,re), re);
    }

    public void recover(BaseRecognizer br, IntStream input, RecognitionException re) {
        throw new ParseException(message(br,re), re);
    }

    public boolean mismatch(BaseRecognizer br, IntStream input, int ttype, BitSet follow)
        throws RecognitionException {

        throw new MismatchedTokenException(ttype, input);
    }

    public Object recoverFromMismatchedToken(BaseRecognizer br, IntStream input, int ttype,
            BitSet follow) throws RecognitionException {
        throw new MismatchedTokenException(ttype, input);
    }

    public exprType errorExpr(PythonTree t) {
        throw new ParseException("Bad Expr Node", t);
    }

    public modType errorMod(PythonTree t) {
        throw new ParseException("Bad Mod Node", t);
    }

    public sliceType errorSlice(PythonTree t) {
        throw new ParseException("Bad Slice Node", t);
    }

    public stmtType errorStmt(PythonTree t) {
        throw new ParseException("Bad Stmt Node", t);
    }

    public void error(String message, PythonTree t) {
        throw new ParseException(message, t);
    }

    private String message(BaseRecognizer br, RecognitionException re) {
		String hdr = br.getErrorHeader(re);
		String msg = br.getErrorMessage(re, br.getTokenNames());
        return hdr+" "+msg;
    }

}
