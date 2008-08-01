package org.python.antlr;

import org.antlr.runtime.BaseRecognizer;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.Lexer;
import org.antlr.runtime.RecognitionException;

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

}
