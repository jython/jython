package org.python.util;

import java.io.Closeable;
import java.io.Reader;
import java.io.StringReader;
import java.util.Properties;

import org.python.antlr.base.mod;
import org.python.core.CompileMode;
import org.python.core.CompilerFlags;
import org.python.core.imp;
import org.python.core.Options;
import org.python.core.ParserFacade;
import org.python.core.Py;
import org.python.core.PyCode;
import org.python.core.PyException;
import org.python.core.PyFile;
import org.python.core.PyFileWriter;
import org.python.core.PyModule;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.python.core.PySystemState;
import org.python.core.__builtin__;
import org.python.core.PyFileReader;

/**
 * The PythonInterpreter class is a standard wrapper for a Jython interpreter for embedding in a
 * Java application.
 */
public class PythonInterpreter implements AutoCloseable, Closeable {

    // Defaults if the interpreter uses thread-local state
    protected PySystemState systemState;
    PyObject globals;

    protected final boolean useThreadLocalState;

    protected static ThreadLocal<Object[]> threadLocals = new ThreadLocal<Object[]>() {

        @Override
        protected Object[] initialValue() {
            return new Object[1];
        }
    };

    protected CompilerFlags cflags = new CompilerFlags();

    private volatile boolean closed = false;

    /**
     * Initializes the Jython runtime. This should only be called once, before any other Python
     * objects (including PythonInterpreter) are created.
     *
     * @param preProperties A set of properties. Typically System.getProperties() is used.
     *            preProperties override properties from the registry file.
     * @param postProperties Another set of properties. Values like python.home, python.path and all
     *            other values from the registry files can be added to this property set.
     *            postProperties override system properties and registry properties.
     * @param argv Command line arguments, assigned to sys.argv.
     */
    public static void
            initialize(Properties preProperties, Properties postProperties, String[] argv) {
        PySystemState.initialize(preProperties, postProperties, argv);
    }

    /**
     * Creates a new interpreter with an empty local namespace.
     */
    public PythonInterpreter() {
        this(null, null);
    }

    /**
     * Creates a new interpreter with the ability to maintain a separate local namespace for each
     * thread (set by invoking setLocals()).
     *
     * @param dict a Python mapping object (e.g., a dictionary) for use as the default namespace
     */
    public static PythonInterpreter threadLocalStateInterpreter(PyObject dict) {
        return new PythonInterpreter(dict, new PySystemState(), true);
    }

    /**
     * Creates a new interpreter with a specified local namespace.
     *
     * @param dict a Python mapping object (e.g., a dictionary) for use as the namespace
     */
    public PythonInterpreter(PyObject dict) {
        this(dict, null);
    }

    public PythonInterpreter(PyObject dict, PySystemState systemState) {
        this(dict, systemState, false);
    }

    protected PythonInterpreter(PyObject dict, PySystemState systemState,
            boolean useThreadLocalState) {
        if (dict == null) {
            dict = Py.newStringMap();
        }
        globals = dict;

        if (systemState == null) {
            systemState = Py.getSystemState();
        }
        this.systemState = systemState;
        setSystemState();

        this.useThreadLocalState = useThreadLocalState;
        if (!useThreadLocalState) {
            PyModule module = new PyModule("__main__", dict);
            systemState.modules.__setitem__("__main__", module);
        }

        Py.importSiteIfSelected();
    }

    public PySystemState getSystemState() {
        return systemState;
    }

    protected void setSystemState() {
        Py.setSystemState(getSystemState());
    }

    /**
     * Sets a Python object to use for the standard input stream, <code>sys.stdin</code>. This
     * stream is used in a byte-oriented way, through calls to <code>read</code> and
     * <code>readline</code> on the object.
     *
     * @param inStream a Python file-like object to use as the input stream
     */
    public void setIn(PyObject inStream) {
        getSystemState().stdin = inStream;
    }

    /**
     * Sets a {@link Reader} to use for the standard input stream, <code>sys.stdin</code>. This
     * stream is wrapped such that characters will be narrowed to bytes. A character greater than
     * <code>U+00FF</code> will raise a Java <code>IllegalArgumentException</code> from within
     * {@link PyString}.
     *
     * @param inStream to use as the input stream
     */
    public void setIn(java.io.Reader inStream) {
        setIn(new PyFileReader(inStream));
    }

    /**
     * Sets a {@link java.io.InputStream} to use for the standard input stream.
     *
     * @param inStream InputStream to use as input stream
     */
    public void setIn(java.io.InputStream inStream) {
        setIn(new PyFile(inStream));
    }

    /**
     * Sets a Python object to use for the standard output stream, <code>sys.stdout</code>. This
     * stream is used in a byte-oriented way (mostly) that depends on the type of file-like object.
     * The behaviour as implemented is:
     * <table border=1>
     * <tr align=center>
     * <td></td>
     * <td colspan=3>Python type of object <code>o</code> written</td>
     * </tr>
     * <tr align=left>
     * <th></th>
     * <th><code>str/bytes</code></th>
     * <th><code>unicode</code></th>
     * <th>Any other type</th>
     * </tr>
     * <tr align=left>
     * <th>{@link PyFile}</th>
     * <td>as bytes directly</td>
     * <td>respect {@link PyFile#encoding}</td>
     * <td>call <code>str(o)</code> first</td>
     * </tr>
     * <tr align=left>
     * <th>{@link PyFileWriter}</th>
     * <td>each byte value as a <code>char</code></td>
     * <td>write as Java <code>char</code>s</td>
     * <td>call <code>o.toString()</code> first</td>
     * </tr>
     * <tr align=left>
     * <th>Other {@link PyObject} <code>f</code></th>
     * <td>invoke <code>f.write(str(o))</code></td>
     * <td>invoke <code>f.write(o)</code></td>
     * <td>invoke <code>f.write(str(o))</code></td>
     * </tr>
     * </table>
     *
     * @param outStream Python file-like object to use as the output stream
     */
    public void setOut(PyObject outStream) {
        getSystemState().stdout = outStream;
    }

    /**
     * Sets a {@link Writer} to use for the standard output stream, <code>sys.stdout</code>. The
     * behaviour as implemented is to output each object <code>o</code> by calling
     * <code>o.toString()</code> and writing this as UTF-16.
     *
     * @param outStream to use as the output stream
     */
    public void setOut(java.io.Writer outStream) {
        setOut(new PyFileWriter(outStream));
    }

    /**
     * Sets a {@link java.io.OutputStream} to use for the standard output stream.
     *
     * @param outStream OutputStream to use as output stream
     */
    public void setOut(java.io.OutputStream outStream) {
        setOut(new PyFile(outStream));
    }

    /**
     * Sets a Python object to use for the standard output stream, <code>sys.stderr</code>. This
     * stream is used in a byte-oriented way (mostly) that depends on the type of file-like object,
     * in the same way as {@link #setOut(PyObject)}.
     *
     * @param outStream Python file-like object to use as the error output stream
     */
    public void setErr(PyObject outStream) {
        getSystemState().stderr = outStream;
    }

    /**
     * Sets a {@link Writer} to use for the standard output stream, <code>sys.stdout</code>. The
     * behaviour as implemented is to output each object <code>o</code> by calling
     * <code>o.toString()</code> and writing this as UTF-16.
     *
     * @param outStream to use as the error output stream
     */
    public void setErr(java.io.Writer outStream) {
        setErr(new PyFileWriter(outStream));
    }

    public void setErr(java.io.OutputStream outStream) {
        setErr(new PyFile(outStream));
    }

    /**
     * Evaluates a string as a Python expression and returns the result.
     */
    public PyObject eval(String s) {
        setSystemState();
        return __builtin__.eval(new PyString(s), getLocals());
    }

    /**
     * Evaluates a Python code object and returns the result.
     */
    public PyObject eval(PyObject code) {
        setSystemState();
        return __builtin__.eval(code, getLocals());
    }

    /**
     * Executes a string of Python source in the local namespace.
     */
    public void exec(String s) {
        setSystemState();
        Py.exec(Py.compile_flags(s, "<string>", CompileMode.exec, cflags), getLocals(), null);
        Py.flushLine();
    }

    /**
     * Executes a Python code object in the local namespace.
     */
    public void exec(PyObject code) {
        setSystemState();
        Py.exec(code, getLocals(), null);
        Py.flushLine();
    }

    /**
     * Executes a file of Python source in the local namespace.
     */
    public void execfile(String filename) {
        PyObject locals = getLocals();
        setSystemState();
        __builtin__.execfile_flags(filename, locals, locals, cflags);
        Py.flushLine();
    }

    public void execfile(java.io.InputStream s) {
        execfile(s, "<iostream>");
    }

    public void execfile(java.io.InputStream s, String name) {
        setSystemState();
        Py.runCode(Py.compile_flags(s, name, CompileMode.exec, cflags), null, getLocals());
        Py.flushLine();
    }

    /**
     * Compiles a string of Python source as either an expression (if possible) or a module.
     *
     * Designed for use by a JSR 223 implementation: "the Scripting API does not distinguish between
     * scripts which return values and those which do not, nor do they make the corresponding
     * distinction between evaluating or executing objects." (SCR.4.2.1)
     */
    public PyCode compile(String script) {
        return compile(script, "<script>");
    }

    public PyCode compile(Reader reader) {
        return compile(reader, "<script>");
    }

    public PyCode compile(String script, String filename) {
        return compile(new StringReader(script), filename);
    }

    public PyCode compile(Reader reader, String filename) {
        mod node = ParserFacade.parseExpressionOrModule(reader, filename, cflags);
        setSystemState();
        return Py.compile_flags(node, filename, CompileMode.eval, cflags);
    }

    public PyObject getLocals() {
        if (!useThreadLocalState) {
            return globals;
        } else {
            PyObject locals = (PyObject)threadLocals.get()[0];
            if (locals != null) {
                return locals;
            }
            return globals;
        }
    }

    public void setLocals(PyObject d) {
        if (!useThreadLocalState) {
            globals = d;
        } else {
            threadLocals.get()[0] = d;
        }
    }

    /**
     * Sets a variable in the local namespace.
     *
     * @param name the name of the variable
     * @param value the object to set the variable to (as converted to an appropriate Python object)
     */
    public void set(String name, Object value) {
        getLocals().__setitem__(name.intern(), Py.java2py(value));
    }

    /**
     * Sets a variable in the local namespace.
     *
     * @param name the name of the variable
     * @param value the Python object to set the variable to
     */
    public void set(String name, PyObject value) {
        getLocals().__setitem__(name.intern(), value);
    }

    /**
     * Returns the value of a variable in the local namespace.
     *
     * @param name the name of the variable
     * @return the value of the variable, or null if that name isn't assigned
     */
    public PyObject get(String name) {
        return getLocals().__finditem__(name.intern());
    }

    /**
     * Returns the value of a variable in the local namespace.
     *
     * The value will be returned as an instance of the given Java class.
     * <code>interp.get("foo", Object.class)</code> will return the most appropriate generic Java
     * object.
     *
     * @param name the name of the variable
     * @param javaclass the class of object to return
     * @return the value of the variable as the given class, or null if that name isn't assigned
     */
    public <T> T get(String name, Class<T> javaclass) {
        PyObject val = getLocals().__finditem__(name.intern());
        if (val == null) {
            return null;
        }
        return Py.tojava(val, javaclass);
    }

    public void cleanup() {
        setSystemState();
        PySystemState sys = Py.getSystemState();
        sys.callExitFunc();
        try {
            sys.stdout.invoke("flush");
        } catch (PyException pye) {
            // fall through
        }
        try {
            sys.stderr.invoke("flush");
        } catch (PyException pye) {
            // fall through
        }
        threadLocals.remove();
        sys.cleanup();
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            cleanup();
        }
    }
}
