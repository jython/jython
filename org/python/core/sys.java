package org.python.core;
import java.util.*;
import java.io.*;

/**
Implements the standard Python sys module.

@author Jim Hugunin - hugunin@python.org
@version 1.0, 3/24/98
@since JPython 0.0
**/

public class sys {
    /**
    The current version of JPython.
    **/
    public static String version = "1.0.2";

    /**
    The copyright notice for this release.
    **/
    public static String copyright =
        "Copyright 1997-1998 Corporation for National Research Initiatives";

    /**
    The arguments passed to this program on the command line.
    **/
	public static PyList argv = new PyList(new PyObject[0]);

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

	public static PyObject modules = new PyStringMap();
	public static PyList path;

	public static String platform = "java1.1";

	public static PyObject ps1 = new PyString(">>> ");
	public static PyObject ps2 = new PyString("... ");

	public static int maxint = Integer.MAX_VALUE;
	public static int minint = Integer.MIN_VALUE;

    private static ClassLoader classLoader = null;
    public static ClassLoader getClassLoader() {
        return classLoader;
    }
    public static void setClassLoader(ClassLoader classLoader) {
        sys.classLoader = classLoader;
    }

    public static PyTuple exc_info() {
        ThreadState ts = Py.getThreadState();
        return new PyTuple(new PyObject[] {ts.exc_type, ts.exc_value, ts.exc_traceback});
    }

	public static Properties registry; // = init_registry();

    public static String prefix;

    private static String findRoot() {
        String root;
        try {
            root = System.getProperty("install.root");
            String version = System.getProperty("java.version");
            if (version.equals("11")) version = "1.1";
            if (version.equals("12")) version = "1.2";
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

        /*int jpy = classpath.indexOf("jpython");
        int JPy = classpath.indexOf("JPython");
        int index;
        if (jpy >= 0 && (JPy == -1 || jpy < JPy)) index = jpy;
        else {
            if (JPy != -1) index = JPy;
            else return null;
        }

        int start = classpath.lastIndexOf(java.io.File.pathSeparator, index)+1;

        int end = classpath.indexOf(java.io.File.pathSeparator, index);
        int end2 = classpath.indexOf(java.io.File.separator, index);
        if ((end2 >=0 && end2 < end) || (end < 0)) end = end2;

        return classpath.substring(start, end);*/
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
				System.err.println("couldn't open registry file: "+file.toString());
			}
		}
    }


	public static Properties initRegistry() {
	    //System.out.println("basic init going on");
	    return initRegistry(System.getProperties());
	}

	public static Properties initRegistry(Properties defaults) {
	    //System.out.println("init_registry");
	    registry = defaults;
	    prefix = findRoot();

	    // Load the default registry from install.root
	    if (prefix != null) {
            try {
			    addRegistryFile(new File(prefix, "registry"));
        		File homeFile = new File(registry.getProperty("user.home"), ".jpython");
        		addRegistryFile(homeFile);
        	} catch (Exception exc) {
        	    ;
        	}
		}
		
		// Initialize the path (and add system defaults)
		path = initPath(registry);
		if (prefix != null) {
		    path.append(new PyString(new File(prefix, "Lib").toString()));
		}

	    //System.out.println("init_registry 1");

		initPackages(registry);

	    //System.out.println("init_registry 2 ");

        // Setup stdout and stderr (and later stdin)
        ThreadState ts = Py.getThreadState();
        //System.out.println("init_registry 2.5: "+ts+", "+ts.interp);
        PyObject sdict = ts.interp.sysdict;
        if (sdict == null) {
            sdict = PyJavaClass.lookup(sys.class).__dict__;
            ts.interp.sysdict = sdict;
        }
        
	    //System.out.println("init_registry 3");

        sdict.__setitem__("stdout",
                        new PyFile(System.out, "<stdout>"));
        Py.stdout = new StdoutWrapper("stdout");

        sdict.__setitem__("stderr",
                        new PyFile(System.err, "<stderr>"));
        Py.stderr = new StdoutWrapper("stderr");
        
        sdict.__setitem__("stdin",
                        new PyFile(System.in, "<stdin>"));

        /* JPython no longer overrides System.out or System.err
            This is consistent with CPython
            It also avoids problems with Linux JDK's and Netscape
        
        try {
            System.setOut(new java.io.PrintStream(Py.stdout));
            System.setErr(new java.io.PrintStream(Py.stderr));
        } catch (Exception exc1) {
            System.err.println("can't redirect stdout or stderr");
        }*/
        
        // Check for innerclasses
        String withinner = registry.getProperty("python.options.innerclasses");
        if (withinner != null && withinner.equalsIgnoreCase("true")) {
            PyJavaClass.withinner = true;
        }
        
        String compileClass = registry.getProperty("python.options.compileClass");
        if (compileClass != null && compileClass.equalsIgnoreCase("true")) {
            Py.compileClass = true;
        }        
        
        // Make sure that Exception classes have been loaded
		PySyntaxError dummy = new PySyntaxError("", 1,1,"", "");
        

		return registry;
	}

	private static PyJavaPackage addPackage(String name) {
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

	private static void initPackages(Properties props) {
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

	private static PyList initPath(Properties props) {
		PyList path = new PyList();
		String pypath = props.getProperty("python.path", "");
		StringTokenizer tok = new StringTokenizer(pypath, java.io.File.pathSeparator);
		while  (tok.hasMoreTokens())  {
			String p = tok.nextToken();
			path.append(new PyString(p.trim()));
		}
		return path;
	}

	public static PyJavaPackage add_package(String n) {
		return addPackage(n);
	}
	
	public static void settrace(PyObject tracefunc) {
	    InterpreterState interp = Py.getThreadState().interp;
	    
	    if (tracefunc == Py.None) {
	        interp.tracefunc = null;
	    } else {
	        interp.tracefunc = new PythonTraceFunction(tracefunc);
	    }
	}
	
	public static void setprofile(PyObject profilefunc) {
	    InterpreterState interp = Py.getThreadState().interp;
	    
	    if (profilefunc == Py.None) {
	        interp.profilefunc = null;
	    } else {
	        interp.profilefunc = new PythonTraceFunction(profilefunc);
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