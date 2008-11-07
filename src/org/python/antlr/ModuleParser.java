package org.python.antlr;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.python.antlr.ast.modType;

public class ModuleParser extends BaseParser {
    public ModuleParser(CharStream cs, String filename, String encoding) {
        this(cs, filename, encoding, false);
    }

    public ModuleParser(CharStream cs, String filename, String encoding, boolean partial) {
        this.charStream = cs;
        this.filename = filename;
        this.encoding = encoding;
        this.partial = partial;
    }

    public modType file_input() {
        modType tree = null;
        PythonLexer lexer = new PyLexer(this.charStream);
        lexer.setErrorHandler(errorHandler);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PythonTokenSource indentedSource = new PythonTokenSource(tokens, filename);
        tokens = new CommonTokenStream(indentedSource);
        PythonParser parser = new PythonParser(tokens, encoding);
        parser.setErrorHandler(errorHandler);
        parser.setTreeAdaptor(new PythonTreeAdaptor());
        try {
            PythonParser.file_input_return r = parser.file_input();
            tree = (modType)r.tree;
        } catch (RecognitionException e) {
            //XXX: this can't happen.  Need to strip the throws from antlr
            //     generated code.
        }
        return tree;
    }
}
