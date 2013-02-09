package org.python.util.install.driver;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.python.util.install.FileHelper;
import org.python.util.install.Installation;
import org.python.util.install.JavaVersionTester;

public class NormalVerifierTest extends TestCase {

    private static final String DQ = "\"";

    private NormalVerifier _verifier;

    protected void setUp() throws Exception {
        super.setUp();
        _verifier = new NormalVerifier();
        // use a directory containing spaces as target directory
        File targetDir = createTargetDirectory();
        assertTrue(targetDir.exists());
        assertTrue(targetDir.isDirectory());
        _verifier.setTargetDir(targetDir);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        if (_verifier.getTargetDir() != null) {
            File autotestFile = new File(_verifier.getTargetDir().getCanonicalPath(),
                                         NormalVerifier.AUTOTEST_PY);
            if (autotestFile.exists()) {
                assertTrue(autotestFile.delete());
            }
        }
    }

    // have to install jython first in order to activate this test
    public void testVerify() throws Exception {}

    public void testGetSimpleCommand() throws Exception {
        String prefix = _verifier.getTargetDir().getCanonicalPath().concat(File.separator);
        String expectedCommand = prefix.concat("jython");
        if (Installation.isWindows()) {
            expectedCommand = expectedCommand.concat(".bat");
        }
        String expectedArgument = prefix.concat("autotest.py");
        String[] command = _verifier.getSimpleCommand();
        assertNotNull(command);
        assertEquals(2, command.length);
        assertEquals(expectedCommand, command[0]);
        assertEquals(expectedArgument, command[1]);
    }

    public void testDoShellScriptTests() {
        assertTrue(_verifier.doShellScriptTests());
    }

    public void testGetShellScriptTestCommandDir() throws DriverException, IOException {
        String expectedDir = _verifier.getTargetDir()
                .getCanonicalPath()
                .concat(File.separator)
                .concat("bin");
        assertEquals(expectedDir, _verifier.getShellScriptTestCommandDir().getCanonicalPath());
    }

    public void testGetShellScriptTestContents() throws Exception {
        String contents = _verifier.getShellScriptTestContents();
        // common asserts
        assertNotNull(contents);
        assertFalse(contents.length() == 0);
        assertFalse(contents.indexOf("{0}") > 0);
        assertFalse(contents.indexOf("{1}") > 0);
        assertFalse(contents.indexOf("{2}") > 0);
        assertFalse(contents.indexOf("{3}") > 0);
        assertTrue(contents.indexOf("autotest.py") > 0);
        String targetDirPath = _verifier.getTargetDir().getCanonicalPath();
        String upScriptPath = _verifier.getSimpleCommand()[1];
        String javaHome = System.getProperty(JavaVersionTester.JAVA_HOME, ""); // change this ++++++
        assertTrue(javaHome.length() > 0);
        // platform specific asserts
        if (Installation.isWindows()) {
            assertTrue(contents.indexOf("set _INSTALL_DIR=") > 0);
            assertTrue(contents.indexOf("set _INSTALL_DIR=".concat(targetDirPath)) > 0);
            assertTrue(contents.indexOf("set _SCRIPT=") > 0);
            assertTrue(contents.indexOf("set _SCRIPT=".concat(upScriptPath)) > 0);
            assertTrue(contents.indexOf("set _JAVA_HOME=") > 0);
            assertTrue(contents.indexOf("set _JAVA_HOME=".concat(javaHome)) > 0);
        } else {
            System.out.println(contents);
            assertTrue(contents.indexOf("_INSTALL_DIR=") > 0);
            assertTrue(contents.indexOf("_INSTALL_DIR=".concat(quote(targetDirPath))) > 0);
            assertTrue(contents.indexOf("_SCRIPT=") > 0);
            assertTrue(contents.indexOf("_SCRIPT=".concat(quote(upScriptPath))) > 0);
            assertTrue(contents.indexOf("_JAVA_HOME=") > 0);
            assertTrue(contents.indexOf("_JAVA_HOME=".concat(quote(javaHome))) > 0);
        }
    }

    public void testGetShellScriptTestCommand() throws Exception {
        String prefix = _verifier.getShellScriptTestCommandDir()
                .getCanonicalPath()
                .concat(File.separator);
        String expectedCommand = prefix.concat("jython_test");
        if (Installation.isWindows()) {
            expectedCommand = expectedCommand.concat(".bat");
        }
        String[] command = _verifier.getShellScriptTestCommand();
        assertNotNull(command);
        assertEquals(1, command.length);
        String commandFileName = command[0];
        assertEquals(expectedCommand, commandFileName);
        File commandFile = new File(commandFileName);
        assertTrue(commandFile.exists());
        String contents = FileHelper.readAll(commandFile);
        assertNotNull(contents);
        assertFalse(contents.length() == 0);
        assertEquals(_verifier.getShellScriptTestContents(), contents);
    }

    private File createTargetDirectory() throws IOException {
        File tmpFile = File.createTempFile("NormalVerifierTest_", "with spaces");
        FileHelper.createTempDirectory(tmpFile);
        return tmpFile;
    }

    private String quote(String value) {
        return DQ.concat(value).concat(DQ);
    }
}
