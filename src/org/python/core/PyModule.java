// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import org.python.expose.ExposedDelete;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

/**
 * The Python Module object.
 *
 */
@ExposedType(name = "module")
public class PyModule extends PyObject implements Traverseproc {
    private final PyObject moduleDoc = new PyString( //FIXME: not used (and not static)
        "module(name[, doc])\n" +
        "\n" +
        "Create a module object.\n" +
        "The name must be a string; the optional doc argument can have any type.");

    /** The module's mutable dictionary */
    @ExposedGet
    public PyObject __dict__;

    public PyModule() {
        super();
    }

    public PyModule(PyType subType) {
        super(subType);
    }

    public PyModule(PyType subType, String name) {
        super(subType);
        module___init__(new PyString(name), Py.None);
    }

    public PyModule(String name) {
        this(name, null);
    }

    public PyModule(String name, PyObject dict) {
        super();
        __dict__ = dict;
        module___init__(new PyString(name), Py.None);
    }

    @ExposedNew
    @ExposedMethod
    final void module___init__(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("__init__", args, keywords, new String[] {"name", "doc"});
        PyObject name = ap.getPyObject(0);
        PyObject docs = ap.getPyObject(1, Py.None);
        module___init__(name, docs);
    }

    private void module___init__(PyObject name, PyObject doc) {
        ensureDict();
        __dict__.__setitem__("__name__", name);
        __dict__.__setitem__("__doc__", doc);
        if (name.equals(new PyString("__main__"))) {
            __dict__.__setitem__("__builtins__", Py.getSystemState().modules.__finditem__("__builtin__"));
            __dict__.__setitem__("__package__", Py.None);
        }
    }

    @Override
    public PyObject fastGetDict() {
        return __dict__;
    }

    @Override
    public PyObject getDict() {
        return __dict__;
    }

    @Override
    @ExposedSet(name = "__dict__")
    public void setDict(PyObject newDict) {
        throw Py.TypeError("readonly attribute");
    }

    @Override
    @ExposedDelete(name = "__dict__")
    public void delDict() {
        throw Py.TypeError("readonly attribute");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden in {@code PyModule} to search for a sub-module of this module (using the key
     * {@code ".".join(self.__name__, name)}) in {@code sys.modules}, on the {@code self.__path__},
     * and as a Java package with the same name. The named sub-module becomes an attribute of this
     * module (in {@code __dict__}).
     */
    @Override
    protected PyObject impAttr(String name) {

        // Some of our look-up needs the full name, deduced from __name__ and name.
        String fullName = getFullName(name);

        if (fullName != null) {
            // Maybe the attribute is a Python sub-module
            PyObject attr = findSubModule(name, fullName);

            // Or is a Java package
            if (attr == null) {
                // Still looking: maybe it's a Java package?
                attr = PySystemState.packageManager.lookupName(fullName);
            }

            // Add as an attribute the thing we found (if not still null)
            return addedSubModule(name, fullName, attr);
        }
        return null;
    }

    /**
     * Find Python sub-module within this object, within {@code sys.modules} or along this module's
     * {@code __path__}.
     *
     * @param name simple name of sub package
     * @param fullName of sub package
     * @return module found or {@code null}
     */
    private PyObject findSubModule(String name, String fullName) {
        PyObject attr =  null;
        if (fullName != null) {
            // The module may already have been loaded in sys.modules
            attr = Py.getSystemState().modules.__finditem__(fullName);
            // Or it may be found as a Python module along this module's __path__
            if (attr == null) {
                PyObject path = __dict__.__finditem__("__path__");
                if (path == null) {
                    attr = imp.find_module(name, fullName, new PyList());
                } else if (path instanceof PyList) {
                    attr = imp.find_module(name, fullName, (PyList) path);
                } else if (path != Py.None) {
                    throw Py.TypeError("__path__ must be list or None");
                }
            }
        }
        return attr;
    }

    /**
     * Add the given attribute to {@code __dict__}, if it is not {@code null} allowing
     * {@code sys.modules[fullName]} to override.
     *
     * @param name of attribute to add
     * @param fullName by which to check in {@code sys.modules}
     * @param attr attribute to add (if not overridden)
     * @return attribute value actually added (may be from {@code sys.modules}) or {@code null}
     */
    private PyObject addedSubModule(String name, String fullName, PyObject attr) {
        if (attr != null) {
            if (fullName != null) {
                // If a module by the full name exists in sys.modules, that takes precedence.
                PyObject entry = Py.getSystemState().modules.__finditem__(fullName);
                if (entry != null) {
                    attr = entry;
                }
            }
            // Enter this as an attribute of this module.
            __dict__.__setitem__(name, attr);
        }
        return attr;
    }

    /**
     * Construct (and intern) the full name of a possible sub-module of this one, using the
     * {@code __name__} attribute and a simple sub-module name. Return {@code null} if any of these
     * requirements is missing.
     *
     * @param name simple name of (possible) sub-module
     * @return interned full name or {@code null}
     */
    private String getFullName(String name) {
        if (__dict__ != null) {
            PyObject pyName = __dict__.__finditem__("__name__");
            if (pyName != null && name != null && name.length() > 0) {
                return (pyName.__str__().toString() + '.' + name).intern();
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden in {@code PyModule} so that if the base-class {@code __findattr_ex__} is
     * unsuccessful, it will to search for the named attribute as a Java sub-package. This is
     * responsible for the automagical import of Java (but not Python) packages when referred to as
     * attributes.
     */
    @Override
    public PyObject __findattr_ex__(String name) {
        // Find the attribute in the dictionary
        PyObject attr = super.__findattr_ex__(name);
        if (attr == null) {
            // The attribute may be a Java sub-package to auto-import.
            String fullName = getFullName(name);
            if (fullName != null) {
                attr = PySystemState.packageManager.lookupName(fullName);
                // Any entry in sys.modules to takes precedence.
                attr = addedSubModule(name, fullName, attr);
            }
        }
        return attr;
    }

    @Override
    public void __setattr__(String name, PyObject value) {
        module___setattr__(name, value);
    }

    @ExposedMethod
    final void module___setattr__(String name, PyObject value) {
        if (name != "__dict__") {
            ensureDict();
        }
        super.__setattr__(name, value);
    }

    @Override
    public void __delattr__(String name) {
        module___delattr__(name);
    }

    @ExposedMethod
    final void module___delattr__(String name) {
        super.__delattr__(name);
    }

    @Override
    public String toString()  {
        return module_toString();
    }

    @ExposedMethod(names = {"__repr__"})
    final String module_toString()  {
        PyObject name = null;
        PyObject filename = null;

        if (__dict__ != null) {
            name = __dict__.__finditem__("__name__");
            filename = __dict__.__finditem__("__file__");
        }
        if (name == null) {
            name = new PyString("?");
        }
        if (filename == null) {
            return String.format("<module '%s' (built-in)>", name);
        }
        return String.format("<module '%s' from '%s'>", name, filename);
    }

    @Override
    public PyObject __dir__() {
        // Some special casing to ensure that classes deriving from PyModule
        // can use their own __dict__. Although it would be nice to do this in
        // PyModuleDerived, current templating in gderived.py does not support
        // including from object, then overriding a specific method.
        PyObject d;
        if (this instanceof PyModuleDerived) {
            d = __findattr_ex__("__dict__");
        } else {
            d = __dict__;
        }
        if (d == null ||
                !(d instanceof AbstractDict ||
                  d instanceof PyDictProxy)) {
            throw Py.TypeError(String.format("%.200s.__dict__ is not a dictionary",
                    getType().fastGetName().toLowerCase()));
        }
        return d.invoke("keys");
    }

    private void ensureDict() {
        if (__dict__ == null) {
            __dict__ = new PyStringMap();
        }
    }

    /**
     * Delegates to {@link Py#newJ(PyModule, Class, Object...)}. For keyword support use
     * {@link #newJ(Class, String[], Object...)}.
     *
     * @see #newJ(Class, String[], Object...)
     * @see Py#newJ(PyModule, Class, String[], Object...)
     * @see Py#newJ(PyObject, Class, PyObject[], String[])
     * @see Py#newJ(PyObject, Class, Object...)
     * @see Py#newJ(PyObject, Class, String[], Object...)
     *
     * @param jcls Java-type of the desired clas, must have the same name
     * @param args constructor-arguments
     * @return a new instance of the desired class
     */
    @SuppressWarnings("unchecked")
    public <T> T newJ(Class<T> jcls, Object... args) {
        return Py.newJ(this, jcls, args);
    }

    /**
     * Delgates to {@link org.python.core.Py#newJ(PyModule, Class, String[], Object...)}.
     * {@code keywords} are applied to the last {@code args} in the list.
     *
     * @see #newJ(Class, Object...)
     * @see Py#newJ(PyModule, Class, String[], Object...)
     * @see Py#newJ(PyObject, Class, PyObject[], String[])
     * @see Py#newJ(PyObject, Class, Object...)
     * @see Py#newJ(PyObject, Class, String[], Object...)
     *
     * @param jcls Java-type of the desired class, must have the same name
     * @param keywords are applied to the last {@code args} in the list
     * @param args constructor-arguments
     * @return a new instance of the desired class
     */
    @SuppressWarnings("unchecked")
    public <T> T newJ(Class<T> jcls, String[] keywords, Object... args) {
        return Py.newJ(this, jcls, keywords, args);
    }

    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        return __dict__ == null ? 0 : visit.visit(__dict__, arg);
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && ob == __dict__;
    }
}
