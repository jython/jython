package org.python.antlr;

import org.antlr.runtime.BaseRecognizer;
import org.antlr.runtime.BitSet;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.Lexer;
import org.antlr.runtime.RecognitionException;
import org.python.antlr.base.expr;
import org.python.antlr.base.mod;
import org.python.antlr.base.slice;
import org.python.antlr.base.stmt;

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

    //expr, mod, slice, stmt
    expr errorExpr(PythonTree t);
    mod errorMod(PythonTree t);
    slice errorSlice(PythonTree t);
    stmt errorStmt(PythonTree t);

    //exceptions

    /**
     * Handle (normally throw a {@code ParseException}) specifying whether to treat as a final
     * decision. In the REPL, when {@code definite=true}, Jython will signal a definite syntax or
     * semantic error, not recoverable by waiting for further input.
     *
     * @param message text to include.
     * @param t not {@code null}, root of the {@link PythonTree} provoking the error.
     * @param definite true if we cannot recover with more input.
     */
    default void error(String message, PythonTree t, boolean definite) {
        error(message, t);
    }

    /**
     * Handle (normally throw a {@code ParseException}) possibly recoverable by waiting for further
     * input.
     *
     * @param message text to include.
     * @param t not {@code null}, root of the {@link PythonTree} provoking the error.
     */
    void error(String message, PythonTree t);
}
