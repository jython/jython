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
public class PythonTreeWalker {

	private boolean _parseOnly;
	private boolean _tolerant;

	public PythonTreeWalker() {
		setParseOnly(false);
		setTolerant(true);
	}

	public PythonTree parse(String[] args) throws Exception {
		PythonTree result = null;
		CharStream input = new ANTLRFileStream(args[0]);
		PythonLexer lexer = new PyLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		tokens.discardOffChannelTokens(true);
		PythonTokenSource indentedSource = new PythonTokenSource(tokens);
		tokens = new CommonTokenStream(indentedSource);
		// System.out.println("tokens="+tokens.getTokens());
		PythonParser parser = new PythonParser(tokens);
		parser.setTreeAdaptor(pyadaptor);
		try {
			PythonParser.file_input_return r = parser.file_input();
			if (parser.hasErrors()) {
				// handle errors swallowed by antlr recovery
				String errors = parser.getErrors().toString();
				if (isTolerant()) {
					System.err.println(errors);
				} else {
					throw new RuntimeException(errors);
				}
			}
			if (args.length > 1) {
				System.out.println(((Tree) r.tree).toStringTree());
			}
			if (!isParseOnly()) {
				CommonTreeNodeStream nodes = new CommonTreeNodeStream((Tree) r.tree);
				nodes.setTokenStream(tokens);
				PythonWalker walker = new PythonWalker(nodes);
				result = walker.module();
				if (args.length > 1) {
					System.out.println(result.toStringTree());
				}
			}
		} catch (RecognitionException e) {
			if (isTolerant()) {
				System.err.println("Error: " + e);
			} else {
				throw e;
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

	/**
	 * If set to <code>true</code>, exceptions are catched and logged to
	 * <code>System.err</code>.
	 * 
	 * @param tolerant
	 */
	public void setTolerant(boolean tolerant) {
		_tolerant = tolerant;
	}

	public boolean isTolerant() {
		return _tolerant;
	}

	/**
	 * override nextToken to set startPos (this seems too hard)
	 */
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
			/*
			 * if (token != null && token.getType() == PythonParser.Target) {
			 * System.out.println("Target found"); }
			 */
			return new PythonTree(token);
		}

		public Object dupNode(Object t) {
			if (t == null) {
				return null;
			}
			return create(((PythonTree) t).token);
		}
	};

}
