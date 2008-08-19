// (C) Copyright 2007 Tobias Ivarsson
package org.python.core;

/**
 * This class contains stuff that almost exists in the library already,
 * but with interfaces that I found more suitable. If others agree this should
 * be migrated into the standard lib.
 * 
 * @author Tobias Ivarsson
 */
public class NewCompilerResources {

    // import facilities, stolen from imp
    
    /**
     * Called from jython generated code when a statement like "from spam.eggs
     * import *" is executed.
     */
    public static void importAll(PyObject module, PyFrame frame) {
        // System.out.println("importAll(" + mod + ")");
        PyObject names;
        boolean filter = true;
        if (module instanceof PyJavaPackage) {
            names = ((PyJavaPackage) module).fillDir();
        } else {
            PyObject __all__ = module.__findattr__("__all__");
            if (__all__ != null) {
                names = __all__;
                filter = false;
            } else {
                names = module.__dir__();
            }
        }

        loadNames(names, module, frame.getLocals(), filter);
    }

    /**
     * From a module, load the attributes found in <code>names</code> into
     * locals.
     * 
     * @param filter if true, if the name starts with an underscore '_' do not
     *            add it to locals
     * @param locals the namespace into which names will be loaded
     * @param names the names to load from the module
     * @param module the fully imported module
     */
    private static void loadNames(PyObject names, PyObject module,
            PyObject locals, boolean filter) {
        PyObject iter = names.__iter__();
        for (PyObject name; (name = iter.__iternext__()) != null;) {
            String sname = ((PyString) name).internedString();
            if (filter && sname.startsWith("_")) {
                continue;
            } else {
                try {
                    locals.__setitem__(sname, module.__getattr__(sname));
                } catch (Exception exc) {
                    continue;
                }
            }
        }
    }

}
