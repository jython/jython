package org.python.core;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import junit.framework.TestCase;

import jnr.posix.util.Platform;
import org.python.util.PythonInterpreter;

public class PySystemStateTest extends TestCase {

    public void testGetJarFileNameFromURL() throws Exception {
        // null
        assertNull(Py.getJarFileNameFromURL(null));
        // plain jar url
        String urlString = "jar:file:/some_dir/some.jar!/a/package/with/A.class";
        URL url = new URL(urlString);
        assertEquals("/some_dir/some.jar", Py.getJarFileNameFromURL(url));
        // jar url to decode
        urlString = "jar:file:/some%20dir/some.jar!/a/package/with/A.class";
        url = new URL(urlString);
        assertEquals("/some dir/some.jar", Py.getJarFileNameFromURL(url));
        // jar url with + signs to escape
        urlString = "jar:file:/some+dir/some.jar!/a/package/with/A.class";
        url = new URL(urlString);
        assertEquals("/some+dir/some.jar", Py.getJarFileNameFromURL(url));
    }

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
            assertEquals("C:/some_dir/some.jar", Py.getJarFileNameFromURL(url));
            // jboss url to decode
            file = "/C:/some%20dir/some.jar/org/python/core/PySystemState.class";
            url = new URL(protocol, host, port, file, handler);
            assertEquals("vfszip:/C:/some%20dir/some.jar/org/python/core/PySystemState.class", url.toString());
            assertEquals("C:/some dir/some.jar", Py.getJarFileNameFromURL(url));
            // jboss url with + to escape
            file = "/C:/some+dir/some.jar/org/python/core/PySystemState.class";
            url = new URL(protocol, host, port, file, handler);
            assertEquals("vfszip:/C:/some+dir/some.jar/org/python/core/PySystemState.class", url.toString());
            assertEquals("C:/some+dir/some.jar", Py.getJarFileNameFromURL(url));
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

    protected static class TestJBossURLStreamHandler extends URLStreamHandler {

        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            throw new RuntimeException("unexpected call to openConnection " + u.toString());
        }
    }
}
