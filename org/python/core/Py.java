package org.python.core;
import org.python.parser.SimpleNode;
import java.lang.reflect.InvocationTargetException;
import java.io.*;

public final class Py {
    static boolean frozen;
    static String frozenPackage=null;
    static boolean initialized;
	
    /* Holds the singleton None and Ellipsis objects */
    /** The singleton None Python object **/
    public static PyObject None;
	
    /** The singleton Ellipsis Python object - written as ... when indexing **/
    public static PyObject Ellipsis;

    /** A zero-length array of Strings to pass to functions that
	don't have any keyword arguments **/
    public static String[] NoKeywords;
	
    /** A zero-length array of PyObject's to pass to functions that
	expect zero-arguments **/
    public static PyObject[] EmptyObjects;
	
    /** A tuple with zero elements **/
    public static PyTuple EmptyTuple;
	
    /** The Python integer 0 - also used as false **/
    public static PyInteger Zero;
	
    /** The Python integer 1 - also used as true **/
    public static PyInteger One;

    /** A zero-length Python string **/
    public static PyString EmptyString;
    
    /** A Python string containing '\n' **/
    public static PyString Newline;
	
    /** A Python string containing ' ' **/
    public static PyString Space;
	
    /** A unique object to indicate no conversion is possible
	in __tojava__ methods **/
    public static Object NoConversion;	
	
    public static PyObject OSError;
    public static PyObject NotImplementedError;
    public static PyObject EnvironmentError;
	
	
    /* The standard Python exceptions */
    public static PyObject OverflowError;
    public static PyException OverflowError(String message) {
	return new PyException(Py.OverflowError, message);
    }

    public static PyObject RuntimeError;
    public static PyException RuntimeError(String message) {
	return new PyException(Py.RuntimeError, message);
    }

    public static PyObject KeyboardInterrupt;
    /*public static PyException KeyboardInterrupt(String message) {
      return new PyException(Py.KeyboardInterrupt, message);
      }*/

    public static PyObject FloatingPointError;
    public static PyException FloatingPointError(String message) {
	return new PyException(Py.FloatingPointError, message);
    }

    public static PyObject SyntaxError;
    public static PyException SyntaxError(String message) {
	return new PyException(Py.SyntaxError, message);
    }

    public static PyObject AttributeError;
    public static PyException AttributeError(String message) {
	return new PyException(Py.AttributeError, message);
    }

    public static PyObject IOError;
    public static PyException IOError(java.io.IOException ioe) {
	//System.err.println("ioe: "+ioe);
	//ioe.printStackTrace();
	String message = ioe.getMessage();
	if (ioe instanceof java.io.FileNotFoundException) {
	    message = "File not found - "+message;
	}
	return new PyException(Py.IOError, message);
    }
    public static PyException IOError(String message) {
	//System.err.println("sioe: "+message);
	    
	return new PyException(Py.IOError, message);
    }

    public static PyObject KeyError;
    public static PyException KeyError(String message) {
	return new PyException(Py.KeyError, message);
    }

    public static PyObject AssertionError;
    public static PyException AssertionError(String message) {
	return new PyException(Py.AssertionError, message);
    }

    public static PyObject TypeError;
    public static PyException TypeError(String message) {
	return new PyException(Py.TypeError, message);
    }

    public static PyObject SystemError;
    public static PyException SystemError(String message) {
	return new PyException(Py.SystemError, message);
    }

    public static PyObject IndexError;
    public static PyException IndexError(String message) {
	return new PyException(Py.IndexError, message);
    }

    public static PyObject ZeroDivisionError;
    public static PyException ZeroDivisionError(String message) {
	return new PyException(Py.ZeroDivisionError, message);
    }

    public static PyObject NameError;
    public static PyException NameError(String message) {
	return new PyException(Py.NameError, message);
    }

    public static PyObject SystemExit;
    /*public static PyException SystemExit(String message) {
      return new PyException(Py.SystemExit, message);
      }*/
    static void maybeSystemExit(PyException exc) {
	if (Py.matchException(exc, Py.SystemExit)) {
	    PyObject value = exc.value;
	    //System.err.println("exiting: "+value.getClass().getName());
	    if (value instanceof PyInstance) {
		PyObject tmp = value.__findattr__("code");
		if (tmp != null)
		    value = tmp;
	    }
	    if (value instanceof PyInteger) {
		System.exit(((PyInteger)value).getValue());
	    } else {
		if (value != Py.None) {
		    try { 
			Py.println(value);
			System.exit(1);
		    }
		    catch (Throwable t0) { }
		}
		System.exit(0);
	    }
	}
    }
	

    public static PyObject ImportError;
    public static PyException ImportError(String message) {
	return new PyException(Py.ImportError, message);
    }

    public static PyObject ValueError;
    public static PyException ValueError(String message) {
	return new PyException(Py.ValueError, message);
    }

    public static PyObject EOFError;
    public static PyException EOFError(String message) {
	return new PyException(Py.EOFError, message);
    }

    public static PyObject MemoryError;
    public static void MemoryError(OutOfMemoryError t) {
	if (Options.showJavaExceptions) {
	    t.printStackTrace();
	}
	System.err.println("Out of Memory");
	System.err.println(
	    "You might want to try the -mx flag to increase heap size");
	System.exit(-1);
    }
	
    public static PyException MemoryError(String message) {
	return new PyException(Py.MemoryError, message);
    }

    public static PyObject ArithmeticError;
    public static PyObject LookupError;
    public static PyObject StandardError;
    public static PyObject Exception;

    public static PyObject JavaError;
    public static PyException JavaError(Throwable t) {
        //System.err.println("t: "+t);
        if (t instanceof PyException) {
            return (PyException)t;
	}
	else if (t instanceof InvocationTargetException) {
	    return JavaError(
		((InvocationTargetException)t).getTargetException());
	}
	/* Remove this automatic coercion, people want to see the real
	 * exceptions!
	     else if (t instanceof java.io.IOException) {
	     return IOError((java.io.IOException)t);
	     } */
	else if (t instanceof OutOfMemoryError) {
	    MemoryError((OutOfMemoryError)t);
	    return null;
	} else {
	    PyObject exc = Py.java2py(t);
	    return new PyException(exc.__class__, exc);
	}
    }

    // Don't allow any constructors.  Class only provides static methods.
    private Py() { ; } 

    /** @deprecated **/
    //public static InterpreterState interp;

    /**
       Convert a given <code>PyObject</code> to an instance of a Java class.
       Identical to <code>o.__tojava__(c)</code> except that it will
       raise a <code>TypeError</code> if the conversion fails.
    
       @param o the <code>PyObject</code> to convert.
       @param c the class to convert it to.
    **/
    public static Object tojava(PyObject o, Class c) {
	Object obj = o.__tojava__(c);
	if (obj == Py.NoConversion) {
	    throw Py.TypeError("can't convert "+o.__repr__()+" to "+
			       c.getName());
	}
	return obj;
    }

    /** @deprecated **/
    public static Object tojava(PyObject o, String s) {
	try {
	    return tojava(o, Class.forName(s));
	} catch (ClassNotFoundException exc) {
	    throw Py.TypeError("can't convert to: "+s);
	}
    }
	
    /* Helper functions for PyProxy's */
	
    /** @deprecated **/
    public static PyObject jfindattr(PyProxy proxy, String name) {
        PyInstance o = proxy._getPyInstance();
        if (o == null)
	    return null;
        PyObject ret = o.__jfindattr__(name);
        if (ret == null)
	    return null;
        
        // Set the current system state to match proxy -- usually this is a waste of time :-(
        Py.setSystemState(proxy._getPySystemState());
        return ret;
    }
    /** @deprecated **/
    public static PyObject jgetattr(PyProxy proxy, String name) {
        PyInstance o = proxy._getPyInstance();
        PyObject ret = null;
        if (o != null) {
            ret = o.__jfindattr__(name);
        }
        if (ret == null)
	    throw Py.AttributeError("abstract method \""+name+
				    "\" not implemented");
        // Set the current system state to match proxy -- usually this is a
        // waste of time :-(
        Py.setSystemState(proxy._getPySystemState());
        return ret;
    }

    /* Convenience methods to create new constants without using "new" */
    private static PyInteger[] integerCache = null;
    
    public static final PyInteger newInteger(int i) {
        if (integerCache == null) {
            integerCache = new PyInteger[1000];
            for(int j=-100; j<900; j++) {
                integerCache[j+100] = new PyInteger(j);
            }
        }
        if (i>=-100 && i < 900) {
            return integerCache[i+100];
        } else {
            return new PyInteger(i);
        }
    }
    // Very bad behavior...
    public static PyInteger newInteger(long i) {
        return new PyInteger((int)i);
    }
    
    public static PyLong newLong(String s) {
        return new PyLong(s);
    }
    
    public static PyComplex newImaginary(double v) {
        return new PyComplex(0, v);
    }
    
    public static PyFloat newFloat(float v) {
        return new PyFloat((double)v);
    }    
    
    public static PyFloat newFloat(double v) {
        return new PyFloat(v);
    }
    
    public static PyString newString(char c) {
        return makeCharacter(new Character(c));
    }    
    
    public static PyString newString(String s) {
        return new PyString(s);
    }
    
    public static PyInteger newBoolean(boolean t) {
        return t ? Py.One : Py.Zero;
    }
    
    public static PyCode newCode(int argcount, String varnames[],
				 String filename, String name,
				 boolean args, boolean keywords,
				 PyFunctionTable funcs, int func_id)
    {
	return new PyTableCode(argcount, varnames,
			       filename, name, 0, args, keywords, funcs,
			       func_id);
    }

    public static PyCode newCode(int argcount, String varnames[],
				 String filename, String name,
				 int firstlineno,
				 boolean args, boolean keywords,
				 PyFunctionTable funcs, int func_id)
    {
	return new PyTableCode(argcount, varnames,
			       filename, name, firstlineno, args, keywords,
			       funcs, func_id);
    }

    public static void setBuiltinExceptions() {
        PyObject dict = PyJavaClass.lookup(
	    org.python.core.__builtin__.class).__dict__;
        dict.__setitem__("Exception", Py.Exception);
        dict.__setitem__("TypeError", Py.TypeError);
        dict.__setitem__("LookupError", Py.LookupError);
        dict.__setitem__("IOError", Py.IOError);
        dict.__setitem__("ArithmeticError", Py.ArithmeticError);
        dict.__setitem__("NotImplementedError", Py.NotImplementedError);
        dict.__setitem__("OSError", Py.OSError);
        dict.__setitem__("SystemError", Py.SystemError);
        dict.__setitem__("RuntimeError", Py.RuntimeError);
        dict.__setitem__("AssertionError", Py.AssertionError);
        dict.__setitem__("FloatingPointError", Py.FloatingPointError);
        dict.__setitem__("ValueError", Py.ValueError);
        dict.__setitem__("NameError", Py.NameError);
        dict.__setitem__("EOFError", Py.EOFError);
        dict.__setitem__("KeyError", Py.KeyError);
        dict.__setitem__("MemoryError", Py.MemoryError);
        dict.__setitem__("SystemExit", Py.SystemExit);
        dict.__setitem__("KeyboardInterrupt", Py.KeyboardInterrupt);
        dict.__setitem__("OverflowError", Py.OverflowError);
        dict.__setitem__("ZeroDivisionError", Py.ZeroDivisionError);
        dict.__setitem__("StandardError", Py.StandardError);
        dict.__setitem__("IndexError", Py.IndexError);
        dict.__setitem__("ImportError", Py.ImportError);
        dict.__setitem__("EnvironmentError", Py.EnvironmentError);
        dict.__setitem__("AttributeError", Py.AttributeError);
        dict.__setitem__("SyntaxError", Py.SyntaxError);
    }


    static void initStringExceptions() {
        TypeError = new PyString("TypeError");
        IOError = new PyString("IOError");
        NotImplementedError = new PyString("NotImplementedError");
        OSError = new PyString("OSError");
        SystemError = new PyString("SystemError");
        AssertionError = new PyString("AssertionError");
        FloatingPointError = new PyString("FloatingPointError");
        ValueError = new PyString("ValueError");
        NameError = new PyString("NameError");
        EOFError = new PyString("EOFError");
        KeyError = new PyString("KeyError");
        MemoryError = new PyString("MemoryError");
        SystemExit = new PyString("SystemExit");
        KeyboardInterrupt = new PyString("KeyboardInterrupt");
        OverflowError = new PyString("OverflowError");
        ZeroDivisionError = new PyString("ZeroDivisionError");
        IndexError = new PyString("IndexError");
        ImportError = new PyString("ImportError");
        AttributeError = new PyString("AttributeError");
        SyntaxError = new PyString("SyntaxError");

        LookupError = new PyTuple(new PyObject[]
            {Py.IndexError, Py.KeyError});

        ArithmeticError = new PyTuple(new PyObject[]
            {Py.ZeroDivisionError, Py.OverflowError, Py.FloatingPointError});

        RuntimeError = new PyTuple(new PyObject[]
            {Py.NotImplementedError});

        EnvironmentError = new PyTuple(new PyObject[]
            {Py.OSError, Py.IOError});

        StandardError = new PyTuple(new PyObject[]
            {Py.ValueError, Py.TypeError, Py.NameError,
	     Py.AssertionError, Py.LookupError, Py.SyntaxError,
	     Py.SystemError, Py.KeyboardInterrupt, Py.AttributeError,
	     Py.MemoryError, Py.EnvironmentError, Py.RuntimeError,
	     Py.ImportError, Py.ArithmeticError, Py.EOFError
	    });
        
        Exception = new PyTuple(new PyObject[]
            {Py.StandardError, Py.SystemExit});

        JavaError = new PyString("JavaError");
        setBuiltinExceptions();
    }

    static void initClassExceptions() {
        PyObject exceptions = imp.load("exceptions");
        PyObject tmp;
        
        tmp = exceptions.__findattr__("Exception");
        if (tmp != null) Exception = tmp;
        tmp = exceptions.__findattr__("TypeError");
        if (tmp != null) TypeError = tmp;
        tmp = exceptions.__findattr__("LookupError");
        if (tmp != null) LookupError = tmp;
        tmp = exceptions.__findattr__("IOError");
        if (tmp != null) IOError = tmp;
        tmp = exceptions.__findattr__("ArithmeticError");
        if (tmp != null) ArithmeticError = tmp;
        tmp = exceptions.__findattr__("NotImplementedError");
        if (tmp != null) NotImplementedError = tmp;
        tmp = exceptions.__findattr__("OSError");
        if (tmp != null) OSError = tmp;
        tmp = exceptions.__findattr__("SystemError");
        if (tmp != null) SystemError = tmp;
        tmp = exceptions.__findattr__("RuntimeError");
        if (tmp != null) RuntimeError = tmp;
        tmp = exceptions.__findattr__("AssertionError");
        if (tmp != null) AssertionError = tmp;
        tmp = exceptions.__findattr__("FloatingPointError");
        if (tmp != null) FloatingPointError = tmp;
        tmp = exceptions.__findattr__("ValueError");
        if (tmp != null) ValueError = tmp;
        tmp = exceptions.__findattr__("NameError");
        if (tmp != null) NameError = tmp;
        tmp = exceptions.__findattr__("EOFError");
        if (tmp != null) EOFError = tmp;
        tmp = exceptions.__findattr__("KeyError");
        if (tmp != null) KeyError = tmp;
        tmp = exceptions.__findattr__("MemoryError");
        if (tmp != null) MemoryError = tmp;
        tmp = exceptions.__findattr__("SystemExit");
        if (tmp != null) SystemExit = tmp;
        tmp = exceptions.__findattr__("KeyboardInterrupt");
        if (tmp != null) KeyboardInterrupt = tmp;
        tmp = exceptions.__findattr__("OverflowError");
        if (tmp != null) OverflowError = tmp;
        tmp = exceptions.__findattr__("ZeroDivisionError");
        if (tmp != null) ZeroDivisionError = tmp;
        tmp = exceptions.__findattr__("StandardError");
        if (tmp != null) StandardError = tmp;
        tmp = exceptions.__findattr__("IndexError");
        if (tmp != null) IndexError = tmp;
        tmp = exceptions.__findattr__("ImportError");
        if (tmp != null) ImportError = tmp;
        tmp = exceptions.__findattr__("EnvironmentError");
        if (tmp != null) EnvironmentError = tmp;
        tmp = exceptions.__findattr__("AttributeError");
        if (tmp != null) AttributeError = tmp;
        tmp = exceptions.__findattr__("SyntaxError");
        if (tmp != null) SyntaxError = tmp;        
        setBuiltinExceptions();
    }

    public static PySystemState defaultSystemState;
    // This is a hack to get initializations to work in proper order
    public static synchronized boolean initPython() {
	PySystemState.initialize();
	return true;
    }

    public static Class findClass(String name) {
        try {
            ClassLoader classLoader = Py.getSystemState().getClassLoader();
            if (classLoader == null)
                return Class.forName(name);
            else
                return classLoader.loadClass(name);
        } catch (ClassNotFoundException exc) {
            //exc.printStackTrace();
            return null;
        } catch (IllegalArgumentException exc1) {
            //exc1.printStackTrace();
            return null;
        }
    }

    private static void setArgv(String arg0, String[] args) {
	PyObject argv[] = new PyObject[args.length+1];
	argv[0] = new PyString(arg0);
	for(int i=1; i<argv.length; i++)
	    argv[i] = new PyString(args[i-1]);
	Py.getSystemState().argv = new PyList(argv);
    }

    private static void initProperties(String[] args, String[] packages,
				       String[] props, 
				       String[] specs, String frozenPackage)
    {
	if (frozenPackage != null) {
	    Py.frozen = true;
	    if (frozenPackage.length() > 0)
		Py.frozenPackage = frozenPackage;
	}
	    
	java.util.Properties sprops;
        try {
            sprops = new java.util.Properties(System.getProperties());
        } catch (Throwable t) {
            sprops = new java.util.Properties();
        }

        if (props != null) {
            for(int i=0; i<props.length; i+=2) {
                sprops.put(props[i], props[i+1]);
            }
        }
        //System.err.println("sprops: "+sprops);
        
        if (args == null)
	    args = new String[0];
        PySystemState.initialize(sprops, null, args);
        
        if (packages != null) {
            for(int i=0; i<packages.length; i+=2) {
                PySystemState.add_package(packages[i], packages[i+1]);
            }
        }
        
        if (specs != null) {
            if (specialClasses == null) {
                specialClasses = new java.util.Hashtable();
            }
            for(int i=0; i<specs.length; i+=2) {
                specialClasses.put(specs[i], Py.findClass(specs[i+1]));
            }
        }
    }

    private static java.util.Hashtable specialClasses = null;

    public static void initProxy(PyProxy proxy, String module, String pyclass,
				 Object[] args, String[] packages,
				 String[] props, boolean frozen)
    {
	initProxy(proxy, module, pyclass, args, packages, props, null, null);
    }
									    
    public static void initProxy(PyProxy proxy, String module, String pyclass,
				 Object[] args, String[] packages,
				 String[] props,
				 String[] specs, String frozenPackage)
    {
	//System.out.println("initProxy");
	//frozen = false;		
	initProperties(null, packages, props, specs, frozenPackage);
		
		
	ThreadState ts = getThreadState();
	if (ts.getInitializingProxy() != null) {
	    proxy._setPyInstance(ts.getInitializingProxy());
	    proxy._setPySystemState(ts.systemState);
	    return;
	}
		
        ClassLoader classLoader = proxy.getClass().getClassLoader();
        if (classLoader != null) {
            Py.getSystemState().setClassLoader(classLoader);
        }

        //System.out.println("path: "+sys.path.__str__());

	PyObject mod = imp.importName(module.intern(), false);
	PyClass pyc = (PyClass)mod.__getattr__(pyclass.intern());

	PyInstance instance = new PyInstance(pyc);
	instance.javaProxy = proxy;
	proxy._setPyInstance((PyInstance)instance);

	PyObject[] pargs; 
        if (args == null || args.length == 0) {
            pargs = Py.EmptyObjects;
        } else {
            pargs = new PyObject[args.length];
	    for(int i=0; i<args.length; i++)
		pargs[i] = Py.java2py(args[i]);
	}
	instance.__init__(pargs, Py.NoKeywords);
    }

    public static void initRunnable(String module, PyObject dict) {
        Class mainClass=null;
        try {
            mainClass = Class.forName(module);
        } catch (ClassNotFoundException exc) {
            System.err.println("Error running main.  Can't find: "+module);
            System.exit(-1);
        }
        PyCode code=null;
        try {
            code = ((PyRunnable)mainClass.newInstance()).getMain();
        } catch (Throwable t) {
            System.err.println("Invalid class (runnable): "+module+"$py");
            System.exit(-1);
        }
        Py.runCode(code, dict, dict);
    }

    public static void runMain(String module, String[] args, String[] packages,
			       String[] props, String[] specs,
			       String frozenPackage)
    {
	//System.err.println("main: "+module);
        initProperties(args, packages, props, specs, frozenPackage);
        
        Class mainClass=null;
        try {
            mainClass = Class.forName(module);
        } catch (ClassNotFoundException exc) {
            System.err.println("Error running main.  Can't find: "+module);
            System.exit(-1);
        }

        ClassLoader classLoader = mainClass.getClassLoader();
        if (classLoader != null) {
            Py.getSystemState().setClassLoader(classLoader);
        }

	try {
            PyCode code=null;
            try {
                code = ((PyRunnable)mainClass.newInstance()).getMain();
            } catch (Throwable t) {
                System.err.println("Invalid class: "+module+"$py");
                System.exit(-1);
            }
            PyObject mod = imp.createFromCode("__main__", code);
	} catch (PyException e) {
	    Py.printException(e);
	    System.exit(-1);
	}
    }

    private static String getStackTrace(Throwable javaError) {
	ByteArrayOutputStream buf = new ByteArrayOutputStream();
	javaError.printStackTrace(new PrintStream(buf));

	String str = buf.toString();
	int index = -1;
	if (index == -1)
	    index = str.indexOf(
		"at org.python.core.PyReflectedConstructor.call");
	if (index == -1)
	    index = str.indexOf("at org.python.core.PyReflectedMethod.call");
	if (index == -1)
	    index = str.indexOf(
		"at org/python/core/PyReflectedConstructor.call");
	if (index == -1)
	    index = str.indexOf("at org/python/core/PyReflectedMethod.call");

	if (index != -1)
	    index = str.lastIndexOf("\n", index);

        int index0 = str.indexOf("\n");

	if (index >= index0)
	    str = str.substring(index0+1,index+1);

	return str;
    }

    /* Display a PyException and stack trace */
    public static void printException(Throwable t) {
	printException(t, null, null);
    }

    public static void printException(Throwable t, PyFrame f) {
	printException(t, f, null);
    }

    public static synchronized void printException(Throwable t, PyFrame f,
						   PyObject file)
    {
	//System.err.println("printingException: "+t+", "+file);
	StdoutWrapper stderr = Py.stderr;
	    
	if (file != null) {
	    stderr = new FixedFileWrapper(file);
	}
	    
	if (Options.showJavaExceptions) {
	    stderr.println("Java Traceback:");
	    java.io.CharArrayWriter buf = new java.io.CharArrayWriter();
	    if (t instanceof PyException) {
		((PyException)t).super__printStackTrace(
		    new java.io.PrintWriter(buf));
	    } else {
		t.printStackTrace(new java.io.PrintWriter(buf));
	    }
	    stderr.print(buf.toString());
	}

	PyException exc = Py.JavaError(t);
		
	maybeSystemExit(exc);

	if (f != null && exc.traceback.tb_frame != f) {
	    exc.traceback = new PyTraceback(exc.traceback);
	}

	setException(exc, f);

    	ThreadState ts = getThreadState();

        ts.systemState.last_value = exc.value;
        ts.systemState.last_type = exc.type;
        ts.systemState.last_traceback = exc.traceback;

    	ts.exception = null;
    	/*_type = null;
	  ts.exc_value = null;
	  ts.exc_traceback = null;*/

	stderr.print(exc.traceback.dumpStack());

	if (exc instanceof PySyntaxError) {
	    PySyntaxError se = (PySyntaxError)exc;
	    stderr.println("  File \""+se.filename+"\", line "+se.lineno);
	    if (se.text.length() != 0) {
		stderr.println("\t"+se.text);
		String space = "\t";
		for(int j=1; j<se.column; j++)
		    space = space+" ";
		stderr.println(space+"^");
	    }
	}

        if (exc.value instanceof PyJavaInstance) {
            Throwable javaError =
		(Throwable)exc.value.__tojava__(Throwable.class);

            if (javaError != null) {
                stderr.println(getStackTrace(javaError));
            }
        }

	PyObject typeName;
	if (exc.type instanceof PyClass) {
	    typeName = new PyString(((PyClass)exc.type).__name__);
	} else {
	    typeName = exc.type;
	}
	if (exc.value != Py.None) {
	    stderr.print(typeName);
	    stderr.print(": ");
	    if (exc instanceof PySyntaxError) {
		stderr.println(exc.value.__getitem__(0));
	    } else {
		stderr.println(exc.value);
	    }
	} else {
	    stderr.println(typeName);
	}
    }

    /* Equivalent to Python's assert statement */
    public static void assert(PyObject test, PyObject message) {
	if (!test.__nonzero__()) {
	    throw new PyException(Py.AssertionError, message);
	}
    }

    public static void assert(PyObject test) {
        assert(test, Py.None);
    }

    /* Helpers to implement except clauses */
    public static PyException setException(Throwable t, PyFrame frame) {
	//System.out.println("Py.setException");
	PyException pye = Py.JavaError(t);
	pye.instantiate();

    	ThreadState ts = getThreadState();

    	ts.exception = pye;
    	/*.type;
	  ts.exc_value = pye.value;
	  ts.exc_traceback = pye.traceback;*/
	return pye;
    }

    public static boolean matchException(PyException pye, PyObject e) {
	// A special case for IOError's to allow them to also match
	// java.io.IOExceptions.  This is a hack for 1.0.x until I can do
	// it right in 1.1
	pye.instantiate();
	if (e == Py.IOError) {
	    if (__builtin__.isinstance(
		pye.value,
		PyJavaClass.lookup(java.io.IOException.class)))
	    {
		return true;
	    }
	}
	if (e instanceof PyClass) {
	    return __builtin__.isinstance(pye.value, (PyClass)e);
	} else {
	    if (e == pye.type)
		return true;
	        
	    if (e instanceof PyTuple) {
		PyObject[] l = ((PyTuple)e).list;
		for(int i=0; i<l.length; i++) {
		    if (matchException(pye, l[i]))
			return true;
		}
	    }
	    return false;
	}
    }

    /* Implement the raise statement */
    // reraise the current exception
    public static PyException makeException() {
        ThreadState ts = getThreadState();
        if (ts.exception == null) {
            throw Py.ValueError("no exception to reraise");
        }
	return ts.exception;
    }
    
    public static PyException makeException(PyObject type) {
	if (type instanceof PyInstance) {
	    return new PyException(type.__class__, type);
	} else {
	    return makeException(type, Py.None);
	}
    }

    public static PyException makeException(PyObject type, PyObject value) {
	PyException exc = new PyException(type, value);
	exc.instantiate();
	return exc;
    }

    public static PyException makeException(PyObject type, PyObject value,
					    PyObject traceback)
    {
	return new PyException(type, value, (PyTraceback)traceback);
    }


    public static PyObject runCode(PyCode code, PyObject locals,
				   PyObject globals)
    {
	//System.out.println("run code");
	PyFrame f;
	/*if (globals == null && locals == null) {
	  f = Py.getFrame();
	  } else {*/
	if (locals == null) {
	    if (globals != null) {
		locals = globals;
	    } else {
		locals = Py.getFrame().getf_locals();
	    }
	}
			
	if (globals == null)
	    globals = Py.getFrame().f_globals;

	PyTableCode tc=null;
	if (code instanceof PyTableCode)
	    tc = (PyTableCode)code;

	f = new PyFrame(tc, locals, globals,
			Py.getThreadState().systemState.builtins);
	//}
	return code.call(f);
    }

    public static void exec(PyObject o, PyObject globals, PyObject locals) {
	PyCode code;
	if (o instanceof PyCode)
	    code = (PyCode)o;
	else {
	    if (o instanceof PyString)
		code = __builtin__.compile(o.toString(), "<string>", "exec");
	    else
		throw Py.TypeError(
		    "exec: argument 1 must be string or code object");
	}
	Py.runCode(code, locals, globals);
    }

    /* The PyThreadState will be stored in a thread-local variable
       when this feature becomes available in JDK1.2

       There are hacks that could improve performance slightly in the
       interim, but I'd rather wait for the right solution.
    */
    private static java.util.Hashtable threads;
    private static ThreadState cachedThreadState;
    public static final ThreadState getThreadState() {
        return getThreadState(null);
    }
    
    public static final ThreadState
	getThreadState(PySystemState newSystemState)
    {
        Thread t = Thread.currentThread();
        ThreadState ts = cachedThreadState;
        if (ts != null && ts.thread == t) {
            return ts;
        }

        if (threads == null)
	    threads = new java.util.Hashtable();

        ts = (ThreadState)threads.get(t);
        if (ts == null) {
            if (newSystemState == null) {
                writeDebug("threadstate", "no current system state");
                //t.dumpStack();
                newSystemState = defaultSystemState;
            }
            ts = new ThreadState(t, newSystemState);
            //System.err.println("new ts: "+ts+", "+ts.systemState);
            threads.put(t, ts);
        }
        cachedThreadState = ts;
        //System.err.println("returning ts: "+ts+", "+ts.systemState);
        return ts;
    }
    
    public static final PySystemState
	setSystemState(PySystemState newSystemState)
    {
        ThreadState ts = getThreadState(newSystemState);
        PySystemState oldSystemState = ts.systemState;
        if (oldSystemState != newSystemState) {
            //System.err.println("Warning: changing systemState for same thread!");
            ts.systemState = newSystemState;
        }
        return oldSystemState;
    }
    
    public static final PySystemState getSystemState() {
        return getThreadState().systemState;
        //defaultSystemState;
    }

    /* Get and set the current frame */

    public static PyFrame getFrame() {
	//System.out.println("getFrame");
	ThreadState ts = getThreadState();
	if (ts == null)
	    return null;
	return ts.frame;
    }

    public static void setFrame(PyFrame f) {
	//System.out.println("setFrame");
	getThreadState().frame = f;
    }

    /* These are not used anymore.  Uncomment them if there is a future
       clamor to make this functionality more easily usable
       public static void pushFrame(PyFrame f) {
       ThreadState ts = getThreadState();
       f.f_back = ts.frame;
       if (f.f_builtins == null) f.f_builtins = f.f_back.f_builtins;
       ts.frame = f;
       }

       public static PyFrame popFrame() {
       ThreadState ts = getThreadState();
       PyFrame f = ts.frame.f_back;
       ts.frame = f;
       return f;
       }
    */

    /* A collection of functions for implementing the print statement */

    public static StdoutWrapper stderr;
    static StdoutWrapper stdout;
    //public static StdinWrapper stdin;

    public static void print(PyObject o) {
        stdout.print(o);
    }

    public static void printComma(PyObject o) {
        stdout.printComma(o);
    }

    public static void println(PyObject o) {
        stdout.println(o);
    }

    public static void println() {
        stdout.println();
    }

    /* A collection of convenience functions for converting PyObjects
       to Java primitives */

    public static boolean py2boolean(PyObject o) {
	return o.__nonzero__();
    }

    public static byte py2byte(PyObject o) {
	if (o instanceof PyInteger) return (byte)((PyInteger)o).getValue();

	Byte i = (Byte)o.__tojava__(Byte.TYPE);
	if (i == null)
	    throw Py.TypeError("integer required");
	return i.byteValue();
    }
    public static short py2short(PyObject o) {
	if (o instanceof PyInteger)
	    return (short)((PyInteger)o).getValue();

	Short i = (Short)o.__tojava__(Short.TYPE);
	if (i == null)
	    throw Py.TypeError("integer required");
	return i.shortValue();
    }
	
    public static int py2int(PyObject o) {
	return py2int(o, "integer required");
    }
	
    public static int py2int(PyObject o, String msg) {
	if (o instanceof PyInteger)
	    return (int)((PyInteger)o).getValue();
        Object obj = o.__tojava__(Integer.TYPE);
        if (obj == Py.NoConversion)
	    throw Py.TypeError(msg);
        return ((Integer)obj).intValue();
    }
	
    public static long py2long(PyObject o) {
	if (o instanceof PyInteger)
	    return (long)((PyInteger)o).getValue();

	Long i = (Long)o.__tojava__(Long.TYPE);
	if (i == null)
	    throw Py.TypeError("integer required");
	return i.longValue();
    }

    public static float py2float(PyObject o) {
	if (o instanceof PyFloat)
	    return (float)((PyFloat)o).getValue();
	if (o instanceof PyInteger)
	    return (float)((PyInteger)o).getValue();

	Float i = (Float)o.__tojava__(Float.TYPE);
	if (i == null)
	    throw Py.TypeError("float required");
	return i.floatValue();
    }
    public static double py2double(PyObject o) {
	if (o instanceof PyFloat)
	    return (double)((PyFloat)o).getValue();
	if (o instanceof PyInteger)
	    return (double)((PyInteger)o).getValue();

	Double i = (Double)o.__tojava__(Double.TYPE);
	if (i == null)
	    throw Py.TypeError("float required");
	return i.doubleValue();
    }
	
    public static char py2char(PyObject o) {
	return py2char(o, "char required");
    }	

    public static char py2char(PyObject o, String msg) {
	if (o instanceof PyString) {
	    PyString s = (PyString)o;
	    if (s.__len__() != 1)
		throw Py.TypeError(msg);
	    return s.toString().charAt(0);
	}
	if (o instanceof PyInteger) {
	    return (char)((PyInteger)o).getValue();
	}

	Character i = (Character)o.__tojava__(Character.TYPE);
	if (i == null)
	    throw Py.TypeError(msg);
	return i.charValue();
    }
	
    public static void py2void(PyObject o) {
	if (o != Py.None) {
	    throw Py.TypeError("None required for void return");
	}
    }
	
    private static PyString[] letters=null;

    static final PyString makeCharacter(Character o) {
        char c = o.charValue();
        
        if (c > 255) {
            return new PyString(o.toString());
        }
        
	if (letters == null) {
	    letters = new PyString[256];
	    for(char j=0; j<256; j++) {
		letters[j] = new PyString(new Character(j).toString());
	    }
	}
	return letters[c];
    }

    // Needs rewriting for efficiency and extensibility
    public static PyObject java2py(Object o) {
	if (o instanceof PyObject)
	    return (PyObject)o;
	if (o instanceof PyProxy)
	    return ((PyProxy)o)._getPyInstance();
		
	if (o instanceof Number) {
	    if (o instanceof Double || o instanceof Float) {
		return new PyFloat(((Number)o).doubleValue());
	    }
	    else if (o instanceof Long) {
		return new PyLong(((Number)o).longValue());
	    }
	    else if (o instanceof Integer ||
		     o instanceof Byte ||
		     o instanceof Short)
	    {
		return new PyInteger(((Number)o).intValue());
	    }
	}
	if (o instanceof Boolean) {
	    return ((Boolean)o).booleanValue() ? Py.One : Py.Zero;
	}
	if (o == null) return Py.None;
	if (o instanceof String) return new PyString((String)o);
	if (o instanceof Character) return makeCharacter((Character)o);
	if (o instanceof Class) return PyJavaClass.lookup((Class)o);

	Class c = o.getClass();
	if (c.isArray()) {
	    return new PyArray(c.getComponentType(), o);
	}
	return new PyJavaInstance(o);
    }

    public static PyObject makeClass(String name, PyObject[] bases,
				     PyCode code, PyObject doc)
    {
	PyFrame frame = getFrame();
	PyObject globals = frame.f_globals;

	PyObject dict = code.call(Py.EmptyObjects, Py.NoKeywords,
				  globals, Py.EmptyObjects);
	if (doc != null)
	    dict.__setitem__("__doc__", doc);

	for(int i=0; i<bases.length; i++) {
	    if (!(bases[i] instanceof PyClass)) {
		PyObject c = bases[i].__class__;
		// Only try the meta-class trick on __class__'s that are
		// PyInstance's.  This will improve error messages for
		// casual mistakes while not really reducing the power of
		// this approach (I think)
		if (c instanceof PyJavaClass) {
		    throw Py.TypeError("base is not a class object: "+
				       bases[i].safeRepr());
		}
		return c.__call__(new PyString(name),
				  new PyTuple(bases),
				  dict);
	    }
	}

	PyClass pyclass = new PyClass();

	if (specialClasses != null) {
	    String nm = name;
	    PyObject mod = globals.__finditem__("__name__");
	    if (mod != null && mod instanceof PyString) {
		nm = ((PyString)mod).toString()+"."+nm;
	    }

            //System.out.println("specialClasses: "+nm);
	    Class jc = (Class)specialClasses.get(nm);
	    //System.out.println("got: "+jc);
            if (jc != null) {
                pyclass.proxyClass = jc;
            }
        }

	pyclass.init(name, new PyTuple(bases), dict);
	return pyclass;
    }

    private static int nameindex=0;
    public static synchronized String getName() {
	String name = "org.python.pycode._pyx"+nameindex;
	nameindex += 1;
	return name;
    }

    public static PyCode compile(SimpleNode node, String filename) {
	return compile(node, getName(), filename);
    }

    public static PyCode compile(SimpleNode node, String name, String filename)
    {
	return compile(node, name, filename, true, false);
    }

    public static PyCode compile(SimpleNode node, String name, String filename,
				 boolean linenumbers, boolean printResults)
    {
	try {
	    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
	    //System.err.println("compiling");
	    org.python.compiler.Module.compile(node, ostream, name, filename,
					       linenumbers, printResults,
					       false);
	    //System.err.println("compiled: "+(nameindex-1));

	    /*  Useful for debugging interactive use *
		FileOutputStream s = new FileOutputStream("c:\\jpython\\JavaCode\\org\\python\\pycode\\"+"_pyx"+(nameindex-1)+".class");
		s.write(ostream.toByteArray());
		s.close();
	    **/
	    //BytecodeLoader.byteloader=null;
	    return BytecodeLoader.makeCode(name, ostream.toByteArray());
	} catch (Throwable t) {
	    throw parser.fixParseError(null, t, filename);
	}
    }

    public static PyCode compile(InputStream istream, String filename,
				 String type)
    {
	SimpleNode node = parser.parse(istream, type, filename);
	boolean printResults = false;
	if (type.equals("single"))
	    printResults = true;
	return Py.compile(node, getName(), filename, true, printResults);
    }

    public static PyObject[] unpackSequence(PyObject o, int length) {
        if (o instanceof PyTuple) {
            PyTuple tup = (PyTuple)o;
            //System.err.println("unpack tuple");
            if (tup.__len__() == length)
		return tup.list;
            throw Py.ValueError("unpack tuple of wrong size");
        }
        
        PyObject[] ret = new PyObject[length];
        try {
            for(int i=0; i<length; i++) {
                PyObject tmp = o.__finditem__(i);
                if (tmp == null) {
                    throw Py.ValueError("unpack sequence too short");
                }
                ret[i] = tmp;
            }
        } catch (PyException exc) {
            if (Py.matchException(exc, Py.AttributeError)) {
                throw Py.TypeError("unpack non-sequence");
            } else {
                throw exc;
            }
        }
        
        if (o.__finditem__(length) != null) {
            throw Py.ValueError("unpack sequence too long");
        }
        return ret;
    }

    public static int id(PyObject o) {
	if (o instanceof PyJavaInstance) {
	    return System.identityHashCode(((PyJavaInstance)o).javaProxy);
	} else {
	    return System.identityHashCode(o);
	}
    }

    public static void printResult(PyObject ret) {
	if (ret != Py.None) {
	    Py.getSystemState().builtins.__setitem__("_", Py.None);
	    Py.println(ret.__repr__());
	    Py.getSystemState().builtins.__setitem__("_", ret);
	}
    }

    public static final int ERROR=-1;
    public static final int WARNING=0;
    public static final int MESSAGE=1;
    public static final int COMMENT=2;
    public static final int DEBUG=3;

    public static void maybeWrite(String type, String msg, int level) {
        if (level <= Options.verbose) {
            System.err.println(type+": "+msg);
        }
    }

    public static void writeError(String type, String msg) {
        maybeWrite(type, msg, ERROR);
    }   
    public static void writeWarning(String type, String msg) {
        maybeWrite(type, msg, WARNING);
    }
    public static void writeMessage(String type, String msg) {
        maybeWrite(type, msg, MESSAGE);
    }
    public static void writeComment(String type, String msg) {
        maybeWrite(type, msg, COMMENT);
    }    
    public static void writeDebug(String type, String msg) {
        maybeWrite(type, msg, DEBUG);
    }

}

/** @deprecated **/
class FixedFileWrapper extends StdoutWrapper {
    private PyObject file;
    public FixedFileWrapper(PyObject file) {
        name = "fixed file";
        this.file = file;
    }
    
    protected PyObject myFile() {
        return file;
    }
}
