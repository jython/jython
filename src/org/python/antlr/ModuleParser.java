package org.python.antlr;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeAdaptor;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.runtime.tree.Tree;
import org.antlr.runtime.tree.TreeAdaptor;
import org.python.antlr.ast.modType;
import org.python.antlr.ast.Module;
import org.python.antlr.ast.stmtType;

public class ModuleParser extends BaseParser {
    public ModuleParser(CharStream cs, String filename) {
        this(cs, filename, false);
    }

    public ModuleParser(CharStream cs, String filename, boolean partial) {
        this.partial = partial;
        this.charStream = cs;
        this.filename = filename;
    }

    public modType file_input() {
        modType tree = null;
        PythonLexer lexer = new PyLexer(this.charStream);
        lexer.setErrorHandler(errorHandler);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PythonTokenSource indentedSource = new PythonTokenSource(tokens, filename);
        tokens = new CommonTokenStream(indentedSource);
        PythonParser parser = new PythonParser(tokens);
        parser.setErrorHandler(errorHandler);
        parser.setTreeAdaptor(new PythonTreeAdaptor());
        try {
            PythonParser.file_input_return r = parser.file_input();
            CommonTreeNodeStream nodes = new CommonTreeNodeStream((Tree)r.tree);
            nodes.setTokenStream(tokens);
            PythonWalker walker = new PythonWalker(nodes);
            walker.setErrorHandler(errorHandler);
            tree = walker.module();
            if (tree == null) {
                //XXX: seems like I should be able to get antlr to give me an empty Module instead
                //     of null so I wouldn't need to build an empty Module by hand here...
                return new Module(new PythonTree(new CommonToken(PyLexer.Module)), new stmtType[0]);
            }
        } catch (RecognitionException e) {
            //XXX: this can't happen.  Need to strip the throws from antlr
            //     generated code.
        }

        return tree;
    }

}
