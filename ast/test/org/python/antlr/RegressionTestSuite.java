package org.python.antlr;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * A regression test suite traversing all *.py files, parsing and walking them
 * <p>
 * The root directory of the *.py files is read from a System property
 * <code>base.dir</code>. If this property is not set, we use the current
 * directory where the runner is started.
 * <p>
 * Example: call the runner with
 * 
 * <pre>
 * -Dbase.dir=/your/dir/containing/py/files
 * </pre>
 */
public class RegressionTestSuite extends TestSuite {
	private static final String BASEDIR_PROPERTY = "base.dir";

	/**
	 * Method called by a JUnit test runner
	 * 
	 * @return the test suite containing all the tests
	 * 
	 * @throws Exception
	 */
	public static Test suite() throws Exception {
		String baseDirName = System.getProperty(BASEDIR_PROPERTY, System.getProperty("user.dir", null));
		Assert.assertNotNull("unable to determine root directory. Try specifying property " + BASEDIR_PROPERTY,
				baseDirName);
		File baseDir = new File(baseDirName);
		Assert.assertTrue(baseDirName + " does not exist", baseDir.exists());
		Assert.assertTrue(baseDirName + " is not a directory", baseDir.isDirectory());
		TestSuite suite = new TestSuite("Regression test " + baseDirName + " recursive.");
		buildSuite(baseDir, new PyFileFilter(), suite);
		return suite;
	}

	private static void buildSuite(File currentDir, FilenameFilter filter, TestSuite currentSuite) throws Exception {
		List<File> potentialDirectories = Arrays.asList(currentDir.listFiles(filter));
		if (potentialDirectories.size() == 0) {
			return;
		}

		List<File> pyFiles = new ArrayList<File>(potentialDirectories.size());
		Collections.sort(potentialDirectories, new FileComparator());
		Iterator<File> directoryIterator = potentialDirectories.iterator();
		while (directoryIterator.hasNext()) {
			File potentialDirectory = directoryIterator.next();
			if (potentialDirectory.isDirectory()) {
				TestSuite subTestSuite = new TestSuite(potentialDirectory.getName());
				buildSuite(potentialDirectory, filter, subTestSuite);
				// only if suite contains tests
				if (subTestSuite.countTestCases() > 0) {
					currentSuite.addTest(subTestSuite);
				}
			} else {
				pyFiles.add(potentialDirectory);
			}
		}
		Iterator<File> fileIterator = pyFiles.iterator();
		while (fileIterator.hasNext()) {
			currentSuite.addTest(new PythonTreeWalkerTestCase(fileIterator.next()));
		}
	}

	private static class PyFileFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			if (name.endsWith(".py")) {
				return true;
			} else {
				if (name.equals(".svn")) {
					return false;
				}
			}
			return new File(dir, name).isDirectory();
		}
	}

	private static class FileComparator implements Comparator<File> {
		public int compare(File f1, File f2) {
			return f1.getAbsolutePath().compareTo(f2.getAbsolutePath());
		}
	}

}
