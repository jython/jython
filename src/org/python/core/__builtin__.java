// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.python.antlr.ast.modType;
import org.python.core.util.RelativeFile;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

class BuiltinFunctions extends PyBuiltinFunctionSet {

    public static final PyObject module = Py.newString("__builtin__");

    public BuiltinFunctions(String name, int index, int argcount) {
        this(name, index, argcount, argcount);
    }

    public BuiltinFunctions(String name, int index, int minargs, int maxargs) {
        super(name, index, minargs, maxargs);
    }

    public PyObject __call__() {
        switch (this.index) {
            case 4:
                return __builtin__.globals();
            case 16:
                return __builtin__.dir();
            case 24:
                return __builtin__.input();
            case 28:
                return __builtin__.locals();
            case 34:
                return Py.newString(__builtin__.raw_input());
            case 41:
                return __builtin__.vars();
            case 43:
                return __builtin__.zip();
            default:
                throw info.unexpectedCall(0, false);
        }
    }

    public PyObject __call__(PyObject arg1) {
        switch (this.index) {
            case 0:
                return Py.newString(__builtin__.chr(Py.py2int(arg1, "chr(): 1st arg can't be coerced to int")));
            case 1:
                return Py.newInteger(__builtin__.len(arg1));
            case 2:
                return __builtin__.range(arg1);
            case 3:
                if (!(arg1 instanceof PyString)) {
                    throw Py.TypeError("ord() expected string of length 1, but " + arg1.getType().fastGetName() + " found");
                }
                return Py.newInteger(__builtin__.ord(arg1));
            case 5:
                return __builtin__.hash(arg1);
            case 6:
                return Py.newUnicode(__builtin__.unichr(Py.py2int(arg1, "unichr(): 1st arg can't be coerced to int")));
            case 7:
                return __builtin__.abs(arg1);
            case 9:
                return __builtin__.apply(arg1);
            case 11:
                return Py.newInteger(__builtin__.id(arg1));
            case 12:
                return __builtin__.sum(arg1);
            case 14:
                return Py.newBoolean(__builtin__.callable(arg1));
            case 16:
                return __builtin__.dir(arg1);
            case 18:
                return __builtin__.eval(arg1);
            case 19:
                try {
                    __builtin__.execfile(arg1.asString(0));
                } catch (ConversionException e) {
                    throw Py.TypeError("execfile's first argument must be str");
                }
                return Py.None;
            case 23:
                return __builtin__.hex(arg1);
            case 24:
                return __builtin__.input(arg1);
            case 25:
                return __builtin__.intern(arg1);
            case 27:
                return __builtin__.iter(arg1);
            case 32:
                return __builtin__.oct(arg1);
            case 34:
                return Py.newString(__builtin__.raw_input(arg1));
            case 36:
                Object o = arg1.__tojava__(PyModule.class);
                if (o == Py.NoConversion) {
                    o = arg1.__tojava__(PyJavaClass.class);
                    if (o == Py.NoConversion) {
                        if (arg1 instanceof PySystemState) {
                            return __builtin__.reload((PySystemState)arg1);
                        }
                        throw Py.TypeError("reload() argument must be a module");
                    }
                    return __builtin__.reload((PyJavaClass) o);
                }
                return __builtin__.reload((PyModule) o);
            case 37:
                return __builtin__.repr(arg1);
            case 41:
                return __builtin__.vars(arg1);
            case 30:
                return fancyCall(new PyObject[]{arg1});
            case 31:
                return fancyCall(new PyObject[]{arg1});
            case 43:
                return fancyCall(new PyObject[]{arg1});
            case 45:
                return __builtin__.reversed(arg1);
            default:
                throw info.unexpectedCall(1, false);
        }
    }

    public PyObject __call__(PyObject arg1, PyObject arg2) {
        switch (this.index) {
            case 2:
                return __builtin__.range(arg1, arg2);
            case 6:
                return Py.newInteger(__builtin__.cmp(arg1, arg2));
            case 9:
                return __builtin__.apply(arg1, arg2);
            case 10:
                return Py.newBoolean(__builtin__.isinstance(arg1, arg2));
            case 12:
                return __builtin__.sum(arg1, arg2);
            case 13:
                return __builtin__.coerce(arg1, arg2);
            case 15:
                __builtin__.delattr(arg1, asString(arg2, "delattr(): attribute name must be string"));
                return Py.None;
            case 17:
                return __builtin__.divmod(arg1, arg2);
            case 18:
                return __builtin__.eval(arg1, arg2);
            case 19:
                try {
                    __builtin__.execfile(arg1.asString(0), arg2);
                } catch (ConversionException e) {
                    throw Py.TypeError("execfile's first argument must be str");
                }
                return Py.None;
            case 20:
                return __builtin__.filter(arg1, arg2);
            case 21:
                return __builtin__.getattr(arg1, arg2);
            case 22:
                return Py.newBoolean(__builtin__.hasattr(arg1, arg2));
            case 26:
                return Py.newBoolean(__builtin__.issubclass(arg1, arg2));
            case 27:
                return __builtin__.iter(arg1, arg2);
            case 33:
                return __builtin__.pow(arg1, arg2);
            case 35:
                return __builtin__.reduce(arg1, arg2);
            case 29:
                return fancyCall(new PyObject[]{arg1, arg2});
            case 30:
                return fancyCall(new PyObject[]{arg1, arg2});
            case 31:
                return fancyCall(new PyObject[]{arg1, arg2});
            case 43:
                return fancyCall(new PyObject[]{arg1, arg2});
            default:
                throw info.unexpectedCall(2, false);
        }
    }

    public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3) {
        switch (this.index) {
            case 2:
                return __builtin__.range(arg1, arg2, arg3);
            case 9:
                try {
                    if (arg3 instanceof PyStringMap) {
                        PyDictionary d = new PyDictionary();
                        d.update(arg3);
                        arg3 = d;
                    }
                    // this catches both casts of arg3 to a PyDictionary, and
                    // all casts of keys in the dictionary to PyStrings inside
                    // apply(PyObject, PyObject, PyDictionary)
                    PyDictionary d = (PyDictionary) arg3;
                    return __builtin__.apply(arg1, arg2, d);
                } catch (ClassCastException e) {
                    throw Py.TypeError("apply() 3rd argument must be a " + "dictionary with string keys");
                }
            case 18:
                return __builtin__.eval(arg1, arg2, arg3);
            case 19:
                __builtin__.execfile(asString(arg1, "execfile's first argument must be str", false), arg2, arg3);
                return Py.None;
            case 21:
                return __builtin__.getattr(arg1, arg2, arg3);
            case 33:
                return __builtin__.pow(arg1, arg2, arg3);
            case 35:
                return __builtin__.reduce(arg1, arg2, arg3);
            case 39:
                __builtin__.setattr(arg1, asString(arg2, "setattr(): attribute name must be string"), arg3);
                return Py.None;
            case 44:
                return fancyCall(new PyObject[]{arg1, arg2, arg3});
            case 29:
                return fancyCall(new PyObject[]{arg1, arg2, arg3});
            case 30:
                return fancyCall(new PyObject[]{arg1, arg2, arg3});
            case 31:
                return fancyCall(new PyObject[]{arg1, arg2, arg3});
            case 43:
                return fancyCall(new PyObject[]{arg1, arg2, arg3});
            default:
                throw info.unexpectedCall(3, false);
        }
    }

    /**
     * @return arg as an interned String, or throws TypeError with mesage if asString throws a ConversionException
     */
    private String asString(PyObject arg, String message) {
        return asString(arg, message, true);
    }

    /**
     * @param intern - should the resulting string be interned
     * @return arg as a String, or throws TypeError with message if asString throws a ConversionException.  
     */
    private String asString(PyObject arg, String message, boolean intern) {

        try {
            return intern ? arg.asString(0).intern() : arg.asString(0);
        } catch (ConversionException e) {
            throw Py.TypeError(message);
        }
    }

    public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3, PyObject arg4) {
        switch (this.index) {
            case 44:
                return fancyCall(new PyObject[]{arg1, arg2, arg3, arg4});
            case 29:
                return fancyCall(new PyObject[]{arg1, arg2, arg3, arg4});
            case 30:
                return fancyCall(new PyObject[]{arg1, arg2, arg3, arg4});
            case 31:
                return fancyCall(new PyObject[]{arg1, arg2, arg3, arg4});
            case 43:
                return fancyCall(new PyObject[]{arg1, arg2, arg3, arg4});
            default:
                throw info.unexpectedCall(4, false);
        }
    }

    public PyObject fancyCall(PyObject[] args) {
        switch (this.index) {
            case 44:
                if (args.length > 5) {
                    throw info.unexpectedCall(args.length, false);
                }
                int flags = 0;
                if (args.length > 3) {
                    flags = Py.py2int(args[3]);
                }
                boolean dont_inherit = false;
                if (args.length > 4) {
                    dont_inherit = Py.py2boolean(args[4]);
                }

                if (args[0] instanceof PyUnicode) {
                    flags += PyTableCode.PyCF_SOURCE_IS_UTF8;   
                }
                return __builtin__.compile(args[0].toString(), args[1].toString(), args[2].toString(), flags, dont_inherit);
            case 29:
                return __builtin__.map(args);
            case 43:
                return __builtin__.zip(args);
            default:
                throw info.unexpectedCall(args.length, false);
        }
    }

    public PyObject getModule() {
        return module;
    }
}

/**
 * The builtin module. All builtin functions are defined here
 */
public class __builtin__ {

    public static void fillWithBuiltins(PyObject dict) {
        /* newstyle */
        dict.__setitem__("object", PyObject.TYPE);
        dict.__setitem__("type", PyType.TYPE);
        dict.__setitem__("bool", PyBoolean.TYPE);
        dict.__setitem__("int", PyInteger.TYPE);
        dict.__setitem__("enumerate", PyEnumerate.TYPE);
        dict.__setitem__("float", PyFloat.TYPE);
        dict.__setitem__("long", PyLong.TYPE);
        dict.__setitem__("complex", PyComplex.TYPE);
        dict.__setitem__("dict", PyDictionary.TYPE);
        dict.__setitem__("list", PyList.TYPE);
        dict.__setitem__("tuple", PyTuple.TYPE);
        dict.__setitem__("set", PySet.TYPE);
        dict.__setitem__("frozenset", PyFrozenSet.TYPE);

        dict.__setitem__("property", PyProperty.TYPE);
        dict.__setitem__("staticmethod", PyStaticMethod.TYPE);
        dict.__setitem__("classmethod", PyClassMethod.TYPE);
        dict.__setitem__("super", PySuper.TYPE);
        dict.__setitem__("str", PyString.TYPE);
        dict.__setitem__("unicode", PyUnicode.TYPE);
        dict.__setitem__("basestring", PyBaseString.TYPE);
        dict.__setitem__("file", PyFile.TYPE);
        dict.__setitem__("open", PyFile.TYPE);
        dict.__setitem__("slice", PySlice.TYPE);
        dict.__setitem__("xrange", PyXRange.TYPE);

        /* - */

        dict.__setitem__("None", Py.None);
        dict.__setitem__("NotImplemented", Py.NotImplemented);
        dict.__setitem__("Ellipsis", Py.Ellipsis);
        dict.__setitem__("True", Py.True);
        dict.__setitem__("False", Py.False);

        // Work in debug mode by default
        // Hopefully add -O option in the future to change this
        dict.__setitem__("__debug__", Py.One);

        dict.__setitem__("abs", new BuiltinFunctions("abs", 7, 1));
        dict.__setitem__("apply", new BuiltinFunctions("apply", 9, 1, 3));
        dict.__setitem__("callable", new BuiltinFunctions("callable", 14, 1));
        dict.__setitem__("coerce", new BuiltinFunctions("coerce", 13, 2));
        dict.__setitem__("chr", new BuiltinFunctions("chr", 0, 1));
        dict.__setitem__("cmp", new BuiltinFunctions("cmp", 6, 2));
        dict.__setitem__("globals", new BuiltinFunctions("globals", 4, 0));
        dict.__setitem__("hash", new BuiltinFunctions("hash", 5, 1));
        dict.__setitem__("id", new BuiltinFunctions("id", 11, 1));
        dict.__setitem__("isinstance", new BuiltinFunctions("isinstance", 10, 2));
        dict.__setitem__("len", new BuiltinFunctions("len", 1, 1));
        dict.__setitem__("ord", new BuiltinFunctions("ord", 3, 1));
        dict.__setitem__("range", new BuiltinFunctions("range", 2, 1, 3));
        dict.__setitem__("sum", new BuiltinFunctions("sum", 12, 1, 2));
        dict.__setitem__("unichr", new BuiltinFunctions("unichr", 6, 1));
        dict.__setitem__("compile", new BuiltinFunctions("compile", 44, 3, -1));
        dict.__setitem__("delattr", new BuiltinFunctions("delattr", 15, 2));
        dict.__setitem__("dir", new BuiltinFunctions("dir", 16, 0, 1));
        dict.__setitem__("divmod", new BuiltinFunctions("divmod", 17, 2));
        dict.__setitem__("eval", new BuiltinFunctions("eval", 18, 1, 3));
        dict.__setitem__("execfile", new BuiltinFunctions("execfile", 19, 1, 3));
        dict.__setitem__("filter", new BuiltinFunctions("filter", 20, 2));
        dict.__setitem__("getattr", new BuiltinFunctions("getattr", 21, 2, 3));
        dict.__setitem__("hasattr", new BuiltinFunctions("hasattr", 22, 2));
        dict.__setitem__("hex", new BuiltinFunctions("hex", 23, 1));
        dict.__setitem__("input", new BuiltinFunctions("input", 24, 0, 1));
        dict.__setitem__("intern", new BuiltinFunctions("intern", 25, 1));
        dict.__setitem__("issubclass", new BuiltinFunctions("issubclass", 26, 2));
        dict.__setitem__("iter", new BuiltinFunctions("iter", 27, 1, 2));
        dict.__setitem__("locals", new BuiltinFunctions("locals", 28, 0));
        dict.__setitem__("map", new BuiltinFunctions("map", 29, 2, -1));
        dict.__setitem__("max", MaxFunction.INSTANCE);
        dict.__setitem__("min", MinFunction.INSTANCE);
        dict.__setitem__("oct", new BuiltinFunctions("oct", 32, 1));
        dict.__setitem__("pow", new BuiltinFunctions("pow", 33, 2, 3));
        dict.__setitem__("raw_input", new BuiltinFunctions("raw_input", 34, 0, 1));
        dict.__setitem__("reduce", new BuiltinFunctions("reduce", 35, 2, 3));
        dict.__setitem__("reload", new BuiltinFunctions("reload", 36, 1));
        dict.__setitem__("repr", new BuiltinFunctions("repr", 37, 1));
        dict.__setitem__("round", RoundFunction.INSTANCE);
        dict.__setitem__("setattr", new BuiltinFunctions("setattr", 39, 3));
        dict.__setitem__("vars", new BuiltinFunctions("vars", 41, 0, 1));
        dict.__setitem__("zip", new BuiltinFunctions("zip", 43, 0, -1));
        dict.__setitem__("reversed", new BuiltinFunctions("reversed", 45, 1));
        dict.__setitem__("__import__", ImportFunction.INSTANCE);
        dict.__setitem__("sorted", SortedFunction.INSTANCE);
        dict.__setitem__("all", AllFunction.INSTANCE);
        dict.__setitem__("any", AnyFunction.INSTANCE);        
    }

    public static PyObject abs(PyObject o) {
        return o.__abs__();
    }

    public static PyObject apply(PyObject o) {
	return o.__call__();
    }
    
    public static PyObject apply(PyObject o, PyObject args) {
        return o.__call__(Py.make_array(args));
    }

    public static PyObject apply(PyObject o, PyObject args, PyDictionary kws) {
        PyObject[] a;
        String[] kw;
        Map table = kws.table;
        if (table.size() > 0) {
            Iterator ik = table.keySet().iterator();
            Iterator iv = table.values().iterator();
            int n = table.size();
            kw = new String[n];
            PyObject[] aargs = Py.make_array(args);
            a = new PyObject[n + aargs.length];
            System.arraycopy(aargs, 0, a, 0, aargs.length);
            int offset = aargs.length;

            for (int i = 0; i < n; i++) {
                kw[i] = ((PyString) ik.next()).internedString();
                a[i + offset] = (PyObject) iv.next();
            }
            return o.__call__(a, kw);
        } else {
            return apply(o, args);
        }
    }

    public static boolean callable(PyObject obj) {
        return obj.isCallable();
    }

    public static int unichr(int i) {
        if (i < 0 || i > PySystemState.maxunicode) {
            throw Py.ValueError("unichr() arg not in range(0x110000)");
        }
        return i;
    }

    public static char chr(int i) {
        if (i < 0 || i > 255) {
            throw Py.ValueError("chr() arg not in range(256)");
        }
        return (char) i;
    }

    public static int cmp(PyObject x, PyObject y) {
        return x._cmp(y);
    }

    public static PyTuple coerce(PyObject o1, PyObject o2) {
        PyObject[] result = o1._coerce(o2);
        if (result != null) {
            return new PyTuple(result);
        }
        throw Py.TypeError("number coercion failed");
    }

    public static PyObject compile(String data, String filename, String kind) {
        return Py.compile_flags(data, filename, kind, Py.getCompilerFlags());
    }

    public static PyObject compile(modType node, String filename, String kind) {
        return Py.compile_flags(node, filename, kind, Py.getCompilerFlags());
    }
            
    public static PyObject compile(String data, String filename, String kind, int flags, boolean dont_inherit) {
        if ((flags & ~PyTableCode.CO_ALL_FEATURES) != 0) {
            throw Py.ValueError("compile(): unrecognised flags");
        }
        return Py.compile_flags(data, filename, kind, Py.getCompilerFlags(flags, dont_inherit));
    }

    public static PyObject compile(modType node, String filename, String kind, int flags, boolean dont_inherit) {
        if ((flags & ~PyTableCode.CO_ALL_FEATURES) != 0) {
            throw Py.ValueError("compile(): unrecognised flags");
        }
        return Py.compile_flags(node, filename, kind, Py.getCompilerFlags(flags, dont_inherit));
    }

    public static void delattr(PyObject o, String n) {
        o.__delattr__(n);
    }

    public static PyObject dir(PyObject o) {
        PyList ret = (PyList) o.__dir__();
        ret.sort();
        return ret;
    }

    public static PyObject dir() {
        PyObject l = locals();
        PyList ret;
        PyObject retObj = l.invoke("keys");
        try {
            ret = (PyList) retObj;
        } catch (ClassCastException e) {
            throw Py.TypeError("Expected keys() to be a list, not '" + retObj.getType().fastGetName() + "'");
        }
        ret.sort();
        return ret;
    }

    public static PyObject divmod(PyObject x, PyObject y) {
        return x._divmod(y);
    }

    public static PyEnumerate enumerate(PyObject seq) {
        return new PyEnumerate(seq);
    }

    private static boolean PyMapping_check(PyObject o, boolean rw) {
        return o == null ||
               o == Py.None ||
               (o instanceof PyDictionary) ||
               (o.__findattr__("__getitem__") != null &&
                (!rw || o.__findattr__("__setitem__") != null));
    }
    
    private static void verify_mappings(PyObject globals, PyObject locals, boolean rw) {
        if (!PyMapping_check(globals, rw)) {
            throw Py.TypeError("globals must be a mapping");
        } 
        if (!PyMapping_check(locals, rw)) {
            throw Py.TypeError("locals must be a mapping");
        }
    }
    
    public static PyObject eval(PyObject o, PyObject globals, PyObject locals) {
        verify_mappings(globals, locals, false);
        PyCode code;
        if (o instanceof PyCode) {
            code = (PyCode) o;
        } else {
            if (o instanceof PyString) {
                code = (PyCode)compile(o.toString(), "<string>", "eval");
            } else {
                throw Py.TypeError("eval: argument 1 must be string or code object");
            }
        }
        return Py.runCode(code, locals, globals);
    }

    public static PyObject eval(PyObject o, PyObject globals) {
        return eval(o, globals, globals);
    }

    public static PyObject eval(PyObject o) {
        if (o instanceof PyTableCode && ((PyTableCode) o).hasFreevars()) {
            throw Py.TypeError("code object passed to eval() may not contain free variables");
        }
        return eval(o, null, null);
    }

    public static void execfile(String name, PyObject globals, PyObject locals) {
        execfile_flags(name, globals, locals, Py.getCompilerFlags());
    }

    public static void execfile_flags(String name, PyObject globals, PyObject locals, CompilerFlags cflags) {
        verify_mappings(globals, locals, true);
        java.io.FileInputStream file;
        try {
            file = new java.io.FileInputStream(new RelativeFile(name));
        } catch (java.io.FileNotFoundException e) {
            throw Py.IOError(e);
        }
        PyCode code;

        try {
            code = (PyCode)Py.compile_flags(file, name, "exec", cflags);
        } finally {
            try {
                file.close();
            } catch (java.io.IOException e) {
                throw Py.IOError(e);
            }
        }
        Py.runCode(code, locals, globals);
    }

    public static void execfile(String name, PyObject globals) {
        execfile(name, globals, globals);
    }

    public static void execfile(String name) {
        execfile(name, null, null);
    }

    public static PyObject filter(PyObject func, PyObject seq) {
        if (seq instanceof PyString) {
            if (seq instanceof PyUnicode) {
                return filterunicode(func, (PyUnicode)seq);
            }
            return filterstring(func, (PyString)seq);
        }
        if (seq instanceof PyTuple) {
            return filtertuple(func, (PyTuple)seq);
        }

        PyList list = new PyList();
        for (PyObject item : seq.asIterable()) {
            if (func == PyBoolean.TYPE || func == Py.None) {
                if (!item.__nonzero__()) {
                    continue;
                }
            } else if (!func.__call__(item).__nonzero__()) {
                continue;
            }
            list.append(item);
        }
        return list;
    }

    public static PyObject filterstring(PyObject func, PyString seq) {
        if (func == Py.None && seq.getType() == PyString.TYPE) {
            // If it's a real string we can return the original, as no character is ever
            // false and __getitem__ does return this character. If it's a subclass we
            // must go through the __getitem__ loop
            return seq;
        }

        StringBuilder builder = new StringBuilder();
        boolean ok;
        for (PyObject item : seq.asIterable()) {
            ok = false;
            if (func == Py.None) {
                if (item.__nonzero__()) {
                    ok = true;
                }
            } else if (func.__call__(item).__nonzero__()) {
                ok = true;
            }
            if (ok) {
                if (!(item instanceof PyString) || item instanceof PyUnicode) {
                    throw Py.TypeError("can't filter str to str: __getitem__ returned different "
                                       + "type");
                }
                builder.append(item.toString());
            }
        }
        return new PyString(builder.toString());
    }

    public static PyObject filterunicode(PyObject func, PyUnicode seq) {
        if (func == Py.None && seq.getType() == PyUnicode.TYPE) {
            // If it's a real string we can return the original, as no character is ever
            // false and __getitem__ does return this character. If it's a subclass we
            // must go through the __getitem__ loop
            return seq;
        }

        StringBuilder builder = new StringBuilder();
        boolean ok;
        for (PyObject item : seq.asIterable()) {
            ok = false;
            if (func == Py.None) {
                if (item.__nonzero__()) {
                    ok = true;
                }
            } else if (func.__call__(item).__nonzero__()) {
                ok = true;
            }
            if (ok) {
                if (!(item instanceof PyUnicode)) {
                    throw Py.TypeError("can't filter unicode to unicode: __getitem__ returned "
                                       + "different type");
                }
                builder.append(item.toString());
            }
        }
        return new PyUnicode(builder.toString());
    }

    public static PyObject filtertuple(PyObject func, PyTuple seq) {
        int len = seq.__len__();
        if (len == 0) {
            if (seq.getType() != PyTuple.TYPE) {
                seq = new PyTuple();
            }
            return seq;
        }

        PyList list = new PyList();
        PyObject item;
        boolean ok;
        for (int i = 0; i < len; i++) {
            ok = false;
            item = seq.__finditem__(i);
            if (func == Py.None) {
                if (item.__nonzero__()) {
                    ok = true;
                }
            } else if (func.__call__(item).__nonzero__()) {
                ok = true;
            }
            if (ok) {
                list.append(item);
            }
        }
        return PyTuple.fromIterable(list);
    }

    public static PyObject getattr(PyObject obj, PyObject name) {
        return getattr(obj, name, null);
    }

    public static PyObject getattr(PyObject obj, PyObject name, PyObject def) {
        String nameStr;
        if (name instanceof PyUnicode) {
            nameStr = ((PyUnicode)name).encode();
        } else if (name instanceof PyString) {
            nameStr = name.asString();
        } else {
            throw Py.TypeError("getattr(): attribute name must be string");
        }

        PyObject result;
        try {
            result = obj.__getattr__(nameStr.intern());
        } catch (PyException pye) {
            if (Py.matchException(pye, Py.AttributeError) && def != null) {
                result = def;
            } else {
                throw pye;
            }
        }
        return result;
    }

    public static PyObject globals() {
        return Py.getFrame().f_globals;
    }

    public static boolean hasattr(PyObject obj, PyObject name) {
        String nameStr;
        if (name instanceof PyUnicode) {
            nameStr = ((PyUnicode)name).encode().intern();
        } else if (name instanceof PyString) {
            nameStr = name.asString();
        } else {
            throw Py.TypeError("hasattr(): attribute name must be string");
        }

        try {
            return obj.__findattr__(nameStr.intern()) != null;
        } catch (PyException pye) {
            // swallow
        }
        return false;
    }

    public static PyInteger hash(PyObject o) {
        return o.__hash__();
    }

    public static PyString hex(PyObject o) {
        return o.__hex__();
    }

    public static long id(PyObject o) {
        return Py.id(o);
    }

    public static PyObject input(PyObject prompt) {
        String line = raw_input(prompt);
        return eval(new PyString(line));
    }

    public static PyObject input() {
        return input(new PyString(""));
    }
    private static final PyStringMap internedStrings = new PyStringMap();

    public static PyString intern(PyObject obj) {
        if (!(obj instanceof PyString) || obj instanceof PyUnicode) {
            throw Py.TypeError("intern() argument 1 must be string, not "
                               + obj.getType().fastGetName());
        }
        if (obj.getType() != PyString.TYPE) {
            throw Py.TypeError("can't intern subclass of string");
        }
        PyString s = (PyString)obj;

        // XXX: for some reason, not seeing this as an instance of PyStringDerived when derived
        if (s instanceof PyStringDerived) {
            throw Py.TypeError("can't intern subclass of string");
        }
        String istring = s.internedString();
        PyObject ret = internedStrings.__finditem__(istring);
        if (ret != null) {
            return (PyString)ret;
        }
        internedStrings.__setitem__(istring, s);
        return s;
    }

    // xxx find where used, modify with more appropriate if necessary
    public static boolean isinstance(PyObject obj, PyObject cls) {
        return Py.isInstance(obj, cls);
    }

    // xxx find where used, modify with more appropriate if necessary
    public static boolean issubclass(PyObject derived, PyObject cls) {
        return Py.isSubClass(derived, cls);
    }

    public static PyObject iter(PyObject obj) {
        return obj.__iter__();
    }

    public static PyObject iter(PyObject callable, PyObject sentinel) {
        return new PyCallIter(callable, sentinel);
    }

    public static int len(PyObject o) {
        try {
            return o.__len__();
        } catch (PyException e) {
            // Make this work like CPython where
            //
            // a = 7; len(a) raises a TypeError,
            // a.__len__() raises an AttributeError
            // and
            // class F: pass
            // f = F(); len(f) also raises an AttributeError
            //
            // Testing the type of o feels unclean though
            if (e.type == Py.AttributeError && !(o instanceof PyInstance)) {
                throw Py.TypeError("len() of unsized object");
            }
            throw e;
        }
    }

    public static PyObject locals() {
        return Py.getFrame().getLocals();
    }

    public static PyObject map(PyObject[] argstar) {
        int n = argstar.length - 1;
        if (n < 1) {
            throw Py.TypeError("map requires at least two arguments");
        }
        PyObject element;
        PyObject f = argstar[0];
        PyList list = new PyList();
        PyObject[] args = new PyObject[n];
        PyObject[] iters = new PyObject[n];

        for (int j = 0; j < n; j++) {
            iters[j] = Py.iter(argstar[j + 1], "argument " + (j + 1) + " to map() must support iteration");
        }

        while (true) {
            boolean any_items = false;
            for (int j = 0; j < n; j++) {
                if ((element = iters[j].__iternext__()) != null) {
                    args[j] = element;
                    any_items = true;
                } else {
                    args[j] = Py.None;
                }
            }
            if (!any_items) {
                break;
            }
            if (f == Py.None) {
                if (n == 1) {
                    list.append(args[0]);
                } else {
                    list.append(new PyTuple(args.clone()));
                }
            } else {
                list.append(f.__call__(args));
            }
        }
        return list;
    }

    public static PyString oct(PyObject o) {
        return o.__oct__();
    }

    public static final int ord(PyObject c) {
        final int length;
        PyString x = (PyString) c;
        if (x instanceof PyUnicode) {
            length = x.string.codePointCount(0, x.string.length());
            if (length == 1) {
                return x.string.codePointAt(0);
            }
        } else {
            length = x.string.length();
            if (length == 1) {
                return x.string.charAt(0);
            }
        }
        throw Py.TypeError("ord() expected a character, but string of length " +
                length + " found");
    }
    
    public static PyObject pow(PyObject x, PyObject y) {
        return x._pow(y);
    }

    private static boolean coerce(PyObject[] objs) {
        PyObject x = objs[0];
        PyObject y = objs[1];
        PyObject[] result;
        result = x._coerce(y);
        if (result != null) {
            objs[0] = result[0];
            objs[1] = result[1];
            return true;
        }
        result = y._coerce(x);
        if (result != null) {
            objs[0] = result[1];
            objs[1] = result[0];
            return true;
        }
        return false;
    }

    public static PyObject pow(PyObject x, PyObject y, PyObject z) {
        if (z == Py.None) {
            return pow(x, y);
        }

        PyObject[] tmp = new PyObject[2];
        tmp[0] = x;
        tmp[1] = y;
        if (coerce(tmp)) {
            x = tmp[0];
            y = tmp[1];
            tmp[1] = z;
            if (coerce(tmp)) {
                x = tmp[0];
                z = tmp[1];
                tmp[0] = y;
                if (coerce(tmp)) {
                    z = tmp[1];
                    y = tmp[0];
                }
            }
        } else {
            tmp[1] = z;
            if (coerce(tmp)) {
                x = tmp[0];
                z = tmp[1];
                tmp[0] = y;
                if (coerce(tmp)) {
                    y = tmp[0];
                    z = tmp[1];
                    tmp[1] = x;
                    if (coerce(tmp)) {
                        x = tmp[1];
                        y = tmp[0];
                    }
                }
            }
        }

        PyObject result = x.__pow__(y, z);
        if (result != null) {
            return result;
        }

        throw Py.TypeError(String.format("unsupported operand type(s) for pow(): '%.100s', "
                                         + "'%.100s', '%.100s'", x.getType().fastGetName(),
                                         y.getType().fastGetName(), z.getType().fastGetName()));
    }

    public static PyObject range(PyObject start, PyObject stop, PyObject step) {
        // Check that step is valid.
        int stepCmp = step.__cmp__(Py.Zero);
        if (stepCmp == -2) {
            throw Py.TypeError("non-integer type for step in range()");
        } else if (stepCmp == 0) {
            throw Py.ValueError("zero step for range()");
        }

        // Calculate the number of values in the range.
        PyObject n = stop.__sub__(start);
        if (n == null) {
            throw Py.TypeError("non-integer type for start or stop in range()");
        }
        n = n.__add__(step);
        if (stepCmp == 1) { // step is positive
            n = n.__sub__(Py.One).__div__(step);
        } else { // step is negative
            n = n.__add__(Py.One).__div__(step);
        }

        // Check that the number of values is valid.
        if (n.__cmp__(Py.Zero) <= 0) {
            return new PyList();
        }
        Object nAsInteger = n.__tojava__(Integer.TYPE);
        if (nAsInteger == Py.NoConversion) {
            if (n instanceof PyLong) {
                throw Py.OverflowError("Can't use range for more than " + Integer.MAX_VALUE + " items.  Try xrange instead.");
            } else {
                throw Py.TypeError("non-integer type for start or stop in range()");
            }
        }

        // Fill in the range.
        int nAsInt = ((Integer) nAsInteger).intValue();
        PyObject j = start;
        PyObject[] objs = new PyObject[nAsInt];
        for (int i = 0; i < nAsInt; i++) {
            objs[i] = j;
            j = j.__add__(step);
        }
        return new PyList(objs);
    }

    public static PyObject range(PyObject n) {
        return range(Py.Zero, n, Py.One);
    }

    public static PyObject range(PyObject start, PyObject stop) {
        return range(start, stop, Py.One);
    }

    private static PyString readline(PyObject file) {
        if (file instanceof PyFile) {
            return new PyString(((PyFile) file).readline());
        } else {
            PyObject ret = file.invoke("readline");
            if (!(ret instanceof PyString)) {
                throw Py.TypeError("object.readline() returned non-string");
            }
            return (PyString) ret;
        }
    }
    
    public static String raw_input(PyObject prompt, PyObject file) {
        PyObject stdout = Py.getSystemState().stdout;
        if (stdout instanceof PyAttributeDeleted) {
            throw Py.RuntimeError("[raw_]input: lost sys.stdout");
        }
        Py.print(stdout, prompt);
        String data = readline(file).toString();
        if (data.endsWith("\n")) {
            return data.substring(0, data.length() - 1);
        } else {
            if (data.length() == 0) {
                throw Py.EOFError("raw_input()");
            }
        }
        return data;
    }

    public static String raw_input(PyObject prompt) {
        PyObject stdin = Py.getSystemState().stdin;
        if (stdin instanceof PyAttributeDeleted) {
            throw Py.RuntimeError("[raw_]input: lost sys.stdin");
        }
        return raw_input(prompt, stdin);
    }

    public static String raw_input() {
        return raw_input(new PyString(""));
    }

    public static PyObject reduce(PyObject f, PyObject l, PyObject z) {
        PyObject result = z;
        PyObject iter = Py.iter(l, "reduce() arg 2 must support iteration");

        for (PyObject item; (item = iter.__iternext__()) != null;) {
            if (result == null) {
                result = item;
            } else {
                result = f.__call__(result, item);
            }
        }
        if (result == null) {
            throw Py.TypeError("reduce of empty sequence with no initial value");
        }
        return result;
    }

    public static PyObject reduce(PyObject f, PyObject l) {
        return reduce(f, l, null);
    }

    public static PyObject reload(PyModule o) {
        return imp.reload(o);
    }

    public static PyObject reload(PyJavaClass o) {
        return imp.reload(o);
    }

    public static PyObject reload(PySystemState o) {
	// reinitialize methods
        o.reload();
        return o;
    }

    public static PyString repr(PyObject o) {
        return o.__repr__();
    }

    public static void setattr(PyObject o, String n, PyObject v) {
        o.__setattr__(n, v);
    }

    public static PyObject sum(PyObject seq, PyObject result) {
        if (result instanceof PyString) {
            throw Py.TypeError("sum() can't sum strings [use ''.join(seq) instead]");
        }
        for (PyObject item : seq.asIterable()) {
            result = result._add(item);
        }
        return result;
    }

    public static PyObject reversed(PyObject seq) {
        if (seq.__findattr__("__getitem__") != null && seq.__findattr__("__len__") != null
            && seq.__findattr__("keys") == null) {
            return new PyReversedIterator(seq);
        } else {
            throw Py.TypeError("argument to reversed() must be a sequence");
        }
    }

    public static PyObject sum(PyObject seq) {
        return sum(seq, Py.Zero);
    }

    public static PyType type(PyObject o) {
        return o.getType();
    }

    public static PyObject vars() {
        return locals();
    }

    public static PyObject vars(PyObject o) {
        try {
            return o.__getattr__("__dict__");
        } catch (PyException e) {
            if (Py.matchException(e, Py.AttributeError)) {
                throw Py.TypeError("vars() argument must have __dict__ attribute");
            }
            throw e;
        }
    }

    public static PyString __doc__zip = new PyString("zip(seq1 [, seq2 [...]]) -> [(seq1[0], seq2[0] ...), (...)]\n" + "\n" + "Return a list of tuples, where each tuple contains the i-th element\n" + "from each of the argument sequences.  The returned list is\n" + "truncated in length to the length of the shortest argument sequence.");

    public static PyObject zip() {
        return new PyList();
    }
    
    public static PyObject zip(PyObject[] argstar) {
        int itemsize = argstar.length;

        // Type check the arguments; they must be sequences. Might as well
        // cache the __iter__() methods.
        PyObject[] iters = new PyObject[itemsize];

        for (int j = 0; j < itemsize; j++) {
            PyObject iter = argstar[j].__iter__();
            if (iter == null) {
                throw Py.TypeError("zip argument #" + (j + 1) + " must support iteration");
            }
            iters[j] = iter;
        }

        PyList ret = new PyList();

        for (int i = 0;; i++) {
            PyObject[] next = new PyObject[itemsize];
            PyObject item;

            for (int j = 0; j < itemsize; j++) {
                try {
                    item = iters[j].__iternext__();
                } catch (PyException e) {
                    if (Py.matchException(e, Py.StopIteration)) {
                        return ret;
                    }
                    throw e;
                }
                if (item == null) {
                    return ret;
                }
                next[j] = item;
            }
            ret.append(new PyTuple(next));
        }
    }

    public static PyObject __import__(String name) {
        return __import__(name, null, null, null);
    }

    public static PyObject __import__(String name, PyObject globals) {
        return __import__(name, globals, null, null);
    }

    public static PyObject __import__(String name, PyObject globals, PyObject locals) {
        return __import__(name, globals, locals, null);
    }

    public static PyObject __import__(String name, PyObject globals, PyObject locals, PyObject fromlist) {
        PyFrame frame = Py.getFrame();
        if (frame == null) {
            return null;
        }
        PyObject builtins = frame.f_builtins;
        if (builtins == null) {
            builtins = PySystemState.builtins;
        }

        PyObject __import__ = builtins.__finditem__("__import__");
        if (__import__ == null) {
            return null;
        }

        PyObject module = __import__.__call__(new PyObject[]{Py.newString(name), globals, locals, fromlist});
        return module;
    }
}

// simulates a PyBuiltinFunction for functions not using the PyBuiltinFunctionSet approach of above
abstract class ExtendedBuiltinFunction extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyBuiltinFunction.class);
    @ExposedGet(name = "__class__")
    @Override
    public PyType getType() {
        return TYPE;
    }
}

class ImportFunction extends ExtendedBuiltinFunction {
    static final ImportFunction INSTANCE = new ImportFunction();

    private ImportFunction() {}
    
    @ExposedNew
    public static PyObject __new__(PyObject[] args, String[] keyword) {
        return INSTANCE;
    }   
    
    @ExposedGet(name = "__doc__")
    public PyObject getDoc() {
        return new PyString("__import__(name, globals={}, locals={}, fromlist=[], level=-1) -> module\n\n" +
                "Import a module.  The globals are only used to determine the context;\n" +
                "they are not modified.  The locals are currently unused.  The fromlist\n" +
                "should be a list of names to emulate ``from name import ...'', or an\n" +
                "empty list to emulate ``import name''.\n" +
                "When importing a module from a package, note that __import__('A.B', ...)\n" + 
                "returns package A when fromlist is empty, but its submodule B when\n" +
                "fromlist is not empty.  Level is used to determine whether to perform \n" +
                "absolute or relative imports.  -1 is the original strategy of attempting\n" +
                "both absolute and relative imports, 0 is absolute, a positive number\n" +
                "is the number of parent directories to search relative to the current module.");
    }

//    public ImportFunction() {
//    }

    public PyObject __call__(PyObject args[], String keywords[]) {
        if (!(args.length < 1 || args[0] instanceof PyString)) {
            throw Py.TypeError("first argument must be a string");
        }
        if (keywords.length > 0) {
            throw Py.TypeError("__import__() takes no keyword arguments");
        }

        int argc = args.length;
        String module = args[0].__str__().toString();

        PyObject globals = (argc > 1 && args[1] != null) ? args[1] : null;
        PyObject fromlist = (argc > 3 && args[3] != null) ? args[3] : Py.EmptyTuple;

        return load(module, globals, fromlist);
    }

    private PyObject load(String module, PyObject globals, PyObject fromlist) {
        PyObject mod = imp.importName(module.intern(), fromlist.__len__() == 0, globals, fromlist);
        return mod;
    }

    public String toString() {
        return "<built-in function __import__>";
    }
    
    
}

class SortedFunction extends ExtendedBuiltinFunction {
    static final SortedFunction INSTANCE = new SortedFunction();

    private SortedFunction() {}
    
    @ExposedNew
    public static PyObject __new__(PyObject[] args, String[] keyword) {
        return INSTANCE;
    }   
    
    @ExposedGet(name = "__doc__")
    @Override
    public PyObject getDoc() {
        return new PyString("sorted(iterable, cmp=None, key=None, reverse=False) --> new sorted list");
    }
    
    @Override
    public PyObject __call__(PyObject args[], String kwds[]) {
        if (args.length == 0) {
            throw Py.TypeError(" sorted() takes at least 1 argument (0 given)");
        } else if (args.length > 4) {
            throw Py.TypeError(" sorted() takes at most 4 arguments (" + args.length + " given)");
        } else {
            PyObject iter = args[0].__iter__();
            if (iter == null) {
                throw Py.TypeError("'" + args[0].getType().fastGetName() + "' object is not iterable");
            }
        }

        PyList seq = new PyList(args[0]);

        PyObject newargs[] = new PyObject[args.length - 1];
        System.arraycopy(args, 1, newargs, 0, args.length - 1);
        ArgParser ap = new ArgParser("sorted", newargs, kwds, new String[]{"cmp", "key", "reverse"}, 0);

        PyObject cmp = ap.getPyObject(0, Py.None);
        PyObject key = ap.getPyObject(1, Py.None);
        PyObject reverse = ap.getPyObject(2, Py.None);

        seq.sort(cmp, key, reverse);
        return seq;
    }

    @Override
    public String toString() {
        return "<built-in function sorted>";
    }
}

class AllFunction extends ExtendedBuiltinFunction {
    static final AllFunction INSTANCE = new AllFunction();

    private AllFunction() {}
    
    @ExposedNew
    public static PyObject __new__(PyObject[] args, String[] keyword) {
        return INSTANCE;
    }   
    
    @ExposedGet(name = "__doc__")
    @Override
    public PyObject getDoc() {
        return new PyString("all(iterable) -> bool\n\nReturn True if bool(x) is True for all values x in the iterable.");
    }

    @Override
    public PyObject __call__(PyObject args[], String kwds[]) {
        if (args.length !=1) {
            throw Py.TypeError(" all() takes exactly one argument (" + args.length + " given)");
        }
        PyObject iter = args[0].__iter__();
        if (iter == null) {
            throw Py.TypeError("'" + args[0].getType().fastGetName() + "' object is not iterable");
        }
        for (PyObject item : iter.asIterable()) {
            if (!item.__nonzero__()) {
                return Py.False;
            }
        }
        return Py.True;
    }
    
    @Override
    public String toString() {
        return "<built-in function all>";
    }
}

class AnyFunction extends ExtendedBuiltinFunction {
    static final AnyFunction INSTANCE = new AnyFunction();

    private AnyFunction() {}
    
    @ExposedNew
    public static PyObject __new__(PyObject[] args, String[] keyword) {
        return INSTANCE;
    }   
    
    @ExposedGet(name = "__doc__")
    @Override
    public PyObject getDoc() {
        return new PyString("any(iterable) -> bool\n\nReturn True if bool(x) is True for any x in the iterable.");
    }

    @Override
    public PyObject __call__(PyObject args[], String kwds[]) {
        if (args.length !=1) {
            throw Py.TypeError(" any() takes exactly one argument (" + args.length + " given)");
        }
        PyObject iter = args[0].__iter__();
        if (iter == null) {
            throw Py.TypeError("'" + args[0].getType().fastGetName() + "' object is not iterable");
        }
        for (PyObject item : iter.asIterable()) {
            if (item.__nonzero__()) {
                return Py.True;
            }
        }
        return Py.False;     
    }
    
    @Override
    public String toString() {
        return "<built-in function any>";
    }
}

class MaxFunction extends ExtendedBuiltinFunction {
    static final MaxFunction INSTANCE = new MaxFunction();
   
    private MaxFunction() {}
  
    @ExposedNew
    public static PyObject __new__(PyObject[] args, String[] keyword) {
        return INSTANCE;
    }  

    @ExposedGet(name = "__doc__")
    @Override
    public PyObject getDoc() {
        return new PyString(
                "max(iterable[, key=func]) -> value\nmax(a, b, c, ...[, key=func]) -> value\n\n" +
                "With a single iterable argument, return its largest item.\n" + 
                "With two or more arguments, return the largest argument.");
    }

    @Override
    public PyObject __call__(PyObject args[], String kwds[]) {
        int argslen = args.length;
        PyObject key = null;
        
        if (args.length - kwds.length == 0) {
            throw Py.TypeError(" max() expected 1 arguments, got 0");
        }
        if (kwds.length > 0) {
            if (kwds[0].equals("key")) {
                key = args[argslen - 1];
                PyObject newargs[] = new PyObject[argslen - 1];
                System.arraycopy(args, 0, newargs, 0, argslen - 1);
                args = newargs;
            }
            else {
                throw Py.TypeError(" max() got an unexpected keyword argument");
            }
        }
        
        if (args.length > 1) {
            return max(new PyTuple(args), key);
        }
        else {
            return max(args[0], key);
        }
    }    
    
    @Override
    public String toString() {
        return "<built-in function max>";
    }
    
    private static PyObject max(PyObject o, PyObject key) {
        PyObject max = null;
        PyObject maxKey = null;
        for (PyObject item : o.asIterable()) {
            PyObject itemKey;
            if (key == null) {
                itemKey = item;
            }
            else {
                itemKey = key.__call__(item);
            }
            if (maxKey == null || itemKey._gt(maxKey).__nonzero__()) {
                maxKey = itemKey;
                max = item;
            }
        }
        if (max == null) {
            throw Py.ValueError("min of empty sequence");
        }
        return max;
    }

}

class MinFunction extends ExtendedBuiltinFunction {
    static final MinFunction INSTANCE = new MinFunction();
    
    private MinFunction() {}
   
    @ExposedNew
    public static PyObject __new__(PyObject[] args, String[] keyword) {
        return INSTANCE;
    }     
    
    
    @ExposedGet(name = "__doc__")
    @Override
    public PyObject getDoc() {
        return new PyString(
                "min(iterable[, key=func]) -> value\nmin(a, b, c, ...[, key=func]) -> value\n\n" +
                "With a single iterable argument, return its smallest item.\n" +
                "With two or more arguments, return the smallest argument.'");
    }

    @Override
    public PyObject __call__(PyObject args[], String kwds[]) {
        int argslen = args.length;
        PyObject key = null;
        
        if (args.length - kwds.length == 0) {
            throw Py.TypeError(" min() expected 1 arguments, got 0");
        }
        if (kwds.length > 0) {
            if (kwds[0].equals("key")) {
                key = args[argslen - 1];
                PyObject newargs[] = new PyObject[argslen - 1];
                System.arraycopy(args, 0, newargs, 0, argslen - 1);
                args = newargs;
            }
            else {
                throw Py.TypeError(" min() got an unexpected keyword argument");
            }
        }
        
        if (args.length > 1) {
            return min(new PyTuple(args), key);
        }
        else {
            return min(args[0], key);
        }
    }
    
    @Override
    public String toString() {
        return "<built-in function min>";
    }
    
    private static PyObject min(PyObject o, PyObject key) {
        PyObject min = null;
        PyObject minKey = null;
        for (PyObject item : o.asIterable()) {
            PyObject itemKey;
            if (key == null) {
                itemKey = item;
            }
            else {
                itemKey = key.__call__(item);
            }
            if (minKey == null || itemKey._lt(minKey).__nonzero__()) {
                minKey = itemKey;
                min = item;
            }
        }
        if (min == null) {
            throw Py.ValueError("min of empty sequence");
        }
        return min;
    }
}

class RoundFunction extends ExtendedBuiltinFunction {
    static final RoundFunction INSTANCE = new RoundFunction();
    private RoundFunction() {}
    
    @ExposedNew
    public static PyObject __new__(PyObject[] args, String[] keyword) {
        return INSTANCE;
    }     
    
    @ExposedGet(name = "__doc__")
    @Override
    public PyObject getDoc() {
        return new PyString(
                "round(number[, ndigits]) -> floating point number\n\n" +
                "Round a number to a given precision in decimal digits (default 0 digits).\n" +
                "This always returns a floating point number.  Precision may be negative.");
    }
    
    @Override
    public String toString() {
        return "<built-in function round>";
    }
    
    @Override
    public PyObject __call__(PyObject args[], String kwds[]) {
        ArgParser ap = new ArgParser("round", args, kwds, new String[]{"number", "ndigits"}, 0);
        PyObject number = ap.getPyObject(0);
        int ndigits = ap.getInt(1, 0);
        return round(Py.py2double(number), ndigits);
    }
    
    private static PyFloat round(double f, int digits) {
        boolean neg = f < 0;
        double multiple = Math.pow(10., digits);
        if (neg) {
            f = -f;
        }
        double tmp = Math.floor(f * multiple + 0.5);
        if (neg) {
            tmp = -tmp;
        }
        return new PyFloat(tmp / multiple);
    }
}
