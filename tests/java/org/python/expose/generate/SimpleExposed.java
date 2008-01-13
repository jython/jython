package org.python.expose.generate;

import org.python.core.Py;
import org.python.core.PyInteger;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;
import org.python.expose.ExposedDelete;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

@ExposedType(name = "simpleexposed")
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

    @ExposedGet(name = "tostring")
    public String toStringVal = TO_STRING_RETURN;

    public static final String TO_STRING_RETURN = "A simple test class";
}