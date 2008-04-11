// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import org.python.compiler.Module;
import org.python.core.adapter.ClassicPyObjectAdapter;
import org.python.core.adapter.ExtensiblePyObjectAdapter;
import org.python.core.util.StringUtil;
import org.python.modules.errno;
import org.python.parser.ast.modType;

public final class Py
{
    static class SingletonResolver implements Serializable {
        private String which;
        
        SingletonResolver(String which) {
            this.which = which;
        }
        
        private Object readResolve() throws ObjectStreamException {
            if (which.equals("None")) {
                return Py.None;
            } else if (which.equals("Ellipsis")) {
                return Py.Ellipsis;
            } else if (which.equals("NotImplemented")) {
                return Py.NotImplemented;
            }
            throw new StreamCorruptedException("unknown singleton: "+which);
        }
    }
    
    
    /* Holds the singleton None and Ellipsis objects */
    /** The singleton None Python object **/
    public static PyObject None;

    /** The singleton Ellipsis Python object - written as ... when indexing */
    public static PyObject Ellipsis;

    /** The singleton NotImplemented Python object. Used in rich comparison */
    public static PyObject NotImplemented;

    /** A zero-length array of Strings to pass to functions that
        don't have any keyword arguments **/
    public static String[] NoKeywords;

    /** A zero-length array of PyObject's to pass to functions that
        expect zero-arguments **/
    public static PyObject[] EmptyObjects;

    /** A tuple with zero elements **/
    public static PyTuple EmptyTuple;

    /** The Python integer 0 **/
    public static PyInteger Zero;

    /** The Python integer 1 **/
    public static PyInteger One;

    /** The Python boolean False **/
    public static PyBoolean False;

    /** The Python boolean True **/
    public static PyBoolean True;

    /** A zero-length Python string **/
    public static PyString EmptyString;

    /** A Python string containing '\n' **/
    public static PyString Newline;

    /** A Python string containing ' ' **/
    public static PyString Space;

    /** Set if the type object is dynamically allocated */
    public static long TPFLAGS_HEAPTYPE;
    
    /** Builtin types that are used to setup PyObject. */
    static final Set<Class> BOOTSTRAP_TYPES = new HashSet<Class>(4);
    static {
        BOOTSTRAP_TYPES.add(PyObject.class);
        BOOTSTRAP_TYPES.add(PyType.class);
        BOOTSTRAP_TYPES.add(PyBuiltinFunction.class);
        BOOTSTRAP_TYPES.add(PyDataDescr.class);
    }

    /** A unique object to indicate no conversion is possible
        in __tojava__ methods **/
    public static Object NoConversion;

    public static PyObject OSError;
    public static PyException OSError(String message) {
        return new PyException(Py.OSError, message);
    }

    public static PyObject NotImplementedError;
    public static PyException NotImplementedError(String message) {
        return new PyException(Py.NotImplementedError, message);
    }

    public static PyObject EnvironmentError;
    public static PyException EnvironmentError(String message) {
        return new PyException(Py.EnvironmentError, message);
    }

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

    public static PyObject IndentationError;
    public static PyObject TabError;

    public static PyObject AttributeError;
    public static PyException AttributeError(String message) {
        return new PyException(Py.AttributeError, message);
    }

    public static PyObject IOError;
    public static PyException IOError(java.io.IOException ioe) {
        String message = ioe.getMessage();
        if (message == null) {
            message = ioe.getClass().getName();
        }
        if (ioe instanceof java.io.FileNotFoundException) {
            message = "File not found - " + message;
            return IOError(errno.ENOENT, message);
        }
        return new PyException(Py.IOError, message);
    }
    public static PyException IOError(String message) {
        return new PyException(Py.IOError, message);
    }

    public static PyException IOError(int errno, String message) {
        PyTuple args = new PyTuple(new PyInteger(errno), new PyString(message));
        return new PyException(Py.IOError, args);
    }

    public static PyObject KeyError;
    public static PyException KeyError(String message) {
        return new PyException(Py.KeyError, message);
    }
    public static PyException KeyError(PyObject key) {
        return new PyException(Py.KeyError, key);
    }
    
    public static PyObject AssertionError;
    public static PyException AssertionError(String message) {
        return new PyException(Py.AssertionError, message);
    }

    public static PyObject TypeError;
    public static PyException TypeError(String message) {
        return new PyException(Py.TypeError, message);
    }

    public static PyObject ReferenceError;
    public static PyException ReferenceError(String message) {
        return new PyException(Py.ReferenceError, message);
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

    public static PyObject UnboundLocalError;
    public static PyException UnboundLocalError(String message) {
        return new PyException(Py.UnboundLocalError, message);
    }

    public static PyObject SystemExit;
    /*public static PyException SystemExit(String message) {
      return new PyException(Py.SystemExit, message);
      }*/
    static void maybeSystemExit(PyException exc) {
        //System.err.println("maybeSystemExit: " + exc.type.toString());
        if (Py.matchException(exc, Py.SystemExit)) {
            PyObject value = exc.value;
            //System.err.println("exiting: "+value.getClass().getName());
            if (value instanceof PyInstance) {
                PyObject tmp = value.__findattr__("code");
                if (tmp != null)
                    value = tmp;
            }
            Py.getSystemState().callExitFunc();
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

    public static PyObject StopIteration;
    public static PyException StopIteration(String message) {
        return new PyException(Py.StopIteration, message);
    }
    
    public static PyObject GeneratorExit;
    public static PyException GeneratorExit(String message) {
        return new PyException(Py.GeneratorExit, message);
    }

    public static PyObject ImportError;
    public static PyException ImportError(String message) {
        return new PyException(Py.ImportError, message);
    }

    public static PyObject ValueError;
    public static PyException ValueError(String message) {
        return new PyException(Py.ValueError, message);
    }

    public static PyObject UnicodeError;
    public static PyException UnicodeError(String message) {
        return new PyException(Py.UnicodeError, message);
    }

    public static PyObject UnicodeTranslateError;
    public static PyException UnicodeTranslateError(String object,
                                                 int start,
                                                 int end, String reason) {
        return new PyException(Py.UnicodeTranslateError, new PyTuple(new PyString(object),
                                                                     new PyInteger(start),
                                                                     new PyInteger(end),
                                                                     new PyString(reason)));
    }

    public static PyObject UnicodeDecodeError;

    public static PyException UnicodeDecodeError(String encoding,
                                                 String object,
                                                 int start,
                                                 int end,
                                                 String reason) {
        return new PyException(Py.UnicodeDecodeError, new PyTuple(new PyString(encoding),
                                                                  new PyString(object),
                                                                  new PyInteger(start),
                                                                  new PyInteger(end),
                                                                  new PyString(reason)));
    }

    public static PyObject UnicodeEncodeError;

    public static PyException UnicodeEncodeError(String encoding,
                                                 String object,
                                                 int start,
                                                 int end,
                                                 String reason) {
        return new PyException(Py.UnicodeEncodeError, new PyTuple(new PyString(encoding),
                                                                  new PyString(object),
                                                                  new PyInteger(start),
                                                                  new PyInteger(end),
                                                                  new PyString(reason)));
    }

    public static PyObject EOFError;
    public static PyException EOFError(String message) {
        return new PyException(Py.EOFError, message);
    }

    public static PyObject MemoryError;
    
    public static void memory_error(OutOfMemoryError t) {
        if (Options.showJavaExceptions) {
            t.printStackTrace();
        }
// this logic would allow to re-enable the old behavior when it makes sense, 
// or better offer a hook?
//        try {
//            byte[] alloc = new byte[(512*1024)];
//        } catch(OutOfMemoryError oome) {
//            System.err.println("Out Of Memory");
//            System.err.println("You might want to try the -mx flag to increase heap size.");
//            System.exit(-1);
//        }
    }

    public static PyException MemoryError(String message) {
        return new PyException(Py.MemoryError, message);
    }

    public static PyObject ArithmeticError;
    public static PyObject LookupError;
    public static PyObject StandardError;
    public static PyObject Exception;

    public static PyObject Warning;
    public static void Warning(String message) {
        warning(Warning, message);
    }

    public static PyObject UserWarning;
    public static void UserWarning(String message) {
        warning(UserWarning, message);
    }

    public static PyObject DeprecationWarning;
    public static void DeprecationWarning(String message) {
        warning(DeprecationWarning, message);
    }
    
    public static PyObject PendingDeprecationWarning;
    public static void PendingDeprecationWarning(String message) {
        warning( PendingDeprecationWarning, message);
    }

    public static PyObject SyntaxWarning;
    public static void SyntaxWarning(String message) {
        warning(SyntaxWarning, message);
    }

    public static PyObject OverflowWarning;
    public static void OverflowWarning(String message) {
        warning(OverflowWarning, message);
    }

    public static PyObject RuntimeWarning;
    public static void RuntimeWarning(String message) {
        warning(RuntimeWarning, message);
    }
    
    public static PyObject FutureWarning;
    public static void FutureWarning(String message) {
        warning(FutureWarning, message);
    }

    private static PyObject warnings_mod;
    private static PyObject importWarnings() {
        if (warnings_mod != null) return warnings_mod;
        PyObject mod;
        try {
            mod = __builtin__.__import__("warnings");
        } catch(PyException e) {
            if (matchException(e,ImportError)) {
                return null;
            }
            throw e;
        }
        warnings_mod = mod;
        return mod;
    }

    private static String warn_hcategory(PyObject category) {
        PyObject name = category.__findattr__("__name__");
        if (name != null) return "["+name+"]";
        return "[warning]";
    }

    public static void warning(PyObject category, String message) {
        PyObject func = null;
        PyObject mod = importWarnings();
        if (mod != null)
            func = mod.__getattr__("warn");
        if (func == null) {
            System.err.println(warn_hcategory(category) + ": " + message);
            return;
        } else {
            func.__call__(Py.newString(message), category);
        }
    }

    public static void warning(PyObject category, String message,
                               String filename, int lineno, String module,
                               PyObject registry)
    {
        PyObject func = null;
        PyObject mod = importWarnings();
        if (mod != null)
            func = mod.__getattr__("warn_explicit");
        if (func == null) {
            System.err.println(filename + ":" + lineno + ":" +
                               warn_hcategory(category) + ": " + message);
            return;
        } else {
            func.__call__(new PyObject[] {
                Py.newString(message), category,
                Py.newString(filename), Py.newInteger(lineno),
                (module == null) ? Py.None : Py.newString(module),
                registry}, Py.NoKeywords);
        }
    }


    public static PyObject JavaError;
    public static PyException JavaError(Throwable t) {
        if(t instanceof PyException) {
            return (PyException)t;
        } else if(t instanceof InvocationTargetException) {
            return JavaError(((InvocationTargetException)t).getTargetException());
        } else if(t instanceof OutOfMemoryError) {
            memory_error((OutOfMemoryError)t);
        }
        PyJavaInstance exc = new PyJavaInstance(t);
        PyException pyex = new PyException(exc.instclass, exc);
        // Set the cause to the original throwable to preserve
        // the exception chain.
        pyex.initCause(t);
        return pyex;
    }

    // Don't allow any constructors. Class only provides static methods.
    private Py() {}

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

    // ??pending: was @deprecated but is actually used by proxie code.
    // Can get rid of it?
    public static Object tojava(PyObject o, String s) {
        Class c = findClass(s);
        if (c == null) throw Py.TypeError("can't convert to: "+s);
        return tojava(o, c); // prev:Class.forName
    }
    /* Helper functions for PyProxy's */

    /** @deprecated */
    public static PyObject jfindattr(PyProxy proxy, String name) {
        PyObject ret = getInstance(proxy, name);
        if (ret == null)
            return null;
        Py.setSystemState(proxy._getPySystemState());
        return ret;
    }

    /** @deprecated */
    public static PyObject jgetattr(PyProxy proxy, String name) {
        PyObject ret = getInstance(proxy, name);
        if (ret == null)
            throw Py.AttributeError("abstract method '" + name + "' not implemented");
        Py.setSystemState(proxy._getPySystemState());
        return ret;
    }

    private static PyObject getInstance(PyProxy proxy, String name) {
        PyInstance o = proxy._getPyInstance();
        if (o == null) {
            proxy.__initProxy__(new Object[0]);
            o = proxy._getPyInstance();
        }
        PyObject ret = o.__jfindattr__(name);
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

    public static PyObject newInteger(long i) {
        if (i < Integer.MIN_VALUE || i > Integer.MAX_VALUE)
            return new PyLong(i);
        else
            return newInteger((int)i);
    }

    public static PyLong newLong(String s) {
        return new PyLong(s);
    }

    public static PyLong newLong(java.math.BigInteger i) {
        return new PyLong(i);
    }

    public static PyLong newLong(int i) {
        return new PyLong(i);
    }

    public static PyLong newLong(long l) {
        return new PyLong(l);
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
        return makeCharacter(c);
    }

    public static PyString newString(String s) {
        return new PyString(s);
    }
    
    public static PyUnicode newUnicode(char c) {
        return (PyUnicode)makeCharacter(c, true);
    }

    public static PyUnicode newUnicode(String s) {
        return new PyUnicode(s);
    }

    public static PyBoolean newBoolean(boolean t) {
        return t ? Py.True : Py.False;
    }

    // nested scopes:
    // String[] cellvars,String[] freevars,int npurecell & int moreflags

    public static PyCode newCode(int argcount, String varnames[],
                                 String filename, String name,
                                 boolean args, boolean keywords,
                                 PyFunctionTable funcs, int func_id,
                                 String[] cellvars,String[] freevars,
                                 int npurecell, int moreflags)
    {
        return new PyTableCode(argcount, varnames,
                               filename, name, 0, args, keywords, funcs,
                               func_id, cellvars, freevars, npurecell,
                               moreflags);
    }

    public static PyCode newCode(int argcount, String varnames[],
                                 String filename, String name,
                                 int firstlineno,
                                 boolean args, boolean keywords,
                                 PyFunctionTable funcs, int func_id,
                                 String[] cellvars,String[] freevars,
                                 int npurecell, int moreflags)

    {
        return new PyTableCode(argcount, varnames,
                               filename, name, firstlineno, args, keywords,
                               funcs, func_id, cellvars, freevars, npurecell,
                               moreflags);
    }

    // --

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

    public static PyCode newJavaCode(Class cls, String name) {
        return new JavaCode(newJavaFunc(cls, name));
    }

    public static PyObject newJavaFunc(Class cls, String name) {
        try {
            java.lang.reflect.Method m = cls.getMethod(name, new Class[] {
                       PyObject[].class, String[].class });
            return new JavaFunc(m);
        } catch (NoSuchMethodException e) {
            throw Py.JavaError(e);
        }
    }

    private static PyObject initExc(String name, PyObject exceptions,
                                    PyObject dict) {
        PyObject tmp = exceptions.__getattr__(name);
        dict.__setitem__(name, tmp);
        return tmp;
    }

    static void initClassExceptions(PyObject dict) {
        PyObject exc = imp.load("exceptions");

        Exception           = initExc("Exception", exc, dict);
        SystemExit          = initExc("SystemExit", exc, dict);
        StopIteration       = initExc("StopIteration", exc, dict);
        GeneratorExit       = initExc("GeneratorExit", exc, dict);
        StandardError       = initExc("StandardError", exc, dict);
        KeyboardInterrupt   = initExc("KeyboardInterrupt", exc, dict);
        ImportError         = initExc("ImportError", exc, dict);
        EnvironmentError    = initExc("EnvironmentError", exc, dict);
        IOError             = initExc("IOError", exc, dict);
        OSError             = initExc("OSError", exc, dict);
        EOFError            = initExc("EOFError", exc, dict);
        RuntimeError        = initExc("RuntimeError", exc, dict);
        NotImplementedError = initExc("NotImplementedError", exc, dict);
        NameError           = initExc("NameError", exc, dict);
        UnboundLocalError   = initExc("UnboundLocalError", exc, dict);
        AttributeError      = initExc("AttributeError", exc, dict);
        SyntaxError         = initExc("SyntaxError", exc, dict);
        IndentationError    = initExc("IndentationError", exc, dict);
        TabError            = initExc("TabError", exc, dict);
        TypeError           = initExc("TypeError", exc, dict);
        AssertionError      = initExc("AssertionError", exc, dict);
        LookupError         = initExc("LookupError", exc, dict);
        IndexError          = initExc("IndexError", exc, dict);
        KeyError            = initExc("KeyError", exc, dict);
        ArithmeticError     = initExc("ArithmeticError", exc, dict);
        OverflowError       = initExc("OverflowError", exc, dict);
        ZeroDivisionError   = initExc("ZeroDivisionError", exc, dict);
        FloatingPointError  = initExc("FloatingPointError", exc, dict);
        ValueError          = initExc("ValueError", exc, dict);
        UnicodeError        = initExc("UnicodeError", exc, dict);
        UnicodeEncodeError  = initExc("UnicodeEncodeError", exc, dict);
        UnicodeDecodeError  = initExc("UnicodeDecodeError", exc, dict);
        UnicodeTranslateError  = initExc("UnicodeTranslateError", exc, dict);
        ReferenceError      = initExc("ReferenceError", exc, dict);
        SystemError         = initExc("SystemError", exc, dict);
        MemoryError         = initExc("MemoryError", exc, dict);
        Warning             = initExc("Warning", exc, dict);
        UserWarning         = initExc("UserWarning", exc, dict);
        DeprecationWarning  = initExc("DeprecationWarning", exc, dict);
        PendingDeprecationWarning      = initExc("PendingDeprecationWarning", exc, dict);
        SyntaxWarning       = initExc("SyntaxWarning", exc, dict);
        OverflowWarning     = initExc("OverflowWarning", exc, dict);
        RuntimeWarning      = initExc("RuntimeWarning", exc, dict);
        FutureWarning      = initExc("FutureWarning", exc, dict);   
        
        // Pre-initialize the PyJavaClass for OutOfMemoryError so when we need
        // it it creating the pieces for it won't cause an additional out of
        // memory error.  Fix for bug #1654484
        PyJavaClass.lookup(java.lang.OutOfMemoryError.class);
    }

    public static PySystemState defaultSystemState;
    // This is a hack to get initializations to work in proper order
    public static synchronized boolean initPython() {
        PySystemState.initialize();
        return true;
    }


    public static Class relFindClass(Class home,String name) {
        try {
            ClassLoader loader = home.getClassLoader();
            if (loader != null) return loader.loadClass(name);
            else return Class.forName(name);
        } catch (ClassNotFoundException exc) {
            return null;
        } catch (Throwable t) {
            throw Py.JavaError(t);
        }
    }

    private static boolean secEnv=false;

    public static Class findClass(String name) {
        try {
            ClassLoader classLoader = Py.getSystemState().getClassLoader();
            if (classLoader != null) return classLoader.loadClass(name);

            if(!secEnv) {
                try {
                    classLoader = imp.getSyspathJavaLoader();
                }
                catch(SecurityException e) {
                    secEnv=true;
                }
                if (classLoader != null) {
                    return classLoader.loadClass(name);
                }
            }

            return Class.forName(name);

        }
        catch (ClassNotFoundException e) {
            //             e.printStackTrace();
            return null;
        }
        catch (IllegalArgumentException e) {
            //             e.printStackTrace();
            return null;
        }
        catch (NoClassDefFoundError e) {
            //             e.printStackTrace();
            return null;
        }
    }

    public static Class findClassEx(String name, String reason) {
        try {
            ClassLoader classLoader = Py.getSystemState().getClassLoader();
            if (classLoader != null) {
                writeDebug("import", "trying " + name + " as " + reason +
                           " in classLoader");
                return classLoader.loadClass(name);
            }

            if(!secEnv) {
                try {
                    classLoader = imp.getSyspathJavaLoader();
                }
                catch(SecurityException e) {
                    secEnv=true;
                }
                if (classLoader != null) {
                    writeDebug("import", "trying " + name + " as " + reason +
                           " in syspath loader");
                    return classLoader.loadClass(name);
                }
            }

            writeDebug("import", "trying " + name + " as " + reason +
                       " in Class.forName");
            return Class.forName(name);
        }
        catch (ClassNotFoundException e) {
            return null;
        }
        catch (IllegalArgumentException e) {
            throw JavaError(e);
        }
        catch (LinkageError e) {
            throw JavaError(e);
        }
    }

    public static void initProxy(PyProxy proxy, String module, String pyclass, Object[] args)
    {
        if (proxy._getPyInstance() != null)
            return;
        ThreadState ts = getThreadState();
        PyInstance instance = ts.getInitializingProxy();
        if (instance != null) {
            if (instance.javaProxy != null)
                throw Py.TypeError("Proxy instance reused");
            instance.javaProxy = proxy;
            proxy._setPyInstance(instance);
            proxy._setPySystemState(ts.systemState);
            return;
        }

        //System.out.println("path: "+sys.path.__str__());
        PyObject mod;
        // ??pending: findClass or should avoid sys.path loading?
        Class modClass = Py.findClass(module+"$_PyInner");
        if (modClass != null) {
            //System.err.println("found as class: "+modClass);
            PyCode code=null;
            try {
                code = ((PyRunnable)modClass.newInstance()).getMain();
            } catch (Throwable t) {
                throw Py.JavaError(t);
            }
            mod = imp.createFromCode(module, code);
        } else {
            mod = imp.importName(module.intern(), false);
            //System.err.println("found as mod: "+mod);
        }
        PyClass pyc = (PyClass)mod.__getattr__(pyclass.intern());

        instance = new PyInstance(pyc);
        instance.javaProxy = proxy;
        proxy._setPyInstance(instance);
        proxy._setPySystemState(ts.systemState);

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

    /**
     * Initializes a default PythonInterpreter and runs the code from
     * {@link PyRunnable#getMain} as __main__
     * 
     * Called by the code generated in {@link Module#addMain()}
     */
    public static void runMain(PyRunnable main, String[] args) throws Exception {
        PySystemState.initialize(null, null, args, main.getClass().getClassLoader());
        try {
            imp.createFromCode("__main__", main.getMain());
        } catch (PyException e) {
            Py.getSystemState().callExitFunc();
            if (Py.matchException(e, Py.SystemExit))
                return;
            throw e;
        }
        Py.getSystemState().callExitFunc();
    }
    //XXX: this needs review to make sure we are cutting out all of the Java
    //     exceptions.
    private static String getStackTrace(Throwable javaError) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        javaError.printStackTrace(new PrintStream(buf));

        String str = buf.toString();
        int index = -1;
        if (index == -1)
            index = str.indexOf(
                "at org.python.core.PyReflectedConstructor.__call__");
        if (index == -1)
            index = str.indexOf("at org.python.core.PyReflectedFunction.__call__");
        if (index == -1)
            index = str.indexOf(
                "at org/python/core/PyReflectedConstructor.__call__");
        if (index == -1)
            index = str.indexOf("at org/python/core/PyReflectedFunction.__call__");

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

        setException(exc, f);

        ThreadState ts = getThreadState();

        ts.systemState.last_value = exc.value;
        ts.systemState.last_type = exc.type;
        ts.systemState.last_traceback = exc.traceback;

        PyObject exceptHook = ts.systemState.__findattr__("excepthook");
        if (exceptHook != null) {
            try {
                exceptHook.__call__(exc.type, exc.value, exc.traceback);
            } catch (PyException exc2) {
                stderr.println("Error in sys.excepthook:");
                displayException(exc2.type, exc2.value, exc2.traceback, file);
                stderr.println();
                stderr.println("Original exception was:");
                displayException(exc.type, exc.value, exc.traceback, file);
            }
        } else {
            stderr.println("sys.excepthook is missing");
            displayException(exc.type, exc.value, exc.traceback, file);
        }

        ts.exception = null;
    }

    public static void displayException(PyObject type, PyObject value,
                                        PyObject tb, PyObject file)
    {
        StdoutWrapper stderr = Py.stderr;
        if (file != null) {
            stderr = new FixedFileWrapper(file);
        }

        if (tb instanceof PyTraceback)
            stderr.print(((PyTraceback) tb).dumpStack());
        if (__builtin__.isinstance(value, Py.SyntaxError)) {
            stderr.println("  File \""+value.__findattr__("filename")+
                           "\", line "+value.__findattr__("lineno"));
            PyObject text = value.__findattr__("text");
            if (text != Py.None && text.__len__() != 0) {
                stderr.println("\t"+text);
                String space = "\t";
                int col = ((PyInteger)value.__findattr__("offset").__int__()).getValue();
                for(int j=1; j<col; j++)
                    space = space+" ";
                stderr.println(space+"^");
            }
        }

        if (value instanceof PyJavaInstance) {
            Object javaError = value.__tojava__(Throwable.class);

            if (javaError != null && javaError != Py.NoConversion) {
                stderr.println(getStackTrace((Throwable)javaError));
            }
        }
        stderr.println(formatException(type, value, tb));
    }

    static String formatException(PyObject type, PyObject value, PyObject tb) {
        StringBuffer buf = new StringBuffer();

        if (type instanceof PyClass) {
            buf.append(((PyClass) type).__name__);
        } else {
            buf.append(type.__str__());
        }
        if (value != Py.None) {
            buf.append(": ");
            if (__builtin__.isinstance(value, Py.SyntaxError)) {
                buf.append(value.__getitem__(0).__str__());
            } else {
                buf.append(value.__str__());
            }
        }
        return buf.toString();
    }


    /* Equivalent to Python's assert statement */
    public static void assert_(PyObject test, PyObject message) {
        if (!test.__nonzero__()) {
            throw new PyException(Py.AssertionError, message);
        }
    }

    public static void assert_(PyObject test) {
        assert_(test, Py.None);
    }

    /* Helpers to implement finally clauses */
    public static void addTraceback(Throwable t, PyFrame frame) {
        PyException e = Py.JavaError(t);

        //Add another traceback object to the exception if needed
        if (e.traceback.tb_frame != frame && e.traceback.tb_frame.f_back != null) {
            e.traceback = new PyTraceback(e.traceback);
        }
    }

    /* Helpers to implement except clauses */
    public static PyException setException(Throwable t, PyFrame frame) {
        PyException pye = Py.JavaError(t);
        pye.instantiate();

        // attach catching frame
        if (frame != null && pye.traceback.tb_frame != frame && pye.traceback.tb_frame.f_back != null) {
            pye.traceback = new PyTraceback(pye.traceback);
        }
       
        ThreadState ts = getThreadState();

        ts.exception = pye;

        return pye;
    }

    public static boolean matchException(PyException pye, PyObject e) {
        pye.instantiate();
        // FIXME, see bug 737978
        //
        // A special case for IOError's to allow them to also match
        // java.io.IOExceptions.  This is a hack for 1.0.x until I can do
        // it right in 1.1
        if(e == Py.IOError) {
            if(__builtin__.isinstance(pye.value,
                                      PyJavaClass.lookup(java.io.IOException.class))) {
                return true;
            }
        }
        // FIXME too, same approach for OutOfMemoryError
        if(e == Py.MemoryError) {
            if(__builtin__.isinstance(pye.value,
                                      PyJavaClass.lookup(java.lang.OutOfMemoryError.class))) {
                return true;
            }
        }
        if(e instanceof PyClass) {
            return __builtin__.isinstance(pye.value, e);
        } else {
            if(e == pye.type)
                return true;
            if(e instanceof PyTuple) {
                PyObject[] l = ((PyTuple)e).getArray();
                for(int i = 0; i < l.length; i++) {
                    if(matchException(pye, l[i]))
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
            return new PyException(type.fastGetClass(), type);
        } else {
            return makeException(type, Py.None);
        }
    }

    public static PyException makeException(PyObject type, PyObject value) {
        if (type instanceof PyInstance) {
            if (value != Py.None) {
                throw TypeError("instance exceptions may not have " +
                                "a separate value");
            } else {
                return new PyException(type.fastGetClass(), type);
            }
        }
        PyException exc = new PyException(type, value);
        exc.instantiate();
        return exc;
    }

    public static PyException makeException(PyObject type, PyObject value,
                                            PyObject traceback)
    {
        if (type instanceof PyInstance) {
            if (value != Py.None) {
                throw TypeError("instance exceptions may not have " +
                                "a separate value");
            } else {
                type = type.fastGetClass();
            }
        }

        if (traceback == None)
            return new PyException(type, value);
        if (!(traceback instanceof PyTraceback))
            throw TypeError("raise 3rd arg must be traceback or None");

        return new PyException(type, value, (PyTraceback)traceback);
    }


    public static PyObject runCode(PyCode code, PyObject locals,
                                   PyObject globals)
    {
        PyFrame f;
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
                        PySystemState.builtins);
        return code.call(f);
    }

    public static void exec(PyObject o, PyObject globals, PyObject locals) {
        PyCode code;
        if(o instanceof PyCode) {
            code = (PyCode)o;
            if(locals == null && o instanceof PyTableCode
                    && ((PyTableCode)o).hasFreevars()) {
                throw Py.TypeError("code object passed to exec may not contain free variables");
            }
        }else {
            String contents = null;
            if (o instanceof PyString)
                contents = o.toString();
            else if (o instanceof PyFile) {
                PyFile fp = (PyFile)o;
                if (fp.getClosed())
                    return;
                contents = fp.read().toString();
            } else
                throw Py.TypeError(
                    "exec: argument 1 must be string, code or file object");
            code = (PyCode)Py.compile_flags(contents, "<string>", "exec",
                                    Py.getCompilerFlags());
        }
        Py.runCode(code, locals, globals);
    }

    private static ThreadStateMapping threadStateMapping = null;

    public static final ThreadState getThreadState() {
        return getThreadState(null);
    }

    public static final ThreadState
        getThreadState(PySystemState newSystemState)
    {
        if (threadStateMapping == null) {
            synchronized (Py.class) {
                if (threadStateMapping == null)
                    threadStateMapping = new ThreadStateMapping();
            }
        }
        return threadStateMapping.getThreadState(newSystemState);
    }

    public static final PySystemState
        setSystemState(PySystemState newSystemState)
    {
        ThreadState ts = getThreadState(newSystemState);
        PySystemState oldSystemState = ts.systemState;
        if (oldSystemState != newSystemState) {
            //System.err.println("Warning: changing systemState "+
            //                   "for same thread!");
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

    /* A collection of functions for implementing the print statement */

    public static StdoutWrapper stderr;
    static StdoutWrapper stdout;
    //public static StdinWrapper stdin;

    public static void print(PyObject file, PyObject o) {
        if (file == None)
            print(o);
        else
            new FixedFileWrapper(file).print(o);
    }
    public static void printComma(PyObject file, PyObject o) {
        if (file == None)
            printComma(o);
        else
            new FixedFileWrapper(file).printComma(o);
    }
    public static void println(PyObject file, PyObject o) {
        if (file == None)
            println(o);
        else
            new FixedFileWrapper(file).println(o);
    }
    public static void printlnv(PyObject file) {
        if (file == None)
            println();
        else
            new FixedFileWrapper(file).println();
    }

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

    /*
     * A collection of convenience functions for converting PyObjects to Java primitives
     */
    public static boolean py2boolean(PyObject o) {
        return o.__nonzero__();
    }

    public static byte py2byte(PyObject o) {
        if(o instanceof PyInteger) {
            return (byte)((PyInteger)o).getValue();
        }
        Object i = o.__tojava__(Byte.TYPE);
        if(i == null || i == Py.NoConversion)
            throw Py.TypeError("integer required");
        return ((Byte)i).byteValue();
    }

    public static short py2short(PyObject o) {
        if(o instanceof PyInteger)
            return (short)((PyInteger)o).getValue();
        Object i = o.__tojava__(Short.TYPE);
        if(i == null || i == Py.NoConversion)
            throw Py.TypeError("integer required");
        return ((Short)i).shortValue();
    }

    public static int py2int(PyObject o) {
        return py2int(o, "integer required");
    }

    public static int py2int(PyObject o, String msg) {
        if(o instanceof PyInteger)
            return ((PyInteger)o).getValue();
        Object obj = o.__tojava__(Integer.TYPE);
        if(obj == Py.NoConversion)
            throw Py.TypeError(msg);
        return ((Integer)obj).intValue();
    }

    public static long py2long(PyObject o) {
        if(o instanceof PyInteger)
            return ((PyInteger)o).getValue();
        Object i = o.__tojava__(Long.TYPE);
        if(i == null || i == Py.NoConversion)
            throw Py.TypeError("integer required");
        return ((Long)i).longValue();
    }

    public static float py2float(PyObject o) {
        if(o instanceof PyFloat)
            return (float)((PyFloat)o).getValue();
        if(o instanceof PyInteger)
            return ((PyInteger)o).getValue();
        Object i = o.__tojava__(Float.TYPE);
        if(i == null || i == Py.NoConversion)
            throw Py.TypeError("float required");
        return ((Float)i).floatValue();
    }

    public static double py2double(PyObject o) {
        if(o instanceof PyFloat)
            return ((PyFloat)o).getValue();
        if(o instanceof PyInteger)
            return ((PyInteger)o).getValue();
        Object i = o.__tojava__(Double.TYPE);
        if(i == null || i == Py.NoConversion)
            throw Py.TypeError("float required");
        return ((Double)i).doubleValue();
    }

    public static char py2char(PyObject o) {
        return py2char(o, "char required");
    }

    public static char py2char(PyObject o, String msg) {
        if(o instanceof PyString) {
            PyString s = (PyString)o;
            if(s.__len__() != 1)
                throw Py.TypeError(msg);
            return s.toString().charAt(0);
        }
        if(o instanceof PyInteger) {
            return (char)((PyInteger)o).getValue();
        }
        Object i = o.__tojava__(Character.TYPE);
        if(i == null || i == Py.NoConversion)
            throw Py.TypeError(msg);
        return ((Character)i).charValue();
    }

    public static void py2void(PyObject o) {
        if(o != Py.None) {
            throw Py.TypeError("None required for void return");
        }
    }

    private static PyString[] letters=null;


    public static final PyString makeCharacter(Character o) {
        return makeCharacter(o.charValue());
    }

    public static final PyString makeCharacter(char c) {
        return makeCharacter(c, false);
    }
    static final PyString makeCharacter(char c, boolean explicitUnicode) {
        if (explicitUnicode || c > 255) {
            return new PyUnicode(new Character(c).toString());
        }

        if (letters == null) {
            letters = new PyString[256];
            for(char j=0; j<256; j++) {
                letters[j] = new PyString(new Character(j).toString());
            }
        }
        return letters[c];
    }
    
    /**
     * Uses the PyObjectAdapter passed to {@link PySystemState#initialize} to turn o into a PyObject.
     * 
     * @see ClassicPyObjectAdapter - default PyObjectAdapter type
     */
	public static PyObject java2py(Object o) {
		return getAdapter().adapt(o);
	}
    
    /**
     * @return the ExtensiblePyObjectAdapter used by java2py.
     */
    public static ExtensiblePyObjectAdapter getAdapter(){
        if(adapter == null) {
            adapter = new ClassicPyObjectAdapter();
        }
        return adapter;
    }
	
    /**
     * Set the ExtensiblePyObjectAdapter used by java2py.
     * 
     * @param adapter The new ExtensiblePyObjectAdapter
     */
    protected static void setAdapter(ExtensiblePyObjectAdapter adapter) {
        Py.adapter = adapter;
    }
    
	/**
	 * Handles wrapping Java objects in PyObject to expose them to jython.
	 */
	private static ExtensiblePyObjectAdapter adapter;

    public static PyObject makeClass(String name, PyObject[] bases,
                                     PyCode code, PyObject doc)
    {
        return makeClass(name, bases, code, doc, null, null);
    }

    public static PyObject makeClass(String name, PyObject[] bases,
                                     PyCode code, PyObject doc,
                                     PyObject[] closure_cells)
    {
        return makeClass(name, bases, code, doc, null, closure_cells);
    }


    public static PyObject makeClass(String name, PyObject[] bases,
                                     PyCode code, PyObject doc,
                                     Class proxyClass) {
        return makeClass(name, bases, code, doc, proxyClass, null);
    }


    private static Class[] pyClassCtrSignature = {
        String.class, PyTuple.class, PyObject.class, Class.class
    };

    static private final PyType CLASS_TYPE = PyType.fromClass(PyClass.class);
    

    public static PyObject makeClass(String name, PyObject[] bases,
                                     PyCode code, PyObject doc,
                                     Class proxyClass,
                                     PyObject[] closure_cells)
    {
        PyFrame frame = getFrame();
        PyObject globals = frame.f_globals;

        PyObject dict = code.call(Py.EmptyObjects, Py.NoKeywords,
                                  globals, Py.EmptyObjects,
                                  new PyTuple(closure_cells));
        if (doc != null && dict.__finditem__("__doc__") == null) {
            dict.__setitem__("__doc__", doc);
        }
        
        if(dict.__finditem__("__module__") == null) {
            PyObject module = frame.getglobal("__name__");
            if(module != null) {
                dict.__setitem__("__module__", module);
            }
        }

        PyObject metaclass;
        
        metaclass = dict.__finditem__("__metaclass__");
        
        if (metaclass == null) {
            if (bases.length != 0) {
                PyObject base = bases[0];       
                metaclass = base.__findattr__("__class__");
                if (metaclass == null) {
                    metaclass = base.getType();
                }
            } else {
                if (globals != null)
                    metaclass = globals.__finditem__("__metaclass__");                     
            }
        }
        if (metaclass == null
                || metaclass == CLASS_TYPE
                || (metaclass instanceof PyJavaClass && 
                        ((PyJavaClass)metaclass).proxyClass == Class.class)) {
            boolean more_general = false;
            for (int i = 0; i < bases.length; i++) {
                if (!(bases[i] instanceof PyClass)) {
                    metaclass = bases[i].getType();
                    more_general = true;
                    break;
                }
            }
            if (!more_general)
                return new PyClass(name, new PyTuple(bases), dict, proxyClass);
        }
        
        if (proxyClass != null) {
            throw Py.TypeError("the meta-class cannot handle java subclassing");
        }
 
        return metaclass.__call__(new PyString(name),new PyTuple(bases),dict);
    }

    private static int nameindex=0;
    public static synchronized String getName() {
        String name = "org.python.pycode._pyx"+nameindex;
        nameindex += 1;
        return name;
    }

    public static CompilerFlags getCompilerFlags() {
        return getCompilerFlags(0,false);
    }

    public static CompilerFlags getCompilerFlags(int flags,boolean dont_inherit) {
        CompilerFlags cflags = null;
        if (dont_inherit) {
            cflags = new CompilerFlags(flags);
        } else {
            PyFrame frame = Py.getFrame();
            if (frame!=null && frame.f_code != null) {
                cflags = new CompilerFlags(frame.f_code.co_flags|flags);
            }
        }
        return cflags;
    }

    // w/o compiler-flags

    public static PyObject compile(modType node, String filename) {
        return compile(node, getName(), filename);
    }

    public static PyObject compile(modType node, String name,
                                 String filename)
    {
        return compile(node, name, filename, true, false);
    }

    public static PyObject compile(modType node, String name,
                                 String filename,
                                 boolean linenumbers,
                                 boolean printResults)
    {
        return compile_flags(node, name, filename, linenumbers,
                             printResults, null);
    }

    public static PyObject compile(InputStream istream, String filename,
                                 String type)
    {
        return compile_flags(istream,filename,type,null);
    }

    // with compiler-flags

    public static PyObject compile_flags(modType node, String name,
                                 String filename,
                                 boolean linenumbers,
                                 boolean printResults,CompilerFlags cflags)
    {
        try {
            ByteArrayOutputStream ostream = new ByteArrayOutputStream();
            Module.compile(node, ostream, name, filename, linenumbers,
                    printResults, false, cflags);

            saveClassFile(name, ostream);

            return BytecodeLoader.makeCode(name, ostream.toByteArray(), filename);
        } catch (Throwable t) {
            throw parser.fixParseError(null, t, filename);
        }
    }

    public static PyObject compile_flags(InputStream istream, String filename,
                                 String type,CompilerFlags cflags)
    {
        if (cflags.only_ast) {
            org.python.antlr.ast.modType node = antlr.parse(istream, type, filename, cflags);
            return Py.java2py(node);
        }

        modType node = parser.parse(istream, type, filename, cflags);
        boolean printResults = false;
        if (type.equals("single"))
            printResults = true;
        return Py.compile_flags(node, getName(), filename, true, printResults,
                                cflags);
    }

    public static PyObject compile_flags(String data,
                                       String filename,
                                       String type,
                                       CompilerFlags cflags) {
        return Py.compile_flags(new ByteArrayInputStream(StringUtil.toBytes(data + "\n\n")),
                                filename,
                                type,
                                cflags);
    }

    public static PyObject compile_command_flags(String string,
                    String filename, String kind, CompilerFlags cflags,boolean stdprompt)
    {
        modType node = parser.partialParse(string + "\n", kind, filename,
                cflags, stdprompt);
    
        if (node == null)
            return Py.None;
        return Py.compile_flags(node, Py.getName(), filename, true, true,
                                cflags);
    }
    
    public static PyObject[] unpackSequence(PyObject o, int length) {
        if (o instanceof PyTuple) {
            PyTuple tup = (PyTuple)o;
            //System.err.println("unpack tuple");
            if (tup.__len__() == length)
                return tup.getArray();
            throw Py.ValueError("unpack tuple of wrong size");
        }

        PyObject[] ret = new PyObject[length];
        PyObject iter = o.__iter__();
        try {
            for (int i = 0; i < length; i++) {
                PyObject tmp = iter.__iternext__();
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

        if (iter.__iternext__() != null) {
            throw Py.ValueError("unpack sequence too long");
        }
        return ret;
    }

    public static PyObject iter(PyObject seq, String message) {
        try {
            return seq.__iter__();
        } catch (PyException exc) {
            if (Py.matchException(exc, Py.TypeError))
                throw Py.TypeError(message);
            throw exc;
        }
    }

    private static IdImpl idimpl = new IdImpl();

    public static long id(PyObject o) {
        return idimpl.id(o);
    }
    
    public static String idstr(PyObject o) {
        return idimpl.idstr(o);
    }
    
    public static long java_obj_id(Object o) {
        return idimpl.java_obj_id(o);
    }
    
    public static void printResult(PyObject ret) {
        Py.getThreadState().systemState.invoke("displayhook", ret);
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


    public static void saveClassFile(String name, ByteArrayOutputStream bytestream) {
        String dirname = Options.proxyDebugDirectory;
        if (dirname == null)
            return;

        byte[] bytes = bytestream.toByteArray();
        File dir = new File(dirname);
        File file = makeFilename(name, dir);
        new File(file.getParent()).mkdirs();
        try {
            FileOutputStream o = new FileOutputStream(file);
            o.write(bytes);
            o.close();
        } catch (Throwable t) { t.printStackTrace(); }
    }

    private static File makeFilename(String name, File dir) {
        int index = name.indexOf(".");
        if (index == -1)
            return new File(dir, name+".class");

        return makeFilename(name.substring(index+1, name.length()),
                            new File(dir, name.substring(0, index)));
    }
    
    private static boolean abstract_issubclass(PyObject derived,PyObject cls) {
        if (derived == cls)
            return true;
        PyObject bases = derived.__findattr__("__bases__");
        if (bases == null)
            return false;
        for (int i = 0; i < bases.__len__(); i++) {
            if (abstract_issubclass(bases.__getitem__(i),cls))
                return true;
        }
        return false;
    }

    public static boolean isInstance(PyObject obj, PyObject cls) {
        return isInstance(obj, cls, 0);
    }
    
    private static boolean isInstance(PyObject obj, PyObject cls, int recursionDepth){
        if (cls instanceof PyType) {
            PyType objtype = obj.getType();
            if (objtype == cls)
                return true;
            return objtype.isSubType((PyType) cls);
        } else if (cls instanceof PyClass) {
            if (!(obj instanceof PyInstance))
                return false;
            return ((PyClass) obj.fastGetClass()).isSubClass((PyClass) cls);
        } else if (cls.getClass() == PyTuple.class) {
            if(recursionDepth > Py.getSystemState().getrecursionlimit()) {
                throw Py.RuntimeError("nest level of tuple too deep");
            }
            for (int i = 0; i < cls.__len__(); i++) {
                if (isInstance(obj, cls.__getitem__(i), recursionDepth + 1)) {
                    return true;
                }
            }
            return false;
        } else {
            if (cls.__findattr__("__bases__") == null)
                throw Py.TypeError(
                    "isinstance() arg 2 must be a class, type,"
                        + " or tuple of classes and types");
            PyObject ocls = obj.__findattr__("__class__");
            if (ocls == null)
                return false;
            return abstract_issubclass(ocls, cls);
        }
    }
	
    
    public static boolean isSubClass(PyObject derived,PyObject cls) {
        return isSubClass(derived, cls, 0);
    }
     
    private static boolean isSubClass(PyObject derived,PyObject cls, int recursionDepth) {
        if (derived instanceof PyType && cls instanceof PyType) {
            if (derived == cls) return true;
            return ((PyType)derived).isSubType((PyType)cls);
        } else if (cls instanceof PyClass && derived instanceof PyClass) {
            return ((PyClass)derived).isSubClass((PyClass)cls);            
        } else if (cls.getClass() == PyTuple.class) {
            if(recursionDepth > Py.getSystemState().getrecursionlimit()) {
                throw Py.RuntimeError("nest level of tuple too deep");
            }
            for (int i = 0; i < cls.__len__(); i++) {
                if (isSubClass(derived, cls.__getitem__(i), recursionDepth + 1)) {
                    return true;
				}	
            }
            return false;
        } else {
            if (derived.__findattr__("__bases__") == null)
                throw Py.TypeError(
                    "issubclass() arg 1 must be a class");            
            if (cls.__findattr__("__bases__") == null)
                throw Py.TypeError(
                    "issubclass() arg 2 must be a class, type,"
                        + " or tuple of classes and types");
            return abstract_issubclass(derived,cls);
        }    
    }

    static PyObject[] make_array(PyObject o) {
        if (o instanceof PyTuple) {
            return ((PyTuple)o).getArray();
        }
        // Guess result size and allocate space.
        int n = 10;
        try {
            n = o.__len__();
        } catch (PyException exc) { }
    
        PyObjectArray objs = new PyObjectArray(n);
        for (PyObject item : o.asIterable()) {
            objs.add(item);
        }
        // Cut back if guess was too large.
        objs.trimToSize();
        return (PyObject[])objs.getArray();
    }    
    
}

/** @deprecated */
class FixedFileWrapper extends StdoutWrapper {
    private PyObject file;
    public FixedFileWrapper(PyObject file) {
        name = "fixed file";
        this.file = file;

        if (file instanceof PyJavaInstance) {
            Object tojava = file.__tojava__(OutputStream.class);
            if (tojava != null && tojava != Py.NoConversion) {
                this.file = new PyFile((OutputStream)tojava, "<java OutputStream>");
            }
        }
    }

    protected PyObject myFile() {
        return file;
    }
}

/**
 * A code object wrapper for a python function.
 */
class JavaCode extends PyCode {
    private PyObject func;

    public JavaCode(PyObject func) {
        this.func = func;
        if (func instanceof PyReflectedFunction)
            this.co_name = ((PyReflectedFunction) func).__name__;
    }

    public PyObject call(PyFrame frame, PyObject closure) {
        System.out.println("call #1");
        return Py.None;
    }

    public PyObject call(PyObject args[], String keywords[],
                         PyObject globals, PyObject[] defaults,
                         PyObject closure)
    {
        return func.__call__(args, keywords);
    }

    public PyObject call(PyObject self, PyObject args[], String keywords[],
                         PyObject globals, PyObject[] defaults,
                         PyObject closure)
    {
        return func.__call__(self, args, keywords);
    }

    public PyObject call(PyObject globals, PyObject[] defaults,
                         PyObject closure)
    {
        return func.__call__();
    }

    public PyObject call(PyObject arg1, PyObject globals,
                         PyObject[] defaults, PyObject closure)
    {
        return func.__call__(arg1);
    }

    public PyObject call(PyObject arg1, PyObject arg2, PyObject globals,
                         PyObject[] defaults, PyObject closure)
    {
        return func.__call__(arg1, arg2);
    }

    public PyObject call(PyObject arg1, PyObject arg2, PyObject arg3,
                         PyObject globals, PyObject[] defaults,
                         PyObject closure)
    {
        return func.__call__(arg1, arg2, arg3);
    }
}

/**
 * A function object wrapper for a java method which comply with the
 * PyArgsKeywordsCall standard.
 */
class JavaFunc extends PyObject {
     java.lang.reflect.Method method;

     public JavaFunc(java.lang.reflect.Method method) {
         this.method = method;
     }

     public PyObject __call__(PyObject[] args, String[] kws) {
          Object[] margs = new Object[] { args, kws };
          try {
              return Py.java2py(method.invoke(null, margs));
          } catch (Throwable t) {
              throw Py.JavaError(t);
          }
     }

    public PyObject _doget(PyObject container) {
        return _doget(container, null);
    }

    public PyObject _doget(PyObject container, PyObject wherefound) {
        if (container == null)
            return this;
        return new PyMethod(container, this, wherefound);
    }

    public boolean _doset(PyObject container) {
        throw Py.TypeError("java function not settable: "+method.getName());
    }
}
