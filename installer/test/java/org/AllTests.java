package org;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * A package recursive test suite.
 * <p>
 * All classes ending with 'Test' are added to the suite.
 * 
 * @see AllTests.TestClassFilter
 */
public class AllTests extends TestSuite {

    /**
     * @return Test suite at the directory where this class resists
     * 
     * @throws Exception
     */
    public static Test suite() throws Exception {
        Class<AllTests> suiteClass = AllTests.class;
        String testSuiteClassName = suiteClass.getName();
        File suiteFile = new File(suiteClass.getClassLoader().getResource(
                testSuiteClassName.replace('.', '/').concat(".class")).getFile());
        String basePackage = suiteClass.getPackage().getName();
        File baseDir = suiteFile.getParentFile();
        TestSuite suite = new TestSuite("Test " + basePackage + " recursive.");
        buildSuite(baseDir.getAbsolutePath().length(), basePackage, baseDir, new TestClassFilter(), suite);
        return suite;
    }

    //
    // private methods
    //

    private static void buildSuite(int prefixLength, String basePackage, File currentDir, FilenameFilter filter,
            TestSuite currentSuite) throws Exception {
        List<File> potentialDirectories = Arrays.asList(currentDir.listFiles(filter));
        if (potentialDirectories.size() == 0) {
            return;
        }
        StringBuffer currentPackageName = new StringBuffer(200);
        currentPackageName.append(basePackage);
        currentPackageName.append(currentDir.getAbsolutePath().substring(prefixLength).replace('\\', '.').replace('/',
                '.'));

        List<File> classFiles = new ArrayList<File>(potentialDirectories.size());
        Collections.sort(potentialDirectories, new FileComparator());
        Iterator<File> directoryIterator = potentialDirectories.iterator();
        while (directoryIterator.hasNext()) {
            File potentialDirectory = (File) directoryIterator.next();
            if (potentialDirectory.isDirectory()) {
                TestSuite subTestSuite = new TestSuite(potentialDirectory.getName());
                buildSuite(prefixLength, basePackage, potentialDirectory, filter, subTestSuite);
                // only if suite contains tests
                if (subTestSuite.countTestCases() > 0) {
                    currentSuite.addTest(subTestSuite);
                }
            } else {
                classFiles.add(potentialDirectory);
            }
        }
        Iterator<File> fileIterator = classFiles.iterator();
        while (fileIterator.hasNext()) {
            File file = (File) fileIterator.next();
            StringBuffer className = new StringBuffer(200);
            className.append(currentPackageName);
            className.append('.');
            String fileName = file.getName();
            className.append(fileName);
            className.setLength(className.length() - 6);
            currentSuite.addTest(new TestSuite(Class.forName(className.toString()), fileName.substring(0, fileName
                    .length() - 6)));
        }
    }

    private static class TestClassFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            if (name.endsWith("Test.class")) {
                return true;
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
