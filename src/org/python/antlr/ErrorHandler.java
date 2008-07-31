package org.python.antlr;

import org.antlr.runtime.BaseRecognizer;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.RecognitionException;

interface ErrorHandler {
    void reportError(BaseRecognizer br, RecognitionException re);
    void recover(BaseRecognizer br, RecognitionException re);
    void recover(BaseRecognizer br, IntStream input, RecognitionException re);
    boolean isRecoverable();
}
