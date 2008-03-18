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

    private static Class[] pyClassCtrSignature = { String.class, PyTuple.class,
            PyObject.class, Class.class };

    static private final PyType CLASS_TYPE = PyType.fromClass(PyClass.class);
    
    public static PyObject makeClass(String name, PyObject[] bases,
            PyObject dict) {
        return makeClass(name, bases, dict, null);
    }

    /**
     * This is a way more similar to how python constructs classes, it should be
     * moved to {@link Py}. The method there can be refactored to invoke this
     * method instead, without loss.
     * 
     * @param name
     * @param bases
     * @param dict
     * @param proxyClass
     * @return A new Python Class
     */
    public static PyObject makeClass(String name, PyObject[] bases,
            PyObject dict, Class proxyClass) {
        PyFrame frame = Py.getFrame();
        PyObject globals = frame.f_globals;

        PyObject metaclass;

        metaclass = dict.__finditem__("__metaclass__");

        if (metaclass == null) {
            if (bases.length != 0) {
                PyObject base = bases[0];
                metaclass = base.__findattr__("__class__");
                if (metaclass == null) {
                    metaclass = base.getType();
                }
            } else {
                if (globals != null)
                    metaclass = globals.__finditem__("__metaclass__");
            }
        }

        if (metaclass == null
                || metaclass == CLASS_TYPE
                || (metaclass instanceof PyJavaClass && ((PyJavaClass) metaclass).proxyClass == Class.class)) {
            boolean more_general = false;
            for (int i = 0; i < bases.length; i++) {
                if (!(bases[i] instanceof PyClass)) {
                    metaclass = bases[i].getType();
                    more_general = true;
                    break;
                }
            }
            if (!more_general)
                return new PyClass(name, new PyTuple(bases), dict, proxyClass);
        }

        if (proxyClass != null) {
            throw Py.TypeError("the meta-class cannot handle java subclassing");
        }

        return metaclass.__call__(new PyString(name), new PyTuple(bases), dict);
    }
    
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

        loadNames(names, module, frame.getf_locals(), filter);
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
