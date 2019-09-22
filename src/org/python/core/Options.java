// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import static org.python.core.RegistryKey.*;

import java.util.logging.Level;

/**
 * A class with static fields for each of the settable options. The options from registry and
 * command line is copied into the fields here and the rest of Jython checks these fields.
 */
public class Options {
    // Jython options. Some of these can be set from the command line
    // options, but all can be controlled through the Jython registry

    /**
     * when an exception occurs in Java code, and it is not caught, should the interpreter print out
     * the Java exception in the traceback?
     */
    public static boolean showJavaExceptions = false;

    /**
     * If true, exceptions raised from Python code will include a Java stack trace in addition to
     * the Python traceback. This can slow raising considerably.
     *
     * @see org.python.core.RegistryKey#PYTHON_OPTIONS_INCLUDE_JAVA_STACK_IN_EXCEPTIONS
     */
    public static boolean includeJavaStackInExceptions = true;

    /**
     * When true, python exception raised in overridden methods will be shown on stderr. This option
     * is remarkably useful when python is used for implementing CORBA server. Some CORBA servers
     * will turn python exception (say a NameError) into an anonymous user exception without any
     * stacktrace. Setting this option will show the stacktrace.
     *
     * @see org.python.core.RegistryKey#PYTHON_OPTIONS_SHOW_PYTHON_PROXY_EXCEPTIONS
     */
    public static boolean showPythonProxyExceptions = false;

    /**
     * If true, Jython respects Java the accessibility flag for fields, methods, and constructors.
     * This means you can only access public members. Set this to false to access all members by
     * toggling the accessible flag on the member.
     *
     * @see org.python.core.RegistryKey#PYTHON_SECURITY_RESPECT_JAVA_ACCESSIBILITY
     */
    public static boolean respectJavaAccessibility = true;

    /**
     * When {@code false} the <code>site.py</code> will not be imported. This may be set by the
     * command line main class ({@code -S} option) or from the registry and is checked in
     * {@link org.python.util.PythonInterpreter}.
     *
     * @see #no_site
     * @see org.python.core.RegistryKey#PYTHON_IMPORT_SITE
     */
    public static boolean importSite = true;

    /**
     * When {@code true} the {@code site.py} was not imported. This is may be set by the command
     * line main class ({@code -S} option) or from the registry. However, in Jython 2,
     * {@code no_site} is simply the opposite of {@link #importSite}, as the interpreter starts up,
     * provided for compatibility with the standard Python {@code sys.flags}. Actual control over
     * the import of the site module in Jython 2, when necessary from Java, is accomplished through
     * {@link #importSite}.
     */
    /*
     * This should be the standard Python way to control import of the site module. Unfortunately,
     * importSite is quite old and we cannot rule out use by applications. Correct in Jython 3.
     */
    public static boolean no_site = false;

    /**
     * Verbosity of informative messages from Jython, retained as a field for reasons of
     * backward-compatibility. Normally this option should be allowed to find its value indirectly
     * through {@code java.util.logging}, as adjusted by the command line {@code -v} option.
     *
     * @deprecated Use {@link Py#getLoggingLevel()},
     *             {@link Py#setLoggingLevel(java.util.logging.Level)}, or {@code java.util.logging}
     *             preferences to configure logging levels.
     */
    @Deprecated
    public static int verbose = PrePy.verbosityFromLevel(PrePy.getLoggingLevel());

    /**
     * Set by the {@code -i} option to the interpreter command, to ask for an interactive session to
     * start after the script ends. It also allows certain streams to be considered interactive when
     * {@code isatty} is not available.
     */
    public static boolean interactive = false;

    /**
     * When a script given on the command line finishes, start an interactive interpreter. It is set
     * {@code true} by the {@code -i} option on the command-line, or programmatically from the
     * script, and reset to {@code false} just before the interactive session starts. (This session
     * only actually starts if the console is interactive.)
     */
    public static boolean inspect = false;

    /**
     * A directory where the dynamically generated classes are written. Nothing is ever read from
     * here, it is only for debugging purposes.
     */
    public static String proxyDebugDirectory;

    /**
     * If true, Jython will use the first module found on sys.path where java File.isFile() returns
     * true. Setting this to true have no effect on unix-type filesystems. On Windows/HFS+ systems
     * setting it to true will enable Jython-2.0 behaviour.
     *
     * @see org.python.core.RegistryKey#PYTHON_OPTIONS_CASE_OK
     */
    public static boolean caseok = false;

    /**
     * If true, enable true division for the '/' operator.
     *
     * @see org.python.core.RegistryKey#PYTHON_OPTIONS_Q_NEW
     */
    public static boolean Qnew = false;

    /** Force stdin, stdout and stderr to be unbuffered, and opened in binary mode. */
    public static boolean unbuffered = false;

    /** Whether -3 (py3k warnings) was enabled via the command line. */
    public static boolean py3k_warning = false;

    /** Whether -B (don't write bytecode on import) was enabled via the command line. */
    public static boolean dont_write_bytecode = false;

    /** Whether -E (ignore environment) was enabled via the command line. */
    public static boolean ignore_environment = false;

    /**
     * Whether -s (don't add user site directory to {@code sys.path}) was on the command line. The
     * implementation is mostly in the {@code site} module.
     */
    public static boolean no_user_site = false;

    // XXX: place holder
    public static int bytes_warning = 0;

    /**
     * Corresponds to -O (Python bytecode optimization), -OO (remove docstrings) flags in CPython.
     * Jython processes the option and makes it visible as of 2.7, but there is no change of
     * behaviour in the current version.
     */
    public static int optimize = 0;

    /**
     * Enable division warning. The value maps to the registry values of
     * <ul>
     * <li>old: 0</li>
     * <li>warn: 1</li>
     * <li>warnall: 2</li>
     * </ul>
     */
    public static int division_warning = 0;

    /**
     * Cache spec for the SRE_STATE code point cache. The value maps to the CacheBuilderSpec string
     * and affects how the SRE_STATE cache will behave/evict cached {@code PyString -> int[]} code
     * points.
     */
    public static final String sreCacheSpecDefault =
            "weakKeys,concurrencyLevel=4,maximumWeight=2621440,expireAfterAccess=30s";
    public static String sreCacheSpec = sreCacheSpecDefault;

    //
    // ####### END OF OPTIONS
    //

    private Options() {}

    private static boolean getBooleanOption(String name, boolean defaultValue) {
        String prop = PySystemState.registry.getProperty(name);
        if (prop == null) {
            return defaultValue;
        }
        return prop.equalsIgnoreCase("true") || prop.equalsIgnoreCase("yes");
    }

    private static String getStringOption(String name, String defaultValue) {
        String prop = PySystemState.registry.getProperty(name);
        if (prop == null) {
            return defaultValue;
        }
        return prop;
    }

    /** Initialize the static fields from the registry options. */
    public static void setFromRegistry() {

        showJavaExceptions =
                getBooleanOption(PYTHON_OPTIONS_SHOW_JAVA_EXCEPTIONS, showJavaExceptions);
        includeJavaStackInExceptions = getBooleanOption(
                PYTHON_OPTIONS_INCLUDE_JAVA_STACK_IN_EXCEPTIONS, includeJavaStackInExceptions);
        showPythonProxyExceptions = getBooleanOption(PYTHON_OPTIONS_SHOW_PYTHON_PROXY_EXCEPTIONS,
                showPythonProxyExceptions);
        respectJavaAccessibility = getBooleanOption(PYTHON_SECURITY_RESPECT_JAVA_ACCESSIBILITY,
                respectJavaAccessibility);
        proxyDebugDirectory =
                getStringOption(PYTHON_OPTIONS_PROXY_DEBUG_DIRECTORY, proxyDebugDirectory);

        // Legacy python.verbose if used may countermand logging.properties::org.python.level
        setLoggingFromRegistry();

        caseok = getBooleanOption(PYTHON_OPTIONS_CASE_OK, caseok);
        Qnew = getBooleanOption(PYTHON_OPTIONS_Q_NEW, Qnew);

        setDivisionWarningFromRegistry();

        sreCacheSpec = getStringOption(PYTHON_SRE_CACHESPEC, sreCacheSpec);
        inspect |= getStringOption(PYTHON_INSPECT, "").length() > 0;
        importSite = getBooleanOption(PYTHON_IMPORT_SITE, importSite);
        no_site = !importSite;
    }

    /**
     * Set the logging level from {@link RegistryKey#PYTHON_VERBOSE}. We recognise the traditional
     * Jython names and those used by {@code java.util.logging}. If that key is present in the
     * Jython registry it will determine the level or the logger named "org.python".
     */
    private static void setLoggingFromRegistry() {
        String prop;
        switch ((prop = getStringOption(PYTHON_VERBOSE, "")).toLowerCase()) {
            case "": // Leave it as it is.
                break;
            case "off":
                PrePy.setLoggingLevel(Level.OFF);
                break;
            case "error":
            case "severe":
                PrePy.setLoggingLevel(Level.SEVERE);
                break;
            case "warning":
            case "warn":
                PrePy.setLoggingLevel(Level.WARNING);
                break;
            case "message":
            case "info":
                PrePy.setLoggingLevel(Level.INFO);
                break;
            case "comment":
            case "config":
                PrePy.setLoggingLevel(Level.CONFIG);
                break;
            case "debug":
            case "fine":
                PrePy.setLoggingLevel(Level.FINE);
                break;
            case "finer":
                PrePy.setLoggingLevel(Level.FINER);
                break;
            case "finest":
                PrePy.setLoggingLevel(Level.FINEST);
                break;
            case "all":
                PrePy.setLoggingLevel(Level.ALL);
                break;
            default:
                throw new IllegalArgumentException("Invalid verbose option: '" + prop + "'");
        }
    }

    /** Set {@link division_warning} from {@link RegistryKey#PYTHON_DIVISION_WARNING}. */
    private static void setDivisionWarningFromRegistry() {
        String prop;
        switch ((prop = getStringOption(PYTHON_DIVISION_WARNING, "")).toLowerCase()) {
            case "":
                break;
            case "old":
                division_warning = 0;
                break;
            case "warn":
                division_warning = 1;
                break;
            case "warnall":
                division_warning = 2;
                break;
            default:
                throw new IllegalArgumentException(
                        "Invalid division_warning option: '" + prop + "'");
        }
    }

}
