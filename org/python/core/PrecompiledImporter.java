package org.python.core;

/**
 * An importer for classes pre-compiled with JythonC.
 *
 */
public class PrecompiledImporter extends PyObject {

    public PrecompiledImporter() {
        this(Py.None);
    }

    public PrecompiledImporter(PyObject path) {
        super();
    }

    /**
     * Find the module for the fully qualified name.
     * @param name the fully qualified name of the module
     * @return a loader instance if this importer can load the module, None otherwise
     */
    public PyObject find_module(String name) {
        return find_module(name, Py.None);
    }

    /**
     * Find the module for the fully qualified name.
     * @param name the fully qualified name of the module
     * @param path if installed on the meta-path None or a module path
     * @return a loader instance if this importer can load the module, None otherwise
     */
    public PyObject find_module(String name, PyObject path) {
        if (Py.frozenModules != null) {
            //System.out.println("precomp: "+name+", "+name);
            Class c;

            if (Py.frozenModules.get(name+".__init__") != null) {
                //System.err.println("trying: "+name+".__init__$_PyInner");
                Py.writeComment("import", "trying " + name + " as precompiled package");
                c = findPyClass(name+".__init__");
                if (c == null) {
                    return Py.None;
                }
                Py.writeComment("import", "'" + name + "' as " +
                                "precompiled package");
                //System.err.println("found: "+name+".__init__$_PyInner");
            } else if (Py.frozenModules.get(name) != null) {
                Py.writeComment("import", "trying " + name + " as precompiled module");
                c = findPyClass(name);
                if (c == null) {
                    return Py.None;
                }
                Py.writeComment("import", "'" + name + "' as " +
                                "precompiled module");
            } else {
                return Py.None;
            }
            return this;
        }
        return Py.None;
    }

    public PyObject load_module(String name) {
        if (Py.frozenModules != null) {
            Class c;
            if (Py.frozenModules.get(name+".__init__") != null) {
                c = findPyClass(name+".__init__");
                if (c == null) {
                    throw Py.ImportError(name);
                }
                PyModule m = imp.addModule(name);
                m.__dict__.__setitem__("__path__", new PyList());
            } else if (Py.frozenModules.get(name) != null) {
                c = findPyClass(name);
                if (c == null) {
                    throw Py.ImportError(name);
                }
            } else {
                throw Py.ImportError(name);
            }
            return imp.createFromClass(name, c);
        }
        throw Py.ImportError(name);
    }

    private Class findPyClass(String name) {
        if (Py.frozenPackage != null) {
            name = Py.frozenPackage+"."+name;
        }
        return Py.findClassEx(name+"$_PyInner", "precompiled");
    }
}
