package org.python.antlr;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTreeAdaptor;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.runtime.tree.Tree;
import org.antlr.runtime.tree.TreeAdaptor;

/**
 * A walker producing a <code>PythonTree</code> AST.
 */
public class PythonTreeTester {

    public enum Block { MODULE, INTERACTIVE, EXPRESSION };

    private boolean _parseOnly;
    private Block _block;

    public PythonTreeTester() {
        setParseOnly(false);
        setBlock(Block.MODULE);
    }

    public PythonTree parse(String[] args) throws Exception {
        PythonTree result = null;
        CharStream input = new ANTLRFileStream(args[0]);
        PythonLexer lexer = new ModuleParser.PyLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.discardOffChannelTokens(true);
        PythonTokenSource indentedSource = new PythonTokenSource(tokens);
        tokens = new CommonTokenStream(indentedSource);
        PythonParser parser = new PythonParser(tokens);
        parser.setTreeAdaptor(new PythonTreeAdaptor());
        Tree r = null;
        switch (_block) {
        case MODULE :
            r = (Tree)parser.file_input().tree;
            break;
        case INTERACTIVE :
            r = (Tree)parser.single_input().tree;
            break;
        case EXPRESSION :
            r = (Tree)parser.eval_input().tree;
                break;
        }
        if (args.length > 1) {
            System.out.println((r).toStringTree());
        }
        if (!isParseOnly()) {
            CommonTreeNodeStream nodes = new CommonTreeNodeStream(r);
            nodes.setTokenStream(tokens);
            PythonWalker walker = new PythonWalker(nodes);
            switch (_block) {
            case MODULE :
                result = walker.module();
                break;
            case INTERACTIVE :
                result = walker.interactive();
                break;
            case EXPRESSION :
                result = walker.expression();
                break;
            }

            if (args.length > 1) {
                System.out.println(result.toStringTree());
            }
        }
        return result;
    }

    /**
     * If set to <code>true</code>, only <code>PythonParser</code> is
     * called.
     * 
     * @param parseOnly
     */
    public void setParseOnly(boolean parseOnly) {
        _parseOnly = parseOnly;
    }

    public boolean isParseOnly() {
        return _parseOnly;
    }

    public void setBlock(Block block) {
        _block = block;
    }

    public Block getBlock() {
        return _block;
    }

}
