package org.python.util.install;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import junit.framework.TestCase;
import junit.runner.TestSuiteLoader;

import org.python.util.install.driver.Autotest;

public class FileHelperTest extends TestCase {

    private static final String JYTHON_TEST_TEMPLATE = "jython_test.template";

    private static final String LOGO_GIF = "logo.gif";

    private static final String JYTHON_SMALL_C_PNG = "jython_small_c.png";

    public void testCreateTempDirectory_WasFile() throws IOException {
        File file = File.createTempFile("some_prefix", "");
        assertTrue(file.exists());
        assertTrue(file.isFile());
        assertTrue(FileHelper.createTempDirectory(file));
        assertTrue(file.exists());
        assertTrue(file.isDirectory());
    }

    public void testCreateTempDirectory_AlreadyPresent() throws IOException {
        File dir = new File(System.getProperty("user.dir"));
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
        assertTrue(FileHelper.createTempDirectory(dir));
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
    }

    public void testCreateTempDirectory() throws IOException {
        File dir = new File(System.getProperty("user.dir"));
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
        File tmpDir = new File(dir, "tmp");
        assertFalse(tmpDir.exists());
        try {
            assertTrue(FileHelper.createTempDirectory(tmpDir));
            assertTrue(tmpDir.exists());
            assertTrue(dir.isDirectory());
        } finally {
            if (tmpDir.exists()) {
                assertTrue(tmpDir.delete());
            }
        }
    }

    public void testCreateTempDirectory_Failure() throws Exception {
        File dir = new File(System.getProperty("user.dir"));
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
        File tmpFile = new File(dir, "tmpFailure");
        assertFalse(tmpFile.exists());
        try {
            tmpFile.createNewFile();
            assertTrue(tmpFile.exists());
            assertTrue(tmpFile.isFile());
            Lock lock = null;
            try {
                lock = new Lock(tmpFile);
                boolean created = FileHelper.createTempDirectory(tmpFile);
                if (Installation.isWindows()) {
                    // locking currently only effective on windows
                    assertFalse(created);
                } else {
                    // change if there is a locking mechanism
                    assertTrue(created);
                }
            } finally {
                if (lock != null) {
                    lock.release();
                }
            }
        } finally {
            if (tmpFile.exists()) {
                assertTrue(tmpFile.delete());
            }
        }
    }

    public void testRmdir() throws IOException {
        File dir = new File(System.getProperty("java.io.tmpdir"), "StartScriptGeneratorTest");
        if (!dir.exists()) {
            assertTrue(dir.mkdirs());
        }
        File bin = new File(dir, "bin");
        if (!bin.exists()) {
            assertTrue(bin.mkdirs());
        }
        File jython = new File(bin, "jython");
        if (!jython.exists()) {
            assertTrue(jython.createNewFile());
        }
        File jython_bat = new File(bin, "jython.bat");
        if (!jython_bat.exists()) {
            assertTrue(jython_bat.createNewFile());
        }
        assertTrue(FileHelper.rmdir(dir));
    }

    public void testRmdir_Failure() throws Exception {
        File dir = new File(System.getProperty("java.io.tmpdir"), "StartScriptGeneratorTest");
        if (!dir.exists()) {
            assertTrue(dir.mkdirs());
        }
        File bin = new File(dir, "bin");
        if (!bin.exists()) {
            assertTrue(bin.mkdirs());
        }
        File jython = new File(bin, "jython");
        if (!jython.exists()) {
            assertTrue(jython.createNewFile());
        }
        File jython_bat = new File(bin, "jython.bat");
        if (!jython_bat.exists()) {
            assertTrue(jython_bat.createNewFile());
        }
        Lock lock = null;
        try {
            lock = new Lock(jython_bat);
            boolean removed = FileHelper.rmdir(dir);
            if (Installation.isWindows()) {
                // locking currently only effective on windows
                assertFalse(removed);
            } else {
                // change if there is a locking mechanism
                assertTrue(removed);
            }
        } finally {
            if (lock != null) {
                lock.release();
            }
            assertTrue(FileHelper.rmdir(dir));
        }
    }

    public void testReadAll() throws Exception {
        File file = File.createTempFile("testReadAll", "");
        final String contents = new String("line1 \n line2 \n");
        FileHelper.write(file, contents);
        String readContents = FileHelper.readAll(file);
        assertEquals(contents, readContents);
    }

    public void testReadAll_InputStream() throws Exception {
        URL url = FileHelper.getRelativeURL(Autotest.class, JYTHON_TEST_TEMPLATE);
        assertNotNull(url);
        URI uri = new URI(url.toString());
        File file = new File(uri);
        assertNotNull(file);
        assertTrue(file.exists());
        String expectedContents = FileHelper.readAll(file);
        InputStream is = FileHelper.getRelativeURLAsStream(Autotest.class, JYTHON_TEST_TEMPLATE);
        assertNotNull(is);
        String contents = FileHelper.readAll(is);
        assertEquals(expectedContents, contents);
        // now from a .jar
        is = FileHelper.getRelativeURLAsStream(TestSuiteLoader.class, LOGO_GIF);
        assertNotNull(is);
        contents = FileHelper.readAll(is);
        assertNotNull(contents);
        assertEquals(964, contents.length());
        assertTrue(contents.startsWith("GIF89a&"));
    }

    public void testReadAll_NonExisting() {
        String readContents = null;
        try {
            readContents = FileHelper.readAll(new File("_non_existing"));
            fail("FileNotFoundException expected");
        } catch (IOException e) {
            assertNull(readContents);
        }
    }

    public void testGetRelativeURL() {
        URL url = FileHelper.getRelativeURL(Installation.class, JYTHON_SMALL_C_PNG);
        assertNotNull(url);
        assertTrue(url.getPath().endsWith("org/python/util/install/".concat(JYTHON_SMALL_C_PNG)));
        // now from a .jar
        url = FileHelper.getRelativeURL(TestSuiteLoader.class, LOGO_GIF);
        assertNotNull(url);
        assertTrue(url.getPath().endsWith("!/junit/runner/".concat(LOGO_GIF)));
    }

    public void testGetRelativeURLAsStream() throws IOException {
        InputStream is = FileHelper.getRelativeURLAsStream(Installation.class, JYTHON_SMALL_C_PNG);
        assertNotNull(is);
        try {
            assertTrue(is.read() >= 0);
        } finally {
            is.close();
        }
        // now from a .jar
        is = FileHelper.getRelativeURLAsStream(TestSuiteLoader.class, LOGO_GIF);
        assertNotNull(is);
        try {
            assertTrue(is.read() >= 0);
        } finally {
            is.close();
        }
    }

    public void testWrite() throws IOException {
        File file = new File("testWrite");
        assertFalse(file.exists());
        try {
            final String contents = new String("line1 \n line2 \n");
            FileHelper.write(file, contents);
            assertTrue(file.exists());
            String readContents = FileHelper.readAll(file);
            assertEquals(contents, readContents);
        } finally {
            if (file.exists()) {
                assertTrue(file.delete());
            }
        }
    }

    public void testWrite_Existing() throws IOException {
        File file = File.createTempFile("testWrite", "");
        assertTrue(file.exists());
        final String firstContents = "first dummy contents";
        FileHelper.write(file, firstContents);
        assertEquals(firstContents, FileHelper.readAll(file));
        final String realContents = new String("line1 \n line2 \n");
        FileHelper.write(file, realContents);
        assertEquals(realContents, FileHelper.readAll(file));
    }

    /**
     * A poor man's file lock (does work on windows only)
     */
    public static class Lock {

        private final FileOutputStream _fos;

        /**
         * acquire a file lock
         * 
         * @param file
         *            The file to be locked
         * @throws FileNotFoundException
         */
        public Lock(File file) throws FileNotFoundException {
            _fos = new FileOutputStream(file);
        }

        /**
         * release the file lock
         * 
         * @throws IOException
         */
        public void release() throws IOException {
            if (_fos != null) {
                _fos.close();
            }
        }
    }
}
