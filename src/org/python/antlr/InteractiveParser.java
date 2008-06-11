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

public class InteractiveParser {

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

    public InteractiveParser(CharStream cs) {
        this.charStream = cs;
    }

    public modType partialParse() {
        /*
        CPython codeop exploits that with CPython parser adding newlines
        to a partial valid sentence move the reported error position,
        this is not true for our parser, so we need a different approach:
        we check whether all sentence tokens have been consumed or
        the remaining ones fullfill lookahead expectations. See:
        PythonGrammar.partial_valid_sentence (def in python.jjt)

        FJW: the above comment needs to be changed when the current partial
        parse strategy gels.
        */
        try {
            return parse();
        } catch (ParseException e) {
            //FIXME: This needs plenty of tuning, this just calls all errors
            //partial matches.
            return null;
        }
    }
            
    public modType parse() {
        modType tree = null;
        PythonLexer lexer = new PyLexer(this.charStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.discardOffChannelTokens(true);
        PythonTokenSource indentedSource = new PythonTokenSource(tokens);
        tokens = new CommonTokenStream(indentedSource);
        PythonParser parser = new PythonParser(tokens);
        parser.setTreeAdaptor(pyadaptor);

        try {
            PythonParser.single_input_return r = parser.single_input();
            CommonTreeNodeStream nodes = new CommonTreeNodeStream((Tree)r.tree);
            nodes.setTokenStream(tokens);
            PythonWalker walker = new PythonWalker(nodes);
            tree = walker.interactive();
        } catch (RecognitionException e) {
            //XXX: this can't happen.  Need to strip the throws from antlr
            //     generated code.
        }
        return tree;
    } 
}
