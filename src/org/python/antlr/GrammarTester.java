package org.python.antlr;

public class GrammarTester {

	public static void main(String[] args) throws Exception {
		PythonTreeTester walker = new PythonTreeTester();
		walker.setParseOnly(true);
		walker.parse(args);
	}

}
