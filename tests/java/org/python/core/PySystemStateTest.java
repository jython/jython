package org.python.core;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

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

        JarExample(String urlJarPath, String urlClassPath, String unixPath) {
            this.urlJarPath = urlJarPath;
            this.urlClassPath = urlClassPath;
            if (Platform.IS_WINDOWS) {
                this.filePath = new File(unixPath).toString();
            } else {
                this.filePath = unixPath;
            }
        }
    }

    /**
     * Examples of URLs (just the path and class noise) and the reference answer. Provide the
     * reference answer like a Un*x path (forward slash).
     */
    private static JarExample[] jarExamples = { //
            // simple jar-file url
            new JarExample("/some_dir/some.jar", "a/package/with/A.class", "/some_dir/some.jar"),
            // jar-file url to decode
            new JarExample("/some%20dir/some.jar", "a/package/with/A.class", "/some dir/some.jar"),
            // In an old implementation using URLDecoder "+" needed special treatment
            new JarExample("/some+dir/some.jar", "a/package/with/A.class", "/some+dir/some.jar"),
            // Some characters should be encoded in the URL, but emerge as themselves in the path.
            new JarExample("/n%c3%a5gon/katalog/r%c3%a4tt.jar", "en/f%c3%b6rpackning/med/En.class",
                    "/någon/katalog/rätt.jar") //
    };

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
