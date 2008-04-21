package org.python.antlr;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeAdaptor;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.runtime.tree.Tree;
import org.antlr.runtime.tree.TreeAdaptor;
import org.python.antlr.ast.modType;

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
            System.err.println("TREE: " + ((PythonTree) t).getLine());
            System.err.println("TREE TOK: " + ((PythonTree) t).token.getLine());
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

    public modType file_input() {
        modType tree = null;
        // CharStream input = new ANTLRFileStream(args[0]);
        PythonLexer lexer = new PyLexer(this.charStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.discardOffChannelTokens(true);
        PythonTokenSource indentedSource = new PythonTokenSource(tokens);
        tokens = new CommonTokenStream(indentedSource);
        // System.out.println("tokens="+tokens.getTokens());
        PythonParser parser = new PythonParser(tokens);
        parser.setTreeAdaptor(pyadaptor);
        try {
            //PythonParser.file_input_return r = parser.file_input();
            Object rx = parser.file_input();
            PythonParser.file_input_return r = (PythonParser.file_input_return)rx;
            System.out.println("tree: " + ((Tree) r.tree).toStringTree());
            System.out.println("-----------------------------------");
            //printTree((CommonTree) r.tree, 0);
            CommonTreeNodeStream nodes = new CommonTreeNodeStream((Tree)r.tree);
            nodes.setTokenStream(tokens);
            PythonWalker walker = new PythonWalker(nodes);
            tree = walker.module();
        } catch (RecognitionException e) {
            // FIXME:
            System.err.println("FIXME: don't eat exceptions:" + e);
        }
        return tree;
    }

    public modType eval_input() {
        // TODO Auto-generated method stub
        return null;
    }

    public modType single_input() {
        return file_input();
    }

}
