package org.python.util.install.driver;

import java.io.File;

import org.python.util.install.Installation;
import org.python.util.install.JarInstaller;

import junit.framework.TestCase;

public class StandaloneVerifierTest extends TestCase {

    private StandaloneVerifier _verifier;

    protected void setUp() throws Exception {
        super.setUp();
        _verifier = new StandaloneVerifier();
        File targetDir = null;
        // have to install jython first in order to activate this test
        // targetDir = new File("C:/Temp/jython.autoinstall.root_54159_dir/006
        // consoleTest_54165_dir");
        _verifier.setTargetDir(targetDir);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        if (_verifier.getTargetDir() != null) {
            File autotestFile = new File(_verifier.getTargetDir().getCanonicalPath(),
                                         StandaloneVerifier.AUTOTEST_PY);
            if (autotestFile.exists()) {
                assertTrue(autotestFile.delete());
            }
        }
    }

    public void testVerify() throws Exception {
        if (_verifier.getTargetDir() != null) {
            _verifier.verify();
        }
    }

    public void testGetSimpleCommand() throws Exception {
        File javaHome = new File(System.getProperty("java.home"));
        assertNotNull(javaHome);
        assertTrue(javaHome.exists());
        File targetDir = new File(System.getProperty(("user.dir"))); // any existing dir
        assertNotNull(targetDir);
        assertTrue(targetDir.exists());
        String prefix = targetDir.getCanonicalPath().concat(File.separator);
        String expectedCommand = javaHome.getCanonicalPath()
                .concat(File.separator)
                .concat("bin")
                .concat(File.separator)
                .concat("java");
        if (Installation.isWindows()) {
            expectedCommand = expectedCommand.concat(".exe");
        }
        String expectedArgument = prefix.concat("autotest.py");
        _verifier.setTargetDir(targetDir);
        String[] command = _verifier.getSimpleCommand();
        assertNotNull(command);
        assertEquals(4, command.length);
        assertEquals(expectedCommand, command[0]);
        assertEquals("-jar", command[1]);
        assertEquals(prefix.concat(JarInstaller.JYTHON_JAR), command[2]);
        assertEquals(expectedArgument, command[3]);
    }

    public void testDoShellScriptTests() {
        // we cannot do shell script tests in standalone mode
        assertFalse(_verifier.doShellScriptTests());
    }
}
