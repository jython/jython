package org.python.tests.mro;

import org.python.core.PyBuiltinMethod;
import org.python.core.PyBuiltinMethodNarrow;
import org.python.core.PyObject;
import org.python.core.PyType;

public class GetitemAdder {

    public static void addPostdefined() {
        PyBuiltinMethod meth = new PyBuiltinMethodNarrow("__getitem__", 1) {

            @Override
            public PyObject __call__(PyObject arg) {
                return arg;
            }
        };
        PyType.fromClass(PostdefinedGetitem.class).addMethod(meth);
    }

    public static void addPredefined() {
        PyBuiltinMethod meth = new PyBuiltinMethodNarrow("__getitem__", 1) {

            @Override
            public PyObject __call__(PyObject arg) {
                return arg;
            }
        };
        PyType.fromClass(FirstPredefinedGetitem.class).addMethod(meth);
        PyType.fromClass(SecondPredefinedGetitem.class).addMethod(meth);
    }
}