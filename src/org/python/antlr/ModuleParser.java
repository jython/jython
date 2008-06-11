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

public class ModuleParser {
    public static class PyLexer extends PythonLexer {
        public PyLexer(CharStream lexer) {
            super(lexer);
        }

        public Token nextToken() {
            startPos = getCharPositionInLine();
            return super.nextToken();
        }
    }

    public static TreeAdaptor pyadaptor = new CommonTreeAdaptor() {
        public Object create(Token token) {
            return new PythonTree(token);
        }

        public Object dupNode(Object t) {
            if (t == null) {
                return null;
            }
            return create(((PythonTree) t).token);
        }
    };  


    private CharStream charStream;
    private boolean partial;

    public ModuleParser(CharStream cs) {
        this(cs, false);
    }

    public ModuleParser(CharStream cs, boolean partial) {
        this.partial = partial;
        this.charStream = cs;
    }

    public modType file_input() {
        modType tree = null;
        PythonLexer lexer = new PyLexer(this.charStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.discardOffChannelTokens(true);
        PythonTokenSource indentedSource = new PythonTokenSource(tokens);
        tokens = new CommonTokenStream(indentedSource);
        PythonParser parser = new PythonParser(tokens);
        parser.setTreeAdaptor(pyadaptor);

        try {
            PythonParser.file_input_return r = parser.file_input();
            CommonTreeNodeStream nodes = new CommonTreeNodeStream((Tree)r.tree);
            nodes.setTokenStream(tokens);
            PythonWalker walker = new PythonWalker(nodes);
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
