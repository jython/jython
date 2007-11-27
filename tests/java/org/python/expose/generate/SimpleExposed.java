package org.python.expose.generate;

import org.python.core.Py;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
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

    @ExposedGet(name = "toString")
    @ExposedMethod(names = {"__repr__", "__str__"})
    public String toString() {
        return toStringVal;
    }

    @ExposedSet
    public void setToString(String newVal) {
        toStringVal = newVal;
    }

    @ExposedDelete
    public void deleteToString() {
        toStringVal = null;
    }

    @ExposedMethod
    public void takesArgument(PyObject arg) {
        assert arg == Py.None;
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

    @ExposedMethod(defaults = {"Py.None"})
    public PyObject defaultToNone(PyObject arg) {
        return arg;
    }

    @ExposedMethod(defaults = {"null"})
    public PyObject defaultToNull(PyObject arg) {
        return arg;
    }

    public String toStringVal = TO_STRING_RETURN;

    public static final String TO_STRING_RETURN = "A simple test class";
}