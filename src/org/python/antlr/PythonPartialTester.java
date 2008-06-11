package org.python.antlr;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.Token;

/**
 * A walker producing a <code>PythonTree</code> AST.
 */
public class PythonPartialTester {

    public static class PPLexer extends PythonPartialLexer {
        public PPLexer(CharStream lexer) {
            super(lexer);
        }

        public Token nextToken() {
            startPos = getCharPositionInLine();
            return super.nextToken();
        }
    }

    public enum Block { MODULE, INTERACTIVE, EXPRESSION };

    private Block _block = Block.INTERACTIVE;

	public void parse(String[] args) throws Exception {
//        try {
            PythonTree result = null;
            CharStream input = new ANTLRFileStream(args[0]);
            PythonPartialLexer lexer = new PPLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            tokens.discardOffChannelTokens(true);
            PythonTokenSource indentedSource = new PythonTokenSource(tokens);
            tokens = new CommonTokenStream(indentedSource);
            PythonPartialParser parser = new PythonPartialParser(tokens);
            switch (_block) {
            case MODULE :
                parser.file_input();
                break;
            case INTERACTIVE :
                parser.single_input();
                break;
            case EXPRESSION :
                parser.eval_input();
                break;
            }
            System.out.println("SUCCEED");
//        } catch (ParseException e) {
//            System.out.println("FAIL" + );
//        }
	}

	public static void main(String[] args) throws Exception {
		PythonPartialTester p = new PythonPartialTester();
		p.parse(args);
	}

}
