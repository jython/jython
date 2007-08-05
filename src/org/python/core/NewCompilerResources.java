// (C) Copyright 2007 Tobias Ivarsson
package org.python.core;

/**
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

                if (base instanceof PyMetaClass) {
                    // jython-only, experimental PyMetaClass hook
                    // xxx keep?
                    try {
                        java.lang.reflect.Constructor ctor = base.getClass().getConstructor(
                                pyClassCtrSignature);
                        return (PyObject) ctor.newInstance(new Object[] { name,
                                new PyTuple(bases), dict, proxyClass });
                    } catch (Exception e) {
                        throw Py.TypeError("meta-class fails to supply proper "
                                + "ctr: " + base.safeRepr());
                    }
                }
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

}
