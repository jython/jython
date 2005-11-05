package org.python.core;

/**
 * An importer for classes pre-compiled with JythonC.
 * 
 */
public class PrecompiledImporter extends PyObject {

    public PrecompiledImporter() {
        super();
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
        if (Py.frozenModules != null) {
            // System.out.println("precomp: "+name+", "+name);
            Class c = null;
            if (Py.frozenModules.get(name + ".__init__") != null) {
                // System.err.println("trying: "+name+".__init__$_PyInner");
                Py.writeDebug("import", "trying " + name
                        + " as precompiled package");
                c = findPyClass(name + ".__init__");
                if (c == null) {
                    return Py.None;
                }
                // System.err.println("found: "+name+".__init__$_PyInner");
                return new PrecompiledLoader(c, true);
            } else if (Py.frozenModules.get(name) != null) {
                Py.writeDebug("import", "trying " + name
                        + " as precompiled module");
                c = findPyClass(name);
                if (c == null) {
                    return Py.None;
                }
                return new PrecompiledLoader(c, false);
            }
        }
        return Py.None;
    }

    /**
     * Returns a string representation of the object.
     * 
     * @return a string representation of the object.
     */
    public String toString() {
        return this.getType().toString();
    }

    public class PrecompiledLoader extends PyObject {

        private Class _class;

        private boolean _package;

        public PrecompiledLoader(Class class_, boolean package_) {
            this._class = class_;
            this._package = package_;
        }

        public PyObject load_module(String name) {
            if (this._package) {
                PyModule m = imp.addModule(name);
                m.__dict__.__setitem__("__path__", new PyList());
                m.__dict__.__setitem__("__loader__", this);
            }
            Py.writeComment("import", "'" + name + "' as precompiled "
                    + (this._package ? "package" : "module"));
            return imp.createFromClass(name, this._class);
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

    private Class findPyClass(String name) {
        if (Py.frozenPackage != null) {
            name = Py.frozenPackage + "." + name;
        }
        return Py.findClassEx(name + "$_PyInner", "precompiled");
    }
}
