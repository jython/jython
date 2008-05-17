package org.python.antlr;

public class GrammarOnly {

	public static void main(String[] args) throws Exception {
		PythonTreeWalker walker = new PythonTreeWalker();
		walker.setTolerant(true);
		walker.setParseOnly(true);
		walker.parse(args);
	}

}
