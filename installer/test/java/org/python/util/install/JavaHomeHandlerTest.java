package org.python.util.install;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

public class JavaHomeHandlerTest extends TestCase {

    private static final String JAVA_HOME = JavaHomeHandler.JAVA_HOME;

    private static final String JAVA = "java";

    private static final String SOME_WEIRD_HOME = "some/weird/home";

    private String _originalJavaHome;

    @Override
    protected void setUp() throws Exception {
        JavaHomeHandler.reset();
        _originalJavaHome = System.getProperty(JAVA_HOME);
    }

    @Override
    protected void tearDown() throws Exception {
        System.setProperty(JAVA_HOME, _originalJavaHome);
    }

    public void testGetExecutableName() throws IOException {
        String executable = new JavaHomeHandler().getExecutableName();
        assertNotNull(executable);
        assertTrue(executable.length() > JAVA.length());
        String homePath = createTempHome().getAbsolutePath();
        executable = new JavaHomeHandler(homePath).getExecutableName();
        assertTrue(executable.length() > JAVA.length());
        assertTrue(executable.indexOf(homePath) >= 0);
        System.setProperty(JAVA_HOME, homePath);
        executable = new JavaHomeHandler().getExecutableName();
        assertTrue(executable.length() > JAVA.length());
        assertTrue(executable.indexOf(homePath) >= 0);
    }

    public void testGetExecutableName_NonExisting() {
        String executable = new JavaHomeHandler(SOME_WEIRD_HOME).getExecutableName();
        assertEquals(JAVA, executable); // fallback
        System.setProperty(JAVA_HOME, SOME_WEIRD_HOME);
        executable = new JavaHomeHandler().getExecutableName();
        assertEquals(JAVA, executable); // fallback
    }

    public void testCreateJavaHomeHandler() throws IOException {
        JavaHomeHandler handler = new JavaHomeHandler();
        assertNotNull(handler);
        System.setProperty(JAVA_HOME, SOME_WEIRD_HOME);
        handler = new JavaHomeHandler();
        assertNotNull(handler);
        System.setProperty(JAVA_HOME, createTempHome().getAbsolutePath());
        handler = new JavaHomeHandler();
        assertNotNull(handler);
    }

    public void testCreateHandler_Deviation() throws IOException {
        JavaHomeHandler handler = new JavaHomeHandler(SOME_WEIRD_HOME);
        assertNotNull(handler);
        handler = new JavaHomeHandler(createTempHome().getAbsolutePath());
        assertNotNull(handler);
    }

    public void testIsDeviation() throws IOException {
        JavaHomeHandler handler = new JavaHomeHandler(createTempHome().getAbsolutePath());
        assertTrue(handler.isDeviation());
        handler = new JavaHomeHandler();
        assertFalse(handler.isDeviation());
        handler = new JavaHomeHandler(System.getProperty(JAVA_HOME));
        assertFalse(handler.isDeviation());
    }

    public void testGetJavaHome() throws IOException {
        String tempHome = createTempHome().getAbsolutePath();
        JavaHomeHandler handler = new JavaHomeHandler(tempHome);
        String home = handler.getHome().getAbsolutePath();
        assertEquals(tempHome, home);
        try {
            handler = new JavaHomeHandler(SOME_WEIRD_HOME);
        } catch (InstallerException ie) {
            assertEquals("no valid java home", ie.getMessage());
        }
    }

    public void testIsValidJavaHome() throws IOException {
        JavaHomeHandler handler = new JavaHomeHandler(SOME_WEIRD_HOME);
        assertFalse(handler.isValidHome());
        handler = new JavaHomeHandler();
        assertTrue(handler.isValidHome());
        handler = new JavaHomeHandler(createTempHome().getAbsolutePath());
        assertTrue(handler.isValidHome());
    }

    private File createTempHome() throws IOException {
        File home = File.createTempFile("JavaHomeHandler", "Test");
        assertTrue(FileHelper.createTempDirectory(home));
        File binDir = new File(home, "bin");
        assertTrue(binDir.mkdirs());
        String executableName = JAVA;
        if (Installation.isWindows()) {
            executableName = executableName.concat(".exe");
        }
        File java = new File(binDir, executableName);
        FileHelper.write(java, "dummy");
        assertTrue(java.exists());
        assertTrue(java.isFile());
        return home;
    }
}
