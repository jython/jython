// Copyright © Corporation for National Research Initiatives
package org.python.core;

public class Options
{
    // JPython options.  Some of these can be set from the command line
    // options, but all can be controlled through the JPython registry

    // when an exception occurs in Java code, and it is not caught, should
    // the interpreter print out the Java exception in the traceback?
    public static boolean showJavaExceptions = false;

    // if this is not null, it must be a string indicating the directory to 
    // save proxy adapter class files to
    public static String proxyCacheDirectory = null;

    // TBD
    public static boolean showPythonProxyExceptions = false;

    // TBD
    public static boolean skipCompile = true;

    // TBD
    public static boolean verbosePackageCache = false;

    // TBD
    public static boolean pollStandardIn = false;

    // TBD
    public static boolean classBasedExceptions = true;

    // If true, JPython respects Java the accessibility flag for fields,
    // methods, and constructors.  This means you can only access public
    // members.  Set this to false to access all members by toggling the
    // accessible flag on the member.
    public static boolean respectJavaAccessibility = true;

    // Allow JPython's classloader to find and load .class files on
    // sys.path.  This only happens for anonymous inner classes, but may
    // have unintended side-effects.  This option is temporary.
    public static boolean extendedClassLoader = true;

    // TBD
    public static boolean importSite = true;

    // TBD
    public static int verbose = Py.MESSAGE;
    
    // TBD
    public static boolean deprecatedKeywordMangling = true;
    
    // TBD
    public static boolean parserVerboseExceptions = false;

    // TBD
    public static String proxyDebugDirectory = null;

    //
    // ####### END OF OPTIONS
    //

    private static boolean getBooleanOption(String name, boolean defaultValue)
    {
        String prop = PySystemState.registry.getProperty("python."+name);
        if (prop == null)
            return defaultValue;
        return prop.equalsIgnoreCase("true") || prop.equalsIgnoreCase("yes");
    }
    
    private static String getStringOption(String name, String defaultValue) {
        String prop = PySystemState.registry.getProperty("python."+name);
        if (prop == null)
            return defaultValue;
        return prop;
    }

    public static void setFromRegistry() {
        // Set the more unusual options
        Options.showJavaExceptions =
            getBooleanOption("options.showJavaExceptions",
                             Options.showJavaExceptions);

        Options.showPythonProxyExceptions = 
            getBooleanOption("options.showPythonProxyExceptions",
                             Options.showPythonProxyExceptions);

        Options.skipCompile = 
            getBooleanOption("options.skipCompile", Options.skipCompile);

        Options.deprecatedKeywordMangling = 
            getBooleanOption("deprecated.keywordMangling",
                             Options.deprecatedKeywordMangling);

        Options.pollStandardIn =
            getBooleanOption("console.poll", Options.pollStandardIn);

        Options.classBasedExceptions =
            getBooleanOption("options.classExceptions",
                             Options.classBasedExceptions);

        Options.respectJavaAccessibility =
            getBooleanOption("security.respectJavaAccessibility",
                             Options.respectJavaAccessibility);

        Options.extendedClassLoader =
            getBooleanOption("options.extendedClassLoader",
                             Options.extendedClassLoader);

        Options.proxyDebugDirectory =
            getStringOption("options.proxyDebugDirectory",
                             Options.proxyDebugDirectory);

        // verbosity is more complicated:
        String prop = PySystemState.registry.getProperty("python.verbose");
        if (prop != null) {
            if (prop.equalsIgnoreCase("error"))
                Options.verbose = Py.ERROR;
            else if (prop.equalsIgnoreCase("warning"))
                Options.verbose = Py.WARNING;
            else if (prop.equalsIgnoreCase("message"))
                Options.verbose = Py.MESSAGE;
            else if (prop.equalsIgnoreCase("comment"))
                Options.verbose = Py.COMMENT;
            else if (prop.equalsIgnoreCase("debug"))
                Options.verbose = Py.DEBUG;
            else
                throw Py.ValueError("Illegal verbose option setting: '"+
                                    prop+"'");
        }
        // additional initializations which must happen after the registry
        // is guaranteed to be initialized.
        JavaAccessibility.initialize();
    }
}
