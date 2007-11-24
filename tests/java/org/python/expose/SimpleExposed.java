package org.python.expose;

import org.python.core.Py;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;

@ExposedType(name = "simpleexposed", constructor="__new__")
public class SimpleExposed extends PyObject {

    public void method() {}

    public int timesCalled;
    
    public static PyObject __new__(PyNewWrapper new_, boolean init, PyType subtype,
                               PyObject[] args, String[] keywords) {
        return Py.One;
    }

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
    
    @ExposedMethod(defaults = {"Py.None"})
    public PyObject defaultToNone(PyObject arg) {
        return arg;
    }
    
    @ExposedMethod(defaults = {"null"})
    public PyObject defaultToNull(PyObject arg) {
        return arg;
    }
    
    public static final String TO_STRING_RETURN = "A simple test class";
}