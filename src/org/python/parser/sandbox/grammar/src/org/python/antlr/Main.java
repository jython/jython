package org.python.antlr;

import java.io.*;
import org.antlr.runtime.*;
import org.antlr.stringtemplate.*;
import org.antlr.stringtemplate.language.*;

public class Main {
    // override nextToken to set startPos (this seems too hard)
    public static class MyLexer extends PythonLexer {
	public MyLexer(CharStream lexer) {
	    super(lexer);
	}
	public Token nextToken() {
	    startPos = getCharPositionInLine();
	    return super.nextToken();
	}
    }

    
    public static void main(String[] args) throws Exception {
	CharStream input = new ANTLRFileStream(args[0]);
	PythonLexer lexer = new MyLexer(input);
	CommonTokenStream tokens = new CommonTokenStream(lexer);
	tokens.discardOffChannelTokens(true);
	PythonTokenSource indentedSource = new PythonTokenSource(tokens);
	tokens = new CommonTokenStream(indentedSource);
	//System.out.println("tokens="+tokens.getTokens());
	PythonParser parser = new PythonParser(tokens);
	parser.file_input();
    }
}
