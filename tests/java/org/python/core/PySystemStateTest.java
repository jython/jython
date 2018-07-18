package org.python.core;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.python.util.PythonInterpreter;

import jnr.posix.util.Platform;
import junit.framework.TestCase;

public class PySystemStateTest extends TestCase {

    /**
     * A class to hold examples of URLs (just the path and class noise) and the reference answer.
     * Provide the reference answer like a Un*x path (forward slash).
     */
    private static class JarExample {

        final String urlJarPath;
        final String urlClassPath;
        final String filePath;

        /** This constructor adapts unixPath to Windows when on Windows. */
        JarExample(String urlJarPath, String urlClassPath, String unixPath) {
            this(urlJarPath, urlClassPath,
                    Platform.IS_WINDOWS ? new File(unixPath).toString() : unixPath, true);
        }

        /** This constructor accepts filePath exactly as given. */
        JarExample(String urlJarPath, String urlClassPath, String filePath, boolean ignored) {
            this.urlJarPath = urlJarPath;
            this.urlClassPath = urlClassPath;
            this.filePath = filePath;
        }
    }

    /**
     * Examples of URLs (just the path and class noise) and the reference answer. Provide the
     * reference answer like a Un*x path (forward slash).
     */
    private static List<JarExample> jarExamples = Arrays.asList(//
            // simple jar-file url
            new JarExample("/some_dir/some.jar", "a/package/with/A.class", "/some_dir/some.jar"),
            // jar-file url to decode
            new JarExample("/some%20dir/some.jar", "a/package/with/A.class", "/some dir/some.jar"),
            // In an old implementation using URLDecoder "+" needed special treatment
            new JarExample("/some+dir/some.jar", "a/package/with/A.class", "/some+dir/some.jar"),
            // Some characters should be encoded in the URL, but emerge as themselves in the path.
            new JarExample("/n%c3%a5gon/katalog/r%c3%a4tt.jar", "en/f%c3%b6rpackning/med/En.class",
                    "/någon/katalog/rätt.jar") //
    );

    /* Check drive-letter  and UNC path handling if on Windows. */
    static {
        if (Platform.IS_WINDOWS) {
            // Add some examples to the list (must be made mutable for that).
            jarExamples = new ArrayList<JarExample>(jarExamples);

            // Drive-letter examples
            jarExamples.add(new JarExample("/C:/some_dir/some.jar", "a/package/with/A.class",
                    "C:\\some_dir\\some.jar", true));
            jarExamples.add(new JarExample("/E:/n%c3%a5gon/katalog/r%c3%a4tt.jar", "med/En.class",
                    "E:\\någon\\katalog\\rätt.jar", true));

            // Simple network file path (UNC path without controversial characters)
            String p = "/org/python/version.properies";
            String r = "\\\\localhost\\shared\\jython-dev.jar";
            // JAR UNC file resource URL as produced by File.getURL or getURI
            jarExamples.add(new JarExample("////localhost/shared/jython-dev.jar", p, r, true));
            // JAR UNC file resource URL as produced by URLClassLoader.getResource
            jarExamples.add(new JarExample("//localhost/shared/jython-dev.jar", p, r, true));

            // Network file path (UNC path with a controversial characters)
            r = "\\\\localhost\\shared\\jy thon%dev.jar";
            // JAR UNC file resource URL based on (deprecated) File.getURL is invalid
            // jarExamples.add(new JarExample("//localhost/shared/jy thon%dev.jar", p, r, true));
            // JAR UNC file resource URL based on File.getURI
            jarExamples.add(new JarExample("////localhost/shared/jy%20thon%25dev.jar", p, r, true));
            // JAR UNC file resource URL as produced by URLClassLoader.getResource
            jarExamples.add(new JarExample("//localhost/shared/jy%20thon%25dev.jar", p, r, true));
        }
    }

    /**
     * Test case for finding the path in the local file system of the file located by a JAR-file
     * URL. A URL is a sequence of characters (from a limited set) that encodes a sequence of octets
     * that may (if the protocol intends it) represent characters in some encoding. In the case of a
     * JAR-file URL, these octets encode the file path elements in UTF-8.
     */
    public void testGetJarFileNameFromURL() throws Exception {
        // null
        assertNull(Py.getJarFileNameFromURL(null));
        // Examples from the table
        for (JarExample ex : jarExamples) {
            // Something like jar:file:/some_dir/some.jar!/a/package/with/A.class
            URL url = new URL("jar:file:" + ex.urlJarPath + "!/" + ex.urlClassPath);
            assertEquals(ex.filePath, Py.getJarFileNameFromURL(url));
        }
    }

    /**
     * Test case for finding the path in the local file system of the file located by a JBoss vfszip
     * URL. This is problematic as an objective because a path in the VFS does not necessarily have
     * a counterpart in the local file system. However, the implementation and test are based on
     * behaviour observed when this is the case.
     */
    public void testGetJarFileNameFromURL_jboss() throws Exception {
        final String protocol = "vfszip";
        final String host = "";
        final int port = -1;
        final URLStreamHandler handler = new TestJBossURLStreamHandler();
        String file;
        URL url;
        if (Platform.IS_WINDOWS) {
            // plain jboss url
            file = "/C:/some_dir/some.jar/org/python/core/PySystemState.class";
            url = new URL(protocol, host, port, file, handler);
            // tests with jboss on windows gave URL's like this:
            assertEquals("vfszip:/C:/some_dir/some.jar/org/python/core/PySystemState.class", url.toString());
            assertEquals("C:\\some_dir\\some.jar", Py.getJarFileNameFromURL(url));
            // jboss url to decode
            file = "/C:/some%20dir/some.jar/org/python/core/PySystemState.class";
            url = new URL(protocol, host, port, file, handler);
            assertEquals("vfszip:/C:/some%20dir/some.jar/org/python/core/PySystemState.class", url.toString());
            assertEquals("C:\\some dir\\some.jar", Py.getJarFileNameFromURL(url));
            // jboss url with + to escape
            file = "/C:/some+dir/some.jar/org/python/core/PySystemState.class";
            url = new URL(protocol, host, port, file, handler);
            assertEquals("vfszip:/C:/some+dir/some.jar/org/python/core/PySystemState.class", url.toString());
            assertEquals("C:\\some+dir\\some.jar", Py.getJarFileNameFromURL(url));
        } else {
            // plain jboss url
            file = "/some_dir/some.jar/org/python/core/PySystemState.class";
            url = new URL(protocol, host, port, file, handler);
            assertEquals("vfszip:/some_dir/some.jar/org/python/core/PySystemState.class", url.toString());
            assertEquals("/some_dir/some.jar", Py.getJarFileNameFromURL(url));
            // jboss url to decode
            file = "/some%20dir/some.jar/org/python/core/PySystemState.class";
            url = new URL(protocol, host, port, file, handler);
            assertEquals("vfszip:/some%20dir/some.jar/org/python/core/PySystemState.class", url.toString());
            assertEquals("/some dir/some.jar", Py.getJarFileNameFromURL(url));
            // jboss url with + to escape
            file = "/some+dir/some.jar/org/python/core/PySystemState.class";
            url = new URL(protocol, host, port, file, handler);
            assertEquals("vfszip:/some+dir/some.jar/org/python/core/PySystemState.class", url.toString());
            assertEquals("/some+dir/some.jar", Py.getJarFileNameFromURL(url));
        }
    }

    public void testImport() throws Exception {
        Options.importSite = false;
        try {
            PySystemState pySystemState = new PySystemState();
            PySystemState.initialize();
            PythonInterpreter interpreter = new PythonInterpreter(null, pySystemState);
            interpreter.exec("import os");
            assertTrue(interpreter.getSystemState().modules.__contains__(new PyString("os")));
        } finally {
            Options.importSite = true;
        }
    }

    /**
     * A URL handler that emulates the behaviour (as far as we're concerned) of
     * {@code org.jboss.virtual.protocol.vfs.Handler}, that we can use to make URLs that behave the
     * same way as JBoss ones.
     *
     */
    protected static class TestJBossURLStreamHandler extends URLStreamHandler {

        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            throw new RuntimeException("unexpected call to openConnection " + u.toString());
        }
    }
}
