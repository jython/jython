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

public class PythonGrammar {
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

    public PythonGrammar(CharStream cs) {
        this(cs, false);
    }

    public PythonGrammar(CharStream cs, boolean partial) {
        this.partial = partial;
        this.charStream = cs;
    }

    public void printTree(CommonTree t, int indent) {
        if (t != null) {
            System.out.println("XXX: " + t.toString() + t.getType());
            StringBuffer sb = new StringBuffer(indent);
            for (int i = 0; i < indent; i++) {
                sb = sb.append("   ");
            }
            for (int i = 0; i < t.getChildCount(); i++) {
                System.out.println(sb.toString() + t.getChild(i).toString() + ":" + t.getChild(i).getType());
                printTree((CommonTree) t.getChild(i), indent + 1);
            }
        }
    }

    //XXX: factor out common code.
    public modType file_input() throws RecognitionException {
        modType tree = null;
        PythonLexer lexer = new PyLexer(this.charStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.discardOffChannelTokens(true);
        PythonTokenSource indentedSource = new PythonTokenSource(tokens);
        tokens = new CommonTokenStream(indentedSource);
        PythonParser parser = new PythonParser(tokens);
        parser.setTreeAdaptor(pyadaptor);

        Object rx = parser.file_input();
        PythonParser.file_input_return r = (PythonParser.file_input_return)rx;
        CommonTreeNodeStream nodes = new CommonTreeNodeStream((Tree)r.tree);
        nodes.setTokenStream(tokens);
        PythonWalker walker = new PythonWalker(nodes);
        tree = walker.module();
        if (tree == null) {
            //XXX: seems like I should be able to get antlr to give me an empty Module instead
            //     of null so I wouldn't need to build an empty Module by hand here...
            return new Module(new PythonTree(new CommonToken(PyLexer.Module)), new stmtType[0]);
        }
        return tree;
    }

    //XXX: factor out common code.
    public modType single_input() throws RecognitionException {
        modType tree = null;
        PythonLexer lexer = new PyLexer(this.charStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.discardOffChannelTokens(true);
        PythonTokenSource indentedSource = new PythonTokenSource(tokens);
        tokens = new CommonTokenStream(indentedSource);
        PythonParser parser = new PythonParser(tokens);
        parser.setTreeAdaptor(pyadaptor);
        Object rx = parser.single_input();
        PythonParser.single_input_return r = (PythonParser.single_input_return)rx;
        CommonTreeNodeStream nodes = new CommonTreeNodeStream((Tree)r.tree);
        nodes.setTokenStream(tokens);
        PythonWalker walker = new PythonWalker(nodes);
        tree = walker.interactive();
        return tree;
    }
}
