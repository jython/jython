package org.python.util;
import org.python.core.*;

/**
 *
 * The PythonInterpreter class is a standard wrapper for a JPython interpreter
 * for use embedding in a Java application.
 *
 * @author  Jim Hugunin
 * @version 1.0, 02/23/97
 */

public class PythonInterpreter {
    PyModule module;

    // Initialize from a possibly precompiled Python module
   /**
     * Create a new Interpreter with an empty dictionary
     */
    public PythonInterpreter() {
        this(new PyStringMap());
    }

    /**
     * Create a new interpreter with the given dictionary to use as its namespace
     *
     * @param dict	the dictionary to use
     */
    // Optional dictionary willl be used for locals namespace
    public PythonInterpreter(PyObject dict) {
        if (sys.registry == null) sys.registry = sys.initRegistry();
        module = new PyModule("main", dict);
    }

    /**
     * Evaluate a string as Python source and return the result
     *
     * @param s	the string to evaluate
     */
    public PyObject eval(String s) throws PyException {
        return __builtin__.eval(new PyString(s), module.__dict__);
    }

    /**
     * Execute a string of Python source in the local namespace
     *
     * @param s	the string to execute
     */
    public void exec(String s) throws PyException {
        Py.exec(new PyString(s), module.__dict__, module.__dict__);
    }

    /**
     * Execute a file of Python source in the local namespace
     *
     * @param s	the name of the file to execute
     */
    public void execfile(String s) throws PyException {
        __builtin__.execfile(s, module.__dict__);
    }

    //public void execfile(java.io.InputStream s) throws PyException {
    //}

    // Getting and setting the locals dictionary
    public PyObject getLocals() {
        return module.__dict__;
    }

    public void setLocals(PyObject d) {
        module.__dict__ = d;
    }

    /**
     * Set a variable in the local namespace
     *
     * @param name	the name of the variable
     * @param value the value to set the variable to.  
        Will be automatically converted to an appropriate Python object.
     */
    public void set(String name, Object value) {
        module.__dict__.__setitem__(name.intern(), Py.java2py(value));
    }

    /**
     * Set a variable in the local namespace
     *
     * @param name	the name of the variable
     * @param value the value to set the variable to
     */
    public void set(String name, PyObject value) {
        module.__dict__.__setitem__(name.intern(), value);
    }


    /**
     * Get the value of a variable in the local namespace
     *
     * @param name	the name of the variable
     */
    public PyObject get(String name) {
        return module.__dict__.__finditem__(name.intern());
    }
    
    /**
     * Get the value of a variable in the local namespace
     * Value will be returned as an instance of the given Java class.
     * <code>interp.get("foo", Object.class)</code> will return the most appropriate
     * generic Java object.
     *
     * @param name	the name of the variable
     * @param javaclass the class of object to return
     */ 
    public Object get(String name, Class javaclass) {
        return Py.tojava(module.__dict__.__finditem__(name.intern()), javaclass);
    }
}

