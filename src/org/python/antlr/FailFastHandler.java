package org.python.antlr;

import org.antlr.runtime.BaseRecognizer;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.RecognitionException;

public class FailFastHandler implements ErrorHandler {
    private BaseRecognizer recognizer;

    public void reportError(BaseRecognizer br, RecognitionException re) {
        throw new ParseException(message(br,re), re);
    }

    public void recover(BaseRecognizer br, RecognitionException re) {
        throw new ParseException(message(br,re), re);
    }

    public void recover(BaseRecognizer br, IntStream input, RecognitionException re) {
        throw new ParseException(message(br,re), re);
    }

    public boolean isRecoverable() {
        return false;
    }

    private String message(BaseRecognizer br, RecognitionException re) {
		String hdr = br.getErrorHeader(re);
		String msg = br.getErrorMessage(re, br.getTokenNames());
        return hdr+" "+msg;
    }
}
