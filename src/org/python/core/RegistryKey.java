package org.python.core;

/**
 * Supported registry keys and their usage.
 *
 * Boolean properties are set with the String values {@code true} or {@code yes} for true, or
 * {@code false} or {@code no} for false.
 */
public class RegistryKey {

    private RegistryKey() {}

    /**
     * {@code python.cachedir} defines the directory to use for caches (currently just package
     * information). This directory should be writable by the user. If this is an absolute path it
     * is used as given, otherwise it is interpreted relative to sys.prefix (typically the directory
     * of this file).
     */
    public static final String PYTHON_CACHEDIR = "python.cachedir";

    /**
     * Setting {@code python.cachedir.skip} to true disables the package scan for the cachedir (as
     * defined by {@code python.cachedir} or a default). Please be aware that disabling this will
     * break importing from java packages.
     */
    public static final String PYTHON_CACHEDIR_SKIP = "python.cachedir.skip";

    /**
     * {@code python.cpython2} is the name of a CPython executable, version 2.7.
     */
    public static final String PYTHON_CPYTHON = "python.cpython2";

    /**
     * {@code python.division.warning} will print deprecation warnings when doing forced floor
     * rounding with the / division operator.
     * <p>
     * "3/2" equals 1 in Python 2.x (forced floor rounding) and 1.5 in Python 3 (convert ints to
     * floats). This is equivalent to -Qwarn on the command line. See PEP 238.
     * <p>
     * Values: {@code old}, {@code warn}, {@code warnall}.
     * <p>
     * This property will be deprecated and removed in 3.x.
     */
    public static final String PYTHON_DIVISION_WARNING = "python.division.warning";

    /**
     * {@code python.console} names the class used for the Jython console. Jython ships with a JLine
     * console (http://jline.sourceforge.net/) out of the box. This is selected by default in the
     * Jython command-line application ({@link org.python.util.jython}) if you do not define
     * {@code python.console} to be another class on the command line. Alternatively, you can set
     * {@code python.console} in the registry, but be aware that this will also affect the console
     * in applications that embed a PythonInterpreter, or use Jython as a JSR-223 script engine.
     * <table>
     * <caption>Values for {@code python.console}</caption>
     * <tr>
     * <td>{@link org.python.util.JLineConsole} (default)</td>
     * </tr>
     * <tr>
     * <td>{@link org.python.core.PlainConsole} (featureless)</td>
     * </tr>
     * </table>
     * <p>
     * You may also set this to the name of a different console class in your classpath that extends
     * PlainConsole. Note that {@code org.python.util.ReadlineConsole} has been removed in 2.7.
     */
    public static final String PYTHON_CONSOLE = "python.console";

    /**
     * {@code python.console.encoding} is the encoding used reading commands from the console. Must
     * be a valid Java codec name, such as cp850.
     */
    public static final String PYTHON_CONSOLE_ENCODING = "python.console.encoding";

    /**
     * {@code python.import.site} controls whether to import {@code site.py}. Boolean.
     * <p>
     * Equivalent to -S on the command line.
     */
    public static final String PYTHON_IMPORT_SITE = "python.import.site";

    /**
     * When {@code python.inspect} is set, and a script given on the command line finishes, start an
     * interactive interpreter. Any non-empty string value will enable this behaviour.
     * <p>
     * Equivalent to the {@code -i} option on the command-line, or The session only actually starts
     * if the console is interactive.
     */
    public static final String PYTHON_INSPECT = "python.inspect";

    /**
     * {@code python.io.encoding} controls the encoding of {@code sys.stdin}, {@code sys.stdout},
     * and {@code sys.stderr}. The encoding must name a Python codec, as in {@code codecs.encode()}.
     */
    public static final String PYTHON_IO_ENCODING = "python.io.encoding";

    /**
     * {@code python.io.errors} is the unicode error handler for I/O encoding problems.
     */
    public static final String PYTHON_IO_ERRORS = "python.io.errors";

    /**
     * {@code python.locale.control} determines locale module behaviour, including enabling locale
     * module support, currently in beta.
     * <p>
     * Values:
     * <table>
     * <caption>Values for {@code python.locale.control}</caption>
     * <tr>
     * <td>{@code settable}</td>
     * <td>Python locale module is available and supports {@code setlocale()} and other standard
     * functions. This will be the default in a future Jython version.</td>
     * </tr>
     * <tr>
     * <td>{@code jython2_legacy} (default)</td>
     * <td>Mix of implicit Java locale and emulated 'C' locale behaviour, consistent with behaviour
     * in Jython 2.7.1 and earlier. Will be deprecated in a future Jython version.</td>
     * </tr>
     * </table>
     * <p>
     * More detail can be found in the documentation for the {@code locale} module and the
     * underlying platform services exposed in {@link org.python.modules._locale._locale}.
     */
    public static final String PYTHON_LOCALE_CONTROL = "python.locale.control";

    /**
     * {@code python.modules.builtin} controls the list of builtin modules; you can add, remove, or
     * override builtin modules. The value for this registry key is a comma separated list of module
     * entries, each entry of which has the following allowable forms:
     * <p>
     * <table>
     * <caption>Values for {@code python.modules.builtin}</caption>
     * <tr>
     * <td>{@code name}</td>
     * <td>The module name is {@code name} and the class name is
     * {@code org.python.modules.name}</td>
     * </tr>
     * <tr>
     * <td>{@code name:class}</td>
     * <td>The module name is {@code name} and the class name is {@code class} where class must be a
     * fully qualified Java class name</td>
     * </tr>
     * <tr>
     * <td>{@code name:null}</td>
     * <td>The module {@code name} is removed from the list of builtin modules</td>
     * </table>
     * <p>
     * A good example would be to use a jni version of os for more functionality by having an entry
     * such as {@code os:com.foo.jni.os}
     */
    public static final String PYTHON_MODULES_BUILTIN = "python.modules.builtin";

    /**
     * If {@code python.options.caseok} is true, Jython will use the first module found on
     * {@code sys.path} where java {@code File.isFile()} returns true. Setting this will have no
     * effect on unix-type filesystems. On Windows/HFS+ systems setting it to true will enable
     * Jython-2.0 behaviour.
     */
    public static final String PYTHON_OPTIONS_CASE_OK = "python.options.caseok";

    /**
     * {@code python.options.includeJavaStackInExceptions} controls whether exceptions raised from
     * Python code will include a Java stack trace in addition to the Python traceback. This can
     * slow raising considerably. Boolean, true by default.
     */
    public static final String PYTHON_OPTIONS_INCLUDE_JAVA_STACK_IN_EXCEPTIONS =
            "python.options.includeJavaStackInExceptions";

    /**
     * When an exception occurs in Java code, and it is not caught,
     * {@code python.options.showJavaExceptions} controls whether the interpreter prints out the
     * Java exception in the traceback.
     *
     * Boolean, false by default.
     */
    public static final String PYTHON_OPTIONS_SHOW_JAVA_EXCEPTIONS =
            "python.options.showJavaExceptions";

    /**
     * When {@code python.options.showPythonProxyExceptions} is true, python exceptions raised in
     * overridden methods will be shown on stderr.
     *
     */
    public static final String PYTHON_OPTIONS_SHOW_PYTHON_PROXY_EXCEPTIONS =
            "python.options.showPythonProxyExceptions";

    /**
     * {@code python.options.proxyDebugDirectory} is the directory where dynamically generated
     * classes are written. Nothing is ever read from here, it is only for debugging purposes.
     *
     */
    public static final String PYTHON_OPTIONS_PROXY_DEBUG_DIRECTORY =
            "python.options.proxyDebugDirectory";

    /**
     * {@code python.options.Qnew} controls whether true division is enabled for the / operator. See
     * PEP 238. Boolean.
     * <p>
     * Equivalent to -Qnew on the command line.
     */
    public static final String PYTHON_OPTIONS_Q_NEW = "python.options.Qnew";

    /**
     * {@code python.os} defines the string used to report the underlying operating system. Used as
     * prefix when resolving which operating system, impacting some OS-specific behaviour.
     */
    public static final String PYTHON_OS = "python.os";

    /**
     * {@code python.packages.fakepath} defines a sequence of directories and JARs that are to be
     * sources of Python packages.
     */
    public static final String PYTHON_PACKAGES_FAKEPATH = "python.packages.fakepath";

    /**
     * {@code python.packages.paths} defines a sequence of property names. Each property is a path
     * string. The default setting causes directories and JARs on the classpath and in the JRE
     * (before Java 9) to be sources of Python packages.
     */
    public static final String PYTHON_PACKAGES_PATHS = "python.packages.paths";

    /**
     * {@code python.packages.directories} defines a sequence of property names. Each property name
     * is a path string, in which the elements are directories. Each directory contains JAR/ZIP
     * files that are to be a source of Python packages. By default, these directories are those
     * where the JVM stores its optional packages as JARs (a mechanism withdrawn in Java 9).
     */
    public static final String PYTHON_PACKAGES_DIRECTORIES = "python.packages.directories";

    /**
     * {@code python.path} is the search path for Python modules, equivalent to CPython's
     * {@code PYTHONPATH} environment variable.
     */
    public static final String PYTHON_PATH = "python.path";

    /**
     * If {@code python.security.respectJavaAccessibility} is false, and you are using a Java
     * version before Java 9, then Jython can access non-public fields, methods, and constructors.
     * Normally, Jython can only provide access to public members of classes.
     * <p>
     * This may be deprecated in the future due to Java changes to accessibility from version 9 (at
     * least for Oracle JDK and OpenJDK). See documentation on the {@code --illegal-access} (new)
     * and {@code --permit-illegal-access} java command line flags for more detail.
     * <p>
     * Boolean.
     */
    public static final String PYTHON_SECURITY_RESPECT_JAVA_ACCESSIBILITY =
            "python.security.respectJavaAccessibility";

    /**
     * {@code python.sre.cachespec} is the specification for the SRE_STATE code point cache used by
     * regular expressions. The spec string is in the comma separated key=value format of
     * {@code com.google.common.cache.CacheBuilder}, within guava (which is also the source of the
     * cache implementation).
     */
    public static final String PYTHON_SRE_CACHESPEC = "python.sre.cachespec";

    /**
     * {@code python.startup} is the name of a file to be run at the start of each interactive
     * session, but not when dropping in with the -i flag in after a script has run.
     */
    public static final String PYTHON_STARTUP = "python.startup";

    /**
     * {@code python.verbose} sets the verbosity level for varying degrees of informative messages.
     * Valid values in order of increasing verbosity are {@code error}, {@code warning},
     * {@code message}, {@code comment}, {@code debug}.
     */
    public static final String PYTHON_VERBOSE = "python.verbose";

    /** {@code user.home} sets the user home directory. */
    public static final String USER_HOME = "user.home";

}
