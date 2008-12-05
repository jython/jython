package org.python.antlr;

import java.io.BufferedReader;
import java.io.IOException;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.python.antlr.base.mod;

public class InteractiveParser extends BaseParser {

    private BufferedReader bufreader;

    public InteractiveParser(BufferedReader br, String filename, String encoding) {
        this.bufreader = br;
        this.filename = filename;
        this.encoding = encoding;
    }

    public mod parse() throws IOException {
        mod tree = null;
        PythonLexer lexer = new PyLexer(new NoCloseReaderStream(bufreader));
        lexer.setErrorHandler(errorHandler);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PythonTokenSource indentedSource = new PythonTokenSource(tokens, filename, true);
        tokens = new CommonTokenStream(indentedSource);
        PythonParser parser = new PythonParser(tokens, encoding);
        parser.setErrorHandler(errorHandler);
        parser.setTreeAdaptor(new PythonTreeAdaptor());

        try {
            PythonParser.single_input_return r = parser.single_input();
            tree = (mod)r.tree;
        } catch (RecognitionException e) {
            //I am only throwing ParseExceptions, but "throws RecognitionException" still gets
            //into the generated code.
            System.err.println("FIXME: pretty sure this can't happen -- but needs to be checked");
        }
        return tree;
    }
}
