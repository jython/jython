package org.python.antlr;

import org.antlr.runtime.BaseRecognizer;
import org.antlr.runtime.BitSet;
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

    /**
     * @return True if the caller should handle the mismatch
     */
    boolean mismatch(BaseRecognizer br, IntStream input, int ttype, BitSet follow)
        throws RecognitionException;

    /**
     * @return null if the caller should handle the mismatch
     */
    Object recoverFromMismatchedToken(BaseRecognizer br, IntStream input, int ttype, BitSet follow)
        throws RecognitionException;

    //exprType, modType, sliceType, stmtType
    exprType errorExpr(PythonTree t);
    modType errorMod(PythonTree t);
    sliceType errorSlice(PythonTree t);
    stmtType errorStmt(PythonTree t);

    //exceptions
    void error(String message, PythonTree t);
}
