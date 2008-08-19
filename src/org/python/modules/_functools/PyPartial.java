/* Copyright (c) Jython Developers */
package org.python.modules._functools;

import java.util.HashMap;
import java.util.Map;

import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

@ExposedType(name = "_functools.partial")
public class PyPartial extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyPartial.class);

    /** The wrapped callable. */
    @ExposedGet
    public PyObject func;

    /** Callable's args. */
    public PyObject[] args;

    /** Callable's keywords. */
    private String[] keywords;

    /** Lazily created dict for extra attributes. */
    private PyObject __dict__;

    public PyPartial() {
        super(TYPE);
    }

    public PyPartial(PyType subType) {
        super(subType);
    }

    @ExposedNew
    public static PyObject partial___new__(PyNewWrapper new_, boolean init, PyType subtype,
                                           PyObject[] args, String[] keywords) {
        if (args.length - keywords.length < 1) {
            throw Py.TypeError("type 'partial' takes at least one argument");
        }

        PyObject func = args[0];
        if (!func.isCallable()) {
            throw Py.TypeError("the first argument must be callable");
        }

        PyObject[] noFunc = new PyObject[args.length - 1];
        System.arraycopy(args, 1, noFunc, 0, args.length - 1);
        args = noFunc;

        PyPartial partial;
        if (new_.for_type == subtype) {
            partial = new PyPartial();
        } else {
            partial = new PyPartialDerived(subtype);
        }

        partial.func = func;
        partial.args = args;
        partial.keywords = keywords;
        return partial;
    }

    @Override
    public PyObject __call__(PyObject[] args, String[] keywords) {
        return partial___call__(args, keywords);
    }

    @ExposedMethod
    public PyObject partial___call__(PyObject[] args, String[] keywords) {
        PyObject[] argAppl;
        String[] kwAppl;
        int partialArgc = this.args.length - this.keywords.length;
        int argc = args.length - keywords.length;

        if (partialArgc == 0 && this.keywords.length == 0) {
            argAppl = args;
            kwAppl = keywords;
        } else if (argc == 0 && keywords.length == 0) {
            argAppl = this.args;
            kwAppl = this.keywords;
        } else {
            // first merge keywords to determine the keyword count
            HashMap<String, PyObject> merged = new HashMap<String, PyObject>();
            int i;
            for (i = 0; i < this.keywords.length; i++) {
                String keyword = this.keywords[i];
                PyObject value = this.args[partialArgc + i];
                merged.put(keyword, value);
            }
            for (i = 0; i < keywords.length; i++) {
                String keyword = keywords[i];
                PyObject value = args[argc + i];
                merged.put(keyword, value);
            }
            int keywordc = merged.size();

            // finally merge into args and keywords arrays
            argAppl = new PyObject[partialArgc + argc + keywordc];
            System.arraycopy(this.args, 0, argAppl, 0, partialArgc);
            System.arraycopy(args, 0, argAppl, partialArgc, argc);

            kwAppl = new String[keywordc];
            i = 0;
            int j = partialArgc + argc;
            for (Map.Entry<String, PyObject> entry : merged.entrySet()) {
                kwAppl[i++] = entry.getKey();
                argAppl[j++] = entry.getValue();
            }
        }
        return func.__call__(argAppl, kwAppl);
    }

    @ExposedGet(name = "args")
    public PyObject getArgs() {
        PyObject[] justArgs;
        if (keywords.length == 0) {
            justArgs = args;
        } else {
            int argc = args.length - keywords.length;
            justArgs = new PyObject[argc];
            System.arraycopy(args, 0, justArgs, 0, argc);
        }
        return new PyTuple(justArgs);
    }

    @ExposedGet(name = "keywords")
    public PyObject getKeywords() {
        if (keywords.length == 0) {
            return Py.None;
        }
        int argc = args.length - keywords.length;
        PyObject kwDict = new PyDictionary();
        for (int i = 0; i < keywords.length; i++) {
            kwDict.__setitem__(Py.newString(keywords[i]), args[argc + i]);
        }
        return kwDict;
    }

    @ExposedGet(name = "__dict__")
    public PyObject getDict() {
        ensureDict();
        return __dict__;
    }

    @ExposedSet(name = "__dict__")
    public void setDict(PyObject val) {
        if (!(val instanceof PyStringMap) && !(val instanceof PyDictionary)) {
            throw Py.TypeError("setting partial object's dictionary to a non-dict");
        }
        __dict__ = val;
    }

    private void ensureDict() {
        if (__dict__ == null) {
            __dict__ = new PyStringMap();
        }
    }
}
