package org.python.antlr;

public class WalkerTester extends PythonTreeTester {

	public static void main(String[] args) throws Exception {
		PythonTreeTester walker = new PythonTreeTester();
		walker.setParseOnly(false);
		walker.parse(args);
	}

}
