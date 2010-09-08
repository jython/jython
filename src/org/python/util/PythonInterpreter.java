package org.python.util;

import java.io.Reader;
import java.io.StringReader;
import java.util.Properties;

import org.python.antlr.base.mod;
import org.python.core.CompileMode;
import org.python.core.CompilerFlags;
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
 * The PythonInterpreter class is a standard wrapper for a Jython interpreter
 * for embedding in a Java application.
 */
public class PythonInterpreter {

    // Defaults if the interpreter uses thread-local state
    protected PySystemState systemState;
    PyObject globals;

    protected ThreadLocal<PyObject> threadLocals;

    protected CompilerFlags cflags = new CompilerFlags();

    /**
     * Initializes the Jython runtime. This should only be called
     * once, before any other Python objects (including
     * PythonInterpreter) are created.
     *
     * @param preProperties
     *            A set of properties. Typically
     *            System.getProperties() is used.  preProperties
     *            override properties from the registry file.
     * @param postProperties
     *            Another set of properties. Values like python.home,
     *            python.path and all other values from the registry
     *            files can be added to this property
     *            set. postProperties override system properties and
     *            registry properties.
     * @param argv
     *            Command line arguments, assigned to sys.argv.
     */
    public static void initialize(Properties preProperties, Properties postProperties, String[] argv) {
        PySystemState.initialize(preProperties, postProperties, argv);
    }

    /**
     * Creates a new interpreter with an empty local namespace.
     */
    public PythonInterpreter() {
        this(null, null);
    }

    /**
     * Creates a new interpreter with the ability to maintain a
     * separate local namespace for each thread (set by invoking
     * setLocals()).
     *
     * @param dict
     *            a Python mapping object (e.g., a dictionary) for use
     *            as the default namespace
     */
    public static PythonInterpreter threadLocalStateInterpreter(PyObject dict) {
        return new PythonInterpreter(dict, null, true);
    }

    /**
     * Creates a new interpreter with a specified local namespace.
     *
     * @param dict 
     *            a Python mapping object (e.g., a dictionary) for use
     *            as the namespace
     */
    public PythonInterpreter(PyObject dict) {
        this(dict, null);
    }

    public PythonInterpreter(PyObject dict, PySystemState systemState) {
        this(dict, systemState, false);
    }

    protected PythonInterpreter(PyObject dict, PySystemState systemState, boolean useThreadLocalState) {
        if (dict == null) {
            dict = new PyStringMap();
        }
        globals = dict;

        if (systemState == null)
            systemState = Py.getSystemState();
        this.systemState = systemState;
        setSystemState();

        if (useThreadLocalState) {
            threadLocals = new ThreadLocal<PyObject>();
        } else {
            PyModule module = new PyModule("__main__", dict);
            systemState.modules.__setitem__("__main__", module);
        }
    }

    public PySystemState getSystemState() {
        return systemState;
    }

    protected void setSystemState() {
        Py.setSystemState(getSystemState());
    }

    /**
     * Sets a Python object to use for the standard input stream.
     *
     * @param inStream
     *            a Python file-like object to use as input stream
     */
    public void setIn(PyObject inStream) {
        getSystemState().stdin = inStream;
    }

    public void setIn(java.io.Reader inStream) {
        setIn(new PyFileReader(inStream));
    }

    /**
     * Sets a java.io.InputStream to use for the standard input
     * stream.
     *
     * @param inStream
     *            InputStream to use as input stream
     */
    public void setIn(java.io.InputStream inStream) {
        setIn(new PyFile(inStream));
    }

    /**
     * Sets a Python object to use for the standard output stream.
     *
     * @param outStream
     *            Python file-like object to use as output stream
     */
    public void setOut(PyObject outStream) {
        getSystemState().stdout = outStream;
    }

    public void setOut(java.io.Writer outStream) {
        setOut(new PyFileWriter(outStream));
    }

    /**
     * Sets a java.io.OutputStream to use for the standard output
     * stream.
     *
     * @param outStream
     *            OutputStream to use as output stream
     */
    public void setOut(java.io.OutputStream outStream) {
        setOut(new PyFile(outStream));
    }

    public void setErr(PyObject outStream) {
        getSystemState().stderr = outStream;
    }

    public void setErr(java.io.Writer outStream) {
        setErr(new PyFileWriter(outStream));
    }

    public void setErr(java.io.OutputStream outStream) {
        setErr(new PyFile(outStream));
    }

    /**
     * Evaluates a string as a Python expression and returns the
     * result.
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
     * Compiles a string of Python source as either an expression (if
     * possible) or a module.
     *
     * Designed for use by a JSR 223 implementation: "the Scripting
     * API does not distinguish between scripts which return values
     * and those which do not, nor do they make the corresponding
     * distinction between evaluating or executing objects."
     * (SCR.4.2.1)
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
        if (threadLocals == null)
            return globals;

        PyObject locals = threadLocals.get();
        if (locals != null)
            return locals;
        return globals;
    }

    public void setLocals(PyObject d) {
        if (threadLocals == null)
            globals = d;
        else
            threadLocals.set(d);
    }

    /**
     * Sets a variable in the local namespace.
     *
     * @param name
     *            the name of the variable
     * @param value
     *            the object to set the variable to (as converted to
     *            an appropriate Python object)
     */
    public void set(String name, Object value) {
        getLocals().__setitem__(name.intern(), Py.java2py(value));
    }

    /**
     * Sets a variable in the local namespace.
     *
     * @param name
     *            the name of the variable
     * @param value
     *            the Python object to set the variable to
     */
    public void set(String name, PyObject value) {
        getLocals().__setitem__(name.intern(), value);
    }

    /**
     * Returns the value of a variable in the local namespace.
     *
     * @param name
     *            the name of the variable
     * @return the value of the variable, or null if that name isn't
     * assigned
     */
    public PyObject get(String name) {
        return getLocals().__finditem__(name.intern());
    }

    /**
     * Returns the value of a variable in the local namespace.
     * 
     * The value will be returned as an instance of the given Java class.
     * <code>interp.get("foo", Object.class)</code> will return the most
     * appropriate generic Java object.
     *
     * @param name
     *            the name of the variable
     * @param javaclass
     *            the class of object to return
     * @return the value of the variable as the given class, or null
     * if that name isn't assigned
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
        sys.cleanup();
    }
}
