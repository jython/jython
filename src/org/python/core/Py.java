// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import org.python.antlr.base.mod;
import org.python.core.adapter.ClassicPyObjectAdapter;
import org.python.core.adapter.ExtensiblePyObjectAdapter;
import org.python.modules.posix.PosixModule;
import org.python.util.Generic;

import com.google.common.base.CharMatcher;

import jline.console.UserInterruptException;
import jnr.constants.Constant;
import jnr.constants.platform.Errno;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import jnr.posix.util.Platform;

/** Builtin types that are used to setup PyObject.
 *
 * Resolve circular dependency with some laziness. */
class BootstrapTypesSingleton {
    private final Set<Class<?>> BOOTSTRAP_TYPES;
    private BootstrapTypesSingleton() {
        BOOTSTRAP_TYPES = Generic.set();
        BOOTSTRAP_TYPES.add(PyObject.class);
        BOOTSTRAP_TYPES.add(PyType.class);
        BOOTSTRAP_TYPES.add(PyBuiltinCallable.class);
        BOOTSTRAP_TYPES.add(PyDataDescr.class);
    }

    private static class LazyHolder {
        private static final BootstrapTypesSingleton INSTANCE = new BootstrapTypesSingleton();
    }

    public static Set<Class<?>> getInstance() {
        return LazyHolder.INSTANCE.BOOTSTRAP_TYPES;
    }
}

public final class Py {

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
            throw new StreamCorruptedException("unknown singleton: " + which);
        }
    }

    /* Holds the singleton None and Ellipsis objects */
    /** The singleton None Python object **/
    public final static PyObject None = new PyNone();
    /** The singleton Ellipsis Python object - written as ... when indexing */
    public final static PyObject Ellipsis = new PyEllipsis();
    /** The singleton NotImplemented Python object. Used in rich comparison */
    public final static PyObject NotImplemented = new PyNotImplemented();
    /** A zero-length array of Strings to pass to functions that
    don't have any keyword arguments **/
    public final static String[] NoKeywords = new String[0];
    /** A zero-length array of PyObject's to pass to functions that
    expect zero-arguments **/
    public final static PyObject[] EmptyObjects = new PyObject[0];
    /** A frozenset with zero elements **/
    public final static PyFrozenSet EmptyFrozenSet = new PyFrozenSet();
    /** A tuple with zero elements **/
    public final static PyTuple EmptyTuple = new PyTuple(Py.EmptyObjects);
    /** The Python integer 0 **/
    public final static PyInteger Zero = new PyInteger(0);
    /** The Python integer 1 **/
    public final static PyInteger One = new PyInteger(1);
    /** The Python boolean False **/
    public final static PyBoolean False = new PyBoolean(false);
    /** The Python boolean True **/
    public final static PyBoolean True = new PyBoolean(true);
    /** A zero-length Python byte string **/
    public final static PyString EmptyString = new PyString("");
    /** A zero-length Python Unicode string **/
    public final static PyUnicode EmptyUnicode = new PyUnicode("");
    /** A Python string containing '\n' **/
    public final static PyString Newline = new PyString("\n");
    /** A Python unicode string containing '\n' **/
    public final static PyUnicode UnicodeNewline = new PyUnicode("\n");
    /** A Python string containing ' ' **/
    public final static PyString Space = new PyString(" ");
    /** A Python unicode string containing ' ' **/
    public final static PyUnicode UnicodeSpace = new PyUnicode(" ");
    /** Set if the type object is dynamically allocated */
    public final static long TPFLAGS_HEAPTYPE = 1L << 9;
    /** Set if the type allows subclassing */
    public final static long TPFLAGS_BASETYPE = 1L << 10;
    /** Type is abstract and cannot be instantiated */
    public final static long TPFLAGS_IS_ABSTRACT = 1L << 20;


    /** A unique object to indicate no conversion is possible
    in __tojava__ methods **/
    public final static Object NoConversion = new PySingleton("Error");
    public static PyObject OSError;
    public static PyException OSError(String message) {
        return new PyException(Py.OSError, message);
    }

    public static PyException OSError(IOException ioe) {
        return fromIOException(ioe, Py.OSError);
    }

    public static PyException OSError(Constant errno) {
        int value = errno.intValue();
        PyObject args = new PyTuple(Py.newInteger(value), PosixModule.strerror(value));
        return new PyException(Py.OSError, args);
    }

    public static PyException OSError(Constant errno, PyObject filename) {
        int value = errno.intValue();
        // see https://github.com/jruby/jruby/commit/947c661e46683ea82f8016dde9d3fa597cd10e56
        // for rationale to do this mapping, but in a nutshell jnr-constants is automatically
        // generated from header files, so that's not the right place to do this mapping,
        // but for Posix compatibility reasons both CPython andCRuby do this mapping;
        // except CPython chooses EEXIST instead of CRuby's ENOENT
        if (Platform.IS_WINDOWS && (value == 20047 || value == Errno.ESRCH.intValue())) {
            value = Errno.EEXIST.intValue();
        }
        // Pass to strerror because jnr-constants currently lacks Errno descriptions on
        // Windows, and strerror falls back to Linux's
        PyObject args = new PyTuple(Py.newInteger(value), PosixModule.strerror(value), filename);
        return new PyException(Py.OSError, args);
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
    public static PyException KeyboardInterrupt(String message) {
        return new PyException(Py.KeyboardInterrupt, message);
    }
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

    public static PyException IOError(IOException ioe) {
        return fromIOException(ioe, Py.IOError);
    }

    public static PyException IOError(String message) {
        return new PyException(Py.IOError, message);
    }

    public static PyException IOError(Constant errno) {
        int value = errno.intValue();
        PyObject args = new PyTuple(Py.newInteger(value), PosixModule.strerror(value));
        return new PyException(Py.IOError, args);
    }

    public static PyException IOError(Constant errno, String filename) {
        return IOError(errno, Py.fileSystemEncode(filename));
    }

    public static PyException IOError(Constant errno, PyObject filename) {
        int value = errno.intValue();
        PyObject args = new PyTuple(Py.newInteger(value), PosixModule.strerror(value), filename);
        return new PyException(Py.IOError, args);
    }

    private static PyException fromIOException(IOException ioe, PyObject err) {
        String message = ioe.getMessage();
        if (message == null) {
            message = ioe.getClass().getName();
        }
        if (ioe instanceof FileNotFoundException) {
            PyTuple args = new PyTuple(Py.newInteger(Errno.ENOENT.intValue()),
                                       Py.newStringOrUnicode("File not found - " + message));
            return new PyException(err, args);
        }
        return new PyException(err, message);
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

    static void maybeSystemExit(PyException exc) {
        if (exc.match(Py.SystemExit)) {
            PyObject value = exc.value;
            if (PyException.isExceptionInstance(exc.value)) {
                value = value.__findattr__("code");
            }
            Py.getSystemState().callExitFunc();
            if (value instanceof PyInteger) {
                System.exit(((PyInteger) value).getValue());
            } else {
                if (value != Py.None) {
                    try {
                        Py.println(value);
                        System.exit(1);
                    } catch (Throwable t) {
                        // continue
                    }
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
                new PyUnicode(object),
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
    }

    public static PyException MemoryError(String message) {
        return new PyException(Py.MemoryError, message);
    }

    public static PyObject BufferError;
    public static PyException BufferError(String message) {
        return new PyException(Py.BufferError, message);
    }

    public static PyObject ArithmeticError;
    public static PyObject LookupError;
    public static PyObject StandardError;
    public static PyObject Exception;
    public static PyObject BaseException;

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
        warning(PendingDeprecationWarning, message);
    }
    public static PyObject SyntaxWarning;

    public static void SyntaxWarning(String message) {
        warning(SyntaxWarning, message);
    }
    public static PyObject RuntimeWarning;

    public static void RuntimeWarning(String message) {
        warning(RuntimeWarning, message);
    }
    public static PyObject FutureWarning;

    public static void FutureWarning(String message) {
        warning(FutureWarning, message);
    }

    public static PyObject ImportWarning;
    public static void ImportWarning(String message) {
        warning(ImportWarning, message);
    }

    public static PyObject UnicodeWarning;
    public static void UnicodeWarning(String message) {
        warning(UnicodeWarning, message);
    }

    public static PyObject BytesWarning;
    public static void BytesWarning(String message) {
        warning(BytesWarning, message);
    }

    public static void warnPy3k(String message) {
        warnPy3k(message, 1);
    }

    public static void warnPy3k(String message, int stacklevel) {
        if (Options.py3k_warning) {
            warning(DeprecationWarning, message, stacklevel);
        }
    }

    private static PyObject warnings_mod;

    private static PyObject importWarnings() {
        if (warnings_mod != null) {
            return warnings_mod;
        }
        PyObject mod;
        try {
            mod = __builtin__.__import__("warnings");
        } catch (PyException e) {
            if (e.match(ImportError)) {
                return null;
            }
            throw e;
        }
        warnings_mod = mod;
        return mod;
    }

    private static String warn_hcategory(PyObject category) {
        PyObject name = category.__findattr__("__name__");
        if (name != null) {
            return "[" + name + "]";
        }
        return "[warning]";
    }

    public static void warning(PyObject category, String message) {
        warning(category, message, 1);
    }

    public static void warning(PyObject category, String message, int stacklevel) {
        PyObject func = null;
        PyObject mod = importWarnings();
        if (mod != null) {
            func = mod.__getattr__("warn");
        }
        if (func == null) {
            System.err.println(warn_hcategory(category) + ": " + message);
            return;
        } else {
            func.__call__(Py.newString(message), category, Py.newInteger(stacklevel));
        }
    }

    public static void warning(PyObject category, String message,
            String filename, int lineno, String module,
            PyObject registry) {
        PyObject func = null;
        PyObject mod = importWarnings();
        if (mod != null) {
            func = mod.__getattr__("warn_explicit");
        }
        if (func == null) {
            System.err.println(filename + ":" + lineno + ":" +
                    warn_hcategory(category) + ": " + message);
            return;
        } else {
            func.__call__(new PyObject[]{
                Py.newString(message), category,
                Py.newString(filename), Py.newInteger(lineno),
                (module == null) ? Py.None : Py.newString(module),
                registry
            }, Py.NoKeywords);
        }
    }
    public static PyObject JavaError;

    public static PyException JavaError(Throwable t) {
        if (t instanceof PyException) {
            return (PyException) t;
        } else if (t instanceof InvocationTargetException) {
            return JavaError(((InvocationTargetException) t).getTargetException());
        } else if (t instanceof StackOverflowError) {
            return Py.RuntimeError("maximum recursion depth exceeded (Java StackOverflowError)");
        } else if (t instanceof OutOfMemoryError) {
            memory_error((OutOfMemoryError) t);
        } else if (t instanceof UserInterruptException) {
            return Py.KeyboardInterrupt("");
        }
        PyObject exc = PyJavaType.wrapJavaObject(t);
        PyException pyex = new PyException(exc.getType(), exc);
        // Set the cause to the original throwable to preserve
        // the exception chain.
        pyex.initCause(t);
        return pyex;
    }

    private Py() {
    }

    /**
    Convert a given <code>PyObject</code> to an instance of a Java class.
    Identical to <code>o.__tojava__(c)</code> except that it will
    raise a <code>TypeError</code> if the conversion fails.
    @param o the <code>PyObject</code> to convert.
    @param c the class to convert it to.
     **/
    public static <T> T tojava(PyObject o, Class<T> c) {
        Object obj = o.__tojava__(c);
        if (obj == Py.NoConversion) {
            throw Py.TypeError("can't convert " + o.__repr__() + " to " +
                    c.getName());
        }
        return (T)obj;
    }

    // ??pending: was @deprecated but is actually used by proxie code.
    // Can get rid of it?
    public static Object tojava(PyObject o, String s) {
        Class<?> c = findClass(s);
        if (c == null) {
            throw Py.TypeError("can't convert to: " + s);
        }
        return tojava(o, c); // prev:Class.forName
    }
    /* Helper functions for PyProxy's */

    /* Convenience methods to create new constants without using "new" */
    private final static PyInteger[] integerCache = new PyInteger[1000];

    static {
        for (int j = -100; j < 900; j++) {
            integerCache[j + 100] = new PyInteger(j);
        }
    }

    public static final PyInteger newInteger(int i) {
        if (i >= -100 && i < 900) {
            return integerCache[i + 100];
        } else {
            return new PyInteger(i);
        }
    }

    public static PyObject newInteger(long i) {
        if (i < Integer.MIN_VALUE || i > Integer.MAX_VALUE) {
            return new PyLong(i);
        } else {
            return newInteger((int) i);
        }
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
        return new PyFloat((double) v);
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

    /**
     * Return a {@link PyString} for the given Java <code>String</code>, if it can be represented as
     * US-ASCII, and a {@link PyUnicode} otherwise.
     *
     * @param s string content
     * @return <code>PyString</code> or <code>PyUnicode</code> according to content of
     *         <code>s</code>.
     */
    public static PyString newStringOrUnicode(String s) {
        return newStringOrUnicode(Py.EmptyString, s);
    }

    /**
     * Return a {@link PyString} for the given Java <code>String</code>, if it can be represented as
     * US-ASCII and if a preceding object is not a <code>PyUnicode</code>, and a {@link PyUnicode}
     * otherwise. In some contexts, we want the result to be a <code>PyUnicode</code> if some
     * preceding result is a <code>PyUnicode</code>.
     *
     * @param precedent string of which the type sets a precedent
     * @param s string content
     * @return <code>PyString</code> or <code>PyUnicode</code> according to content of
     *         <code>s</code>.
     */
    public static PyString newStringOrUnicode(PyObject precedent, String s) {
        if (!(precedent instanceof PyUnicode) && CharMatcher.ascii().matchesAllOf(s)) {
            return Py.newString(s);
        } else {
            return Py.newUnicode(s);
        }
    }

    public static PyString newStringUTF8(String s) {
        if (CharMatcher.ascii().matchesAllOf(s)) {
            // ascii of course is a subset of UTF-8
            return Py.newString(s);
        } else {
            return Py.newString(codecs.PyUnicode_EncodeUTF8(s, null));
        }
    }

    /**
     * Return a file name or path as Unicode (Java UTF-16 <code>String</code>), decoded if necessary
     * from a Python <code>bytes</code> object, using the file system encoding. In Jython, this
     * encoding is UTF-8, irrespective of the OS platform. This method is comparable with Python 3
     * <code>os.fsdecode</code>, but for Java use, in places such as the <code>os</code> module. If
     * the argument is not a <code>PyUnicode</code>, it will be decoded using the nominal Jython
     * file system encoding. If the argument <i>is</i> a <code>PyUnicode</code>, its
     * <code>String</code> is returned.
     *
     * @param filename as <code>bytes</code> to decode, or already as <code>unicode</code>
     * @return unicode version of path
     */
    public static String fileSystemDecode(PyString filename) {
        String s = filename.getString();
        if (filename instanceof PyUnicode || CharMatcher.ascii().matchesAllOf(s)) {
            // Already encoded or usable as ASCII
            return s;
        } else {
            // It's bytes, so must decode properly
            assert "utf-8".equals(PySystemState.FILE_SYSTEM_ENCODING.toString());
            return codecs.PyUnicode_DecodeUTF8(s, null);
        }
    }

    /**
     * As {@link #fileSystemDecode(PyString)} but raising <code>ValueError</code> if not a
     * <code>str</code> or <code>unicode</code>.
     *
     * @param filename as <code>bytes</code> to decode, or already as <code>unicode</code>
     * @return unicode version of the file name
     */
    public static String fileSystemDecode(PyObject filename) {
        if (filename instanceof PyString) {
            return fileSystemDecode((PyString)filename);
        } else {
            throw Py.TypeError(String.format("coercing to Unicode: need string, %s type found",
                    filename.getType().fastGetName()));
        }
    }

    /**
     * Return a PyString object we can use as a file name or file path in places where Python
     * expects a <code>bytes</code> (that is a <code>str</code>) object in the file system encoding.
     * In Jython, this encoding is UTF-8, irrespective of the OS platform.
     * <p>
     * This is subtly different from CPython's use of "file system encoding", which tracks the
     * platform's choice so that OS services may be called that have a bytes interface. Jython's
     * interaction with the OS occurs via Java using String arguments representing Unicode values,
     * so we have no need to match the encoding actually chosen by the platform (e.g. 'mbcs' on
     * Windows). Rather we need a nominal Jython file system encoding, for use where the standard
     * library forces byte paths on us (in Python 2). There is no reason for this choice to vary
     * with OS platform. Methods receiving paths as <code>bytes</code> will
     * {@link #fileSystemDecode(PyString)} them again for Java.
     *
     * @param filename as <code>unicode</code> to encode, or already as <code>bytes</code>
     * @return encoded bytes version of path
     */
    public static PyString fileSystemEncode(String filename) {
        if (CharMatcher.ascii().matchesAllOf(filename)) {
            // Just wrap it as US-ASCII is a subset of the file system encoding
            return Py.newString(filename);
        } else {
            // It's non just US-ASCII, so must encode properly
            assert "utf-8".equals(PySystemState.FILE_SYSTEM_ENCODING.toString());
            return Py.newString(codecs.PyUnicode_EncodeUTF8(filename, null));
        }
    }

    /**
     * Return a PyString object we can use as a file name or file path in places where Python
     * expects a <code>bytes</code> (that is, <code>str</code>) object in the file system encoding.
     * In Jython, this encoding is UTF-8, irrespective of the OS platform. This method is comparable
     * with Python 3 <code>os.fsencode</code>. If the argument is a PyString, it is returned
     * unchanged. If the argument is a PyUnicode, it is converted to a <code>bytes</code> using the
     * nominal Jython file system encoding.
     *
     * @param filename as <code>unicode</code> to encode, or already as <code>bytes</code>
     * @return encoded bytes version of path
     */
    public static PyString fileSystemEncode(PyString filename) {
        return (filename instanceof PyUnicode) ? fileSystemEncode(filename.getString()) : filename;
    }

    /**
     * Convert a <code>PyList</code> path to a list of Java <code>String</code> objects decoded from
     * the path elements to strings guaranteed usable in the Java API.
     *
     * @param path a Python search path
     * @return equivalent Java list
     */
    private static List<String> fileSystemDecode(PyList path) {
        List<String> list = new ArrayList<>(path.__len__());
        for (PyObject filename : path.getList()) {
            list.add(fileSystemDecode(filename));
        }
        return list;
    }

    public static PyStringMap newStringMap() {
        // enable lazy bootstrapping (see issue #1671)
        if (!PyType.hasBuilder(PyStringMap.class)) {
            BootstrapTypesSingleton.getInstance().add(PyStringMap.class);
        }
        return new PyStringMap();
    }

    public static PyUnicode newUnicode(char c) {
        return (PyUnicode) makeCharacter(c, true);
    }

    static PyObject newUnicode(int codepoint) {
        return makeCharacter(codepoint, true);
    }

    public static PyUnicode newUnicode(String s) {
        return new PyUnicode(s);
    }

    public static PyUnicode newUnicode(String s, boolean isBasic) {
        return new PyUnicode(s, isBasic);
    }

    public static PyBoolean newBoolean(boolean t) {
        return t ? Py.True : Py.False;
    }

    public static PyObject newDate(Date date) {
        if (date == null) {
            return Py.None;
        }
        PyObject datetimeModule = __builtin__.__import__("datetime");
        PyObject dateClass = datetimeModule.__getattr__("date");
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        return dateClass.__call__(newInteger(cal.get(Calendar.YEAR)),
                                  newInteger(cal.get(Calendar.MONTH) + 1),
                                  newInteger(cal.get(Calendar.DAY_OF_MONTH)));

    }

    public static PyObject newTime(Time time) {
        if (time == null) {
            return Py.None;
        }
        PyObject datetimeModule = __builtin__.__import__("datetime");
        PyObject timeClass = datetimeModule.__getattr__("time");
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        return timeClass.__call__(newInteger(cal.get(Calendar.HOUR_OF_DAY)),
                                  newInteger(cal.get(Calendar.MINUTE)),
                                  newInteger(cal.get(Calendar.SECOND)),
                                  newInteger(cal.get(Calendar.MILLISECOND) *
                                             1000));
    }

    public static PyObject newDatetime(Timestamp timestamp) {
        if (timestamp == null) {
            return Py.None;
        }
        PyObject datetimeModule = __builtin__.__import__("datetime");
        PyObject datetimeClass = datetimeModule.__getattr__("datetime");
        Calendar cal = Calendar.getInstance();
        cal.setTime(timestamp);
        return datetimeClass.__call__(new PyObject[] {
                                      newInteger(cal.get(Calendar.YEAR)),
                                      newInteger(cal.get(Calendar.MONTH) + 1),
                                      newInteger(cal.get(Calendar.DAY_OF_MONTH)),
                                      newInteger(cal.get(Calendar.HOUR_OF_DAY)),
                                      newInteger(cal.get(Calendar.MINUTE)),
                                      newInteger(cal.get(Calendar.SECOND)),
                                      newInteger(timestamp.getNanos() / 1000)});
    }

    public static PyObject newDecimal(String decimal) {
        if (decimal == null) {
            return Py.None;
        }
        PyObject decimalModule = __builtin__.__import__("decimal");
        PyObject decimalClass = decimalModule.__getattr__("Decimal");
        return decimalClass.__call__(newString(decimal));
    }

    public static PyCode newCode(int argcount, String varnames[],
            String filename, String name,
            boolean args, boolean keywords,
            PyFunctionTable funcs, int func_id,
            String[] cellvars, String[] freevars,
            int npurecell, int moreflags) {
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
            String[] cellvars, String[] freevars,
            int npurecell, int moreflags) {
        return new PyTableCode(argcount, varnames,
                filename, name, firstlineno, args, keywords,
                funcs, func_id, cellvars, freevars, npurecell,
                moreflags);
    }

    // --
    public static PyCode newCode(int argcount, String varnames[],
            String filename, String name,
            boolean args, boolean keywords,
            PyFunctionTable funcs, int func_id) {
        return new PyTableCode(argcount, varnames,
                filename, name, 0, args, keywords, funcs,
                func_id);
    }

    public static PyCode newCode(int argcount, String varnames[],
            String filename, String name,
            int firstlineno,
            boolean args, boolean keywords,
            PyFunctionTable funcs, int func_id) {
        return new PyTableCode(argcount, varnames,
                filename, name, firstlineno, args, keywords,
                funcs, func_id);
    }

    public static PyCode newJavaCode(Class<?> cls, String name) {
        return new JavaCode(newJavaFunc(cls, name));
    }

    public static PyObject newJavaFunc(Class<?> cls, String name) {
        try {
            Method m = cls.getMethod(name, new Class<?>[]{PyObject[].class, String[].class});
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

        BaseException = initExc("BaseException", exc, dict);
        Exception = initExc("Exception", exc, dict);
        SystemExit = initExc("SystemExit", exc, dict);
        StopIteration = initExc("StopIteration", exc, dict);
        GeneratorExit = initExc("GeneratorExit", exc, dict);
        StandardError = initExc("StandardError", exc, dict);
        KeyboardInterrupt = initExc("KeyboardInterrupt", exc, dict);
        ImportError = initExc("ImportError", exc, dict);
        EnvironmentError = initExc("EnvironmentError", exc, dict);
        IOError = initExc("IOError", exc, dict);
        OSError = initExc("OSError", exc, dict);
        EOFError = initExc("EOFError", exc, dict);
        RuntimeError = initExc("RuntimeError", exc, dict);
        NotImplementedError = initExc("NotImplementedError", exc, dict);
        NameError = initExc("NameError", exc, dict);
        UnboundLocalError = initExc("UnboundLocalError", exc, dict);
        AttributeError = initExc("AttributeError", exc, dict);

        SyntaxError = initExc("SyntaxError", exc, dict);
        IndentationError = initExc("IndentationError", exc, dict);
        TabError = initExc("TabError", exc, dict);
        TypeError = initExc("TypeError", exc, dict);
        AssertionError = initExc("AssertionError", exc, dict);
        LookupError = initExc("LookupError", exc, dict);
        IndexError = initExc("IndexError", exc, dict);
        KeyError = initExc("KeyError", exc, dict);
        ArithmeticError = initExc("ArithmeticError", exc, dict);
        OverflowError = initExc("OverflowError", exc, dict);
        ZeroDivisionError = initExc("ZeroDivisionError", exc, dict);
        FloatingPointError = initExc("FloatingPointError", exc, dict);
        ValueError = initExc("ValueError", exc, dict);
        UnicodeError = initExc("UnicodeError", exc, dict);
        UnicodeEncodeError = initExc("UnicodeEncodeError", exc, dict);
        UnicodeDecodeError = initExc("UnicodeDecodeError", exc, dict);
        UnicodeTranslateError = initExc("UnicodeTranslateError", exc, dict);
        ReferenceError = initExc("ReferenceError", exc, dict);
        SystemError = initExc("SystemError", exc, dict);
        MemoryError = initExc("MemoryError", exc, dict);
        BufferError = initExc("BufferError", exc, dict);
        Warning = initExc("Warning", exc, dict);
        UserWarning = initExc("UserWarning", exc, dict);
        DeprecationWarning = initExc("DeprecationWarning", exc, dict);
        PendingDeprecationWarning = initExc("PendingDeprecationWarning", exc, dict);
        SyntaxWarning = initExc("SyntaxWarning", exc, dict);
        RuntimeWarning = initExc("RuntimeWarning", exc, dict);
        FutureWarning = initExc("FutureWarning", exc, dict);
        ImportWarning = initExc("ImportWarning", exc, dict);
        UnicodeWarning = initExc("UnicodeWarning", exc, dict);
        BytesWarning = initExc("BytesWarning", exc, dict);

        // Pre-initialize the PyJavaClass for OutOfMemoryError so when we need
        // it it creating the pieces for it won't cause an additional out of
        // memory error.  Fix for bug #1654484
        PyType.fromClass(OutOfMemoryError.class);
    }
    public static volatile PySystemState defaultSystemState;
    // This is a hack to get initializations to work in proper order
    public static synchronized boolean initPython() {
        PySystemState.initialize();
        return true;
    }

    private static boolean syspathJavaLoaderRestricted = false;

    /**
     * Common code for findClass and findClassEx
     * @param name Name of the Java class to load and initialize
     * @param reason Reason for loading it, used for debugging. No debug output
     *               is generated if it is null
     * @return the loaded class
     * @throws ClassNotFoundException if the class wasn't found by the class loader
     */
    private static Class<?> findClassInternal(String name, String reason) throws ClassNotFoundException {
        ClassLoader classLoader = Py.getSystemState().getClassLoader();
        if (classLoader != null) {
            if (reason != null) {
                writeDebug("import", "trying " + name + " as " + reason +
                          " in sys.classLoader");
            }
            return loadAndInitClass(name, classLoader);
        }
        if (!syspathJavaLoaderRestricted) {
            try {
                classLoader = imp.getSyspathJavaLoader();
                if (classLoader != null && reason != null) {
                    writeDebug("import", "trying " + name + " as " + reason +
                            " in SysPathJavaLoader");
                }
            } catch (SecurityException e) {
                syspathJavaLoaderRestricted = true;
            }
        }
        if (syspathJavaLoaderRestricted) {
            classLoader = imp.getParentClassLoader();
            if (classLoader != null && reason != null) {
                writeDebug("import", "trying " + name + " as " + reason +
                        " in Jython's parent class loader");
            }
        }
        if (classLoader != null) {
            try {
                return loadAndInitClass(name, classLoader);
            } catch (ClassNotFoundException cnfe) {
                // let the default classloader try
                // XXX: by trying another classloader that may not be on a
                //      parent/child relationship with the Jython's parent
                //      classsloader we are risking some nasty class loading
                //      problems (such as having two incompatible copies for
                //      the same class that is itself a dependency of two
                //      classes loaded from these two different class loaders)
            }
        }
        if (reason != null) {
            writeDebug("import", "trying " + name + " as " + reason +
                       " in context class loader, for backwards compatibility");
        }
        return loadAndInitClass(name, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Tries to find a Java class.
     * @param name Name of the Java class.
     * @return The class, or null if it wasn't found
     */
    public static Class<?> findClass(String name) {
        try {
            return findClassInternal(name, null);
        } catch (ClassNotFoundException e) {
            //             e.printStackTrace();
            return null;
        } catch (IllegalArgumentException e) {
            //             e.printStackTrace();
            return null;
        } catch (NoClassDefFoundError e) {
            //             e.printStackTrace();
            return null;
        }
    }

    /**
     * Tries to find a Java class.
     *
     * Unless {@link #findClass(String)}, it raises a JavaError
     * if the class was found but there were problems loading it.
     * @param name Name of the Java class.
     * @param reason Reason for finding the class. Used for debugging messages.
     * @return The class, or null if it wasn't found
     * @throws JavaError wrapping LinkageErrors/IllegalArgumentExceptions
     * occurred when the class is found but can't be loaded.
     */
    public static Class<?> findClassEx(String name, String reason) {
        try {
            return findClassInternal(name, reason);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (IllegalArgumentException e) {
            throw JavaError(e);
        } catch (LinkageError e) {
            throw JavaError(e);
        }
    }

    // An alias to express intent (since boolean flags aren't exactly obvious).
    // We *need* to initialize classes on findClass/findClassEx, so that import
    // statements can trigger static initializers
    private static Class<?> loadAndInitClass(String name, ClassLoader loader) throws ClassNotFoundException {
        return Class.forName(name, true, loader);
    }


    public static void initProxy(PyProxy proxy, String module, String pyclass, Object[] args)
    {
        if (proxy._getPyInstance() != null) {
            return;
        }
        PyObject instance = (PyObject)(ThreadContext.initializingProxy.get()[0]);
        ThreadState ts = Py.getThreadState();
        if (instance != null) {
            if (JyAttribute.hasAttr(instance, JyAttribute.JAVA_PROXY_ATTR)) {
                throw Py.TypeError("Proxy instance reused");
            }
            JyAttribute.setAttr(instance, JyAttribute.JAVA_PROXY_ATTR, proxy);
            proxy._setPyInstance(instance);
            proxy._setPySystemState(ts.getSystemState());
            return;
        }

        // Ensure site-packages are available before attempting to import module.
        // This step enables supporting modern Python apps when using proxies
        // directly from Java (eg through clamp).
        importSiteIfSelected();

        PyObject mod = imp.importName(module.intern(), false);
        PyType pyc = (PyType)mod.__getattr__(pyclass.intern());


        PyObject[] pargs;
        if (args == null || args.length == 0) {
            pargs = Py.EmptyObjects;
        } else {
            pargs = Py.javas2pys(args);
        }
        instance = pyc.__call__(pargs);
        JyAttribute.setAttr(instance, JyAttribute.JAVA_PROXY_ATTR, proxy);
        proxy._setPyInstance(instance);
        proxy._setPySystemState(ts.getSystemState());
    }

    /**
     * Initializes a default PythonInterpreter and runs the code from
     * {@link PyRunnable#getMain} as __main__
     *
     * Called by the code generated in {@link org.python.compiler.Module#addMain()}
     */
    public static void runMain(PyRunnable main, String[] args) throws Exception {
        runMain(new PyRunnableBootstrap(main), args);
    }

    /**
     * Initializes a default PythonInterpreter and runs the code loaded from the
     * {@link CodeBootstrap} as __main__ Called by the code generated in
     * {@link org.python.compiler.Module#addMain()}
     */
    public static void runMain(CodeBootstrap main, String[] args)
            throws Exception {
        PySystemState.initialize(null, null, args, main.getClass().getClassLoader());
        try {
            imp.createFromCode("__main__", CodeLoader.loadCode(main));
        } catch (PyException e) {
            Py.getSystemState().callExitFunc();
            if (e.match(Py.SystemExit)) {
                return;
            }
            throw e;
        }
        Py.getSystemState().callExitFunc();
    }

    //XXX: this needs review to make sure we are cutting out all of the Java exceptions.
    private static String getStackTrace(Throwable javaError) {
        CharArrayWriter buf = new CharArrayWriter();
        javaError.printStackTrace(new PrintWriter(buf));

        String str = buf.toString();
        int index = -1;
        if (index == -1) {
            index = str.indexOf(
                    "at org.python.core.PyReflectedConstructor.__call__");
        }
        if (index == -1) {
            index = str.indexOf("at org.python.core.PyReflectedFunction.__call__");
        }
        if (index == -1) {
            index = str.indexOf(
                    "at org/python/core/PyReflectedConstructor.__call__");
        }
        if (index == -1) {
            index = str.indexOf("at org/python/core/PyReflectedFunction.__call__");
        }

        if (index != -1) {
            index = str.lastIndexOf("\n", index);
        }

        int index0 = str.indexOf("\n");

        if (index >= index0) {
            str = str.substring(index0 + 1, index + 1);
        }

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
            PyObject file) {
        StdoutWrapper stderr = Py.stderr;

        if (file != null) {
            stderr = new FixedFileWrapper(file);
        }

        if (Options.showJavaExceptions) {
            stderr.println("Java Traceback:");
            java.io.CharArrayWriter buf = new java.io.CharArrayWriter();
            if (t instanceof PyException) {
                ((PyException)t).super__printStackTrace(new java.io.PrintWriter(buf));
            } else {
                t.printStackTrace(new java.io.PrintWriter(buf));
            }
            stderr.print(buf.toString());
        }

        PyException exc = Py.JavaError(t);

        maybeSystemExit(exc);

        setException(exc, f);

        ThreadState ts = getThreadState();
        PySystemState sys = ts.getSystemState();
        sys.last_value = exc.value;
        sys.last_type = exc.type;
        sys.last_traceback = exc.traceback;

        PyObject exceptHook = sys.__findattr__("excepthook");
        if (exceptHook != null) {
            try {
                exceptHook.__call__(exc.type, exc.value, exc.traceback);
            } catch (PyException exc2) {
                exc2.normalize();
                flushLine();
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

    /**
     * Print the description of an exception as a big string. The arguments are closely equivalent
     * to the tuple returned by Python <code>sys.exc_info</code>, on standard error or a given
     * byte-oriented file. Compare with Python <code>traceback.print_exception</code>.
     *
     * @param type of exception
     * @param value the exception parameter (second argument to <code>raise</code>)
     * @param tb traceback of the call stack where the exception originally occurred
     * @param file to print encoded string to, or null meaning standard error
     */
    public static void displayException(PyObject type, PyObject value, PyObject tb, PyObject file) {

        // Output is to standard error, unless a file object has been given.
        StdoutWrapper stderr = Py.stderr;

        // As we format the exception in Unicode, we deal with encoding in this method
        String encoding, errors = codecs.REPLACE;

        if (file != null) {
            // Ostensibly writing to a file: assume file content encoding (file.encoding)
            stderr = new FixedFileWrapper(file);
            encoding = codecs.getDefaultEncoding();
        } else {
            // Not a file, assume we should encode for the console
            encoding = getAttr(Py.getSystemState().__stderr__, "encoding", null);
        }

        // But if the stream can tell us directly, of course we use that answer.
        encoding = getAttr(stderr.myFile(), "encoding", encoding);
        errors = getAttr(stderr.myFile(), "errors", errors);

        flushLine();

        // The creation of the report operates entirely in Java String (to support Unicode).
        try {
            // Be prepared for formatting or printing to fail
            PyString bytes = exceptionToBytes(type, value, tb, encoding, errors);
            stderr.print(bytes);
        } catch (Exception ex) {
            // Looks like that exception just won't convert or print
            value = Py.newString("<exception str() failed>");
            PyString bytes = exceptionToBytes(type, value, tb, encoding, errors);
            stderr.print(bytes);
        }
    }

    /** Get a String attribute from an object or a return a default. */
    private static String getAttr(PyObject target, String internedName, String def) {
        PyObject attr = target.__findattr__(internedName);
        if (attr == null) {
            return def;
        } else if (attr instanceof PyUnicode) {
            return ((PyUnicode)attr).getString();
        } else {
            return attr.__str__().getString();
        }
    }

    /**
     * Helper for {@link #displayException(PyObject, PyObject, PyObject, PyObject)}, falling back to
     * US-ASCII as the last resort encoding.
     */
    private static PyString exceptionToBytes(PyObject type, PyObject value, PyObject tb,
            String encoding, String errors) {
        String string = exceptionToString(type, value, tb);
        String bytes; // not UTF-16
        try {
            // Format the exception and stack-trace in all its glory
            bytes = codecs.encode(Py.newUnicode(string), encoding, errors);
        } catch (Exception ex) {
            // Sometimes a working codec is just too much to ask
            bytes = codecs.PyUnicode_EncodeASCII(string, string.length(), codecs.REPLACE);
        }
        return Py.newString(bytes);
    }

    /**
     * Format the description of an exception as a big string. The arguments are closely equivalent
     * to the tuple returned by Python <code>sys.exc_info</code>. Compare with Python
     * <code>traceback.format_exception</code>.
     *
     * @param type of exception
     * @param value the exception parameter (second argument to <code>raise</code>)
     * @param tb traceback of the call stack where the exception originally occurred
     * @return string representation of the traceback and exception
     */
    static String exceptionToString(PyObject type, PyObject value, PyObject tb) {

        // Compose the stack dump, syntax error, and actual exception in this buffer:
        StringBuilder buf;

        if (tb instanceof PyTraceback) {
            buf = new StringBuilder(((PyTraceback)tb).dumpStack());
        } else {
            buf = new StringBuilder();
        }

        if (__builtin__.isinstance(value, Py.SyntaxError)) {
            // The value part of the exception is a syntax error: first emit that.
            appendSyntaxError(buf, value);
            // Now supersede it with just the syntax error message for the next phase.
            value = value.__findattr__("msg");
            if (value == null) {
                value = Py.None;
            }
        }

        if (value.getJavaProxy() != null) {
            Object javaError = value.__tojava__(Throwable.class);
            if (javaError != null && javaError != Py.NoConversion) {
                // The value is some Java Throwable: append that too
                buf.append(getStackTrace((Throwable)javaError));
            }
        }

        // Formatting the value may raise UnicodeEncodeError: client must deal
        buf.append(formatException(type, value)).append('\n');
        return buf.toString();
    }

    /**
     * Helper to {@link #tracebackToString(PyObject, PyObject)} when the value in an exception turns
     * out to be a syntax error.
     */
    private static void appendSyntaxError(StringBuilder buf, PyObject value) {

        PyObject filename = value.__findattr__("filename");
        PyObject text = value.__findattr__("text");
        PyObject lineno = value.__findattr__("lineno");

        buf.append("  File \"");
        buf.append(filename == Py.None || filename == null ? "<string>" : filename.toString());
        buf.append("\", line ");
        buf.append(lineno == null ? Py.newString('0') : lineno);
        buf.append('\n');

        if (text != Py.None && text != null && text.__len__() != 0) {
            appendSyntaxErrorText(buf, value.__findattr__("offset").asInt(), text.toString());
        }
    }

    /**
     * Generate two lines showing where a SyntaxError was caused.
     *
     * @param buf to append with generated message text
     * @param offset the offset into text
     * @param text a source code line
     */
    private static void appendSyntaxErrorText(StringBuilder buf, int offset, String text) {
        if (offset >= 0) {
            if (offset > 0 && offset == text.length()) {
                offset--;
            }

            // Eat lines if the offset is on a subsequent line
            while (true) {
                int nl = text.indexOf("\n");
                if (nl == -1 || nl >= offset) {
                    break;
                }
                offset -= nl + 1;
                text = text.substring(nl + 1, text.length());
            }

            // lstrip
            int i = 0;
            for (; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c != ' ' && c != '\t') {
                    break;
                }
                offset--;
            }
            text = text.substring(i, text.length());
        }

        buf.append("    ");
        buf.append(text);
        if (text.length() == 0 || !text.endsWith("\n")) {
            buf.append('\n');
        }
        if (offset == -1) {
            return;
        }

        // The indicator line "        ^"
        buf.append("    ");
        for (offset--; offset > 0; offset--) {
            buf.append(' ');
        }
        buf.append("^\n");
    }

    public static String formatException(PyObject type, PyObject value) {
        return formatException(type, value, false);
    }

    public static String formatException(PyObject type, PyObject value, boolean useRepr) {
        StringBuilder buf = new StringBuilder();

        if (PyException.isExceptionClass(type)) {
            String className = PyException.exceptionClassName(type);
            int lastDot = className.lastIndexOf('.');
            if (lastDot != -1) {
                className = className.substring(lastDot + 1);
            }
            PyObject moduleName = type.__findattr__("__module__");
            if (moduleName == null) {
                buf.append("<unknown>");
            } else {
                String moduleStr = moduleName.toString();
                if (!moduleStr.equals("exceptions")) {
                    buf.append(moduleStr);
                    buf.append(".");
                }
            }
            buf.append(className);
        } else {
            // Never happens since Python 2.7? Do something sensible anyway.
            buf.append(asMessageString(type, useRepr));
        }

        if (value != null && value != Py.None) {
            String s = asMessageString(value, useRepr);
            // Print colon and object (unless it renders as "")
            if (s.length() > 0) {
                buf.append(": ").append(s);
            }
        }

        return buf.toString();
    }

    /** Defensive method to avoid exceptions from decoding (or import encodings) */
    private static String asMessageString(PyObject value, boolean useRepr) {
        if (useRepr) {
            value = value.__repr__();
        }
        if (value instanceof PyUnicode) {
            return value.asString();
        } else {
            return value.__str__().getString();
        }
    }

    public static void writeUnraisable(Throwable unraisable, PyObject obj) {
        PyException pye = JavaError(unraisable);
        stderr.println(String.format("Exception %s in %s ignored",
                                     formatException(pye.type, pye.value, true), obj));
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
        Py.JavaError(t).tracebackHere(frame, true);
    }

    /* Helpers to implement except clauses */
    public static PyException setException(Throwable t, PyFrame frame) {
        PyException pye = Py.JavaError(t);
        pye.normalize();
        pye.tracebackHere(frame);
        getThreadState().exception = pye;
        return pye;
    }


    /**
     * @deprecated As of Jython 2.5, use {@link PyException#match} instead.
     */
    @Deprecated
    public static boolean matchException(PyException pye, PyObject exc) {
        return pye.match(exc);
    }


    // XXX: the following 4 are backwards compat. for the
    // oldcompiler. newcompiler should just call doRaise instead
    public static PyException makeException(PyObject type, PyObject value,
                                            PyObject traceback) {
        return PyException.doRaise(type, value, traceback);
    }

    public static PyException makeException(PyObject type, PyObject value) {
        return makeException(type, value, null);
    }

    public static PyException makeException(PyObject type) {
        return makeException(type, null);
    }

    public static PyException makeException() {
        return makeException(null);
    }

    public static PyObject runCode(PyCode code, PyObject locals, PyObject globals) {
        PyFrame f;
        ThreadState ts = getThreadState();
        if (locals == null || locals == Py.None) {
            if (globals != null && globals != Py.None) {
                locals = globals;
            } else {
                locals = ts.frame.getLocals();
            }
        }

        if (globals == null || globals == Py.None) {
            globals = ts.frame.f_globals;
        } else if (globals.__finditem__("__builtins__") == null) {
            // Apply side effect of copying into globals,
            // per documentation of eval and observed behavior of exec
            try {
                globals.__setitem__("__builtins__", Py.getSystemState().modules.__finditem__("__builtin__").__getattr__("__dict__"));
            } catch (PyException e) {
                // Quietly ignore if cannot set __builtins__ - Jython previously allowed a much wider range of
                // mappable objects for the globals mapping than CPython, do not want to break existing code
                // as we try to get better CPython compliance
                if (!e.match(AttributeError)) {
                    throw e;
                }
            }
        }

        PyBaseCode baseCode = null;
        if (code instanceof PyBaseCode) {
            baseCode = (PyBaseCode) code;
        }

        f = new PyFrame(baseCode, locals, globals, Py.getSystemState().getBuiltins());
        return code.call(ts, f);
    }

    public static void exec(PyObject o, PyObject globals, PyObject locals) {
        PyCode code;
        int flags = 0;
        if (o instanceof PyTuple) {
            PyTuple tuple = (PyTuple) o;
            int len = tuple.__len__();
            if ((globals == null || globals.equals(None))
                    && (locals == null || locals.equals(None))
                    && (len >= 2 && len <= 3)) {
                o = tuple.__getitem__(0);
                globals = tuple.__getitem__(1);
                if (len == 3) {
                    locals = tuple.__getitem__(2);
                }
            }
        }
        if (o instanceof PyCode) {
            code = (PyCode) o;
            if (locals == null && o instanceof PyBaseCode && ((PyBaseCode) o).hasFreevars()) {
                throw Py.TypeError("code object passed to exec may not contain free variables");
            }
        } else {
            String contents = null;
            if (o instanceof PyString) {
                if (o instanceof PyUnicode) {
                    flags |= CompilerFlags.PyCF_SOURCE_IS_UTF8;
                }
                contents = o.toString();
            } else if (o instanceof PyFile) {
                PyFile fp = (PyFile) o;
                if (fp.getClosed()) {
                    return;
                }
                contents = fp.read().toString();
            } else {
                throw Py.TypeError(
                        "exec: argument 1 must be string, code or file object");
            }
            code = Py.compile_flags(contents, "<string>", CompileMode.exec,
                                    getCompilerFlags(flags, false));
        }
        Py.runCode(code, locals, globals);
    }

    private final static ThreadStateMapping threadStateMapping = new ThreadStateMapping();

    public static final ThreadState getThreadState() {
        return getThreadState(null);
    }

    public static final ThreadState getThreadState(PySystemState newSystemState) {
        return threadStateMapping.getThreadState(newSystemState);
    }

    public static final PySystemState setSystemState(PySystemState newSystemState) {
        ThreadState ts = getThreadState(newSystemState);
        PySystemState oldSystemState = ts.getSystemState();
        if (oldSystemState != newSystemState) {
            //XXX: should we make this a real warning?
            //System.err.println("Warning: changing systemState "+
            //                   "for same thread!");
            ts.setSystemState(newSystemState);
        }
        return oldSystemState;
    }

    public static final PySystemState getSystemState() {
        return getThreadState().getSystemState();
    }

    /* Get and set the current frame */
    public static PyFrame getFrame() {
        ThreadState ts = getThreadState();
        if (ts == null) {
            return null;
        }
        return ts.frame;
    }

    public static void setFrame(PyFrame f) {
        getThreadState().frame = f;
    }

    /**
     * The handler for interactive consoles, set by {@link #installConsole(Console)} and accessed by
     * {@link #getConsole()}.
     */
    private static Console console;

    /**
     * Get the Jython Console (used for <code>input()</code>, <code>raw_input()</code>, etc.) as
     * constructed and set by {@link PySystemState} initialization.
     *
     * @return the Jython Console
     */
    public static Console getConsole() {
        if (console == null) {
            // We really shouldn't ask for a console before PySystemState initialization but ...
            try {
                // ... something foolproof that we can supersede.
                installConsole(new PlainConsole("ascii"));
            } catch (Exception e) {
                // This really, really shouldn't happen
                throw Py.RuntimeError("Could not create fall-back PlainConsole: " + e);
            }
        }
        return console;
    }

    /**
     * Install the provided Console, first uninstalling any current one. The Jython Console is used
     * for <code>raw_input()</code> etc., and may provide line-editing and history recall at the
     * prompt. A Console may replace <code>System.in</code> with its line-editing input method.
     *
     * @param console The new Console object
     * @throws UnsupportedOperationException if some prior Console refuses to uninstall
     * @throws IOException if {@link Console#install()} raises it
     */
    public static void installConsole(Console console) throws UnsupportedOperationException,
            IOException {
        if (Py.console != null) {
            // Some Console class already installed: may be able to uninstall
            Py.console.uninstall();
            Py.console = null;
        }

        // Install the specified Console
        console.install();
        Py.console = console;

        // Cause sys (if it exists) to export the console handler that was installed
        if (Py.defaultSystemState != null) {
            Py.defaultSystemState.__setattr__("_jy_console", Py.java2py(console));
        }
    }

    /**
     * Check (using the {@link POSIX} library and <code>jnr-posix</code> library) whether we are in
     * an interactive environment. Amongst other things, this affects the type of console that may
     * be legitimately installed during system initialisation. Note that the result may vary
     * according to whether a <code>jnr-posix</code> native library is found along
     * <code>java.library.path</code>, or the pure Java fall-back is used.
     *
     * @return true if (we think) we are in an interactive environment
     */
    public static boolean isInteractive() {
        // python.launcher.tty is authoratative; see http://bugs.jython.org/issue2325
        String isTTY = System.getProperty("python.launcher.tty");
        if (isTTY != null && isTTY.equals("true")) {
            return true;
        }
        if (isTTY != null && isTTY.equals("false")) {
            return false;
        }
        // Decide if System.in is interactive
        try {
            POSIX posix = POSIXFactory.getPOSIX();
            FileDescriptor in = FileDescriptor.in;
            return posix.isatty(in);
        } catch (SecurityException ex) {
            return false;
        }
    }

    private static final String IMPORT_SITE_ERROR = ""
            + "Cannot import site module and its dependencies: %s\n"
            + "Determine if the following attributes are correct:\n" //
            + "  * sys.path: %s\n"
            + "    This attribute might be including the wrong directories, such as from CPython\n"
            + "  * sys.prefix: %s\n"
            + "    This attribute is set by the system property python.home, although it can\n"
            + "    be often automatically determined by the location of the Jython jar file\n\n"
            + "You can use the -S option or python.import.site=false to not import the site module";

    public static boolean importSiteIfSelected() {
        if (Options.importSite) {
            try {
                // Ensure site-packages are available
                imp.load("site");
                return true;
            } catch (PyException pye) {
                if (pye.match(Py.ImportError)) {
                    PySystemState sys = Py.getSystemState();
                    String value = pye.value.__getattr__("args").__getitem__(0).toString();
                    List<String> path = fileSystemDecode(sys.path);
                    String prefix = fileSystemDecode(PySystemState.prefix);
                    throw Py.ImportError(String.format(IMPORT_SITE_ERROR, value, path, prefix));
                } else {
                    throw pye;
                }
            }
        }
        return false;
    }

    /* A collection of functions for implementing the print statement */
    public static StdoutWrapper stderr = new StderrWrapper();
    static StdoutWrapper stdout = new StdoutWrapper();

    //public static StdinWrapper stdin;
    public static void print(PyObject file, PyObject o) {
        if (file == None) {
            print(o);
        } else {
            new FixedFileWrapper(file).print(o);
        }
    }

    public static void printComma(PyObject file, PyObject o) {
        if (file == None) {
            printComma(o);
        } else {
            new FixedFileWrapper(file).printComma(o);
        }
    }

    public static void println(PyObject file, PyObject o) {
        if (file == None) {
            println(o);
        } else {
            new FixedFileWrapper(file).println(o);
        }
    }

    public static void printlnv(PyObject file) {
        if (file == None) {
            println();
        } else {
            new FixedFileWrapper(file).println();
        }
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

    public static void flushLine() {
        stdout.flushLine();
    }

    /*
     * A collection of convenience functions for converting PyObjects to Java primitives
     */
    public static boolean py2boolean(PyObject o) {
        return o.__nonzero__();
    }

    public static byte py2byte(PyObject o) {
        if (o instanceof PyInteger) {
            return (byte) ((PyInteger) o).getValue();
        }
        Object i = o.__tojava__(Byte.TYPE);
        if (i == null || i == Py.NoConversion) {
            throw Py.TypeError("integer required");
        }
        return ((Byte) i).byteValue();
    }

    public static short py2short(PyObject o) {
        if (o instanceof PyInteger) {
            return (short) ((PyInteger) o).getValue();
        }
        Object i = o.__tojava__(Short.TYPE);
        if (i == null || i == Py.NoConversion) {
            throw Py.TypeError("integer required");
        }
        return ((Short) i).shortValue();
    }

    public static int py2int(PyObject o) {
        return py2int(o, "integer required");
    }

    public static int py2int(PyObject o, String msg) {
        if (o instanceof PyInteger) {
            return ((PyInteger) o).getValue();
        }
        Object obj = o.__tojava__(Integer.TYPE);
        if (obj == Py.NoConversion) {
            throw Py.TypeError(msg);
        }
        return ((Integer) obj).intValue();
    }

    public static long py2long(PyObject o) {
        if (o instanceof PyInteger) {
            return ((PyInteger) o).getValue();
        }
        Object i = o.__tojava__(Long.TYPE);
        if (i == null || i == Py.NoConversion) {
            throw Py.TypeError("integer required");
        }
        return ((Long) i).longValue();
    }

    public static float py2float(PyObject o) {
        if (o instanceof PyFloat) {
            return (float) ((PyFloat) o).getValue();
        }
        if (o instanceof PyInteger) {
            return ((PyInteger) o).getValue();
        }
        Object i = o.__tojava__(Float.TYPE);
        if (i == null || i == Py.NoConversion) {
            throw Py.TypeError("float required");
        }
        return ((Float) i).floatValue();
    }

    public static double py2double(PyObject o) {
        if (o instanceof PyFloat) {
            return ((PyFloat) o).getValue();
        }
        if (o instanceof PyInteger) {
            return ((PyInteger) o).getValue();
        }
        Object i = o.__tojava__(Double.TYPE);
        if (i == null || i == Py.NoConversion) {
            throw Py.TypeError("float required");
        }
        return ((Double) i).doubleValue();
    }

    public static char py2char(PyObject o) {
        return py2char(o, "char required");
    }

    public static char py2char(PyObject o, String msg) {
        if (o instanceof PyString) {
            PyString s = (PyString) o;
            if (s.__len__() != 1) {
                throw Py.TypeError(msg);
            }
            return s.toString().charAt(0);
        }
        if (o instanceof PyInteger) {
            return (char) ((PyInteger) o).getValue();
        }
        Object i = o.__tojava__(Character.TYPE);
        if (i == null || i == Py.NoConversion) {
            throw Py.TypeError(msg);
        }
        return ((Character) i).charValue();
    }

    public static void py2void(PyObject o) {
        if (o != Py.None) {
            throw Py.TypeError("None required for void return");
        }
    }

    private final static PyString[] letters = new PyString[256];

    static {
        for (char j = 0; j < 256; j++) {
            letters[j] = new PyString(j);
        }
    }

    public static final PyString makeCharacter(Character o) {
        return makeCharacter(o.charValue());
    }

    public static final PyString makeCharacter(char c) {
        if (c <= 255) {
            return letters[c];
        } else {
            // This will throw IllegalArgumentException since non-byte value
            return new PyString(c);
        }
    }

    static final PyString makeCharacter(int codepoint, boolean toUnicode) {
        if (toUnicode) {
            return new PyUnicode(codepoint);
        } else if (codepoint < 0 || codepoint > 255) {
            // This will throw IllegalArgumentException since non-byte value
            return new PyString('\uffff');
        }
        return letters[codepoint];
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
     * Uses the PyObjectAdapter passed to {@link PySystemState#initialize} to turn
     * <code>objects</code> into an array of PyObjects.
     *
     * @see ClassicPyObjectAdapter - default PyObjectAdapter type
     */
    public static PyObject[] javas2pys(Object... objects) {
        PyObject[] objs = new PyObject[objects.length];
        for (int i = 0; i < objs.length; i++) {
            objs[i] = java2py(objects[i]);
        }
        return objs;
    }

    /**
     * @return the ExtensiblePyObjectAdapter used by java2py.
     */
    public static ExtensiblePyObjectAdapter getAdapter() {
        if (adapter == null) {
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

    // XXX: The following two makeClass overrides are *only* for the
    // old compiler, they should be removed when the newcompiler hits
    public static PyObject makeClass(String name, PyObject[] bases, PyCode code) {
        return makeClass(name, bases, code, null);
    }

    public static PyObject makeClass(String name, PyObject[] bases, PyCode code,
                                     PyObject[] closure_cells) {
        ThreadState state = getThreadState();
        PyObject dict = code.call(state, Py.EmptyObjects, Py.NoKeywords,
                state.frame.f_globals, Py.EmptyObjects, new PyTuple(closure_cells));
        return makeClass(name, bases, dict);
    }

    public static PyObject makeClass(String name, PyObject base, PyObject dict) {
        PyObject[] bases = base == null ? EmptyObjects : new PyObject[] {base};
        return makeClass(name, bases, dict);
    }

    /**
     * Create a new Python class.
     *
     * @param name the String name of the class
     * @param bases an array of PyObject base classes
     * @param dict the class's namespace, containing the class body
     * definition
     * @return a new Python Class PyObject
     */
    public static PyObject makeClass(String name, PyObject[] bases, PyObject dict) {
        PyObject metaclass = dict.__finditem__("__metaclass__");

        if (metaclass == null) {
            if (bases.length != 0) {
                PyObject base = bases[0];
                metaclass = base.__findattr__("__class__");
                if (metaclass == null) {
                    metaclass = base.getType();
                }
            } else {
                PyObject globals = getFrame().f_globals;
                if (globals != null) {
                    metaclass = globals.__finditem__("__metaclass__");
                }
                if (metaclass == null) {
                    metaclass = PyClass.TYPE;
                }
            }
        }

        try {
            return metaclass.__call__(new PyString(name), new PyTuple(bases), dict);
        } catch (PyException pye) {
            if (!pye.match(TypeError)) {
                throw pye;
            }
            pye.value = Py.newString(String.format("Error when calling the metaclass bases\n    "
                                                   + "%s", pye.value.__str__().toString()));
            throw pye;
        }
    }
    private static int nameindex = 0;

    public static synchronized String getName() {
        String name = "org.python.pycode._pyx" + nameindex;
        nameindex += 1;
        return name;
    }

    public static CompilerFlags getCompilerFlags() {
        return CompilerFlags.getCompilerFlags();
    }

    public static CompilerFlags getCompilerFlags(int flags, boolean dont_inherit) {
        final PyFrame frame;
        if (dont_inherit) {
            frame = null;
        } else {
            frame = Py.getFrame();
        }
        return CompilerFlags.getCompilerFlags(flags, frame);
    }

    public static CompilerFlags getCompilerFlags(CompilerFlags flags, boolean dont_inherit) {
        final PyFrame frame;
        if (dont_inherit) {
            frame = null;
        } else {
            frame = Py.getFrame();
        }
        return CompilerFlags.getCompilerFlags(flags, frame);
    }

    // w/o compiler-flags
    public static PyCode compile(InputStream istream, String filename, CompileMode kind) {
        return compile_flags(istream, filename, kind, new CompilerFlags());
    }

    /**
     * Entry point for compiling modules.
     *
     * @param node Module node, coming from the parsing process
     * @param name Internal name for the compiled code. Typically generated by
     *        calling {@link #getName()}.
     * @param filename Source file name
     * @param linenumbers True to track source line numbers on the generated
     *        code
     * @param printResults True to call the sys.displayhook on the result of
     *                     the code
     * @param cflags Compiler flags
     * @return Code object for the compiled module
     */
    public static PyCode compile_flags(mod node, String name, String filename,
                                         boolean linenumbers, boolean printResults,
                                         CompilerFlags cflags) {
        return CompilerFacade.compile(node, name, filename, linenumbers, printResults, cflags);
    }

    public static PyCode compile_flags(mod node, String filename,
                                         CompileMode kind, CompilerFlags cflags) {
        return Py.compile_flags(node, getName(), filename, true,
                                kind == CompileMode.single, cflags);
    }

    /**
     * Compiles python source code coming from a file or another external stream
     */
    public static PyCode compile_flags(InputStream istream, String filename,
                                         CompileMode kind, CompilerFlags cflags) {
        mod node = ParserFacade.parse(istream, kind, filename, cflags);
        return Py.compile_flags(node, filename, kind, cflags);
    }

    /**
     * Compiles python source code coming from String (raw bytes) data.
     *
     * If the String is properly decoded (from PyUnicode) the PyCF_SOURCE_IS_UTF8 flag
     * should be specified.
     */
    public static PyCode compile_flags(String data, String filename,
                                         CompileMode kind, CompilerFlags cflags) {
        if (data.contains("\0")) {
            throw Py.TypeError("compile() expected string without null bytes");
        }
        if (cflags != null && cflags.dont_imply_dedent) {
            data += "\n";
        } else {
            data += "\n\n";
        }
        mod node = ParserFacade.parse(data, kind, filename, cflags);
        return Py.compile_flags(node, filename, kind, cflags);
    }

    public static PyObject compile_command_flags(String string, String filename,
            CompileMode kind, CompilerFlags cflags, boolean stdprompt) {
        mod node = ParserFacade.partialParse(string + "\n", kind, filename,
                                                 cflags, stdprompt);
        if (node == null) {
            return Py.None;
        }

        return Py.compile_flags(node, Py.getName(), filename, true, true, cflags);
    }

    public static PyObject[] unpackSequence(PyObject obj, int length) {
        if (obj instanceof PyTuple && obj.__len__() == length) {
            // optimization
            return ((PyTuple)obj).getArray();
        }

        PyObject[] ret = new PyObject[length];
        PyObject iter = obj.__iter__();
        for (int i = 0; i < length; i++) {
            PyObject tmp = iter.__iternext__();
            if (tmp == null) {
                throw Py.ValueError(String.format("need more than %d value%s to unpack", i,
                                                  i == 1 ? "" : "s"));
            }
            ret[i] = tmp;
        }

        if (iter.__iternext__() != null) {
            throw Py.ValueError("too many values to unpack");
        }
        return ret;
    }

    public static PyObject iter(PyObject seq, String message) {
        try {
            return seq.__iter__();
        } catch (PyException exc) {
            if (exc.match(Py.TypeError)) {
                throw Py.TypeError(message);
            }
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
        Py.getThreadState().getSystemState().invoke("displayhook", ret);
    }
    public static final int ERROR = -1;
    public static final int WARNING = 0;
    public static final int MESSAGE = 1;
    public static final int COMMENT = 2;
    public static final int DEBUG = 3;

    public static void maybeWrite(String type, String msg, int level) {
        if (level <= Options.verbose) {
            System.err.println(type + ": " + msg);
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
        if (dirname == null) {
            return;
        }

        byte[] bytes = bytestream.toByteArray();
        File dir = new File(dirname);
        File file = makeFilename(name, dir);
        new File(file.getParent()).mkdirs();
        try {
            FileOutputStream o = new FileOutputStream(file);
            o.write(bytes);
            o.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static File makeFilename(String name, File dir) {
        int index = name.indexOf(".");
        if (index == -1) {
            return new File(dir, name + ".class");
        }

        return makeFilename(name.substring(index + 1, name.length()),
                new File(dir, name.substring(0, index)));
    }

    public static boolean isInstance(PyObject inst, PyObject cls) {
        // Quick test for an exact match
        if (inst.getType() == cls) {
            return true;
        }

        if (cls instanceof PyTuple) {
            for (PyObject item : cls.asIterable()) {
                if (isInstance(inst, item)) {
                    return true;
                }
            }
            return false;
        }

        PyObject checkerResult;
        if ((checkerResult = dispatchToChecker(inst, cls, "__instancecheck__")) != null) {
            return checkerResult.__nonzero__();
        }

        return recursiveIsInstance(inst, cls);
    }

    static boolean recursiveIsInstance(PyObject inst, PyObject cls) {
        if (cls instanceof PyClass && inst instanceof PyInstance) {
            PyClass inClass = ((PyInstance) inst).fastGetClass();
            return inClass.isSubClass((PyClass) cls);
        }
        if (cls instanceof PyType) {
            PyType type = (PyType)cls;

            //Special case PyStringMap to compare as an instance type dict.
            if (inst instanceof PyStringMap &&
                type.equals(PyDictionary.TYPE)) {
                    return true;
            }

            PyType instType = inst.getType();

            // equiv. to PyObject_TypeCheck
            if (instType == type || instType.isSubType(type)) {
                return true;
            }

            PyObject instCls = inst.__findattr__("__class__");
            if (instCls != null && instCls != instType && instCls instanceof PyType) {
                return ((PyType) instCls).isSubType(type);
            }
            return false;
        }

        checkClass(cls, "isinstance() arg 2 must be a class, type, or tuple of classes and types");
        PyObject instCls = inst.__findattr__("__class__");
        if (instCls == null) {
            return false;
        }
        return abstractIsSubClass(instCls, cls);
    }

    public static boolean isSubClass(PyObject derived, PyObject cls) {
        if (cls instanceof PyTuple) {
            for (PyObject item : cls.asIterable()) {
                if (isSubClass(derived, item)) {
                    return true;
                }
            }
            return false;
        }

        PyObject checkerResult;
        if ((checkerResult = dispatchToChecker(derived, cls, "__subclasscheck__")) != null) {
            return checkerResult.__nonzero__();
        }

        return recursiveIsSubClass(derived, cls);
    }

    static boolean recursiveIsSubClass(PyObject derived, PyObject cls) {
        if (derived instanceof PyType && cls instanceof PyType) {
            if (derived == cls) {
                return true;
            }
            PyType type = (PyType)cls;
            PyType subtype = (PyType)derived;

            // Special case PyStringMap to compare as a subclass of
            // PyDictionary. Note that we don't need to check for stringmap
            // subclasses, since stringmap can't be subclassed. PyStringMap's
            // TYPE is computed lazily, so we have to use PyType.fromClass :(
            if (type == PyDictionary.TYPE &&
                subtype == PyType.fromClass(PyStringMap.class)) {
                return true;
            }

            return subtype.isSubType(type);
        }
        if (derived instanceof PyClass && cls instanceof PyClass) {
            return ((PyClass) derived).isSubClass((PyClass) cls);
        }

        checkClass(derived, "issubclass() arg 1 must be a class");
        checkClass(cls, "issubclass() arg 2 must be a class or tuple of classes");
        return abstractIsSubClass(derived, cls);
    }

    private static boolean abstractIsSubClass(PyObject derived, PyObject cls) {
        while (true) {
            if (derived == cls) {
                return true;
            }

            PyTuple bases = abstractGetBases(derived);
            if (bases == null) {
                return false;
            }

            int basesSize = bases.size();
            if (basesSize == 0) {
                return false;
            }
            if (basesSize == 1) {
                // Avoid recursivity in the single inheritance case
                derived = bases.pyget(0);
                continue;
            }

            for (PyObject base : bases.asIterable()) {
                if (abstractIsSubClass(base, cls)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Attempt to dispatch an isinstance/issubclass call to cls's associated
     * __instancecheck__/__subclasscheck__.
     *
     * @param checkerArg the argument to call the checker with
     * @param cls a Python class
     * @param checkerName the checker name
     * @return null if cls provides no checker, otherwise the result of calling the
     * checker
     */
    private static PyObject dispatchToChecker(PyObject checkerArg, PyObject cls,
            String checkerName) {
        //Ignore old style classes.
        if (cls instanceof PyClass) {
            return null;
        }
        /* Here we would actually like to call cls.__findattr__("__metaclass__")
         * rather than cls.getType(). However there are circumstances where the
         * metaclass doesn't show up as __metaclass__. On the other hand we need
         * to avoid that checker refers to builtin type___subclasscheck__ or
         * type___instancecheck__. Filtering out checker-instances of
         * PyBuiltinMethodNarrow does the trick. We also filter out PyMethodDescr
         * to shortcut some unnecessary looping.
         */
        PyObject checker = cls.getType().__findattr__(checkerName);
        if (checker == null || checker instanceof PyMethodDescr ||
                checker instanceof PyBuiltinMethodNarrow) {
            return null;
        }
        return checker.__call__(cls, checkerArg);
    }

    /**
     * Return the __bases__ of cls. Returns null if no valid __bases__ are found.
     */
    private static PyTuple abstractGetBases(PyObject cls) {
        PyObject bases = cls.__findattr__("__bases__");
        return bases instanceof PyTuple ? (PyTuple) bases : null;
    }

    /**
     * Throw a TypeError with the specified message if cls does not appear to be a Python
     * class.
     */
    private static void checkClass(PyObject cls, String message) {
        if (abstractGetBases(cls) == null) {
            throw Py.TypeError(message);
        }
    }

    static PyObject[] make_array(PyObject iterable) {
        // Special-case the common tuple and list cases, for efficiency
        if (iterable instanceof PySequenceList) {
            return ((PySequenceList) iterable).getArray();
        }

        // Guess result size and allocate space. The typical make_array arg supports
        // __len__, with one exception being generators, so avoid the overhead of an
        // exception from __len__ in their case
        int n = 10;
        if (!(iterable instanceof PyGenerator)) {
            try {
                n = iterable.__len__();
            } catch (PyException pye) {
                // ok
            }
        }

        List<PyObject> objs = new ArrayList<PyObject>(n);
        for (PyObject item : iterable.asIterable()) {
            objs.add(item);
        }
        return objs.toArray(Py.EmptyObjects);
    }

    /**
     * Infers the usual Jython executable name from the position of the
     * jar-file returned by {@link #getJarFileName()} by replacing the
     * file name with "bin/jython". This is intended as an easy fallback
     * for cases where {@code sys.executable} is {@code None} due to
     * direct launching via the java executable.<br>
     * Note that this does not necessarily return the actual executable,
     * but instead infers the place where it is usually expected to be.
     * Use {@code sys.executable} to get the actual executable (may be
     * {@code None}.
     *
     * In contrast to {@link #getJarFileName()} and
     * {@link #getJarFileNameFromURL(java.net.URL)} this method returns
     * the path using system-specific separator characters.
     *
     * @return usual Jython-executable as absolute path
     */
    public static String getDefaultExecutableName() {
        return getDefaultBinDir()+File.separator+(
                Platform.IS_WINDOWS ? "jython.exe" : "jython");
    }

    /**
     * Infers the usual Jython bin-dir from the position of the jar-file
     * returned by {@link #getJarFileName()} byr replacing the file name
     * with "bin". This is intended as an easy fallback for cases where
     * {@code sys.executable} is {@code null} due to direct launching via
     * the java executable.<br>
     * Note that this does not necessarily return the actual bin-directory,
     * but instead infers the place where it is usually expected to be.
     *
     * In contrast to {@link #getJarFileName()} and
     * {@link #getJarFileNameFromURL(java.net.URL)} this method returns
     * the path using system-specific separator characters.
     *
     * @return usual Jython bin-dir as absolute path
     */
    public static String getDefaultBinDir() {
        String jar = _getJarFileName();
        if (File.separatorChar != '/') {
            jar = jar.replace('/', File.separatorChar);
        }
        return jar.substring(0, jar.lastIndexOf(File.separatorChar)+1)+"bin";
    }

    /**
     * Utility-method to obtain the name (including absolute path) of the currently used
     * jython-jar-file. Usually this is jython.jar, but can also be jython-dev.jar or
     * jython-standalone.jar or something custom.
     *
     * @return the full name of the jar file containing this class, <code>null</code>
     *         if not available.
     */
    public static String getJarFileName() {
        String jar = _getJarFileName();
        if (File.separatorChar != '/') {
            jar = jar.replace('/', File.separatorChar);
        }
        return jar;
    }

    /**
     * Utility-method to obtain the name (including absolute path) of the currently used
     * jython-jar-file. Usually this is jython.jar, but can also be jython-dev.jar or
     * jython-standalone.jar or something custom.
     *
     * Note that it does not use system-specific seperator-chars, but always '/'.
     *
     * @return the full name of the jar file containing this class, <code>null</code>
     *         if not available.
     */
    public static String _getJarFileName() {
        Class<Py> thisClass = Py.class;
        String fullClassName = thisClass.getName();
        String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
        URL url = thisClass.getResource(className + ".class");
        return getJarFileNameFromURL(url);
    }

    /**exclusively used by {@link #getJarFileNameFromURL(java.net.URL)}.*/
    private static final String JAR_URL_PREFIX = "jar:file:";
    /**exclusively used by {@link #getJarFileNameFromURL(java.net.URL)}.*/
    private static final String JAR_SEPARATOR = "!";
    /**exclusively used by {@link #getJarFileNameFromURL(java.net.URL)}.*/
    private static final String VFSZIP_PREFIX = "vfszip:";
    /**exclusively used by {@link #getJarFileNameFromURL(java.net.URL)}.*/
    private static final String VFS_PREFIX = "vfs:";

    /**
     * Converts a url that points to a jar-file to the actual jar-file name.
     * Note that it does not use system-specific seperator-chars, but always '/'.
     */
    public static String getJarFileNameFromURL(URL url) {
        String jarFileName = null;
        if (url != null) {
            try {
                // escape plus signs, since the URLDecoder would turn them into spaces
                final String plus = "\\+";
                final String escapedPlus = "__ppluss__";
                String rawUrl = url.toString();
                rawUrl = rawUrl.replaceAll(plus, escapedPlus);
                String urlString = URLDecoder.decode(rawUrl, "UTF-8");
                urlString = urlString.replaceAll(escapedPlus, plus);
                int jarSeparatorIndex = urlString.lastIndexOf(JAR_SEPARATOR);
                if (urlString.startsWith(JAR_URL_PREFIX) && jarSeparatorIndex > 0) {
                    // jar:file:/install_dir/jython.jar!/org/python/core/PySystemState.class
                    int start = JAR_URL_PREFIX.length();
                    if (Platform.IS_WINDOWS) {
                        start++;
                    }
                    jarFileName = urlString.substring(start, jarSeparatorIndex);
                } else if (urlString.startsWith(VFSZIP_PREFIX)) {
                    // vfszip:/some/path/jython.jar/org/python/core/PySystemState.class
                    final String path = Py.class.getName().replace('.', '/');
                    int jarIndex = urlString.indexOf(".jar/".concat(path));
                    if (jarIndex > 0) {
                        jarIndex += 4;
                        int start = VFSZIP_PREFIX.length();
                        if (Platform.IS_WINDOWS) {
                            // vfszip:/C:/some/path/jython.jar/org/python/core/PySystemState.class
                            start++;
                        }
                        jarFileName = urlString.substring(start, jarIndex);
                    }
                } else if (urlString.startsWith(VFS_PREFIX)) {
                    // vfs:/some/path/jython.jar/org/python/core/PySystemState.class
                    final String path = Py.class.getName().replace('.', '/');
                    int jarIndex = urlString.indexOf(".jar/".concat(path));
                    if (jarIndex > 0) {
                        jarIndex += 4;
                        int start = VFS_PREFIX.length();
                        if (Platform.IS_WINDOWS) {
                            // vfs:/C:/some/path/jython.jar/org/python/core/PySystemState.class
                            start++;
                        }
                        jarFileName = urlString.substring(start, jarIndex);
                    }
                }
            } catch (Exception e) {}
        }
        return jarFileName;
    }

//------------------------contructor-section---------------------------
    static class py2JyClassCacheItem {
        List<Class<?>> interfaces;
        List<PyObject> pyClasses;

        public py2JyClassCacheItem(Class<?> initClass, PyObject initPyClass) {
            if (!initClass.isInterface()) {
                throw
                    new IllegalArgumentException("cls must be an interface.");
            }
            interfaces = new ArrayList<>(1);
            pyClasses = new ArrayList<>(1);
            interfaces.add(initClass);
            pyClasses.add(initPyClass);
        }

        public PyObject get(Class<?> cls) {
            for (int i = 0; i < interfaces.size(); ++i) {
                if (cls.isAssignableFrom(interfaces.get(i))) {
                    return pyClasses.get(i);
                }
            }
            return null;
        }

        public void add(Class<?> cls, PyObject pyCls) {
            if (!cls.isInterface()) {
                throw
                    new IllegalArgumentException("cls must be an interface.");
            }
            interfaces.add(0, cls);
            pyClasses.add(0, pyCls);
            for (int i = interfaces.size()-1; i > 0; --i) {
                if (interfaces.get(i).isAssignableFrom(cls)) {
                    interfaces.remove(i);
                    pyClasses.remove(i);
                }
            }
        }
    }

    protected static PyObject ensureInterface(PyObject cls, Class<?> interfce) {
        PyObject pjc = PyType.fromClass(interfce);
        if (Py.isSubClass(cls, pjc)) {
            return cls;
        }
        PyObject[] bases = {cls, pjc};
        return Py.makeClass(interfce.getName(), bases, new PyStringMap());
    }

    /**
     * Returns a Python-class that extends {@code cls} and {@code interfce}.
     * If {@code cls} already extends {@code interfce}, simply {@code cls}
     * is returned. Otherwise a new class is created (if not yet cached).
     * It caches such classes and only creates a new one if no appropriate
     * class was cached yet.
     *
     * @return a Python-class that extends {@code cls} and {@code interfce}
     */
    public static synchronized PyObject javaPyClass(PyObject cls, Class<?> interfce) {
        py2JyClassCacheItem cacheItem = (py2JyClassCacheItem)
                JyAttribute.getAttr(cls, JyAttribute.PYCLASS_PY2JY_CACHE_ATTR);
        PyObject result;
        if (cacheItem == null) {
            result = ensureInterface(cls, interfce);
            cacheItem = new py2JyClassCacheItem(interfce, result);
            JyAttribute.setAttr(cls, JyAttribute.PYCLASS_PY2JY_CACHE_ATTR, cacheItem);
        } else {
            result = cacheItem.get(interfce);
            if (result == null) {
                result = ensureInterface(cls, interfce);
                cacheItem.add(interfce, result);
            }
        }
        return result;
    }

    /**
     * This method is a compact helper to access Python-constructors from Java.
     * It creates an instance of {@code cls} and retruns it in form of
     * {@code jcls}, which must be an interface. This method even works if
     * {@code cls} does not extend {@code jcls} in Python-code. In that case,
     * it uses {@link #javaPyClass(PyObject, Class)} to create an appropriate
     * class on the fly.<br>
     * It automatically converts {@code args} to {@link org.python.core.PyObject}s.<br>
     * For keyword-support use
     * {@link #newJ(PyObject, Class, String[], Object...)}.
     *
     * {@see #newJ(PyObject, Class, PyObject[], String[])}
     * {@see #newJ(PyObject, Class, String[], Object...)}
     * {@see #newJ(PyModule, Class, Object...)}
     * {@see #newJ(PyModule, Class, String[], Object...)}
     * {@see org.python.core.PyModule#newJ(Class, Object...)}
     * {@see org.python.core.PyModule#newJ(Class, String[], Object...)}
     *
     * @param cls - the class to be instanciated
     * @param jcls - the Java-type to be returned
     * @param args are automatically converted to Jython-PyObjects
     * @return an instance of cls in form of the interface jcls
     */
    @SuppressWarnings("unchecked")
    public static <T> T newJ(PyObject cls, Class<T> jcls, Object... args) {
        PyObject cls2 = javaPyClass(cls, jcls);
        PyObject resultPy = cls2.__call__(Py.javas2pys(args));
        return (T) resultPy.__tojava__(jcls);
    }

    /**
     * This method is a compact helper to access Python-constructors from Java.
     * It creates an instance of {@code cls} and retruns it in form of
     * {@code jcls}, which must be an interface. This method even works if
     * {@code cls} does not extend {@code jcls} in Python-code. In that case,
     * it uses {@link #javaPyClass(PyObject, Class)} to create an appropriate
     * class on the fly.<br>
     * {@code keywordss} are applied to the last {@code args} in the list.
     *
     * {@see #newJ(PyObject, Class, Object...)}
     * {@see #newJ(PyObject, Class, String[], Object...)}
     * {@see #newJ(PyModule, Class, Object...)}
     * {@see #newJ(PyModule, Class, String[], Object...)}
     * {@see org.python.core.PyModule#newJ(Class, Object...)}
     * {@see org.python.core.PyModule#newJ(Class, String[], Object...)}
     *
     * @param cls - the class to be instanciated
     * @param jcls - the Java-type to be returned
     * @param keywords are applied to the last args
     * @param args for the Python-class constructor
     * @return an instance of cls in form of the interface jcls
     */
    @SuppressWarnings("unchecked")
    public static <T> T newJ(PyObject cls, Class<T> jcls, PyObject[] args, String[] keywords) {
        PyObject cls2 = javaPyClass(cls, jcls);
        PyObject resultPy = cls2.__call__(args, keywords);
        return (T) resultPy.__tojava__(jcls);
    }

    /**
     * This method is a compact helper to access Python-constructors from Java.
     * It creates an instance of {@code cls} and retruns it in form of
     * {@code jcls}, which must be an interface. This method even works if
     * {@code cls} does not extend {@code jcls} in Python-code. In that case,
     * it uses {@link #javaPyClass(PyObject, Class)} to create an appropriate
     * class on the fly.<br>
     * It automatically converts {@code args} to {@link org.python.core.PyObject}s.<br>
     * {@code keywordss} are applied to the last {@code args} in the list.
     *
     * {@see #newJ(PyObject, Class, PyObject[], String[])}
     * {@see #newJ(PyObject, Class, Object...)}
     * {@see #newJ(PyModule, Class, Object...)}
     * {@see #newJ(PyModule, Class, String[], Object...)}
     * {@see org.python.core.PyModule#newJ(Class, Object...)}
     * {@see org.python.core.PyModule#newJ(Class, String[], Object...)}
     *
     * @param cls - the class to be instanciated
     * @param jcls - the Java-type to be returned
     * @param keywords are applied to the last args
     * @param args are automatically converted to Jython-PyObjects
     * @return an instance of cls in form of the interface jcls
     */
    @SuppressWarnings("unchecked")
    public static <T> T newJ(PyObject cls, Class<T> jcls, String[] keywords, Object... args) {
        PyObject cls2 = javaPyClass(cls, jcls);
        PyObject resultPy = cls2.__call__(Py.javas2pys(args), keywords);
        return (T) resultPy.__tojava__(jcls);
    }

    /**
     * Works like {@link #newJ(PyObject, Class, Object...)}, but looks
     * up the Python-class in the module-dict using the interface-name, i.e.
     * {@code jcls.getSimpleName()}.<br>
     * For keywords-support use {@link #newJ(PyModule, Class, String[], Object...)}.
     *
     * {@see #newJ(PyModule, Class, String[], Object...)}
     * {@see #newJ(PyObject, Class, PyObject[], String[])}
     * {@see #newJ(PyObject, Class, Object...)}
     * {@see #newJ(PyObject, Class, String[], Object...)}
     * {@see org.python.core.PyModule#newJ(Class, Object...)}
     * {@see org.python.core.PyModule#newJ(Class, String[], Object...)}
     *
     * @param module the module containing the desired class
     * @param jcls Java-type of the desired clas, must have the same name
     * @param args constructor-arguments
     * @return a new instance of the desired class
     */
    @SuppressWarnings("unchecked")
    public static <T> T newJ(PyModule module, Class<T> jcls, Object... args) {
        PyObject cls = module.__getattr__(jcls.getSimpleName().intern());
        return newJ(cls, jcls, args);
    }

    /**
     * Works like {@link #newJ(PyObject, Class, String[], Object...)}, but looks
     * up the Python-class in the module-dict using the interface-name, i.e.
     * {@code jcls.getSimpleName()}.<br>
     * {@code keywordss} are applied to the last {@code args} in the list.
     *
     * {@see #newJ(PyModule, Class, Object...)}
     * {@see #newJ(PyObject, Class, PyObject[], String[])}
     * {@see #newJ(PyObject, Class, Object...)}
     * {@see #newJ(PyObject, Class, String[], Object...)}
     * {@see org.python.core.PyModule#newJ(Class, Object...)}
     * {@see org.python.core.PyModule#newJ(Class, String[], Object...)}
     *
     * @param module the module containing the desired class
     * @param jcls Java-type of the desired class, must have the same name
     * @param keywords are applied to the last {@code args} in the list
     * @param args constructor-arguments
     * @return a new instance of the desired class
     */
    @SuppressWarnings("unchecked")
    public static <T> T newJ(PyModule module, Class<T> jcls, String[] keywords, Object... args) {
        PyObject cls = module.__getattr__(jcls.getSimpleName().intern());
        return newJ(cls, jcls, keywords, args);
    }
//----------------end of constructor-section------------------
}

class FixedFileWrapper extends StdoutWrapper {

    private PyObject file;

    public FixedFileWrapper(PyObject file) {
        name = "fixed file";
        this.file = file;

        if (file.getJavaProxy() != null) {
            Object tojava = file.__tojava__(OutputStream.class);
            if (tojava != null && tojava != Py.NoConversion) {
                this.file = new PyFile((OutputStream) tojava);
            }
        }
    }

    @Override
    protected PyObject myFile() {
        return file;
    }
}

/**
 * A code object wrapper for a python function.
 */
class JavaCode extends PyCode implements Traverseproc {

    private PyObject func;

    public JavaCode(PyObject func) {
        this.func = func;
        if (func instanceof PyReflectedFunction) {
            this.co_name = ((PyReflectedFunction) func).__name__;
        }
    }

    @Override
    public PyObject call(ThreadState state, PyFrame frame, PyObject closure) {
        /* This should actually
         *     throw new UnsupportedOperationException(
         *             "JavaCode doesn't support call with signature "+
         *             "(ThreadState state, PyFrame frame, PyObject closure).");
         * However since this would be an API-change, for 2.7 series we just warn.
         */
        Py.warning(Py.RuntimeWarning, "JavaCode doesn't support call with signature "+
                "(ThreadState state, PyFrame frame, PyObject closure).");
        return Py.None;
    }

    @Override
    public PyObject call(ThreadState state, PyObject args[], String keywords[],
            PyObject globals, PyObject[] defaults,
            PyObject closure) {
        return func.__call__(args, keywords);
    }

    @Override
    public PyObject call(ThreadState state, PyObject self, PyObject args[], String keywords[],
            PyObject globals, PyObject[] defaults,
            PyObject closure) {
        return func.__call__(self, args, keywords);
    }

    @Override
    public PyObject call(ThreadState state, PyObject globals, PyObject[] defaults,
            PyObject closure) {
        return func.__call__();
    }

    @Override
    public PyObject call(ThreadState state, PyObject arg1, PyObject globals,
            PyObject[] defaults, PyObject closure) {
        return func.__call__(arg1);
    }

    @Override
    public PyObject call(ThreadState state, PyObject arg1, PyObject arg2, PyObject globals,
            PyObject[] defaults, PyObject closure) {
        return func.__call__(arg1, arg2);
    }

    @Override
    public PyObject call(ThreadState state, PyObject arg1, PyObject arg2, PyObject arg3,
            PyObject globals, PyObject[] defaults,
            PyObject closure) {
        return func.__call__(arg1, arg2, arg3);
    }

    @Override
    public PyObject call(ThreadState state, PyObject arg1, PyObject arg2,
            PyObject arg3, PyObject arg4, PyObject globals,
            PyObject[] defaults, PyObject closure) {
        return func.__call__(arg1, arg2, arg3, arg4);
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        return func != null ? visit.visit(func, arg) : 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && ob == func;
    }
}

/**
 * A function object wrapper for a java method that complies with the
 * PyArgsKeywordsCall standard.
 */
@Untraversable
class JavaFunc extends PyObject {

    Method method;

    public JavaFunc(Method method) {
        this.method = method;
    }

    @Override
    public PyObject __call__(PyObject[] args, String[] kws) {
        Object[] margs = new Object[]{args, kws};
        try {
            return Py.java2py(method.invoke(null, margs));
        } catch (Throwable t) {
            throw Py.JavaError(t);
        }
    }

    @Override
    public PyObject _doget(PyObject container) {
        return _doget(container, null);
    }

    @Override
    public PyObject _doget(PyObject container, PyObject wherefound) {
        if (container == null) {
            return this;
        }
        return new PyMethod(this, container, wherefound);
    }

    public boolean _doset(PyObject container) {
        throw Py.TypeError("java function not settable: " + method.getName());
    }
}
