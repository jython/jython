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

public class ExpressionParser {

    private CharStream charStream;

    //Extract superclass from this and the other XParsers.
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

    public ExpressionParser(CharStream cs) {
        this.charStream = cs;
    }

    public modType parse() throws RecognitionException {
        modType tree = null;
        PythonLexer lexer = new PyLexer(this.charStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.discardOffChannelTokens(true);
        PythonTokenSource indentedSource = new PythonTokenSource(tokens);
        tokens = new CommonTokenStream(indentedSource);
        PythonParser parser = new PythonParser(tokens);
        parser.setTreeAdaptor(pyadaptor);

        Object rx = parser.eval_input();
        PythonParser.eval_input_return r = (PythonParser.eval_input_return)rx;
        CommonTreeNodeStream nodes = new CommonTreeNodeStream((Tree)r.tree);
        nodes.setTokenStream(tokens);
        PythonWalker walker = new PythonWalker(nodes);
        tree = walker.expression();
        return tree;
    }
}
