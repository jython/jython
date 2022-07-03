/*
 * Copyright (c) Corporation for National Research Initiatives
 * Copyright (c) Jython Developers
 */
package org.python.core;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

import org.python.antlr.base.mod;
import org.python.core.stringlib.IntegerFormatter;
import org.python.core.util.ExtraMath;
import org.python.core.util.RelativeFile;
import org.python.modules._functools._functools;

@Untraversable
class BuiltinFunctions extends PyBuiltinFunctionSet {

    public static final PyObject module = PyString.fromInterned("__builtin__");

    public BuiltinFunctions(String name, int index, int argcount) {
        this(name, index, argcount, argcount);
    }

    public BuiltinFunctions(String name, int index, int minargs, int maxargs) {
        super(name, index, minargs, maxargs);
    }

    @Override
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

    @Override
    public PyObject __call__(PyObject arg1) {
        switch (this.index) {
            case 0:
                return Py.newString(__builtin__.chr(Py.py2int(arg1,
                                                              "chr(): 1st arg can't be coerced to "
                                                              + "int")));
            case 1:
                return Py.newInteger(__builtin__.len(arg1));
            case 2:
                return __builtin__.range(arg1);
            case 3:
                return Py.newInteger(__builtin__.ord(arg1));
            case 5:
                return __builtin__.hash(arg1);
            case 6:
                return Py.newUnicode(__builtin__.unichr(arg1));
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
                __builtin__.execfile(Py.fileSystemDecode(arg1));
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
                return __builtin__.reload(arg1);
            case 37:
                return __builtin__.repr(arg1);
            case 41:
                return __builtin__.vars(arg1);
            case 30:
                return fancyCall(new PyObject[] {arg1});
            case 31:
                return fancyCall(new PyObject[] {arg1});
            case 43:
                return fancyCall(new PyObject[] {arg1});
            case 45:
                return __builtin__.reversed(arg1);
            default:
                throw info.unexpectedCall(1, false);
        }
    }

    @Override
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
                __builtin__.delattr(arg1, arg2);
                return Py.None;
            case 17:
                return __builtin__.divmod(arg1, arg2);
            case 18:
                return __builtin__.eval(arg1, arg2);
            case 19:
                __builtin__.execfile(Py.fileSystemDecode(arg1), arg2);
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
                return _functools.reduce(arg1, arg2);
            case 29:
                return fancyCall(new PyObject[] {arg1, arg2});
            case 30:
                return fancyCall(new PyObject[] {arg1, arg2});
            case 31:
                return fancyCall(new PyObject[] {arg1, arg2});
            case 43:
                return fancyCall(new PyObject[] {arg1, arg2});
            default:
                throw info.unexpectedCall(2, false);
        }
    }

    @Override
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
                    PyDictionary d = (PyDictionary) arg3;
                    return __builtin__.apply(arg1, arg2, d);
                } catch (ClassCastException e) {
                    throw Py.TypeError("apply() 3rd argument must be a "
                                       + "dictionary with string keys");
                }
            case 18:
                return __builtin__.eval(arg1, arg2, arg3);
            case 19:
                __builtin__.execfile(Py.fileSystemDecode(arg1), arg2, arg3);
                return Py.None;
            case 21:
                return __builtin__.getattr(arg1, arg2, arg3);
            case 33:
                return __builtin__.pow(arg1, arg2, arg3);
            case 35:
                return _functools.reduce(arg1, arg2, arg3);
            case 39:
                __builtin__.setattr(arg1, arg2, arg3);
                return Py.None;
            case 44:
                return fancyCall(new PyObject[] {arg1, arg2, arg3});
            case 29:
                return fancyCall(new PyObject[] {arg1, arg2, arg3});
            case 30:
                return fancyCall(new PyObject[] {arg1, arg2, arg3});
            case 31:
                return fancyCall(new PyObject[] {arg1, arg2, arg3});
            case 43:
                return fancyCall(new PyObject[] {arg1, arg2, arg3});
            default:
                throw info.unexpectedCall(3, false);
        }
    }

    @Override
    public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3, PyObject arg4) {
        switch (this.index) {
            case 44:
                return fancyCall(new PyObject[] {arg1, arg2, arg3, arg4});
            case 29:
                return fancyCall(new PyObject[] {arg1, arg2, arg3, arg4});
            case 30:
                return fancyCall(new PyObject[] {arg1, arg2, arg3, arg4});
            case 31:
                return fancyCall(new PyObject[] {arg1, arg2, arg3, arg4});
            case 43:
                return fancyCall(new PyObject[] {arg1, arg2, arg3, arg4});
            default:
                throw info.unexpectedCall(4, false);
        }
    }

    @Override
    public PyObject fancyCall(PyObject[] args) {
        switch (this.index) {
            case 29:
                return __builtin__.map(args);
            case 43:
                return __builtin__.zip(args);
            default:
                throw info.unexpectedCall(args.length, false);
        }
    }

    @Override
    public PyObject getModule() {
        return module;
    }
}

/**
 * The builtin module. All builtin functions are defined here
 */
public class __builtin__ {

    private static final PyStringMap internedStrings = new PyStringMap();

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
        dict.__setitem__("slice", PySlice.TYPE);
        dict.__setitem__("xrange", PyXRange.TYPE);

        dict.__setitem__("None", Py.None);
        dict.__setitem__("NotImplemented", Py.NotImplemented);
        dict.__setitem__("Ellipsis", Py.Ellipsis);
        dict.__setitem__("True", Py.True);
        dict.__setitem__("False", Py.False);
        dict.__setitem__("bytes", PyString.TYPE);
        dict.__setitem__("bytearray", PyByteArray.TYPE);
        dict.__setitem__("buffer", Py2kBuffer.TYPE);
        dict.__setitem__("memoryview", PyMemoryView.TYPE);

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
        dict.__setitem__("max", new MaxFunction());
        dict.__setitem__("min", new MinFunction());
        dict.__setitem__("oct", new BuiltinFunctions("oct", 32, 1));
        dict.__setitem__("pow", new BuiltinFunctions("pow", 33, 2, 3));
        dict.__setitem__("raw_input", new BuiltinFunctions("raw_input", 34, 0, 1));
        dict.__setitem__("reduce", new BuiltinFunctions("reduce", 35, 2, 3));
        dict.__setitem__("reload", new BuiltinFunctions("reload", 36, 1));
        dict.__setitem__("repr", new BuiltinFunctions("repr", 37, 1));
        dict.__setitem__("round", new RoundFunction());
        dict.__setitem__("setattr", new BuiltinFunctions("setattr", 39, 3));
        dict.__setitem__("vars", new BuiltinFunctions("vars", 41, 0, 1));
        dict.__setitem__("zip", new BuiltinFunctions("zip", 43, 0, -1));
        dict.__setitem__("compile", new CompileFunction());
        dict.__setitem__("open", new OpenFunction());
        dict.__setitem__("reversed", new BuiltinFunctions("reversed", 45, 1));
        dict.__setitem__("__import__", new ImportFunction());
        dict.__setitem__("sorted", new SortedFunction());
        dict.__setitem__("all", new AllFunction());
        dict.__setitem__("any", new AnyFunction());
        dict.__setitem__("format", new FormatFunction());
        dict.__setitem__("print", new PrintFunction());
        dict.__setitem__("next", new NextFunction());
        dict.__setitem__("bin", new BinFunction());
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
        Map<PyObject, PyObject> table = kws.getMap();
        if (table.size() > 0) {
            Iterator<PyObject> ik = table.keySet().iterator();
            Iterator<PyObject> iv = table.values().iterator();
            int n = table.size();
            kw = new String[n];
            PyObject[] aargs = Py.make_array(args);
            a = new PyObject[n + aargs.length];
            System.arraycopy(aargs, 0, a, 0, aargs.length);
            int offset = aargs.length;

            for (int i = 0; i < n; i++) {
                PyObject name = ik.next();
                if (name.getClass() != PyString.class) {
                    throw Py.TypeError(String.format("keywords must be strings"));
                }
                kw[i] = ((PyString)name).internedString();
                a[i + offset] = iv.next();
            }
            return o.__call__(a, kw);
        } else {
            return apply(o, args);
        }
    }

    public static boolean callable(PyObject obj) {
        return obj.isCallable();
    }

    public static int unichr(PyObject obj) {
        long l = obj.asLong();
        if (l < PySystemState.minint) {
            throw Py.OverflowError("signed integer is less than minimum");
        } else if (l > PySystemState.maxint) {
            throw Py.OverflowError("signed integer is greater than maximum");
        }
        return unichr((int)l);
    }

    public static int unichr(int i) {
        if (i < 0 || i > PySystemState.maxunicode) {
            throw Py.ValueError("unichr() arg not in range(0x110000)");
        }
        if (i >= 0xD800 && i <= 0xDFFF) {
            throw Py.ValueError("unichr() arg is a lone surrogate in range (0xD800, 0xDFFF) (Jython UTF-16 encoding)");
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

    public static void delattr(PyObject obj, PyObject name) {
        obj.__delattr__(asName(name, "delattr"));
    }

    public static PyObject dir(PyObject o) {
        PyObject ret = o.__dir__();
        if (!Py.isInstance(ret, PyList.TYPE)) {
            throw Py.TypeError("__dir__() must return a list, not " + ret.getType().fastGetName());
        }
        ((PyList)ret).sort();
        return ret;
    }

    public static PyObject dir() {
        PyObject l = locals();
        PyList ret;
        PyObject retObj = l.invoke("keys");
        try {
            ret = (PyList) retObj;
        } catch (ClassCastException e) {
            throw Py.TypeError(String.format("Expected keys() to be a list, not '%s'",
                                             retObj.getType().fastGetName()));
        }
        ret.sort();
        return ret;
    }

    public static PyObject divmod(PyObject x, PyObject y) {
        return x._divmod(y);
    }

    private static boolean isMappingType(PyObject o) {
        return o == null || o == Py.None || o.isMappingType();
    }

    private static void verify_mappings(PyObject globals, PyObject locals) {
        if (!isMappingType(globals)) {
            throw Py.TypeError("globals must be a mapping");
        }
        if (!isMappingType(locals)) {
            throw Py.TypeError("locals must be a mapping");
        }
    }

    public static PyObject eval(PyObject o, PyObject globals, PyObject locals) {
        verify_mappings(globals, locals);
        PyCode code;
        if (o instanceof PyCode) {
            code = (PyCode) o;
        } else {
            if (o instanceof PyString) {
                code = (PyCode)CompileFunction.compile(o, "<string>", "eval");
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
        if (o instanceof PyBaseCode && ((PyBaseCode) o).hasFreevars()) {
            throw Py.TypeError("code object passed to eval() may not contain free variables");
        }
        return eval(o, null, null);
    }

    public static void execfile(String name, PyObject globals, PyObject locals) {
        execfile_flags(name, globals, locals, Py.getCompilerFlags());
    }

    public static void execfile_flags(String name, PyObject globals, PyObject locals,
                                      CompilerFlags cflags) {
        verify_mappings(globals, locals);
        FileInputStream file;
        try {
            file = new FileInputStream(new RelativeFile(name));
        } catch (FileNotFoundException e) {
            throw Py.IOError(e);
        }
        PyCode code;

        try {
            code = Py.compile_flags(file, name, CompileMode.exec, cflags);
        } finally {
            try {
                file.close();
            } catch (IOException e) {
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
            return filterBaseString(func, (PyString)seq,
                                    seq instanceof PyUnicode ? PyUnicode.TYPE : PyString.TYPE);
        }
        if (seq instanceof PyTuple) {
            return filterTuple(func, (PyTuple)seq);
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

    public static PyObject filterBaseString(PyObject func, PyBaseString seq, PyType stringType) {
        if (func == Py.None && seq.getType() == stringType) {
            // If it's a real string we can return the original, as no character is ever
            // false and __getitem__ does return this character. If it's a subclass we
            // must go through the __getitem__ loop
            return seq;
        }

        StringBuilder builder = new StringBuilder();
        for (PyObject item : seq.asIterable()) {
            if (func == Py.None) {
                if (!item.__nonzero__()) {
                    continue;
                }
            } else if (!func.__call__(item).__nonzero__()) {
                continue;
            }
            if (!Py.isInstance(item, stringType)) {
                String name = stringType.fastGetName();
                throw Py.TypeError(String.format("can't filter %s to %s: __getitem__ returned "
                                                 + "different type", name, name));
            }
            builder.append(item.toString());
        }

        String result = builder.toString();
        return stringType == PyString.TYPE ? new PyString(result) : new PyUnicode(result);
    }

    public static PyObject filterTuple(PyObject func, PyTuple seq) {
        int len = seq.size();
        if (len == 0) {
            if (seq.getType() != PyTuple.TYPE) {
                return Py.EmptyTuple;
            }
            return seq;
        }

        PyList list = new PyList();
        PyObject item;
        for (int i = 0; i < len; i++) {
            item = seq.__finditem__(i);
            if (func == Py.None) {
                if (!item.__nonzero__()) {
                    continue;
                }
            } else if (!func.__call__(item).__nonzero__()) {
                continue;
            }
            list.append(item);
        }
        return PyTuple.fromIterable(list);
    }

    public static PyObject getattr(PyObject obj, PyObject name) {
        return getattr(obj, name, null);
    }

    public static PyObject getattr(PyObject obj, PyObject nameObj, PyObject def) {
        String name = asName(nameObj, "getattr");
        PyObject result = null;
        PyException attributeError = null;

        try {
            result = obj.__findattr_ex__(name);
        } catch (PyException pye) {
            if (!pye.match(Py.AttributeError)) {
                throw pye;
            }
            attributeError = pye;
        }
        if (result != null) {
            return result;
        }
        if (def != null) {
            return def;
        }

        if (attributeError == null) {
            // throws AttributeError
            obj.noAttributeError(name);
        }
        throw attributeError;
    }

    public static PyObject globals() {
        return Py.getFrame().f_globals;
    }

    public static boolean hasattr(PyObject obj, PyObject nameObj) {
        String name = asName(nameObj, "hasattr");
        try {
            return obj.__findattr__(name) != null;
        } catch (PyException pye) {
            if (pye.match(Py.KeyboardInterrupt) || pye.match(Py.SystemExit)) {
                throw pye;
            }
            //Otherwise swallow exception.
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
        return input(Py.EmptyString);
    }

    public static PyString intern(PyObject obj) {
        if (!(obj instanceof PyString) || obj instanceof PyUnicode) {
            throw Py.TypeError("intern() argument 1 must be string, not "
                               + obj.getType().fastGetName());
        }
        if (obj.getType() != PyString.TYPE) {
            throw Py.TypeError("can't intern subclass of string");
        }
        PyString s = (PyString)obj;
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

    public static int len(PyObject obj) {
        return obj.__len__();
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
            iters[j] = Py.iter(argstar[j + 1], "argument " + (j + 1)
                               + " to map() must support iteration");
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

    /**
     * Built-in Python function ord() applicable to the string-like types <code>str</code>,
     * <code>bytearray</code>, <code>unicode</code>.
     *
     * @param c string-like object of length 1
     * @return ordinal value of character or byte value in
     * @throws PyException {@code TypeError} if not a string-like type
     */
    public static final int ord(PyObject c) throws PyException {
        final int length;

        if (c instanceof PyUnicode) {
            String cu = ((PyUnicode)c).getString();
            length = cu.codePointCount(0, cu.length());
            if (length == 1) {
                return cu.codePointAt(0);
            }

        } else if (c instanceof PyString) {
            String cs = ((PyString)c).getString();
            length = cs.length();
            if (length == 1) {
                return cs.charAt(0);
            }

        } else if (c instanceof BaseBytes) {
            BaseBytes cb = (BaseBytes)c;
            length = cb.__len__();
            if (length == 1) {
                return cb.intAt(0);
            }

        } else {
            // Not any of the acceptable types
            throw Py.TypeError("ord() expected string of length 1, but "
                    + c.getType().fastGetName() + " found");
        }
        /*
         * It was a qualifying string-like object, but if we didn't return or throw by now, the
         * problem was the length.
         */
        throw Py.TypeError("ord() expected a character, but string of length " + length + " found");
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

    public static PyObject range(PyObject n) {
        return range(Py.Zero, n, Py.One);
    }

    public static PyObject range(PyObject start, PyObject stop) {
        return range(start, stop, Py.One);
    }

    public static PyObject range(PyObject ilow, PyObject ihigh, PyObject istep) {
        ilow = getRangeLongArgument(ilow, "start");
        ihigh = getRangeLongArgument(ihigh, "end");
        istep = getRangeLongArgument(istep, "step");

        int n;
        int cmpResult = istep._cmp(Py.Zero);
        if (cmpResult == 0) {
            throw Py.ValueError("range() step argument must not be zero");
        }
        if (cmpResult > 0) {
            n = getLenOfRangeLongs(ilow, ihigh, istep);
        } else {
            n = getLenOfRangeLongs(ihigh, ilow, istep.__neg__());
        }
        if (n < 0) {
            throw Py.OverflowError("range() result has too many items");
        }

        PyObject[] range = new PyObject[n];
        for (int i = 0; i < n; i++) {
            range[i] = ilow;
            ilow = ilow._add(istep);
        }
        return new PyList(range);
    }

    /**
     * Return number of items in range (lo, hi, step), when arguments are PyInteger or
     * PyLong objects. step > 0 required. Return a value < 0 if & only if the true value
     * is too large to fit in an int, or there is an error.
     *
     * @param lo PyInteger or PyLong value
     * @param hi PyInteger or PyLong value
     * @param step PyInteger or PyLong value (> 0)
     * @return int length of range
     */
    private static int getLenOfRangeLongs(PyObject lo, PyObject hi, PyObject step) {
        // if (lo >= hi), return length of 0
        if (lo._cmp(hi) >= 0) {
            return 0;
        }
        try {
            // See PyXRange.getLenOfRange for the primitive version
            PyObject diff = hi._sub(lo)._sub(Py.One);
            PyObject n = diff.__floordiv__(step).__add__(Py.One);
            return n.asInt();
        } catch (PyException pye) {
            return -1;
        }
    }

    /**
     * Helper function for handleRangeLongs. If arg is int or long object, returns it.  If
     * arg is float, raises type error. As a last resort, creates a new int by calling arg
     * type's __int__ method if it is defined.
     */
    private static PyObject getRangeLongArgument(PyObject arg, String name) {
        if (arg instanceof PyInteger || arg instanceof PyLong) {
            return arg;
        }

        // isNumberType is roughly arg.__findattr__("__int__") != null. Equiv. to
        // CPython's type.nb_init != null
        if (arg instanceof PyFloat || !arg.isNumberType()) {
            throw Py.TypeError(String.format("range() integer %s argument expected, got %s.",
                                             name, arg.getType().fastGetName()));
        }

        PyObject intObj = arg.__int__();
        if (intObj instanceof PyInteger || intObj instanceof PyLong) {
            return intObj;
        }
        throw Py.TypeError("__int__ should return int object");
    }

    private static PyString readline(PyObject file) {
        if (file instanceof PyFile) {
            return ((PyFile) file).readline();
        } else {
            PyObject ret = file.invoke("readline");
            if (!(ret instanceof PyString)) {
                throw Py.TypeError("object.readline() returned non-string");
            }
            return (PyString) ret;
        }
    }

    /**
     * Companion to <code>raw_input</code> built-in function used when the interactive interpreter
     * is directed to a file.
     *
     * @param prompt to issue at console before read
     * @param file a file-like object to read from
     * @return line of text from the file (encoded as bytes values compatible with PyString)
     */
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

    /**
     * Implementation of <code>raw_input(prompt)</code> built-in function using the console
     * indirectly via <code>sys.stdin</code> and <code>sys.stdin</code>.
     *
     * @param prompt to issue at console before read
     * @return line of text from console (encoded as bytes values compatible with PyString)
     */
    public static String raw_input(PyObject prompt) {
        PyObject stdin = Py.getSystemState().stdin;
        if (stdin instanceof PyAttributeDeleted) {
            throw Py.RuntimeError("[raw_]input: lost sys.stdin");
        }
        return raw_input(prompt, stdin);
    }

    /**
     * Implementation of <code>raw_input()</code> built-in function using the console directly.
     *
     * @return line of text from console (encoded as bytes values compatible with PyString)
     */
    public static String raw_input() {
        return raw_input(Py.EmptyString);
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

    public static PyObject reload(PyObject o) {
        Object module = o.__tojava__(PyModule.class);
        if (module == Py.NoConversion) {
            if (o instanceof PySystemState) {
                return __builtin__.reload((PySystemState)o);
            } else if(o instanceof PyJavaType) {
                // This has always been a no-op.  Should be disabled in py3k.
                return o;
            }
            throw Py.TypeError("reload() argument must be a module");
        }
        return __builtin__.reload((PyModule) module);
    }

    public static PyObject reload(PyModule o) {
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

    public static void setattr(PyObject obj, PyObject name, PyObject value) {
        obj.__setattr__(asName(name, "setattr"), value);
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
        PyObject reversed = seq.__findattr__("__reversed__");
        if (reversed != null) {
            return reversed.__call__();
        } else if (seq.__findattr__("__getitem__") != null && seq.__findattr__("__len__") != null
            && seq.__findattr__("keys") == null) {
            reversed = new PyReversedIterator(seq);
        } else {
            throw Py.TypeError("argument to reversed() must be a sequence");
        }
        return reversed;
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
            if (e.match(Py.AttributeError)) {
                throw Py.TypeError("vars() argument must have __dict__ attribute");
            }
            throw e;
        }
    }

    public static PyString __doc__zip = PyString.fromInterned(
        "zip(seq1 [, seq2 [...]]) -> [(seq1[0], seq2[0] ...), (...)]\n\n" +
        "Return a list of tuples, where each tuple contains the i-th element\n" +
        "from each of the argument sequences.  The returned list is\n" +
        "truncated in length to the length of the shortest argument sequence.");

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
                    if (e.match(Py.StopIteration)) {
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
        return __import__(name, null, null, null, imp.DEFAULT_LEVEL);
    }

    public static PyObject __import__(String name, PyObject globals) {
        return __import__(name, globals, null, null, imp.DEFAULT_LEVEL);
    }

    public static PyObject __import__(String name, PyObject globals, PyObject locals) {
        return __import__(name, globals, locals, null, imp.DEFAULT_LEVEL);
    }

    public static PyObject __import__(String name, PyObject globals, PyObject locals,
                                      PyObject fromlist) {
        return __import__(name, globals, locals, fromlist, imp.DEFAULT_LEVEL);
    }

    public static PyObject __import__(String name, PyObject globals, PyObject locals,
                                      PyObject fromlist, int level) {
        PyFrame frame = Py.getFrame();
        PyObject builtins;
        if (frame != null && frame.f_builtins != null) {
            builtins = frame.f_builtins;
        } else {
            builtins = Py.getSystemState().builtins;
        }

        PyObject __import__ = builtins.__finditem__("__import__");
        if (__import__ == null) {
            return null;
        }

        PyObject[] args;
        if (level < 0) {
        	// for backward compatibility provide only 4 arguments
        	args = new PyObject[] {Py.newString(name), globals, locals,
        			fromlist};
        } else {
        	args = new PyObject[] {Py.newString(name), globals, locals,
        			fromlist, Py.newInteger(level)};
        }
        PyObject module = __import__.__call__(args);
        return module;
    }

    /**
     * Return an interned String from name, raising a TypeError when conversion fails.
     *
     * @param name a PyObject
     * @param function name of the python function caller
     * @return an interned String
     */
    private static String asName(PyObject name, String function) {
        if (name instanceof PyUnicode) {
            return ((PyUnicode)name).encode().intern();
        } else if (name instanceof PyString) {
            return ((PyString)name).internedString();
        }
        throw Py.TypeError(function + "(): attribute name must be string");
    }
}

@Untraversable
class ImportFunction extends PyBuiltinFunction {
    ImportFunction() {
        super("__import__",
              "__import__(name, globals={}, locals={}, fromlist=[], level=-1) -> module\n\n" +
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

    private static final String[] ARGS = {"name", "globals", "locals", "fromlist", "level"};

    @Override
    public PyObject __call__(PyObject args[], String keywords[]) {
        ArgParser ap = new ArgParser("__import__", args, keywords, ARGS, 1);
        PyObject module = ap.getPyObject(0);
        String name;
        if (module instanceof PyUnicode) {
            name = ((PyUnicode) module).encode("ascii").toString();
        } else {
            name = ap.getString(0);
        }
        PyObject globals = ap.getPyObject(1, null);
        PyObject fromlist = ap.getPyObject(3, Py.EmptyTuple);
        int level = ap.getInt(4, imp.DEFAULT_LEVEL);
        return imp.importName(name.intern(), fromlist == Py.None || fromlist.__len__() == 0,
                              globals, fromlist, level);
    }
}

@Untraversable
class SortedFunction extends PyBuiltinFunction {
    SortedFunction() {
        super("sorted", "sorted(iterable, cmp=None, key=None, reverse=False) --> new sorted list");
    }

    @Override
    public PyObject __call__(PyObject args[], String kwds[]) {
        if (args.length == 0) {
            throw Py.TypeError("sorted() takes at least 1 argument (0 given)");
        } else if (args.length > 4) {
            throw Py.TypeError(String.format("sorted() takes at most 4 arguments (%s given)",
                                             args.length));
        } else {
            PyObject iter = args[0].__iter__();
            if (iter == null) {
                throw Py.TypeError(String.format("'%s' object is not iterable",
                                                 args[0].getType().fastGetName()));
            }
        }

        PyList seq = new PyList(args[0]);

        PyObject newargs[] = new PyObject[args.length - 1];
        System.arraycopy(args, 1, newargs, 0, args.length - 1);
        ArgParser ap = new ArgParser("sorted", newargs, kwds,
                                     new String[] {"cmp", "key", "reverse"}, 0);
        PyObject cmp = ap.getPyObject(0, Py.None);
        PyObject key = ap.getPyObject(1, Py.None);
        PyObject reverse = ap.getPyObject(2, Py.None);

        seq.sort(cmp, key, reverse);
        return seq;
    }
}

@Untraversable
class AllFunction extends PyBuiltinFunctionNarrow {
    AllFunction() {
        super("all", 1, 1,
              "all(iterable) -> bool\n\n" +
              "Return True if bool(x) is True for all values x in the iterable.");
    }

    @Override
    public PyObject __call__(PyObject arg) {
        PyObject iter = arg.__iter__();
        if (iter == null) {
            throw Py.TypeError("'" + arg.getType().fastGetName() + "' object is not iterable");
        }
        for (PyObject item : iter.asIterable()) {
            if (!item.__nonzero__()) {
                return Py.False;
            }
        }
        return Py.True;
    }
}

@Untraversable
class AnyFunction extends PyBuiltinFunctionNarrow {
    AnyFunction() {
        super("any", 1, 1,
              "any(iterable) -> bool\n\n" +
              "Return True if bool(x) is True for any x in the iterable.");
    }

    @Override
    public PyObject __call__(PyObject arg) {
        PyObject iter = arg.__iter__();
        if (iter == null) {
            throw Py.TypeError("'" + arg.getType().fastGetName() + "' object is not iterable");
        }
        for (PyObject item : iter.asIterable()) {
            if (item.__nonzero__()) {
                return Py.True;
            }
        }
        return Py.False;
    }
}

@Untraversable
class FormatFunction extends PyBuiltinFunctionNarrow {
    FormatFunction() {
        super("format", 1, 2,
              "format(value[, format_spec]) -> string\n\n" +
              "Returns value.__format__(format_spec)\n" +
               "format_spec defaults to \"\"");
    }

    @Override
    public PyObject __call__(PyObject arg1) {
        return __call__(arg1, Py.EmptyString);
    }

    @Override
    public PyObject __call__(PyObject arg1, PyObject arg2) {
        PyObject formatted = arg1.__format__(arg2);
        if (!Py.isInstance(formatted, PyString.TYPE) && !Py.isInstance(formatted, PyUnicode.TYPE)  ) {
            throw Py.TypeError("instance.__format__ must return string or unicode, not " + formatted.getType().fastGetName());
        }
        return formatted;
    }
}

@Untraversable
class PrintFunction extends PyBuiltinFunction {
    PrintFunction() {

        super("print",
              "print(value, ..., sep=' ', end='\\n', file=sys.stdout)\n\n" +
              "Prints the values to a stream, or to sys.stdout by default.\n" +
              "Optional keyword arguments:\n" +
              "file: a file-like object (stream); defaults to the current sys.stdout.\n" +
              "sep:  string inserted between values, default a space.\n" +
              "end:  string appended after the last value, default a newline.\n");
    }

    @Override
    public PyObject __call__(PyObject args[], String kwds[]) {
        int kwlen = kwds.length;
        int argslen = args.length;
        boolean useUnicode = false;
        PyObject values[] = new PyObject[argslen - kwlen];
        System.arraycopy(args, 0, values, 0, argslen - kwlen);
        PyObject keyValues[] = new PyObject[kwlen];
        System.arraycopy(args, argslen - kwlen, keyValues, 0, kwlen);
        ArgParser ap = new ArgParser("print", keyValues, kwds, new String[] {"sep", "end", "file"});
        for (PyObject keyValue: keyValues) {
            if (keyValue instanceof PyUnicode) {
                //If "file" is passed in as PyUnicode, that's OK as it will error later.
                useUnicode = true;
            }
        }
        String sep = ap.getString(0, null);
        String end = ap.getString(1, null);
        PyObject file = ap.getPyObject(2, null);
        return print(values, sep, end, file, useUnicode);
    }

    private static PyObject print(PyObject values[], String sep, String end,
                                  PyObject file, boolean useUnicode) {
        StdoutWrapper out;
        if (file != null && file != Py.None) {
            out = new FixedFileWrapper(file);
        } else {
            out = Py.stdout;
        }
        if (values.length == 0) {
            out.println(useUnicode);
        } else {
            if (!useUnicode) {
                for (PyObject value: values) {
                    if (value instanceof PyUnicode) {
                        useUnicode = true;
                        break;
                    }
                }
            }

            PyObject sepObject;
            if (sep == null) {
                sepObject = useUnicode ? Py.UnicodeSpace : Py.Space;
            } else {
                sepObject = useUnicode ? Py.newUnicode(sep) : Py.newString(sep);
            }

            PyObject endObject;
            if (end == null) {
                endObject = useUnicode ? Py.UnicodeNewline : Py.Newline;
            } else {
                endObject = useUnicode ? Py.newUnicode(end) : Py.newString(end);
            }

            out.print(values, sepObject, endObject);
        }
        return Py.None;
    }
}

@Untraversable
class MaxFunction extends PyBuiltinFunction {
    MaxFunction() {
        super("max",
              "max(iterable[, key=func]) -> value\n" +
              "max(a, b, c, ...[, key=func]) -> value\n\n" +
              "With a single iterable argument, return its largest item.\n" +
              "With two or more arguments, return the largest argument.");
    }

    @Override
    public PyObject __call__(PyObject args[], String kwds[]) {
        int argslen = args.length;
        PyObject key = null;

        if (args.length - kwds.length == 0) {
            throw Py.TypeError("max() expected 1 arguments, got 0");
        }
        if (kwds.length > 0) {
            if (kwds[0].equals("key")) {
                key = args[argslen - 1];
                PyObject newargs[] = new PyObject[argslen - 1];
                System.arraycopy(args, 0, newargs, 0, argslen - 1);
                args = newargs;
            } else {
                throw Py.TypeError("max() got an unexpected keyword argument");
            }
        }

        if (args.length > 1) {
            return max(new PyTuple(args), key);
        } else {
            return max(args[0], key);
        }
    }

    private static PyObject max(PyObject o, PyObject key) {
        PyObject max = null;
        PyObject maxKey = null;
        for (PyObject item : o.asIterable()) {
            PyObject itemKey;
            if (key == null) {
                itemKey = item;
            } else {
                itemKey = key.__call__(item);
            }
            if (maxKey == null || itemKey._gt(maxKey).__nonzero__()) {
                maxKey = itemKey;
                max = item;
            }
        }
        if (max == null) {
            throw Py.ValueError("max of empty sequence");
        }
        return max;
    }

}

@Untraversable
class MinFunction extends PyBuiltinFunction {
    MinFunction() {
        super("min",
              "min(iterable[, key=func]) -> value\n" +
              "min(a, b, c, ...[, key=func]) -> value\n\n" +
              "With a single iterable argument, return its smallest item.\n" +
              "With two or more arguments, return the smallest argument.'");
    }

    @Override
    public PyObject __call__(PyObject args[], String kwds[]) {
        int argslen = args.length;
        PyObject key = null;

        if (args.length - kwds.length == 0) {
            throw Py.TypeError("min() expected 1 arguments, got 0");
        }
        if (kwds.length > 0) {
            if (kwds[0].equals("key")) {
                key = args[argslen - 1];
                PyObject newargs[] = new PyObject[argslen - 1];
                System.arraycopy(args, 0, newargs, 0, argslen - 1);
                args = newargs;
            } else {
                throw Py.TypeError("min() got an unexpected keyword argument");
            }
        }

        if (args.length > 1) {
            return min(new PyTuple(args), key);
        } else {
            return min(args[0], key);
        }
    }

    private static PyObject min(PyObject o, PyObject key) {
        PyObject min = null;
        PyObject minKey = null;
        for (PyObject item : o.asIterable()) {
            PyObject itemKey;
            if (key == null) {
                itemKey = item;
            } else {
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

@Untraversable
class RoundFunction extends PyBuiltinFunction {
    RoundFunction() {
        super("round", "round(number[, ndigits]) -> floating point number\n\n" +
              "Round a number to a given precision in decimal digits (default 0 digits).\n" +
              "This always returns a floating point number.  Precision may be negative.");
    }

    @Override
    public PyObject __call__(PyObject args[], String kwds[]) {
        ArgParser ap = new ArgParser("round", args, kwds, new String[] {"number", "ndigits"}, 0);
        PyObject number = ap.getPyObject(0);
        int ndigits = ap.getIndex(1, 0);
        double x = number.asDouble();
        double r = ExtraMath.round(x, ndigits);
        if (Double.isInfinite(r) && !Double.isInfinite(x)) {
            // Rounding caused magnitude to increase beyond representable range
            throw Py.OverflowError("rounded value too large to represent");
        } else {
            return new PyFloat(r);
        }
    }
}

@Untraversable
class CompileFunction extends PyBuiltinFunction {
    CompileFunction() {
        super("compile",
              "compile(source, filename, mode[, flags[, dont_inherit]]) -> code object\n\n"
              + "Compile the source string (a Python module, statement or expression)\n"
              + "into a code object that can be executed by the exec statement or eval().\n"
              + "The filename will be used for run-time error messages.\n"
              + "The mode must be 'exec' to compile a module, 'single' to compile a\n"
              + "single (interactive) statement, or 'eval' to compile an expression.\n"
              + "The flags argument, if present, controls which future statements influence\n"
              + "the compilation of the code.\n"
              + "The dont_inherit argument, if non-zero, stops the compilation inheriting\n"
              + "the effects of any future statements in effect in the code calling\n"
              + "compile; if absent or zero these statements do influence the compilation,\n"
              + "in addition to any features explicitly specified.");
    }

    @Override
    public PyObject __call__(PyObject args[], String kwds[]) {
        ArgParser ap = new ArgParser("compile", args, kwds,
                                     new String[] {"source", "filename", "mode", "flags",
                                                   "dont_inherit"},
                                     3);
        PyObject source = ap.getPyObject(0);
        String filename = Py.fileSystemDecode(ap.getPyObject(1));
        String mode = ap.getString(2);
        int flags = ap.getInt(3, 0);
        boolean dont_inherit = ap.getPyObject(4, Py.False).__nonzero__();
        return compile(source, filename, mode, flags, dont_inherit);
    }

    public static PyObject compile(PyObject source, String filename, String mode) {
        return compile(source, filename, mode, 0, false);
    }

    public static PyObject compile(PyObject source, String filename, String mode, int flags,
                                   boolean dont_inherit) {
        CompilerFlags cflags = Py.getCompilerFlags(flags, dont_inherit);
        CompileMode kind = CompileMode.getMode(mode);

        return compile(source, filename, kind, cflags, dont_inherit);
    }

    public static PyObject compile(PyObject source, String filename, CompileMode kind,
                                   CompilerFlags cflags, boolean dont_inherit) {
        cflags = Py.getCompilerFlags(cflags, dont_inherit);

        mod ast = py2node(source);
        if (ast == null) {
            if (!(source instanceof PyString)) {
                throw Py.TypeError("expected a readable buffer object");
            }
            cflags.source_is_utf8 = source instanceof PyUnicode;

            String data = source.toString();

            if (data.contains("\0")) {
                throw Py.TypeError("compile() expected string without null bytes");
            }
            if (cflags != null && cflags.dont_imply_dedent) {
                data += "\n";
            } else {
                data += "\n\n";
            }
            ast = ParserFacade.parse(data, kind, filename, cflags);
        }

        if (cflags.only_ast) {
            return Py.java2py(ast);
        } else {
            return Py.compile_flags(ast, filename, kind, cflags);
        }
    }

    /**
     * @returns mod if obj is a wrapper around an AST mod else returns
     *          null
     *
     * XXX: Reaches into implementation details -- needs to be reviewed if our
     *      java integration changes.
     */
    private static mod py2node(PyObject obj) {
        Object node = obj.__tojava__(mod.class);
        if (node == Py.NoConversion) {
            return null;
        }
        return (mod)node;
    }
}

@Untraversable
class OpenFunction extends PyBuiltinFunction {
    OpenFunction() {
        super("open", "Open a file using the file() type, returns a file object.  This is the\n"
              + "preferred way to open a file.");
    }

    private static final String warning =
            "Passing an Input/OutputStream to open is deprecated, use "
            + "org.python.core.util.FileUtil.wrap(stream[, bufsize]) instead.";

    @Override
    public PyObject __call__(PyObject args[], String kwds[]) {
        ArgParser ap = new ArgParser("file", args, kwds, new String[] {"name", "mode", "buffering"},
                                     1);
        PyObject obj = ap.getPyObject(0);
        if (obj.getJavaProxy() != null) {
            int bufsize = ap.getInt(2, -1);
            Object javaProxy = obj.getJavaProxy();
            if (javaProxy instanceof InputStream) {
                Py.warning(Py.DeprecationWarning, warning);
                return new PyFile((InputStream) javaProxy, bufsize);
            } else if (javaProxy instanceof OutputStream) {
                Py.warning(Py.DeprecationWarning, warning);
                return new PyFile((OutputStream) javaProxy, bufsize);
            }
        }
        return PyFile.TYPE.__call__(args, kwds);
    }
}

@Untraversable
class NextFunction extends PyBuiltinFunction {
    NextFunction() {
        super("next", "next(iterator[, default])\n\n"
              + "Return the next item from the iterator. If default is given and the iterator\n"
              + "is exhausted, it is returned instead of raising StopIteration.");
    }

    @Override
    public PyObject __call__(PyObject args[], String kwds[]) {
        ArgParser ap = new ArgParser("next", args, kwds, new String[] {"iterator", "default"}, 1);
        ap.noKeywords();
        PyObject it = ap.getPyObject(0);
        PyObject def = ap.getPyObject(1, null);

        PyObject next;
        if ((next = it.__findattr__("next")) == null) {
            throw Py.TypeError(String.format("'%.200s' object is not an iterator",
                                             it.getType().fastGetName()));
        }

        try {
            return next.__call__();
        } catch (PyException pye) {
            if (!pye.match(Py.StopIteration) || def == null) {
                throw pye;
            }
        }
        return def;
    }
}

@Untraversable
class BinFunction extends PyBuiltinFunction {
    BinFunction() {
        super("bin", "bin(number)\n\n"
              + "Return the binary representation of an integer or long integer.");
    }

    @Override
    public PyObject __call__(PyObject args[], String kwds[]) {
        ArgParser ap = new ArgParser("bin", args, kwds, new String[] {"number"}, 1);
        ap.noKeywords();
        return IntegerFormatter.bin(ap.getPyObject(0));
    }
}
