// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import org.python.expose.ExposedDelete;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

/**
 * A Python function.
 */
@ExposedType(name = "function")
public class PyFunction extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyFunction.class);

    /** The writable name, also available via func_name. */
    @ExposedGet
    @ExposedSet
    public String __name__;

    /** The writable doc string, also available via func_doc. */
    @ExposedGet
    @ExposedSet
    public PyObject __doc__;

    /** The read only namespace; a dict (PyStringMap). */
    @ExposedGet
    public PyObject func_globals;

    /**
     * Default argument values for associated kwargs. Exposed as a
     * tuple to Python. Writable.
     */
    public PyObject[] func_defaults;

    /** The actual funtion's code, writable. */
    @ExposedGet
    public PyCode func_code;

    /**
     * A function's lazily created __dict__; allows arbitrary
     * attributes to be tacked on. Read only.
     */
    public PyObject __dict__;

    /** A read only closure tuple for nested scopes. */
    @ExposedGet
    public PyObject func_closure;

    /** Writable object describing what module this function belongs to. */
    @ExposedGet
    @ExposedSet
    public PyObject __module__;

    public PyFunction(PyObject globals, PyObject[] defaults, PyCode code, PyObject doc,
                      PyObject[] closure_cells) {
        func_globals = globals;
        __name__ = code.co_name;
        __doc__ = doc != null ? doc : Py.None;
        // XXX: workaround the compiler passing Py.EmptyObjects
        // instead of null for defaults, whereas we want func_defaults
        // to be None (null) in that situation
        func_defaults = (defaults != null && defaults.length == 0) ? null : defaults;
        func_code = code;
        func_closure = closure_cells != null ? new PyTuple(closure_cells) : null;
        PyObject moduleName = globals.__finditem__("__name__");
        __module__ = moduleName != null ? moduleName : Py.None;
    }

    public PyFunction(PyObject globals, PyObject[] defaults, PyCode code, PyObject doc) {
        this(globals, defaults, code, doc, null);
    }

    public PyFunction(PyObject globals, PyObject[] defaults, PyCode code) {
        this(globals, defaults, code, null, null);
    }

    public PyFunction(PyObject globals, PyObject[] defaults, PyCode code,
                      PyObject[] closure_cells) {
        this(globals, defaults, code, null, closure_cells);
    }

    @ExposedNew
    static final PyObject function___new__(PyNewWrapper new_, boolean init, PyType subtype,
                                           PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("function", args, keywords,
                                     new String[] {"code", "globals", "name", "argdefs",
                                                   "closure"}, 0);
        PyObject code = ap.getPyObject(0);
        PyObject globals = ap.getPyObject(1);
        PyObject name = ap.getPyObject(2, Py.None);
        PyObject defaults = ap.getPyObject(3, Py.None);
        PyObject closure = ap.getPyObject(4, Py.None);

        if (!(code instanceof PyTableCode)) {
            throw Py.TypeError("function() argument 1 must be code, not " +
                               code.getType().fastGetName());
        }
        if (name != Py.None && !Py.isInstance(name, PyString.TYPE)) {
            throw Py.TypeError("arg 3 (name) must be None or string");
        }
        if (defaults != Py.None && !(defaults instanceof PyTuple)) {
            throw Py.TypeError("arg 4 (defaults) must be None or tuple");
        }

        PyTableCode tcode = (PyTableCode)code;
        int nfree = tcode.co_freevars == null ? 0 : tcode.co_freevars.length;
        if (!(closure instanceof PyTuple)) {
            if (nfree > 0 && closure == Py.None) {
                throw Py.TypeError("arg 5 (closure) must be tuple");
            } else if (closure != Py.None) {
                throw Py.TypeError("arg 5 (closure) must be None or tuple");
            }
        }

        int nclosure = closure == Py.None ? 0 : closure.__len__();
        if (nfree != nclosure) {
            throw Py.ValueError(String.format("%s requires closure of length %d, not %d",
                                              tcode.co_name, nfree, nclosure));
        }
        if (nclosure > 0) {
            for (PyObject o : ((PyTuple)closure).asIterable()) {
                if (!(o instanceof PyCell)) {
                    throw Py.TypeError(String.format("arg 5 (closure) expected cell, found %s",
                                                     o.getType().fastGetName()));
                }
            }
        }

        PyFunction function = new PyFunction(globals,
                                             defaults == Py.None
                                             ? null : ((PyTuple)defaults).getArray(),
                                             tcode, null,
                                             closure == Py.None
                                             ? null : ((PyTuple)closure).getArray());
        if (name != Py.None) {
            function.__name__ = name.toString();
        }
        return function;
    }

    @ExposedGet(name = "func_name")
    public PyString getFuncName() {
        return new PyString(__name__);
    }

    @ExposedSet(name = "func_name")
    public void setFuncName(PyString func_name) {
        __name__ = func_name.asString();
    }

    @ExposedGet(name = "func_doc")
    public PyObject getFuncDoc() {
        return __doc__;
    }

    @ExposedSet(name = "func_doc")
    public void setFuncDoc(PyObject func_doc) {
        __doc__ = func_doc;
    }

    @ExposedDelete(name = "func_doc")
    public void delFuncDoc() {
        delDoc();
    }

    @ExposedDelete(name = "__doc__")
    public void delDoc() {
        __doc__ = Py.None;
    }

    @ExposedGet(name = "func_defaults")
    public PyObject getFuncDefaults() {
        if (func_defaults == null) {
            return Py.None;
        }
        return new PyTuple(func_defaults);
    }

    @ExposedSet(name = "func_defaults")
    public void setFuncDefaults(PyObject func_defaults) {
        if (func_defaults != Py.None && !(func_defaults instanceof PyTuple)) {
            throw Py.TypeError("func_defaults must be set to a tuple object");
        }
        this.func_defaults = func_defaults == Py.None ? null : ((PyTuple)func_defaults).getArray();
    }

    @ExposedDelete(name = "func_defaults")
    public void delFuncDefaults() {
        func_defaults = null;
    }

    @ExposedSet(name = "func_code")
    public void setFuncCode(PyCode code) {
        if (func_code == null || !(code instanceof PyTableCode)) {
            throw Py.TypeError("func_code must be set to a code object");
        }
        PyTableCode tcode = (PyTableCode)code;
        int nfree = tcode.co_freevars == null ? 0 : tcode.co_freevars.length;
        int nclosure = func_closure != null ? func_closure.__len__() : 0;
        if (nclosure != nfree) {
            throw Py.ValueError(String.format("%s() requires a code object with %d free vars,"
                                              + " not %d", __name__, nclosure, nfree));
        }
        this.func_code = code;
    }

    @ExposedDelete(name = "__module__")
    public void delModule() {
        __module__ = Py.None;
    }

    @Override
    public PyObject fastGetDict() {
        return __dict__;
    }

    @ExposedGet(name = "__dict__")
    public PyObject getDict() {
        ensureDict();
        return __dict__;
    }

    @ExposedSet(name = "__dict__")
    public void setDict(PyObject value) {
        if (!(value instanceof PyDictionary) && !(value instanceof PyStringMap)) {
            throw Py.TypeError("setting function's dictionary to a non-dict");
        }
        __dict__ = value;
    }

    @ExposedDelete(name = "__dict__")
    public void delDict() {
        throw Py.TypeError("function's dictionary may not be deleted");
    }

    @ExposedGet(name = "func_dict")
    public PyObject getFuncDict() {
        return getDict();
    }

    @ExposedSet(name = "func_dict")
    public void setFuncDict(PyObject value) {
        setDict(value);
    }

    @ExposedDelete(name = "func_dict")
    public void delFuncDict() {
        delDict();
    }

    @ExposedSet(name = "func_globals")
    public void setFuncGlobals(PyObject value) {
        // __setattr__ would set __dict__['func_globals'] = value
        // without this method
        throw Py.TypeError("readonly attribute");
    }

    @ExposedSet(name = "func_closure")
    public void setFuncClosure(PyObject value) {
        // same idea as setFuncGlobals
        throw Py.TypeError("readonly attribute");
    }

    private void ensureDict() {
        if (__dict__ == null) {
            __dict__ = new PyStringMap();
        }
    }

    @Override
    public void __setattr__(String name, PyObject value) {
        function___setattr__(name, value);
    }

    @ExposedMethod
    final void function___setattr__(String name, PyObject value) {
        ensureDict();
        super.__setattr__(name, value);
    }

    @Override
    public PyObject getDoc() {
        return __doc__;
    }

    @Override
    public PyObject __get__(PyObject obj, PyObject type) {
        return function___get__(obj, type);
    }

    @ExposedMethod(defaults = "null")
    final PyObject function___get__(PyObject obj, PyObject type) {
        return new PyMethod(this, obj, type);
    }

    @Override
    public PyObject __call__() {
        return func_code.call(func_globals, func_defaults, func_closure);
    }

    @Override
    public PyObject __call__(PyObject arg) {
        return func_code.call(arg, func_globals, func_defaults, func_closure);
    }

    @Override
    public PyObject __call__(PyObject arg1, PyObject arg2) {
        return func_code.call(arg1, arg2, func_globals, func_defaults, func_closure);
    }

    @Override
    public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3) {
        return func_code.call(arg1, arg2, arg3, func_globals, func_defaults,
                              func_closure);
    }

    @Override
    public PyObject __call__(PyObject[] args, String[] keywords) {
        return function___call__(args, keywords);
    }

    @ExposedMethod
    final PyObject function___call__(PyObject[] args, String[] keywords) {
        return func_code.call(args, keywords, func_globals, func_defaults, func_closure);
    }

    @Override
    public PyObject __call__(PyObject arg1, PyObject[] args, String[] keywords) {
        return func_code.call(arg1, args, keywords, func_globals, func_defaults, func_closure);
    }

    @Override
    public String toString() {
        return String.format("<function %s at %s>", __name__, Py.idstr(this));
    }

    @Override
    public boolean isMappingType() { return false; }

    @Override
    public boolean isNumberType() { return false; }

    @Override
    public boolean isSequenceType() { return false; }
}
