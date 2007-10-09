package org.python.expose;

import org.python.core.Py;
import org.python.core.PyObject;

@ExposedType(name = "simpleexposed")
public class SimpleExposed extends PyObject {

    public void method() {}

    public int timesCalled;

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
        return TO_STRING_RETURN;
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

    public static final String TO_STRING_RETURN = "A simple test class";
}