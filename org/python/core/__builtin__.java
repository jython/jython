// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import org.python.parser.SimpleNode;
import java.util.Hashtable;
import java.math.BigInteger;


class BuiltinFunctions extends PyBuiltinFunctionSet
{
    public BuiltinFunctions(String name, int index, int argcount) {
        super(name, index, argcount, argcount, false, null);
    }

    public BuiltinFunctions(String name, int index, int minargs, int maxargs){
        super(name, index, minargs, maxargs, false, null);
    }

    public PyObject __call__() {
        switch (index) {
        case 4:
            return __builtin__.globals();
        default:
            throw argCountError(0);
        }
    }

    public PyObject __call__(PyObject arg1) {
        switch (index) {
        case 0:
            return Py.newString(__builtin__.chr(
                Py.py2int(arg1, "chr(): 1st arg can't be coerced to int")));
        case 1:
            return Py.newInteger(__builtin__.len(arg1));
        case 2:
            return __builtin__.range(
                Py.py2int(arg1, "range(): 1st arg can't be coerced to int"));
        case 3:
            return Py.newInteger(__builtin__.ord(
                Py.py2char(arg1, "ord(): 1st arg can't be coerced to char")));
        case 5:
            return __builtin__.hash(arg1);
        case 7:
            return __builtin__.list(arg1);
        case 8:
            return __builtin__.tuple(arg1);
        case 11:
            return Py.newInteger(__builtin__.id(arg1));
        default:
            throw argCountError(1);
        }
    }

    public PyObject __call__(PyObject arg1, PyObject arg2) {
        switch (index) {
        case 2:
            return __builtin__.range(
                Py.py2int(arg1, "range(): 1st arg can't be coerced to int"),
                Py.py2int(arg2, "range(): 2nd arg can't be coerced to int"));
        case 6:
            return Py.newInteger(__builtin__.cmp(arg1, arg2));
        case 9:
            return __builtin__.apply(arg1, arg2);
        case 10:
            return Py.newBoolean(__builtin__.isinstance(arg1, arg2));
        default:
            throw argCountError(2);
        }
    }

    public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3) {
        switch (index) {
        case 2:
            return __builtin__.range(
                Py.py2int(arg1, "range(): 1st arg can't be coerced to int"),
                Py.py2int(arg2, "range(): 2nd arg can't be coerced to int"),
                Py.py2int(arg3, "range(): 3rd arg can't be coerced to int"));
        case 9:
            try {
                if (arg3 instanceof PyStringMap) {
                    PyDictionary d = new PyDictionary();
                    d.update((PyStringMap) arg3);
                    arg3 = d;
                }

                // this catches both casts of arg3 to a PyDictionary, and
                // all casts of keys in the dictionary to PyStrings inside
                // apply(PyObject, PyObject, PyDictionary)
                PyDictionary d = (PyDictionary)arg3;
                return __builtin__.apply(arg1, arg2, d);
            }
            catch (ClassCastException e) {
                throw Py.TypeError("apply() 3rd argument must be a "+
                                   "dictionary with string keys");
            }
        default:
            throw argCountError(3);
        }
    }
}



/**
 * The builtin module. All builtin functions are defined here
 */
public class __builtin__ implements ClassDictInit
{
    /** <i>Internal use only. Do not call this method explicit.</i> */
    public static void classDictInit(PyObject dict) {
        dict.__setitem__("None", Py.None);
        dict.__setitem__("NotImplemented", Py.NotImplemented);
        dict.__setitem__("Ellipsis", Py.Ellipsis);
        dict.__setitem__("True", Py.One);
        dict.__setitem__("False", Py.Zero);

        // Work in debug mode by default
        // Hopefully add -O option in the future to change this
        dict.__setitem__("__debug__", Py.One);

        dict.__setitem__("chr", new BuiltinFunctions("chr", 0, 1));
        dict.__setitem__("len", new BuiltinFunctions("len", 1, 1));
        dict.__setitem__("range", new BuiltinFunctions("range", 2, 1, 3));
        dict.__setitem__("ord", new BuiltinFunctions("ord", 3, 1));
        dict.__setitem__("globals", new BuiltinFunctions("globals", 4, 0));
        dict.__setitem__("hash", new BuiltinFunctions("hash", 5, 1));
        dict.__setitem__("cmp", new BuiltinFunctions("cmp", 6, 2));
        dict.__setitem__("list", new BuiltinFunctions("list", 7, 1));
        dict.__setitem__("tuple", new BuiltinFunctions("tuple", 8, 1));
        dict.__setitem__("apply", new BuiltinFunctions("apply", 9, 2, 3));
        dict.__setitem__("isinstance",
                                  new BuiltinFunctions("isinstance", 10, 2));
        dict.__setitem__("id", new BuiltinFunctions("id", 11, 1));
        dict.__setitem__("__import__", new ImportFunction());

        dict.__delitem__("execfile_flags"); // -execfile_flags
    }

    public static PyObject abs(PyObject o) {
        if (o.isNumberType())
            return o.__abs__();
        throw Py.TypeError("bad operand type for abs()");
    }

    public static PyObject apply(PyObject o, PyObject args) {
        return o.__call__(make_array(args));
    }

    public static PyObject apply(PyObject o, PyObject args,
                                 PyDictionary kws) {
        PyObject[] a;
        String[] kw;
        Hashtable table = kws.table;
        if (table.size() > 0) {
            java.util.Enumeration ek = table.keys();
            java.util.Enumeration ev = table.elements();
            int n = table.size();
            kw = new String[n];
            PyObject[] aargs = make_array(args);
            a = new PyObject[n+aargs.length];
            System.arraycopy(aargs, 0, a, 0, aargs.length);
            int offset = aargs.length;

            for (int i=0; i<n; i++) {
                kw[i] = ((PyString)ek.nextElement()).internedString();
                a[i+offset] = (PyObject)ev.nextElement();
            }
            return o.__call__(a, kw);
        } else {
            return apply(o, args);
        }
    }

    public static boolean callable(PyObject o) {
        return o.__findattr__("__call__") != null;
    }

    public static char unichr(int i) {
        return chr(i);
    }

    public static char chr(int i) {
        if (i < 0 || i > 65535)
            throw Py.ValueError("chr() arg not in range(65535)");
        return (char)i;
    }

    public static int cmp(PyObject x, PyObject y) {
        return x._cmp(y);
    }

    public static PyTuple coerce(PyObject o1, PyObject o2) {
        Object ctmp;
        PyTuple ret;
        if (o1.__class__ == o2.__class__) {
            return new PyTuple(new PyObject[] {o1, o2});
        }
        ctmp = o1.__coerce_ex__(o2);
        if (ctmp != null && ctmp != Py.None) {
            if (ctmp instanceof PyObject[]) {
                return new PyTuple((PyObject[])ctmp);
            } else {
                return new PyTuple(new PyObject[] {o1, (PyObject)ctmp});
            }
        }
        ctmp = o2.__coerce_ex__(o1);
        if (ctmp != null && ctmp != Py.None) {
            if (ctmp instanceof PyObject[]) {
                return new PyTuple((PyObject[])ctmp);
            } else {
                return new PyTuple(new PyObject[] {(PyObject)ctmp, o2});
            }
        }
        throw Py.TypeError("number coercion failed");
    }

    public static PyCode compile(String data, String filename, String type) {
        return  Py.compile_flags(data,filename,type,Py.getCompilerFlags());
    }

    public static PyCode compile(String data, String filename, String type,
                                 int flags, boolean inherit) {
        return  Py.compile_flags(data,filename,type,new CompilerFlags(flags));
    }

    public static PyComplex complex(PyObject real, PyObject imag) {
        if (real instanceof PyString)
            throw Py.TypeError("complex() can't take second arg" + 
                               " if first is a string");
        if (imag instanceof PyString)
            throw Py.TypeError("complex() second arg can't be a string");
        return (PyComplex)real.__complex__().__add__(
            imag.__complex__().__mul__(PyComplex.J));
    }

    public static PyComplex complex(PyObject real) {
        return real.__complex__();
    }


    public static void delattr(PyObject o, PyString n) {
        o.__delattr__(n);
    }

    public static PyObject dir(PyObject o) {
        PyList ret = (PyList)o.__dir__();
        ret.sort();
        return ret;
    }

    public static PyObject dir() {
        PyObject l = locals();
        PyList ret;

        if (l instanceof PyStringMap)
            ret = ((PyStringMap)l).keys();
        if (l instanceof PyDictionary)
            ret = ((PyDictionary)l).keys();

        ret = (PyList)l.invoke("keys");
        ret.sort();
        return ret;
    }

    public static PyObject divmod(PyObject x, PyObject y) {
        return x._divmod(y);
    }

    public static PyObject eval(PyObject o, PyObject globals, PyObject locals)
    {
        PyCode code;
        if (o instanceof PyCode)
            code = (PyCode)o;
        else {
            if (o instanceof PyString) {
                // eval does not inherit co_nested
                code = Py.compile_flags(((PyString)o).toString(),
                                           "<string>", "eval", null);
            } else
                throw Py.TypeError(
                    "eval: argument 1 must be string or code object");
        }
        return Py.runCode(code, locals, globals);
    }

    public static PyObject eval(PyObject o, PyObject globals) {
        return eval(o, globals, globals);
    }

    public static PyObject eval(PyObject o) {
        return eval(o, null, null);
    }

    public static void execfile(String name, PyObject globals,
                                PyObject locals)
    {
        execfile_flags(name,globals,locals,Py.getCompilerFlags());
    }


    public static void execfile_flags(String name, PyObject globals,
                                PyObject locals,CompilerFlags cflags)
    {
        java.io.FileInputStream file;
        try {
            file = new java.io.FileInputStream(name);
        } catch (java.io.FileNotFoundException e) {
            throw Py.IOError(e);
        }
        PyCode code;

        try {
            code = Py.compile_flags(file, name, "exec",cflags);
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

    public static PyObject filter(PyObject f, PyString s) {
        if (f == Py.None)
            return s;
        PyObject[] args = new PyObject[1];
        char[] chars = s.toString().toCharArray();
        int i;
        int j;
        int n = chars.length;
        for(i=0, j=0; i<n; i++) {
            args[0] = Py.makeCharacter(chars[i]);
            if (!f.__call__(args).__nonzero__())
                continue;
            chars[j++] = chars[i];
        }
        return new PyString(new String(chars, 0, j));
    }


    public static PyObject filter(PyObject f, PyObject l) {
        PyList list = new PyList();
        PyObject iter = l.__iter__();
        for (PyObject item = null; (item = iter.__iternext__()) != null; ) {
            if (f == Py.None) {
                if (!item.__nonzero__())
                    continue;
            } else {
                if (!f.__call__(item).__nonzero__())
                    continue;
            }
            list.append(item);
        }
        return list;
    }

    public static PyFloat float$(PyObject o) {
        return o.__float__();
    }

    public static PyObject getattr(PyObject o, PyString n) {
        return o.__getattr__(n);
    }

    public static PyObject getattr(PyObject o, PyString n, PyObject def) {
        PyObject val = o.__findattr__(n);
        if (val != null)
             return val;
        return def;
    }

    public static PyObject globals() {
        return Py.getFrame().f_globals;
    }

    public static boolean hasattr(PyObject o, PyString n) {
        try {
            return o.__findattr__(n) != null;
        } catch (PyException exc) {
            if (Py.matchException(exc, Py.AttributeError))
                return false;
            throw exc;
        }
    }

    public static PyInteger hash(PyObject o) {
        return o.__hash__();
    }

    public static PyString hex(PyObject o) {
        return o.__hex__();
    }

    public static int id(PyObject o) {
        return Py.id(o);
    }


    public static PyObject input(PyObject prompt) {
        String line = raw_input(prompt);
        return eval(new PyString(line));
    }

    public static PyObject input() {
        return input(new PyString(""));
    }

    public static PyInteger int$(PyString o, int base) {
        return Py.newInteger(o.__str__().atoi(base));
    }

    public static PyInteger int$(PyObject o) {
        return o.__int__();
    }

    public static PyObject object() {
        return new PyObject();
    }

    private static PyStringMap internedStrings;
    public static PyString intern(PyString s) {
        if (internedStrings == null) {
            internedStrings = new PyStringMap();
        }

        String istring = s.internedString();
        PyObject ret = internedStrings.__finditem__(istring);
        if (ret != null)
            return (PyString)ret;

        internedStrings.__setitem__(istring, s);
        return s;
    }

    public static boolean isinstance(PyObject obj, PyObject cls) {
        if (cls instanceof PyClass) {
            return issubclass(obj.__class__, (PyClass) cls);
        } if (cls.getClass() == PyTuple.class) {
            for (int i = 0; i < cls.__len__(); i++) {
                if (isinstance(obj, cls.__getitem__(i)))
                    return true;
            }
            return false;
        } else {
            throw Py.TypeError("isinstance(): 2nd arg is not a class");
        }
    }


    public static boolean issubclass(PyClass subClass, PyClass superClass) {
        if (subClass == null || superClass == null)
            throw Py.TypeError("arguments must be classes");
        if (subClass == superClass)
            return true;
        if (subClass.proxyClass != null && superClass.proxyClass != null) {
            if (superClass.proxyClass.isAssignableFrom(subClass.proxyClass))
                return true;
        }
        if (subClass.__bases__ == null || superClass.__bases__ == null)
            return false;
        PyObject[] bases = subClass.__bases__.list;
        int n = bases.length;
        for(int i=0; i<n; i++) {
            PyClass c = (PyClass)bases[i];
            if (issubclass(c, superClass))
                return true;
        }
        return false;
    }


    public static int len(PyObject o) {
        try {
            return o.__len__();
        }
        catch (PyException e) {
            // Make this work like CPython where
            //
            // a = 7; len(a) raises a TypeError,
            // a.__len__() raises an AttributeError
            // and
            // class F: pass
            // f = F(); len(f) also raises an AttributeError
            //
            // Testing the type of o feels unclean though
            if (e.type == Py.AttributeError && !(o instanceof PyInstance))
                throw Py.TypeError("len() of unsized object");
            else
                throw e;
        }
    }

    public static PyList list(PyObject o) {
        if (o instanceof PyList)
            return (PyList) o.__getslice__(Py.None, Py.None, Py.One);
        if (o instanceof PyTuple) {
            // always make a copy, otherwise the list will share the
            // underlying data structure with the tuple object, which
            // renders the tuple mutable!
            PyTuple t = (PyTuple)o;
            PyObject[] a = new PyObject[t.__len__()];
            System.arraycopy(t.list, 0, a, 0, a.length);
            return new PyList(a);
        }
        return new PyList(o);
    }

    public static PyObject locals() {
        return Py.getFrame().getf_locals();
    }

    public static PyLong long$(BigInteger o) {
        return Py.newLong(o);
    }

    public static PyLong long$(PyObject o) {
        return o.__long__();
    }

    public static PyLong long$(PyString o, int base) {
        return o.__str__().atol(base);
    }

    public static PyObject map(PyObject[] argstar) {
        int n = argstar.length-1;
        if (n < 1)
            throw Py.TypeError("map requires at least two arguments");
        PyObject element;
        PyObject f = argstar[0];
        PyList list = new PyList();
        PyObject[] args = new PyObject[n];
        PyObject[] iters = new PyObject[n];

        for (int j = 0; j < n; j++) {
            iters[j] = Py.iter(argstar[j+1], "argument " + (j+1) +
                               " to map() must support iteration");
        }

        while (true) {
            boolean any_items = false;
            for(int j = 0; j < n; j++) {
                if ((element = iters[j].__iternext__()) != null) {
                    args[j] = element;
                    any_items = true;
                } else {
                    args[j] = Py.None;
                }
            }
            if (!any_items)
                break;
            if (f == Py.None) {
                if (n == 1) {
                    list.append(args[0]);
                } else {
                    list.append(new PyTuple((PyObject[])args.clone()));
                }
            } else {
                list.append(f.__call__(args));
            }
        }
        return list;
    }


    // I've never been happy with max and min builtin's...

    public static PyObject max(PyObject[] l) {
        if (l.length == 1)
            return max(l[0]);
        else return max(new PyTuple(l));
    }

    private static PyObject max(PyObject o) {
        PyObject max = null;
        PyObject iter = o.__iter__();
        for (PyObject item; (item = iter.__iternext__()) != null; ) {
            if (max == null || item._gt(max).__nonzero__())
                max = item;
        }
        if (max == null)
            throw Py.ValueError("max of empty sequence");
        return max;
    }

    public static PyObject min(PyObject[] l) {
        if (l.length == 1)
            return min(l[0]);
        else return min(new PyTuple(l));
    }

    private static PyObject min(PyObject o) {
        PyObject min = null;
        PyObject iter = o.__iter__();
        for (PyObject item; (item = iter.__iternext__()) != null; ) {
            if (min == null || item._lt(min).__nonzero__())
                min = item;
        }
        if (min == null)
            throw Py.ValueError("min of empty sequence");
        return min;
    }

    public static PyString oct(PyObject o) {
        return o.__oct__();
    }

    /**
     * Open a file read-only.
     * @param name the file to open.
     * @exception java.io.IOException
     */
    public static PyFile open(String name) throws java.io.IOException {
        return new PyFile(name, "r", -1);
    }

    /**
     * Open a file with the specified mode.
     * @param name name of the file to open.
     * @param mode open mode of the file. Use "r", "w", "r+", "w+" and "a".
     * @exception java.io.IOException
     */
    public static PyFile open(String name, String mode)
        throws java.io.IOException
    {
        return new PyFile(name, mode, -1);
    }

    /**
     * Open a file with the specified mode and buffer size.
     * @param name name of the file to open.
     * @param mode open mode of the file. Use "r", "w", "r+", "w+" and "a".
     * @param bufsize size of the internal buffer. Not currently used.
     * @exception java.io.IOException
     */
    public static PyFile open(String name, String mode, int bufsize)
        throws java.io.IOException
    {
        return new PyFile(name, mode, bufsize);
    }

    public static final int ord(char c) {
        return (int)(c);
    }

    public static PyObject pow(PyObject x, PyObject y) {
        return x._pow(y);
    }

    private static boolean coerce(PyObject[] objs) {
        PyObject x = objs[0];
        PyObject y = objs[1];
        if (x.__class__ == y.__class__)
            return true;
        Object ctmp = x.__coerce_ex__(y);
        if (ctmp != null && ctmp != Py.None) {
            if (ctmp instanceof PyObject[]) {
                x = ((PyObject[])ctmp)[0];
                y = ((PyObject[])ctmp)[1];
            } else {
                y = (PyObject)ctmp;
            }
        }
        objs[0] = x; objs[1] = y;
        if (x.__class__ == y.__class__)
            return true;
        ctmp = y.__coerce_ex__(x);
        if (ctmp != null && ctmp != Py.None) {
            if (ctmp instanceof PyObject[]) {
                y = ((PyObject[])ctmp)[0];
                x = ((PyObject[])ctmp)[1];
            } else {
                x = (PyObject)ctmp;
            }
        }
        objs[0] = x; objs[1] = y;
        //System.out.println(""+x.__class__+" : "+y.__class__);
        return x.__class__ == y.__class__;
    }

    public static PyObject pow(PyObject xi, PyObject yi, PyObject zi) {
        PyObject x=xi;
        PyObject y=yi;
        PyObject z=zi;

        boolean doit=false;

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
                    doit=true;
                }
            }
        } else {
            tmp[1] = z;
            if (coerce(tmp)) {
                x=tmp[0];
                z=tmp[1];
                tmp[0] = y;
                if (coerce(tmp)) {
                    y=tmp[0]; z = tmp[1];
                    tmp[1] = x;
                    if (coerce(tmp)) {
                        x=tmp[1];
                        y=tmp[0];
                        doit = true;
                    }
                }
            }
        }

        if (x.__class__ == y.__class__ && x.__class__ == z.__class__) {
            x = x.__pow__(y, z);
            if (x != null)
                return x;
        }
        throw Py.TypeError("__pow__ not defined for these operands");
    }

    public static PyObject range(int start, int stop, int step) {
        if (step == 0)
            throw Py.ValueError("zero step for range()");
        int n;
        if (step > 0)
            n = (stop-start+step-1)/step;
        else
            n = (stop-start+step+1)/step;

        if (n <= 0)
            return new PyList();
        PyObject[] l = new PyObject[n];
        int j=start;
        for (int i=0; i<n; i++) {
            l[i] = Py.newInteger(j);
            j+= step;
        }
        return new PyList(l);
    }

    public static PyObject range(int n) {
        return range(0,n,1);
    }

    public static PyObject range(int start, int stop) {
        return range(start,stop,1);
    }

    private static PyString readline(PyObject file) {
        if (file instanceof PyFile) {
            return ((PyFile)file).readline();
        } else {
            PyObject ret = file.invoke("readline");
            if (!(ret instanceof PyString)) {
                throw Py.TypeError("object.readline() returned non-string");
            }
            return (PyString)ret;
        }
    }

    public static String raw_input(PyObject prompt) {
        Py.print(prompt);
        PyObject stdin = Py.getSystemState().stdin;
        String data = readline(stdin).toString();
        if (data.endsWith("\n")) {
            return data.substring(0, data.length()-1);
        } else {
            if (data.length() == 0) {
                throw Py.EOFError("raw_input()");
            }
        }
        return data;
    }

    public static String raw_input() {
        return raw_input(new PyString(""));
    }

    public static PyObject reduce(PyObject f, PyObject l, PyObject z) {
        PyObject result = z;
        PyObject iter = Py.iter(l, "reduce() arg 2 must support iteration");
        
        for (PyObject item; (item = iter.__iternext__()) != null; ) {
            if (result == null)
                result = item;
            else
                result = f.__call__(result, item);
        }
        if (result == null) {
            throw Py.TypeError(
                    "reduce of empty sequence with no initial value");
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

    public static PyString repr(PyObject o) {
        return o.__repr__();
    }

    //This seems awfully special purpose...
    public static PyFloat round(double f, int digits) {
        boolean neg = f < 0;
        double multiple = Math.pow(10., digits);
        if (neg)
            f = -f;
        double tmp = Math.floor(f*multiple+0.5);
        if (neg)
            tmp = -tmp;
        return new PyFloat(tmp/multiple);
    }

    public static PyFloat round(double f) {
        return round(f, 0);
    }

    public static void setattr(PyObject o, PyString n, PyObject v) {
        o.__setattr__(n, v);
    }

    public static PySlice slice(PyObject start, PyObject stop,
                                PyObject step)
    {
        return new PySlice(start, stop, step);
    }

    public static PySlice slice(PyObject start, PyObject stop) {
        return slice(start, stop, Py.None);
    }

    public static PySlice slice(PyObject stop) {
        return slice(Py.None, stop, Py.None);
    }

    public static PyObject iter(PyObject obj) {
        return obj.__iter__();
    }

    public static PyObject iter(PyObject callable, PyObject sentinel) {
        return new PyCallIter(callable, sentinel);
    }

    public static PyString str(PyObject o) {
        return o.__str__();
    }

    public static PyString unicode(PyObject v) {
        return unicode(v.__str__(), null, null);
    }

    public static PyString unicode(PyString v, String encoding) {
        return unicode(v, encoding, null);
    }

    public static PyString unicode(PyString v, String encoding,
                                  String errors)
    {
        return codecs.decode(v, encoding, errors);
    }

    public static PyTuple tuple(PyObject o) {
        if (o instanceof PyTuple)
            return (PyTuple)o;
        if (o instanceof PyList) {
            // always make a copy, otherwise the tuple will share the
            // underlying data structure with the list object, which
            // renders the tuple mutable!
            PyList l = (PyList)o;
            PyObject[] a = new PyObject[l.length];
            System.arraycopy(l.list, 0, a, 0, a.length);
            return new PyTuple(a);
        }
        return new PyTuple(make_array(o));
    }

    public static PyClass type(PyObject o) {
        if (o instanceof PyInstance) {
            // was just PyInstance.class, goes with experimental
            // PyMetaClass hook
            return PyJavaClass.lookup(o.getClass());
        } else {
            return o.__class__;
        }
    }

    public static PyObject vars(PyObject o) {
        return o.__getattr__("__dict__");
    }

    public static PyObject vars() {
        return locals();
    }

    public static PyObject xrange(int start, int stop, int step) {
        return new PyXRange(start, stop, step);
    }

    public static PyObject xrange(int n) {
        return xrange(0,n,1);
    }

    public static PyObject xrange(int start, int stop) {
        return xrange(start,stop,1);
    }

    public static PyString __doc__zip = new PyString(
      "zip(seq1 [, seq2 [...]]) -> [(seq1[0], seq2[0] ...), (...)]\n"+
      "\n"+
      "Return a list of tuples, where each tuple contains the i-th element\n"+
      "from each of the argument sequences.  The returned list is\n"+
      "truncated in length to the length of the shortest argument sequence."
    );

    public static PyObject zip(PyObject[] argstar) {
        int itemsize = argstar.length;
        if (itemsize < 1)
            throw Py.TypeError("zip requires at least one sequence");

        // Type check the arguments; they must be sequences.  Might as well
        // cache the __iter__() methods.
        PyObject[] iters = new PyObject[itemsize];

        for (int j=0; j < itemsize; j++) {
            PyObject iter = argstar[j].__iter__();
            if (iter == null) {
                throw Py.TypeError("zip argument #" + (j + 1) +
                                   " must support iteration");
            }
            iters[j] = iter;
        }

        PyList ret = new PyList();

        for (int i=0;; i++) {
            PyObject[] next = new PyObject[itemsize];
            PyInteger index = new PyInteger(i);
            PyObject item;

            for (int j=0; j < itemsize; j++) {
                try {
                    item = iters[j].__iternext__();
                }
                catch (PyException e) {
                    if (Py.matchException(e, Py.StopIteration))
                        return ret;
                    throw e;
                }
                if (item == null)
                    return ret;
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

    public static PyObject __import__(String name, PyObject globals,
                                      PyObject locals) {
        return __import__(name, globals, locals, null);
    }

    public static PyObject __import__(String name, PyObject globals,
                                      PyObject locals,PyObject fromlist)
    {
        PyFrame frame = Py.getFrame();
        if (frame == null)
            return null;

        PyObject builtins = frame.f_builtins;
        if (builtins == null)
            builtins = Py.getSystemState().builtins;

        PyObject __import__ = builtins.__finditem__("__import__");
        if (__import__ == null)
            return null;

        PyObject module = __import__.__call__(new PyObject[] {
                Py.newString(name),
                globals,
                locals,
                fromlist } );
        return module;
    }

    private static PyObject[] make_array(PyObject o) {
        if (o instanceof PyTuple)
            return ((PyTuple)o).list;

        int n = o.__len__();
        PyObject[] objs= new PyObject[n];

        for(int i=0; i<n; i++) {
            objs[i] = o.__finditem__(i); // XXX: convert to __iter__!
        }
        return objs;
    }
}


class ImportFunction extends PyObject {
    public ImportFunction() {}

    public PyObject __call__(PyObject args[], String keywords[]) {
        if (!(args.length < 1 || args[0] instanceof PyString))
            throw Py.TypeError("first argument must be a string");
        if (keywords.length > 0)
            throw Py.TypeError("__import__() takes no keyword arguments");

        int argc = args.length;
        String module = args[0].__str__().toString();

        PyObject globals = (argc > 1 && args[1] != null)
            ? args[1] : null;
        PyObject fromlist = (argc > 3 && args[3] != null)
            ? args[3] : Py.EmptyTuple;

        return load(module, globals, fromlist);
    }

    private PyObject load(String module,
                          PyObject globals,
                          PyObject fromlist)
    {
        PyObject mod = imp.importName(module.intern(),
                                      fromlist.__len__() == 0,
                                      globals);
        return mod;
    }

    public String toString() {
        return "<built-in function __import__>";
    }
}
