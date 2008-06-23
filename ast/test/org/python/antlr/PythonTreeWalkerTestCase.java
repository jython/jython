package org.python.antlr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import junit.framework.TestCase;

/**
 * Decorates the PythonTreeWalker class as a JUnit test case for a single .py
 * file
 */
public class PythonTreeWalkerTestCase extends TestCase {

	private String _path;

	/**
	 * Constructor called from rerun menu point in eclipse
	 * 
	 * @param name The name of the test.
	 */
	public PythonTreeWalkerTestCase(String name) {
		super(name);
		setPath(name);
	}

	/**
	 * Create a test case which walks <code>pyFile</code>.
	 * 
	 * @param pyFile The *.py file
	 */
	public PythonTreeWalkerTestCase(File pyFile) {
		this(pyFile.getAbsolutePath());
	}

	@Override
	protected void runTest() throws Throwable {
		String path = getPath();
		File file = new File(path);
		assertTrue("file " + path + " not found", file.exists());
		PythonTreeWalker treeWalker = new PythonTreeWalker();
		treeWalker.setTolerant(false);
		treeWalker.setParseOnly(false);
		PythonTree tree = treeWalker.parse(new String[] { path });
		if (tree == null) {
			if (!isEmpty(file)) {
				assertNotNull("no tree generated for file ".concat(path), tree);
			}
		}
	}

	@Override
	public int countTestCases() {
		return 1;
	}

	private void setPath(String path) {
		_path = path;
	}

	private String getPath() {
		return _path;
	}

	/**
	 * 'empty' check for a .py file
	 * 
	 * @param file The file to check
	 * @return <code>true</code> if the file is really empty, or only contains
	 *         comments
	 * @throws IOException
	 */
	private boolean isEmpty(File file) throws IOException {
		boolean isEmpty = true;
		assertTrue("file " + file.getAbsolutePath() + " is not readable", file.canRead());
		BufferedReader reader = new BufferedReader(new FileReader(file));
		try {
			String line = reader.readLine();
			while (line != null && isEmpty) {
				line = line.trim();
				if (line.length() > 0 && !line.startsWith("#")) {
					isEmpty = false;
				}
				line = reader.readLine();
			}
		} finally {
			reader.close();
		}
		return isEmpty;
	}

}
