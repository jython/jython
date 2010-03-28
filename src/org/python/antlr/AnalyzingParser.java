/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.antlr;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.python.antlr.ast.Name;
import org.python.antlr.base.mod;

import java.util.List;

/**
 * Parser used by the indexer.
 */
public class AnalyzingParser extends BaseParser {

    public static class AnalyzerTreeAdaptor extends PythonTreeAdaptor {
        /**
         * Make sure a parenthesized {@link Name} expr has its start/stop bounds
         * set to the bounds of the identifier.
         */
        @Override
        public void setTokenBoundaries(Object t, Token startToken, Token stopToken) {
            //XXX: should do this for all expr types, and have a prop list on Expr
            //that records all enclosing paren locations for IDE use cases.
            if (!(t instanceof Name)
                || startToken == null
                || stopToken == null
                || startToken.getType() != PythonParser.LPAREN
                || stopToken.getType() != PythonParser.RPAREN) {
                super.setTokenBoundaries(t, startToken, stopToken);
            }
        }
    }

    public AnalyzingParser(CharStream stream, String filename, String encoding) {
        super(stream, filename, encoding);
        errorHandler = new RecordingErrorHandler();
    }

    public List<RecognitionException> getRecognitionErrors() {
        return ((RecordingErrorHandler)errorHandler).errs;
    }

    @Override
    protected PythonParser setupParser(boolean single) {
        PythonParser parser = super.setupParser(single);
        parser.setTreeAdaptor(new AnalyzerTreeAdaptor());
        return parser;
    }

    public static void main(String[] args) {
        CharStream in = null;
        try {
            in = new ANTLRFileStream(args[0]);
        } catch (Exception x) {
            x.printStackTrace();
        }
        AnalyzingParser p = new AnalyzingParser(in, args[0], "ascii");
        mod ast = p.parseModule();
        if (ast != null) {
            System.out.println("parse result: \n" + ast.toStringTree());
        } else {
            System.out.println("failure: \n" + p.getRecognitionErrors());
        }
    }
}
