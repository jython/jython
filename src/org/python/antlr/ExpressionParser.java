package org.python.antlr;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.python.antlr.base.mod;

public class ExpressionParser extends BaseParser {

    public ExpressionParser(CharStream cs, String filename, String encoding) {
        this.charStream = cs;
        this.filename = filename;
        this.encoding = encoding;
    }

    public mod parse() {
        mod tree = null;
        PythonLexer lexer = new PyLexer(this.charStream);
        lexer.setErrorHandler(errorHandler);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PythonTokenSource indentedSource = new PythonTokenSource(tokens, filename);
        tokens = new CommonTokenStream(indentedSource);
        PythonParser parser = new PythonParser(tokens, encoding);
        parser.setErrorHandler(errorHandler);
        parser.setTreeAdaptor(new PythonTreeAdaptor());

        try {
            PythonParser.eval_input_return r = parser.eval_input();
            tree = (mod)r.tree;
        } catch (RecognitionException e) {
            //XXX: this can't happen.  Need to strip the throws from antlr
            //     generated code.
        }
        return tree;
    }
}
