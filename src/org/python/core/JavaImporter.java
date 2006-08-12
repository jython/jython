package org.python.core;

/**
 * Load Java classes.
 */
public class JavaImporter extends PyObject {

    public JavaImporter() {
        super();
    }
    
    public PyObject __call__(PyObject args[], String keywords[]) {
        if(args[0].toString().endsWith("__classpath__")){
            return this;
        }
        throw Py.ImportError("unable to handle");
    }

    /**
     * Find the module for the fully qualified name.
     * 
     * @param name the fully qualified name of the module
     * @return a loader instance if this importer can load the module, None
     *         otherwise
     */
    public PyObject find_module(String name) {
        return find_module(name, Py.None);
    }

    /**
     * Find the module for the fully qualified name.
     * 
     * @param name the fully qualified name of the module
     * @param path if installed on the meta-path None or a module path
     * @return a loader instance if this importer can load the module, None
     *         otherwise
     */
    public PyObject find_module(String name, PyObject path) {
        Py.writeDebug("import", "trying " + name
                + " in packagemanager for path " + path);
        PyObject ret = PySystemState.packageManager.lookupName(name.intern());
        if (ret != null) {
            Py.writeComment("import", "'" + name + "' as java package");
            return this;
        }
        return Py.None;
    }

    public PyObject load_module(String name) {
        return PySystemState.packageManager.lookupName(name.intern());
    }

    /**
     * Returns a string representation of the object.
     * 
     * @return a string representation of the object.
     */
    public String toString() {
        return this.getType().toString();
    }
}
