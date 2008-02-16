// Copyright (c) Corporation for National Research Initiatives
package org.python.util;
import org.python.core.*;
import java.util.*;

/**
 * The PythonInterpreter class is a standard wrapper for a Jython
 * interpreter for use embedding in a Java application.
 *
 * @author  Jim Hugunin
 * @version 1.0, 02/23/97
 */

public class PythonInterpreter {
    PyModule module;
    protected PySystemState systemState;
    PyObject locals;

    protected CompilerFlags cflags = null;

    /**
     * Initializes the jython runtime. This should only be called once, and
     * should be called before any other python objects are created (including a
     * PythonInterpreter).
     * 
     * @param preProperties
     *            A set of properties. Typically System.getProperties() is used.
     * @param postProperties
     *            An other set of properties. Values like python.home,
     *            python.path and all other values from the registry files can
     *            be added to this property set. PostProperties will override
     *            system properties and registry properties.
     * @param argv
     *            Command line argument. These values will assigned to sys.argv.
     */
    public static void initialize(Properties preProperties,
                                  Properties postProperties,
                                  String[] argv) {
        PySystemState.initialize(preProperties, postProperties, argv);
    }

    /**
     * Create a new Interpreter with an empty dictionary
     */
    public PythonInterpreter() {
        this(null, null);
    }

    /**
     * Create a new interpreter with the given dictionary to use as its
     * namespace
     *
     * @param dict      the dictionary to use
     */

    // Optional dictionary will be used for locals namespace
    public PythonInterpreter(PyObject dict) {
        this(dict, null);
    }

    public PythonInterpreter(PyObject dict, PySystemState systemState) {
        if (dict == null)
            dict = new PyStringMap();
        if (systemState == null) {
            systemState = Py.getSystemState();
            if (systemState == null)
                systemState = new PySystemState();
        }
        module = new PyModule("__main__", dict);
        this.systemState = systemState;
        locals = module.__dict__;
        setState();
    }

    protected void setState() {
        Py.setSystemState(systemState);
    }

    /**
     * Set the Python object to use for the standard output stream
     *
     * @param outStream Python file-like object to use as output stream
     */
    public void setOut(PyObject outStream) {
        systemState.stdout = outStream;
    }

    /**
     * Set a java.io.Writer to use for the standard output stream
     *
     * @param outStream Writer to use as output stream
     */
    public void setOut(java.io.Writer outStream) {
        setOut(new PyFile(outStream));
    }

    /**
     * Set a java.io.OutputStream to use for the standard output stream
     *
     * @param outStream OutputStream to use as output stream
     */
    public void setOut(java.io.OutputStream outStream) {
        setOut(new PyFile(outStream));
    }

    public void setErr(PyObject outStream) {
        systemState.stderr = outStream;
    }

    public void setErr(java.io.Writer outStream) {
        setErr(new PyFile(outStream));
    }

    public void setErr(java.io.OutputStream outStream) {
        setErr(new PyFile(outStream));
    }

    /**
     * Evaluate a string as Python source and return the result
     *
     * @param s the string to evaluate
     */
    public PyObject eval(String s) {
        setState();
        return __builtin__.eval(new PyString(s), locals);
    }

    /**
     * Execute a string of Python source in the local namespace
     *
     * @param s the string to execute
     */
    public void exec(String s) {
        setState();
        Py.exec(Py.compile_flags(s, "<string>", "exec",cflags),
                locals, locals);
    }

    /**
     * Execute a Python code object in the local namespace
     *
     * @param code the code object to execute
     */
    public void exec(PyObject code) {
        setState();
        Py.exec(code, locals, locals);
    }

    /**
     * Execute a file of Python source in the local namespace
     *
     * @param s the name of the file to execute
     */
    public void execfile(String s) {
        setState();
        __builtin__.execfile_flags(s, locals, locals, cflags);
    }

    public void execfile(java.io.InputStream s) {
        execfile(s, "<iostream>");
    }

    public void execfile(java.io.InputStream s, String name) {
        setState();
        Py.runCode(Py.compile_flags(s, name, "exec",cflags), locals, locals);
    }

    // Getting and setting the locals dictionary
    public PyObject getLocals() {
        return locals;
    }

    public void setLocals(PyObject d) {
        locals = d;
    }

    /**
     * Set a variable in the local namespace
     *
     * @param name      the name of the variable
     * @param value the value to set the variable to.
     Will be automatically converted to an appropriate Python object.
    */
    public void set(String name, Object value) {
        locals.__setitem__(name.intern(), Py.java2py(value));
    }

    /**
     * Set a variable in the local namespace
     *
     * @param name      the name of the variable
     * @param value the value to set the variable to
     */
    public void set(String name, PyObject value) {
        locals.__setitem__(name.intern(), value);
    }

    /**
     * Get the value of a variable in the local namespace
     * 
     * @param name
     *            the name of the variable
     * @return the value of the variable, or null if that name isn't assigned
     */
    public PyObject get(String name) {
        return locals.__finditem__(name.intern());
    }

    /**
     * Get the value of a variable in the local namespace Value will be returned
     * as an instance of the given Java class.
     * <code>interp.get("foo", Object.class)</code> will return the most
     * appropriate generic Java object.
     * 
     * @param name
     *            the name of the variable
     * @param javaclass
     *            the class of object to return
     * @return the value of the variable as the given class, or null if that
     *         name isn't assigned
     */
    public Object get(String name, Class javaclass) {
        PyObject val = locals.__finditem__(name.intern());
        if(val == null) {
            return null;
        }
        return Py.tojava(val, javaclass);
    }

    public void cleanup() {
        systemState.callExitFunc();
    }
}
