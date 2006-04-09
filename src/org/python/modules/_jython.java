
package org.python.modules;

import org.python.core.*;

class JythonInternalFunctions extends PyBuiltinFunctionSet
{
    public JythonInternalFunctions(String name, int index, int argcount) {
        super(name, index, argcount, argcount, false, null);
    }

    public PyObject __call__(PyObject arg) {
        switch (index) {
        case 0:
            if (!(arg instanceof PyJavaClass))
                throw Py.TypeError("is_lazy(): arg is not a jclass");
            return Py.newBoolean(((PyJavaClass)arg).isLazy());
        default:
            throw argCountError(1);
        }
    }
}

public class _jython implements ClassDictInit
{
    public static void classDictInit(PyObject dict) {
        dict.__setitem__("is_lazy",
                         new JythonInternalFunctions("is_lazy", 0, 1));
    }

}
