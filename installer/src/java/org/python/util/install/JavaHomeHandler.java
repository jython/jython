package org.python.util.install;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Unified entry point for treatment of java.home
 * <p>
 * Note that the system property <code>java.home</code> is never changed
 */
public final class JavaHomeHandler {

    public static final String JAVA_HOME = "java.home";

    private static final String JAVA = "java";

    private static final String JAVA_EXE = "java.exe";

    private static final String BIN = "bin";

    private static final String DEFAULT = "_default_";

    private static final String EMPTY = "";

    /**
     * A map for java home strings and their respective executable names
     */
    private static Map<String, String> _executableNames;

    /**
     * The current java home
     */
    private String _currentJavaHome;

    /**
     * create a java home handler for the default java home
     */
    public JavaHomeHandler() {
        this(DEFAULT);
    }

    /**
     * create a java home handler for a java home deviation
     * 
     * @param currentJavaHome
     *            The deviated java home
     */
    public JavaHomeHandler(String currentJavaHome) {
        setCurrentJavaHome(currentJavaHome);
        check(getCurrentJavaHome());
    }

    /**
     * get the name of the java executable
     * 
     * @return A name of a java executable which can be passed to {@link ChildProcess}
     */
    public String getExecutableName() {
        return getExecutableName(getCurrentJavaHome());
    }

    /**
     * tell the validity of the current java home
     * <p>
     * Note: if the current java home is not valid, {@link JavaHomeHandler#getExecutableName()}
     * still returns the name of a callable java
     * 
     * @return <code>true</code> if we have a valid java home, <code>false</code> otherwise.
     */
    public boolean isValidHome() {
        return !getFallbackExecutableName().equals(getExecutableName());
    }

    /**
     * get the current java home, if it is valid
     * 
     * @return The current java home
     * @throws InstallerException
     *             if there is no valid java home
     * 
     * @see JavaHomeHandler#isValidHome()
     */
    public File getHome() throws InstallerException {
        if (!isValidHome()) {
            throw new InstallerException("no valid java home");
        } else {
            return new File(getCurrentJavaHome());
        }
    }

    /**
     * @return <code>true</code> if the current java home is a deviation, <code>false</code>
     *         otherwise
     */
    public boolean isDeviation() {
        // make sure the default java home is also known
        if (!getExecutableNames().containsKey(DEFAULT)) {
            check(DEFAULT);
        }
        return !getExecutableName(DEFAULT).equals(getExecutableName(getCurrentJavaHome()));
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(80);
        builder.append("[");
        if(!isValidHome()) {
            builder.append("in");
        }
        builder.append("valid java home: ");
        builder.append(getCurrentJavaHome());
        builder.append("; executable: ");
        builder.append(getExecutableName());
        builder.append("]");
        return builder.toString();
    }

    /**
     * reset the handler (clear all stored java homes)
     */
    static void reset() {
        getExecutableNames().clear();
    }

    private String getExecutableName(String javaHome) {
        Map<String, String> executableNames = getExecutableNames();
        if (!executableNames.containsKey(javaHome)) {
            check(javaHome);
        }
        return executableNames.get(javaHome);
    }

    private void check(String javaHome) {
        boolean valid = false;
        boolean isDefault = false;
        File javaExecutableFile = null;
        if (DEFAULT.equals(javaHome)) {
            isDefault = true;
            javaHome = System.getProperty(JAVA_HOME, EMPTY);
        }
        if (javaHome.length() > 0) {
            File javaHomeDir = new File(javaHome);
            if (javaHomeDir.exists() && javaHomeDir.isDirectory()) {
                File binDir = new File(javaHomeDir, BIN);
                if (binDir.exists() && binDir.isDirectory()) {
                    javaExecutableFile = getExecutableFile(binDir);
                    if (javaExecutableFile.exists()) {
                        valid = true;
                    }
                }
            }
        }
        if (valid) {
            addExecutable(javaHome, javaExecutableFile);
            if (isDefault) {
                addExecutable(DEFAULT, javaExecutableFile);
                if (DEFAULT.equals(getCurrentJavaHome())) {
                    // update the current home to the real one
                    setCurrentJavaHome(javaHome);
                }
            }
        } else {
            addFallbackExecutable(javaHome);
            if (isDefault) {
                addFallbackExecutable(DEFAULT);
            }
        }
    }

    private String getFallbackExecutableName() {
        return JAVA;
    }

    private void addFallbackExecutable(String javaHome) {
        getExecutableNames().put(javaHome, getFallbackExecutableName());
    }

    private void addExecutable(String javaHome, File javaExecutableFile) {
        getExecutableNames().put(javaHome, javaExecutableFile.getAbsolutePath());
    }

    private File getExecutableFile(File binDir) {
        if (Installation.isWindows()) {
            return new File(binDir, JAVA_EXE);
        } else {
            return new File(binDir, JAVA);
        }
    }

    private static Map<String, String> getExecutableNames() {
        if (_executableNames == null) {
            _executableNames = new HashMap<String, String>();
        }
        return _executableNames;
    }

    private String getCurrentJavaHome() {
        if (_currentJavaHome == null) {
            return DEFAULT;
        } else {
            return _currentJavaHome;
        }
    }

    private void setCurrentJavaHome(String currentJavaHome) {
        _currentJavaHome = currentJavaHome.trim();
    }
}
