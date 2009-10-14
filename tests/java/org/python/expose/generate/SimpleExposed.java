package org.python.expose.generate;

import org.python.core.Py;
import org.python.core.PyInteger;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.core.ThreadState;
import org.python.expose.ExposedClassMethod;
import org.python.expose.ExposedDelete;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

@ExposedType(name = "simpleexposed", isBaseType = false, doc = "Docstring")
public class SimpleExposed extends PyObject {

    public void method() {}

    public int timesCalled;

    @ExposedNew
    public static PyObject __new__(PyNewWrapper new_,
                                   boolean init,
                                   PyType subtype,
                                   PyObject[] args,
                                   String[] keywords) {
        return Py.One;
    }

    @ExposedMethod
    void invisible() {}

    @ExposedMethod
    public void simple_method() {
        timesCalled++;
    }

    @ExposedMethod
    public void simpleexposed_prefixed() {}

    @ExposedMethod
    public boolean __nonzero__() {
        return false;
    }

    @ExposedMethod(names = {"__repr__", "__str__"})
    public String toString() {
        return toStringVal;
    }

    @ExposedSet(name = "tostring")
    public void setToString(String newVal) {
        toStringVal = newVal;
    }

    @ExposedDelete(name = "tostring")
    public void deleteToString() {
        toStringVal = null;
    }

    @ExposedMethod
    public double takesArgument(PyObject arg) {
        return Py.py2double(arg);
    }

    @ExposedMethod(type = MethodType.BINARY)
    public PyObject __add__(PyObject arg) {
        if(arg == Py.False) {
            return Py.One;
        }
        return null;
    }

    @ExposedMethod(type = MethodType.CMP)
    public int __cmp__(PyObject other) {
        if(other == Py.False) {
            return 1;
        }
        return -2;
    }

    @ExposedMethod(defaults = "Py.None")
    public PyObject defaultToNone(PyObject arg) {
        return arg;
    }

    @ExposedMethod(defaults = "null")
    public PyObject defaultToNull(PyObject arg) {
        return arg;
    }

    @ExposedMethod(defaults = "1")
    public PyObject defaultToOne(int arg) {
        return new PyInteger(arg);
    }

    @ExposedMethod(defaults = {"a", "1", "2", "3"})
    public String manyPrimitives(char c, short s, double d, byte b) {
        return "" + c + s + d + b;
    }

    @ExposedMethod
    public long fullArgs(PyObject[] args, String[] kws) {
        return args.length + kws.length;
    }

    @ExposedMethod
    public short shortReturn() {
        return 12;
    }

    @ExposedMethod
    public byte byteReturn() {
        return 0;
    }

    @ExposedMethod
    public char charReturn() {
        return 'a';
    }

    @ExposedMethod
    public String stringReturnNull() {
        return null;
    }

    @ExposedClassMethod 
    public static char classmethod(PyType onType) {
        return 'a';
    }

    @ExposedClassMethod(defaults = {"null", "Py.None"})
    public static int defaultsclassmethod(PyType onType, String possiblyNull, PyObject possiblyNone) {
        if (possiblyNull == null) {
            return 0;
        } else if (possiblyNull.equals("hello")) {
            return 1;
        } else if (possiblyNone.equals(Py.None)) {
            return 2;
        } else {
            return 3;
        }
    }

    @ExposedGet(name = "tostring", doc = "tostring docs")
    public String toStringVal = TO_STRING_RETURN;

    public static final String TO_STRING_RETURN = "A simple test class";

    @ExposedMethod
    public String needsThreadState(ThreadState state, String s) {
        return needsThreadStateClass(state, null, s, null);
    }

    @ExposedMethod
    public int needsThreadStateWide(ThreadState state, PyObject[] args, String[] kws) {
        if (state == null) {
            return -1;
        }
        return args.length + kws.length;
    }

    @ExposedClassMethod(defaults = {"null"})
    public static String needsThreadStateClass(ThreadState state, PyType onType, String s,
                                               String possiblyNull) {
        if (state != null) {
            s += " got state " + state.hashCode();
        }
        if (onType != null) {
            s += " got type";
        }
        if (possiblyNull != null) {
            s += possiblyNull;
        }
        return s;
    }
}
