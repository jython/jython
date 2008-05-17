package org.python.antlr;

public class Main extends PythonTreeWalker {

	public static void main(String[] args) throws Exception {
		PythonTreeWalker walker = new PythonTreeWalker();
		walker.setTolerant(true);
		walker.setParseOnly(false);
		walker.parse(args);
	}

}
