package org.python.core;
import java.util.*;
import java.io.*;

/**
Implements the standard Python sys module.

@author Jim Hugunin - hugunin@python.org
@version 1.0, 3/24/98
@since JPython 0.0
**/

public class PySystemState extends PyObject {
    /**
    The current version of JPython.
    **/
    public static String version = "1.1alpha1";

    /**
    The copyright notice for this release.
    **/
    public static String copyright =
        "Copyright 1997-1998 Corporation for National Research Initiatives";

    /**
    The arguments passed to this program on the command line.
    **/
	public PyList argv = new PyList(new PyObject[0]);

    /**
    Exit a Python program with the given status.

    @param status the value to exit with
    @exception PySystemExit always throws this exception.
                When caught at top level the program will exit.
    **/
	public static void exit(PyObject status) {
		throw new PyException(Py.SystemExit, status);
	}

    /**
    Exit a Python program with the status 0.
    **/
	public static void exit() {
		exit(Py.None);
	}

	public PyObject modules; // = new PyStringMap();
	public PyList path;
	public PyObject builtins;

	public static String platform = "java1.1";

	public PyObject ps1 = new PyString(">>> ");
	public PyObject ps2 = new PyString("... ");

	public static int maxint = Integer.MAX_VALUE;
	public static int minint = Integer.MIN_VALUE;

    private ClassLoader classLoader = null;
    public ClassLoader getClassLoader() {
        return classLoader;
    }
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public static PyTuple exc_info() {
        PyException exc = Py.getThreadState().exception;
        if (exc == null) return new PyTuple(new PyObject[] {Py.None,Py.None,Py.None});
        return new PyTuple(new PyObject[] {exc.type, exc.value, exc.traceback});
    }

	public Properties registry; // = init_registry();

    public String prefix;

    private static String findRoot() {
        String root;
        try {
            root = System.getProperty("install.root");
            String version = System.getProperty("java.version");
            if (version.equals("11")) version = "1.1";
            if (version.equals("12")) version = "1.2";
            if (version.startsWith("java")) version = version.substring(4, version.length());
            if (version.startsWith("jdk") || version.startsWith("jre")) {
                version = version.substring(3, version.length());
            }
            if (version != null) platform = "java"+version;
        } catch (Exception exc) {
            return null;
        }
        if (root != null) return root;

        // If install.root is undefined find jpython.jar in class.path
        String classpath = System.getProperty("java.class.path");
        if (classpath == null) return null;

        int jpy = classpath.toLowerCase().indexOf("jpython.jar");
        if (jpy == -1) {
            return null;
        }
        int start = classpath.lastIndexOf(java.io.File.pathSeparator, jpy)+1;
        return classpath.substring(start, jpy);
    }

    private void addRegistryFile(File file) {
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
				System.err.println("couldn't open registry file: "+file.toString());
			}
		}
    }


	/*public static Properties initRegistry() {
	    //System.out.println("basic init going on");
	    return initRegistry(System.getProperties());
	}*/

    private boolean getBooleanOption(String name, boolean defaultValue) {
        String prop = registry.getProperty("python.options."+name);
        if (prop == null) return defaultValue;
        return prop.equalsIgnoreCase("true") || prop.equalsIgnoreCase("yes");
    }
    
  
    private String getStringOption(String name, String defaultValue) {
        String prop = registry.getProperty("python.options."+name);
        if (prop == null) return defaultValue;
        return prop;
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

        return super.__findattr__(name);
    }
    
	public void __setattr__(String name, PyObject value) {
		if (__class__ == null) return;
		PyObject ret = __class__.lookup(name, false);
		if (ret != null) {
		    ret._doset(this, value);
		    return;
		}
		throw Py.AttributeError(name);
	}    

    String safeRepr() {
        return "module 'sys'";
    }

    public String toString() {
        return "sys module";
    }
    
    public PySystemState() {
        this(System.getProperties(), new String[0]);
    }

	public PySystemState(Properties defaults, String[] argv) {
        registry = defaults;
        prefix = findRoot();
        modules = new PyStringMap();

	    // Load the default registry
	    if (prefix != null) {
            try {
			    addRegistryFile(new File(prefix, "registry"));
        		File homeFile = new File(registry.getProperty("user.home"), ".jpython");
        		addRegistryFile(homeFile);
        	} catch (Exception exc) {
        	    ;
        	}
		}

        // Set up the initial standard ins and outs
        __stdout__ = stdout = new PyFile(System.out, "<stdout>");
        __stderr__ = stderr = new PyFile(System.err, "<stderr>");
        __stdin__ = stdin = new PyFile(System.in, "<stdin>");

        // Should move these to Py
        Py.stderr = new StdoutWrapper("stderr");
        Py.stdout = new StdoutWrapper("stdout");

        // Set up options from registry
        setOptionsFromRegistry();

	    // This isn't quite right...
        builtins = PyJavaClass.lookup(__builtin__.class).__dict__;
	}
	
	public void setOptionsFromRegistry() {
		// Initialize the path (and add system defaults)
		path = initPath(registry);
		if (prefix != null) {
		    path.append(new PyString(new File(prefix, "Lib").toString()));
		}

        // Set up the known Java packages
		initPackages(registry);	    
	    
	    // Set the more unusual options
	    Options.showJavaExceptions = 
	        getBooleanOption("showJavaExceptions", Options.showJavaExceptions);
	    Options.skipCompile = 
	        getBooleanOption("skipCompile", Options.skipCompile);
	    Options.proxyCacheDirectory = 
	        getStringOption("proxyCacheDirectory", Options.proxyCacheDirectory);
	    Options.showPythonProxyExceptions = 
	        getBooleanOption("showPythonProxyExceptions", Options.showPythonProxyExceptions);
	}
	    

	private PyJavaPackage addPackage(String name) {
	    //System.out.println("add package: "+name);
		int dot = name.indexOf('.');
		String first_name=name;
		String last_name=null;
		if (dot != -1) {
			first_name = name.substring(0,dot);
			last_name = name.substring(dot+1, name.length());
		}

		first_name = first_name.intern();
		PyJavaPackage p = (PyJavaPackage)modules.__finditem__(first_name);
		if (p == null) {
			p = new PyJavaPackage(first_name);
		}
		modules.__setitem__(first_name, p);
		if (last_name != null) return p.addPackage(last_name);
		else return p;
	}

	private static String default_java_packages =
		"lang,io,util";

	private void initPackages(Properties props) {
		//I wish that properties were truly a hierarchical structure, but...
		if (props.getProperty("java.packages.java") == null) {
			props.put("java.packages.java", default_java_packages);
		}
		Enumeration e = props.propertyNames();
		while (e.hasMoreElements()) {
			String s = (String)e.nextElement();
			if (s.startsWith("java.packages")) {
				String pack;
				if (s.length() == 13) pack = null;
				else pack = s.substring(14);
				//System.out.println(s+", "+pack);
				PyJavaPackage top;
				if (pack == null) top = null;
				else top = addPackage(pack);
				String jpacks = props.getProperty(s);
				StringTokenizer tok = new StringTokenizer(jpacks, " \n\r,");
				while  (tok.hasMoreTokens())  {
					if (top == null) addPackage(tok.nextToken());
					else top.addPackage(tok.nextToken());
				}

			}
		}
	}

	private PyList initPath(Properties props) {
		PyList path = new PyList();
		String pypath = props.getProperty("python.path", "");
		StringTokenizer tok = new StringTokenizer(pypath, java.io.File.pathSeparator);
		while  (tok.hasMoreTokens())  {
			String p = tok.nextToken();
			path.append(new PyString(p.trim()));
		}
		return path;
	}

	public PyJavaPackage add_package(String n) {
		return addPackage(n);
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
}

class PythonTraceFunction extends TraceFunction {
    PyObject tracefunc;

    PythonTraceFunction(PyObject tracefunc) {
        this.tracefunc = tracefunc;
    }

    private synchronized TraceFunction safeCall(PyFrame frame, String label, PyObject arg) {
        ThreadState ts = Py.getThreadState();
        if (ts.tracing) return null;

        if (tracefunc == null) return null;

        //System.err.println("trace - "+label+" - "+frame.f_code.co_name+" - "+frame.f_lineno+" - "+this);
        ts.tracing = true;
        PyObject ret = tracefunc.__call__(frame, new PyString(label), arg);
        ts.tracing = false;
        if (ret == tracefunc) return this;
        if (ret == Py.None) return null;
        //System.err.println("end trace - "+label+" - "+frame.f_code.co_name+" - "+frame.f_lineno+" - "+this+" - "+ret);//+" - "+tracefunc);
        return new PythonTraceFunction(ret);
    }

    public TraceFunction traceCall(PyFrame frame) {
        return safeCall(frame, "call", Py.None);
    }

    public TraceFunction traceReturn(PyFrame frame, PyObject ret) {
        return safeCall(frame, "return", ret);
    }

    public TraceFunction traceLine(PyFrame frame, int line) {
        return safeCall(frame, "line", Py.None);
    }

    public TraceFunction traceException(PyFrame frame, PyException exc) {
        return safeCall(frame, "exception",
                new PyTuple(new PyObject[] {exc.type, exc.value, exc.traceback}));
    }
}