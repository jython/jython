// Copyright (c) Corporation for National Research Initiatives

// This class implements the standard Python sys module.

package org.python.core;

import java.util.*;
import java.io.*;
import org.python.modules.Setup;

/**
 * The "sys" module.
 */

public class PySystemState extends PyObject
{
    /**
     * The current version of JPython.
     */
    public static String version = "2.1b1";

    private static int PY_MAJOR_VERSION = 2;
    private static int PY_MINOR_VERSION = 1;
    private static int PY_MICRO_VERSION = 0;
    private static int PY_RELEASE_LEVEL = 0xB;
    private static int PY_RELEASE_SERIAL = 1;

    public static int hexversion = ((PY_MAJOR_VERSION << 24) | 
                                    (PY_MINOR_VERSION << 16) |
                                    (PY_MICRO_VERSION <<  8) |
                                    (PY_RELEASE_LEVEL <<  4) |
                                    (PY_RELEASE_SERIAL << 0));

    public static PyTuple version_info;

    /**
     * The copyright notice for this release.
     */
    // TBD: should we use \u00a9 Unicode c-inside-circle?
    public static String copyright =
        "Copyright (c) 2000, Jython Developers\n" +
        "All rights reserved.\n\n" +

        "Copyright (c) 2000 BeOpen.com.\n" +
        "All Rights Reserved.\n\n"+

        "Copyright (c) 2000 The Apache Software Foundation.  All rights\n" +
        "reserved.\n\n" +

        "Copyright (c) 1995-2000 Corporation for National Research "+
        "Initiatives.\n" +
        "All Rights Reserved.\n\n" +

        "Copyright (c) 1991-1995 Stichting Mathematisch Centrum, " +
        "Amsterdam.\n" +
        "All Rights Reserved.\n\n";

    /**
     * The arguments passed to this program on the command line.
     */
    public PyList argv = new PyList();

    /**
     * Exit a Python program with the given status.
     *
     * @param status the value to exit with
     * @exception PySystemExit always throws this exception.
     * When caught at top level the program will exit.
     */
    public static void exit(PyObject status) {
        throw new PyException(Py.SystemExit, status);
    }

    /**
     * Exit a Python program with the status 0.
     */
    public static void exit() {
        exit(Py.None);
    }

    public PyObject modules; // = new PyStringMap();
    public PyList path;
    public PyObject builtins;

    public static String platform = "java";

    public PyObject ps1 = new PyString(">>> ");
    public PyObject ps2 = new PyString("... ");

    public static int maxint = Integer.MAX_VALUE;
    public static int minint = Integer.MIN_VALUE;

    public PyObject executable = Py.None;

    public static PyList warnoptions;

    private static PyJavaClass __builtin__class;

    private ClassLoader classLoader = null;
    public ClassLoader getClassLoader() {
        return classLoader;
    }
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public static PyTuple exc_info() {
        PyException exc = Py.getThreadState().exception;
        if (exc == null)
            return new PyTuple(new PyObject[] {Py.None,Py.None,Py.None});
        return new PyTuple(new PyObject[] {exc.type, exc.value,
                                           exc.traceback});
    }

    public static PyFrame _getframe() {
        return _getframe(-1);
    }

    public static PyFrame _getframe(int depth) {
        PyFrame f = Py.getFrame();

        while (depth > 0 && f != null) {
            f = f.f_back;
            --depth;
        }
        if (f == null)
             throw Py.ValueError("call stack is not deep enough");
        return f;
    }

    public PyObject stdout, stderr, stdin;
    public PyObject __stdout__, __stderr__, __stdin__;

    public PyObject last_value = Py.None;
    public PyObject last_type = Py.None;
    public PyObject last_traceback = Py.None;

    public PyObject __findattr__(String name) {
        if (name == "exc_value") {
            PyException exc = Py.getThreadState().exception;
            if (exc == null) return null;
            return exc.value;
        }
        if (name == "exc_type") {
            PyException exc = Py.getThreadState().exception;
            if (exc == null) return null;
            return exc.type;
        }
        if (name == "exc_traceback") {
            PyException exc = Py.getThreadState().exception;
            if (exc == null) return null;
            return exc.traceback;
        }
        if (name == "warnoptions") {
            if (warnoptions == null)
                warnoptions = new PyList();
            return warnoptions;
        }

        PyObject ret = super.__findattr__(name);
        if (ret != null) return ret;

        return __dict__.__finditem__(name);
    }

    public PyObject __dict__;
    public void __setattr__(String name, PyObject value) {
        if (__class__ == null)
            return;
        PyObject ret = __class__.lookup(name, false);
        if (ret != null) {
            ret._doset(this, value);
            return;
        }
        if (__dict__ == null) {
            __dict__ = new PyStringMap();
        }
        __dict__.__setitem__(name, value);
        //throw Py.AttributeError(name);
    }

    public void __delattr__(String name) {
        if (__dict__ != null) {
            __dict__.__delitem__(name);
            return;
        }
        throw Py.AttributeError("del '"+name+"'");
    }


    public String safeRepr() throws PyIgnoreMethodTag {
        return "module 'sys'";
    }

    public String toString() {
        return "sys module";
    }

    public PySystemState() {
        initialize();
        modules = new PyStringMap();

        argv = (PyList)defaultArgv.repeat(1);
        path = (PyList)defaultPath.repeat(1);

        // Set up the initial standard ins and outs
        __stdout__ = stdout = new PyFile(System.out, "<stdout>");
        __stderr__ = stderr = new PyFile(System.err, "<stderr>");
        __stdin__ = stdin = new PyFile(getSystemIn(), "<stdin>");

        // This isn't quite right...
        builtins = __builtin__class.__getattr__("__dict__");
        PyModule __builtin__ = new PyModule("__builtin__", builtins);
        modules.__setitem__("__builtin__", __builtin__);

        if (__class__ != null) {
            __dict__ = new PyStringMap();
            __dict__.invoke("update", __class__.__getattr__("__dict__"));
            __dict__.__setitem__("displayhook", 
                        new PySystemStateFunctions("displayhook", 10, 1, 1));
        }
    }

    private static PyList defaultPath;
    private static PyList defaultArgv;

    public static Properties registry; // = init_registry();
    public static String prefix;
    public static String exec_prefix="";

    private static String findRoot(Properties preProperties,
                                   Properties postProperties)
    {
        String root = null;
        try {
            if (postProperties != null)
                root = postProperties.getProperty("python.home");
            if (root == null)
                root = preProperties.getProperty("python.home");
            if (root == null)
                root = preProperties.getProperty("install.root");

            String version = preProperties.getProperty("java.version");
            if (version == null)
                version = "???";
            String lversion = version.toLowerCase();
            if (lversion.startsWith("java"))
                version = version.substring(4, version.length());

            if (lversion.startsWith("jdk") || lversion.startsWith("jre")) {
                version = version.substring(3, version.length());
            }
            if (version.equals("11"))
                version = "1.1";
            if (version.equals("12"))
                version = "1.2";
            if (version != null)
                platform = "java"+version;
        } catch (Exception exc) {
            return null;
        }
        //System.err.println("root: "+root);
        if (root != null)
            return root;

        // If install.root is undefined find jpython.jar in class.path
        String classpath = preProperties.getProperty("java.class.path");
        if (classpath == null)
            return null;

        int jpy = classpath.toLowerCase().indexOf("jython.jar");
        if (jpy == -1) {
            return null;
        }
        int start = classpath.lastIndexOf(java.io.File.pathSeparator, jpy)+1;
        return classpath.substring(start, jpy);
    }

    private static void initRegistry(Properties preProperties,
                                     Properties postProperties)
    {
        if (registry != null) {
            Py.writeError("systemState", "trying to reinitialize registry");
            return;
        }

        registry = preProperties;
        prefix = exec_prefix = findRoot(preProperties, postProperties);

        // Load the default registry
        if (prefix != null) {
            if (prefix.length() == 0) {
                prefix = exec_prefix = ".";
            }
            try {
                addRegistryFile(new File(prefix, "registry"));
                File homeFile = new File(registry.getProperty("user.home"),
                                         ".jython");
                addRegistryFile(homeFile);
            } catch (Exception exc) {
                ;
            }
        }
        if (postProperties != null) {
            for (Enumeration e=postProperties.keys(); e.hasMoreElements();)
            {
                String key = (String)e.nextElement();
                String value = (String)postProperties.get(key);
                registry.put(key, value);
            }
        }
        // Set up options from registry
        Options.setFromRegistry();
    }

    private static void addRegistryFile(File file) {
        if (file.exists()) {
            registry = new Properties(registry);
            try {
                FileInputStream fp = new FileInputStream(file);
                try {
                    registry.load(fp);
                } finally {
                    fp.close();
                }
            } catch (IOException e) {
                System.err.println("couldn't open registry file: " +
                                   file.toString());
            }
        }
    }

    private static boolean initialized = false;
    public static void initialize() {
        if (initialized)
            return;
        initialize(System.getProperties(), null, new String[] {""});
    }

    public static synchronized void initialize(Properties preProperties,
                                               Properties postProperties,
                                               String[] argv)
    {
        initialize(preProperties, postProperties, argv, null);
    }

    public static synchronized void initialize(Properties preProperties,
                                               Properties postProperties,
                                               String[] argv,
                                               ClassLoader classLoader)
    {

        //System.err.println("initializing system state");
        //Thread.currentThread().dumpStack();

        if (initialized) {
            if (postProperties != null) {
                Py.writeError("systemState",
                              "trying to reinitialize with new properties");
            }
            return;
        }
        initialized = true;

        // initialize the JPython registry
        initRegistry(preProperties, postProperties);

        // other initializations
        initBuiltins(registry);
        initStaticFields();

        // Initialize the path (and add system defaults)
        defaultPath = initPath(registry);
        defaultArgv = initArgv(argv);

        // Set up the known Java packages
        initPackages(registry);

        // Finish up standard Python initialization...
        Py.defaultSystemState = new PySystemState();
        Py.setSystemState(Py.defaultSystemState);

        if (classLoader != null)
            Py.defaultSystemState.setClassLoader(classLoader);
        Py.initClassExceptions(__builtin__class.__getattr__("__dict__"));
        // Make sure that Exception classes have been loaded
        PySyntaxError dummy = new PySyntaxError("", 1,1,"", "");
    }

    private static void initStaticFields() {
        Py.None = new PyNone();
        Py.NoKeywords = new String[0];
        Py.EmptyObjects = new PyObject[0];

        Py.EmptyTuple = new PyTuple(Py.EmptyObjects);
        Py.NoConversion = new PySingleton("Error");
        Py.Ellipsis = new PyEllipsis();

        Py.Zero = new PyInteger(0);
        Py.One = new PyInteger(1);

        Py.EmptyString = new PyString("");
        Py.Newline = new PyString("\n");
        Py.Space = new PyString(" ");
        __builtin__class = PyJavaClass.lookup(__builtin__.class);

        // Setup standard wrappers for stdout and stderr...
        Py.stderr = new StderrWrapper();
        Py.stdout = new StdoutWrapper();

        String s = null;
        if (PY_RELEASE_LEVEL == 0x0A)
            s = "alpha";
        else if (PY_RELEASE_LEVEL == 0x0B)
            s = "beta";
        else if (PY_RELEASE_LEVEL == 0x0C)
            s = "candidate";
        else if (PY_RELEASE_LEVEL == 0x0C)
            s = "final";
        version_info = new PyTuple(new PyObject[] {
                            Py.newInteger(PY_MAJOR_VERSION),
                            Py.newInteger(PY_MINOR_VERSION),
                            Py.newInteger(PY_MICRO_VERSION),
                            Py.newString(s),
                            Py.newInteger(PY_RELEASE_SERIAL) });
    }

    public static PackageManager packageManager;
    public static File cachedir;

    private static void initCacheDirectory(Properties props) {
        if (Py.frozen) {
            cachedir = null;
            return;
        }
        cachedir = new File(props.getProperty("python.cachedir", "cachedir"));
        if (!cachedir.isAbsolute()) {
            cachedir = new File(PySystemState.prefix, cachedir.getPath());
        }
    }

    private static void initPackages(Properties props) {
        initCacheDirectory(props);
        File pkgdir;
        if (cachedir != null) {
            pkgdir = new File(cachedir, "packages");
        } else {
            pkgdir = null;
        }
        packageManager = new SysPackageManager(pkgdir, props);
    }

    private static PyList initArgv(String[] args) {
        PyList argv = new PyList();
        if (args != null) {
            for (int i=0; i<args.length; i++) {
                argv.append(new PyString(args[i]));
            }
        }
        return argv;
    }

    private static Hashtable builtinNames;
    public static String[] builtin_module_names = null;

    private static void addBuiltin(String name) {
        String classname;
        String modname;

        int colon = name.indexOf(':');
        if (colon != -1) {
            // name:fqclassname
            modname = name.substring(0, colon).trim();
            classname = name.substring(colon+1, name.length()).trim();
            if (classname.equals("null"))
                // name:null, i.e. remove it
                classname = null;
        }
        else {
            modname = name.trim();
            classname = "org.python.modules." + modname;
        }
        if (classname != null)
            builtinNames.put(modname, classname);
        else
            builtinNames.remove(modname);
    }

    private static void initBuiltins(Properties props) {
        builtinNames = new Hashtable();

        // add builtins specified in the Setup.java file
        for (int i=0; i < Setup.builtinModules.length; i++)
            addBuiltin(Setup.builtinModules[i]);

        // add builtins specified in the registry file
        String builtinprop = props.getProperty("python.modules.builtin", "");
        StringTokenizer tok = new StringTokenizer(builtinprop, ",");
        while (tok.hasMoreTokens())
            addBuiltin(tok.nextToken());

        int n = builtinNames.size();
        builtin_module_names = new String[n];
        Enumeration keys = builtinNames.keys();
        for (int i=0; i<n; i++)
            builtin_module_names[i] = (String)keys.nextElement();
    }

    static String getBuiltin(String name) {
        return (String)builtinNames.get(name);
    }

    private static PyList initPath(Properties props) {
        PyList path = new PyList();
        if (!Py.frozen) {
            addPaths(path, props.getProperty("python.prepath", "."));

            if (prefix != null) {
                String libpath = new File(prefix, "Lib").toString();
                path.append(new PyString(libpath));
            }

            addPaths(path, props.getProperty("python.path", ""));
        }
        return path;
    }


    private static void addPaths(PyList path, String pypath) {
        StringTokenizer tok = new StringTokenizer(pypath,
                                                  java.io.File.pathSeparator);
        while  (tok.hasMoreTokens())
            path.append(new PyString(tok.nextToken().trim()));
    }

    public static PyJavaPackage add_package(String n) {
        return add_package(n, null);
    }

    public static PyJavaPackage add_package(String n, String contents) {
        return packageManager.makeJavaPackage(n, contents, null);
    }

    /**
     * Add a classpath directory to the list of places that are searched
     * for java packages. 
     * <p>
     * <b>Note</b>. Classes found in directory and subdirectory are not 
     * made available to jython by this call. It only make the java 
     * package found ion the directory available. This call is mostly
     * usefull if jython is embedded in an application that deals with
     * its own classloaders. A servlet container is a very good example.
     * Calling add_classdir("<context>/WEB-INF/classes") makes the java 
     * packages in WEB-INF classes available to jython import. However the
     * actual classloading is completely handled by the servlet container's
     * context classloader.
     */
    public static void add_classdir(String directoryPath) {
        packageManager.addDirectory(new File(directoryPath));
    }

    /**
     * Add a .jar & .zip directory to the list of places that are searched
     * for java .jar and .zip files. The .jar and .zip files found will be
     * cached 
     * <p>
     * <b>Note</b>. Classes in .jar and .zip files found in the directory
     * are not made available to jython by this call. See the note for
     * add_classdir(dir) for more details.
     *
     * @see #add_classdir
     */
    public static void add_extdir(String directoryPath) {
        packageManager.addJarDir(directoryPath);
    }

    public TraceFunction tracefunc = null;
    public TraceFunction profilefunc = null;

    public void settrace(PyObject tracefunc) {
        //InterpreterState interp = Py.getThreadState().interp;
        if (tracefunc == Py.None) {
            this.tracefunc = null;
        } else {
            this.tracefunc = new PythonTraceFunction(tracefunc);
        }
    }

    public void setprofile(PyObject profilefunc) {
        //InterpreterState interp = Py.getThreadState().interp;

        if (profilefunc == Py.None) {
            this.profilefunc = null;
        } else {
            this.profilefunc = new PythonTraceFunction(profilefunc);
        }
    }

    private InputStream getSystemIn() {
        if (Options.pollStandardIn) {
            return new PollingInputStream(System.in);
        } else {
            return System.in;
        }
    }

    public String getdefaultencoding() {
        return codecs.getDefaultEncoding();
    }

    public void setdefaultencoding(String encoding) {
        codecs.setDefaultEncoding(encoding);
    }


    // Not public by design. We can't rebind the displayhook if
    // a reflected function is inserted in the class dict.

    static void displayhook(PyObject o) {
        /* Print value except if None */
        /* After printing, also assign to '_' */
        /* Before, set '_' to None to avoid recursion */
        if (o == Py.None)
             return;

        PySystemState sys = Py.getThreadState().systemState;
        sys.builtins.__setitem__("_", Py.None);
        Py.stdout.println(o.__repr__());
        sys.builtins.__setitem__("_", o);
    }

    public void callExitFunc() throws PyIgnoreMethodTag {
        PyObject exitfunc = __findattr__("exitfunc");
        if (exitfunc != null) {
            try {
                exitfunc.__call__();
            } catch (PyException exc) {
                if (!Py.matchException(exc, Py.SystemExit)) {
                    Py.println(stderr, Py.newString("Error in sys.exitfunc:"));
                }
                Py.printException(exc);
            }
        }
    }
}


// This class is based on a suggestion from Yunho Jeon
class PollingInputStream extends FilterInputStream {
    public PollingInputStream(InputStream s) {
        super(s);
    }

    private void waitForBytes() throws IOException {
        try {
            while(available()==0) {
                //System.err.println("waiting...");
                Thread.currentThread().sleep(100);
            }
        } catch (InterruptedException e) {
            throw new PyException(Py.KeyboardInterrupt,
                                  "interrupt waiting on <stdin>");
        }
    }

    public int read() throws IOException {
        waitForBytes();
        return super.read();
    }

    public int read(byte b[], int off, int len) throws IOException {
        waitForBytes();
        return super.read(b, off, len);
    }
}


class PySystemStateFunctions extends PyBuiltinFunctionSet
{
    PySystemStateFunctions(String name, int index, int minargs, int maxargs) {
        super(name, index, minargs, maxargs, false, null);
    }

    public PyObject __call__(PyObject arg) {
        PySystemState sys = Py.getThreadState().systemState;
        switch (index) {
        case 10:
            sys.displayhook(arg);
            return Py.None;
        default:
            throw argCountError(1);
        }
    }
}
