// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

/**
 * A class with static fields for each of the settable options. The options from
 * registry and command line is copied into the fields here and the rest of
 * Jyhton checks these fields.
 */
public class Options {
    // Jython options. Some of these can be set from the command line
    // options, but all can be controlled through the JPython registry

    /**
     * when an exception occurs in Java code, and it is not caught, should the
     * interpreter print out the Java exception in the traceback?
     */
    public static boolean showJavaExceptions = false;

    /**
     * When true, python exception raised in overriden methods will be shown on
     * stderr. This option is remarkable usefull when python is used for
     * implementing CORBA server. Some CORBA servers will turn python exception
     * (say a NameError) into an anonymous user exception without any
     * stacktrace. Setting this option will show the stacktrace.
     */
    public static boolean showPythonProxyExceptions = false;

    /**
     * To force JIT compilation of Jython code -- should be unnecessary Setting
     * this to true will cause jdk1.2rc1 to core dump on Windows
     */
    public static boolean skipCompile = true;

    /**
     * Setting this to true will cause the console to poll standard in. This
     * might be helpful on systems without system-level threads.
     */
    public static boolean pollStandardIn = false;

    /**
     * If true, JPython respects Java the accessibility flag for fields,
     * methods, and constructors. This means you can only access public members.
     * Set this to false to access all members by toggling the accessible flag
     * on the member.
     */
    public static boolean respectJavaAccessibility = true;

    /**
     * When false the <code>site.py</code> will not be imported. This is only
     * honored by the command line main class.
     */
    public static boolean importSite = true;

    /**
     * Set verbosity to Py.ERROR, Py.WARNING, Py.MESSAGE, Py.COMMENT, or
     * Py.DEBUG for varying levels of informative messages from Jython. Normally
     * this option is set from the command line.
     */
    public static int verbose = Py.MESSAGE;

    /**
     * Setting this to true will support old 1.0 style keyword+"_" names. This
     * isn't needed any more due to improvements in the parser
     */
    public static boolean deprecatedKeywordMangling = false;

    /**
     * A directory where the dynamicly generated classes are written. Nothing is
     * ever read from here, it is only for debugging purposes.
     */
    public static String proxyDebugDirectory = null;

    /**
     * If true, Jython will use the first module found on sys.path where java
     * File.isFile() returns true. Setting this to true have no effect on
     * unix-type filesystems. On Windows/HPS+ systems setting it to true will
     * enable Jython-2.0 behaviour.
     */
    public static boolean caseok = false;

    /**
     * If true, enable truedivision for the '/' operator.
     */
    public static boolean Qnew = false;

    /**
     * Enable division warning. The value maps to the registry values of
     * <ul>
     * <li>old: 0</li>
     * <li>warn: 1</li>
     * <li>warnall: 2</li>
     * </ul>
     */
    public static int divisionWarning = 0;

    //
    // ####### END OF OPTIONS
    //

    private Options() {
        ;
    }

    private static boolean getBooleanOption(String name, boolean defaultValue) {
        String prop = PySystemState.registry.getProperty("python." + name);
        if (prop == null) {
            return defaultValue;
        }
        return prop.equalsIgnoreCase("true") || prop.equalsIgnoreCase("yes");
    }

    private static String getStringOption(String name, String defaultValue) {
        String prop = PySystemState.registry.getProperty("python." + name);
        if (prop == null) {
            return defaultValue;
        }
        return prop;
    }

    /**
     * Initialize the static fields from the registry options.
     */
    public static void setFromRegistry() {
        // Set the more unusual options
        Options.showJavaExceptions = getBooleanOption(
                "options.showJavaExceptions", Options.showJavaExceptions);

        Options.showPythonProxyExceptions = getBooleanOption(
                "options.showPythonProxyExceptions",
                Options.showPythonProxyExceptions);

        Options.skipCompile = getBooleanOption("options.skipCompile",
                Options.skipCompile);

        Options.deprecatedKeywordMangling = getBooleanOption(
                "deprecated.keywordMangling", Options.deprecatedKeywordMangling);

        Options.pollStandardIn = getBooleanOption("console.poll",
                Options.pollStandardIn);

        Options.respectJavaAccessibility = getBooleanOption(
                "security.respectJavaAccessibility",
                Options.respectJavaAccessibility);

        Options.proxyDebugDirectory = getStringOption(
                "options.proxyDebugDirectory", Options.proxyDebugDirectory);

        // verbosity is more complicated:
        String prop = PySystemState.registry.getProperty("python.verbose");
        if (prop != null) {
            if (prop.equalsIgnoreCase("error")) {
                Options.verbose = Py.ERROR;
            } else if (prop.equalsIgnoreCase("warning")) {
                Options.verbose = Py.WARNING;
            } else if (prop.equalsIgnoreCase("message")) {
                Options.verbose = Py.MESSAGE;
            } else if (prop.equalsIgnoreCase("comment")) {
                Options.verbose = Py.COMMENT;
            } else if (prop.equalsIgnoreCase("debug")) {
                Options.verbose = Py.DEBUG;
            } else {
                throw Py.ValueError("Illegal verbose option setting: '" + prop
                        + "'");
            }
        }

        Options.caseok = getBooleanOption("options.caseok", Options.caseok);

        Options.Qnew = getBooleanOption("options.Qnew", Options.Qnew);

        prop = PySystemState.registry.getProperty("python.divisionWarning");
        if (prop != null) {
            if (prop.equalsIgnoreCase("old")) {
                Options.divisionWarning = 0;
            } else if (prop.equalsIgnoreCase("warn")) {
                Options.divisionWarning = 1;
            } else if (prop.equalsIgnoreCase("warnall")) {
                Options.divisionWarning = 2;
            } else {
                throw Py.ValueError("Illegal divisionWarning option "
                        + "setting: '" + prop + "'");
            }
        }
        // additional initializations which must happen after the registry
        // is guaranteed to be initialized.
        JavaAccessibility.initialize();
    }
}
