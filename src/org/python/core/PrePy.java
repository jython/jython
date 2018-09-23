package org.python.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessControlException;
import java.util.Properties;

import jnr.posix.util.Platform;

/**
 * This class is part of the Jython run-time system, and contains only "pre-Python" data and methods
 * that may safely be used before the type system is ready. The Jython type system springs into
 * existence in response to a program's first use of any {@code PyObject}, for example when creating
 * the first interpreter. When preparing an application (from the command line options, say) for
 * creation of the first interpreter, it useful to defer type system creation until pre-Python
 * configuration is complete. See PEP 432 for further rationale.
 * <p>
 * Creation of the type system may happen as a side effect of referring using (almost) any object
 * from a class that statically refers to a {@code PyObject}, for example {@code Py} or
 * {@code PySystemState}. The present class is intended to hold utility methods and configuration
 * useful in the pre-Python phase.
 */
// Do not refer to any PyObject, Py export or PySystemState in this class.
public class PrePy {

    /**
     * Get the System properties if we are allowed to. Configuration values set via
     * {@code -Dprop=value} to the java command will be found here. If a security manager prevents
     * access, we will return a new (empty) object instead.
     *
     * @return {@code System} properties or a new {@code Properties} object
     */
    public static Properties getSystemProperties() {
        try {
            return System.getProperties();
        } catch (AccessControlException ace) {
            return new Properties();
        }
    }

    /**
     * Get a System property if it is defined, not null, and we are allowed to access it, otherwise
     * return the given default.
     *
     * @param key of the entry to return
     * @param defaultValue to return if null or disallowed
     * @return property value or given default
     */
    public static String getSystemProperty(String key, String defaultValue) {
        try {
            String value = System.getProperty(key, null);
            return value != null ? value : defaultValue;
        } catch (AccessControlException ace) {
            return defaultValue;
        }
    }

    /**
     * Determine whether <b>standard input</b> is an interactive stream. If the Java system property
     * {@code python.launcher.tty} is defined and equal to {@code true} or {@code false}, then that
     * provides the result. This property is normally supplied by the launcher. In the absence of
     * this certainty, we use {@link #haveConsole()}.
     *
     * @return true if (we think) standard input is an interactive stream
     */
    public static boolean isInteractive() {
        // python.launcher.tty is authoritative; see http://bugs.jython.org/issue2325
        String tty = getSystemProperty("python.launcher.tty", "");
        if (tty.equalsIgnoreCase("true")) {
            return true;
        } else if (tty.equalsIgnoreCase("false")) {
            return false;
        } else {
            // See if we have access to System.console()
            return haveConsole();
        }
    }

    /** Return {@code true} iff the console is accessible through System.console(). */
    public static boolean haveConsole() {
        try {
            return System.console() != null;
        } catch (SecurityException se) {
            return false;
        }
    }

    /**
     * Check whether an input stream is interactive. This emulates CPython
     * {@code Py_FdIsInteractive} within the constraints of pure Java. The input stream is
     * considered ``interactive'' if either
     * <ol type="a">
     * <li>it is {@code System.in} and {@link #isInteractive()} is {@code true}, or</li>
     * <li>the {@code -i} flag was given ({@link Options#interactive}={@code true}), and the
     * filename associated with it is {@code null} or {@code"<stdin>"} or {@code "???"}.</li>
     * </ol>
     *
     * @param fp stream (tested only for {@code System.in})
     * @param filename
     * @return true iff thought to be interactive
     */
    public static boolean isInteractive(InputStream fp, String filename) {
        if (fp == System.in && isInteractive()) {
            return true;
        } else if (!Options.interactive) {
            return false;
        } else {
            return filename == null || filename.equals("<stdin>") || filename.equals("???");
        }
    }

    /**
     * Infers the usual Jython executable name from the position of the jar-file returned by
     * {@link #getJarFileName()} by replacing the file name with "bin/jython". This is intended as
     * an easy fallback for cases where {@code sys.executable} is {@code None} due to direct
     * launching via the java executable.
     * <p>
     * Note that this does not necessarily return the actual executable, but instead infers the
     * place where it is usually expected to be. Use {@code sys.executable} to get the actual
     * executable (may be {@code None}.
     *
     * @return usual Jython-executable as absolute path
     */
    public static String getDefaultExecutableName() {
        return getDefaultBinDir() + File.separator
                + (Platform.IS_WINDOWS ? "jython.exe" : "jython");
    }

    /**
     * Infers the usual Jython bin-dir from the position of the jar-file returned by
     * {@link #getJarFileName()} byr replacing the file name with "bin". This is intended as an easy
     * fallback for cases where {@code sys.executable} is {@code null} due to direct launching via
     * the java executable.
     * <p>
     * Note that this does not necessarily return the actual bin-directory, but instead infers the
     * place where it is usually expected to be.
     *
     * @return usual Jython bin-dir as absolute path
     */
    public static String getDefaultBinDir() {
        String jar = _getJarFileName();
        return jar.substring(0, jar.lastIndexOf(File.separatorChar) + 1) + "bin";
    }

    /**
     * Utility-method to obtain the name (including absolute path) of the currently used
     * jython-jar-file. Usually this is jython.jar, but can also be jython-dev.jar or
     * jython-standalone.jar or something custom.
     *
     * @return the full name of the jar file containing this class, <code>null</code> if not
     *         available.
     */
    public static String getJarFileName() {
        String jar = _getJarFileName();
        return jar;
    }

    /**
     * Utility-method to obtain the name (including absolute path) of the currently used
     * jython-jar-file. Usually this is jython.jar, but can also be jython-dev.jar or
     * jython-standalone.jar or something custom.
     *
     * @return the full name of the jar file containing this class, <code>null</code> if not
     *         available.
     */
    private static String _getJarFileName() {
        Class<PrePy> thisClass = PrePy.class;
        String fullClassName = thisClass.getName();
        String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
        URL url = thisClass.getResource(className + ".class");
        return getJarFileNameFromURL(url);
    }

    /**
     * Return the path in the file system (as a string) of a JAR located by a URL. Three protocols
     * are supported, Java JAR-file protocol, and two JBoss protocols "vfs" and "vfszip".
     * <p>
     * The JAR-file protocol URL, which must be a {@code jar:file:} reference to a contained element
     * (that is, it has a "!/" part) is able to identify an actual JAR in a file system that may
     * then be opened using {@code jarFile = new JarFile(jarFileName)}. The path to the JAR is
     * returned. If the JAR is accessed by another mechanism ({@code http:} say) this will fail.
     * <p>
     * The JBoss URL must be a reference to exactly
     * {@code vfs:<JAR>/org/python/core/PySystemState.class}, or the same thing using the
     * {@code vfszip:} protocol, where &lt;JAR&gt; stands for the absolute path to the Jython JAR in
     * VFS. There is no "!/" marker: in JBoss VFS a JAR is treated just like a directory and can no
     * longer be opened as a JAR. The method essentially just swaps a VFS protocol for the Java
     * {@code file:} protocol. The path returned will be correct only if this naive swap is valid.
     *
     * @param url into the JAR
     * @return the file path or {@code null} in the event of a detectable error
     */
    public static String getJarFileNameFromURL(URL url) {
        URI fileURI = null;
        try {
            switch (url == null ? "" : url.getProtocol()) {

                case "jar":
                    // url is jar:file:/some/path/some.jar!/package/with/A.class
                    if (Platform.IS_WINDOWS) {
                        // ... or jar:file://host/some/path/some.jar!/package/with/A.class
                        // ... or jar:file:////host/some/path/some.jar!/package/with/A.class
                        url = tweakWindowsFileURL(url);
                    }
                    URLConnection c = url.openConnection();
                    fileURI = ((JarURLConnection) c).getJarFileURL().toURI();
                    break;

                case "vfs":
                case "vfszip":
                    // path is /some/path/some-jython.jar/org/python/core/PySystemState.class
                    String path = url.getPath();
                    final String target = ".jar/" + Py.class.getName().replace('.', '/');
                    int jarIndex = path.indexOf(target);
                    if (jarIndex > 0) {
                        // path contains the target class in a JAR, so make a file URL for it
                        fileURI = new URL("file:" + path.substring(0, jarIndex + 4)).toURI();
                    }
                    break;

                default:
                    // Unknown protocol or url==null: fileURI = null
                    break;
            }
        } catch (IOException | URISyntaxException | IllegalArgumentException e) {
            // Handler cannot open connection or URL is malformed some way: fileURI = null
        }

        // The JAR file is now identified in fileURI but needs decoding to a file
        return fileURI == null ? null : new File(fileURI).toString();
    }

    /**
     * If the argument is a {@code jar:file:} or {@code file:} URL, compensate for a bug in Java's
     * construction of URLs affecting {@code java.io.File} and {@code java.net.URLConnection} on
     * Windows. This is a helper for {@link #getJarFileNameFromURL(URL)}.
     * <p>
     * This bug bites when a JAR file is at a (Windows) UNC location, and a {@code jar:file:} URL is
     * derived from {@code Class.getResource()} as it is in {@link #_getJarFileName()}. When URL is
     * supplied to {@link #getJarFileNameFromURL(URL)}, the bug leads to a URI that falsely treats a
     * server as an "authority". It subsequently causes an {@code IllegalArgumentException} with the
     * message "URI has an authority component" when we try to construct a File. See
     * {@link https://bugs.java.com/view_bug.do?bug_id=6360233} ("won't fix").
     *
     * @param url Possibly malformed URL
     * @return corrected URL
     */
    private static URL tweakWindowsFileURL(URL url) throws MalformedURLException {
        String urlstr = url.toString();
        int fileIndex = urlstr.indexOf("file://"); // 7 chars
        if (fileIndex >= 0) {
            // Intended UNC path. If there is no slash following these two, insert "/" here:
            int insert = fileIndex + 7;
            if (urlstr.length() > insert && urlstr.charAt(insert) != '/') {
                url = new URL(urlstr.substring(0, insert) + "//" + urlstr.substring(insert));
            }
        }
        return url;
    }
}
