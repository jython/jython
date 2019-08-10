package org.python.core;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Load Java classes.
 */
@Untraversable
public class JavaImporter extends PyObject {

    public static final String JAVA_IMPORT_PATH_ENTRY = "__classpath__";
    private static Logger log = Logger.getLogger("org.python.import");

    @Override
    public PyObject __call__(PyObject args[], String keywords[]) {
        if(args[0].toString().endsWith(JAVA_IMPORT_PATH_ENTRY)){
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
        log.log(Level.FINE, "# trying {0} in package manager for path {1}",
                new Object[] {name, path});
        PyObject ret = PySystemState.packageManager.lookupName(name.intern());
        if (ret != null) {
            log.log(Level.CONFIG, "import {0} # as java package", name);
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
    @Override
    public String toString() {
        return this.getType().toString();
    }
}
