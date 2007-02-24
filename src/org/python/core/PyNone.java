// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.Serializable;

/**
 * A class representing the singleton None object,
 */
public class PyNone extends PyObject implements Serializable
{
    
    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */

    public static final String exposed_name="NoneType";

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        class exposed___repr__ extends PyBuiltinMethodNarrow {

            exposed___repr__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___repr__(self,info);
            }

            public PyObject __call__() {
                return new PyString(((PyNone)self).NoneType_toString());
            }

        }
        dict.__setitem__("__repr__",new PyMethodDescr("__repr__",PyNone.class,0,0,new exposed___repr__(null,null)));
        class exposed___nonzero__ extends PyBuiltinMethodNarrow {

            exposed___nonzero__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___nonzero__(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyNone)self).NoneType___nonzero__());
            }

        }
        dict.__setitem__("__nonzero__",new PyMethodDescr("__nonzero__",PyNone.class,0,0,new exposed___nonzero__(null,null)));
    }
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py

    private static final PyType NONETYPE = PyType.fromClass(PyNone.class);

    PyNone() {
        super(NONETYPE);
    }

    private Object writeReplace() {
        return new Py.SingletonResolver("None");
    }    
    
    public boolean __nonzero__() {
        return NoneType___nonzero__();
    }

    final boolean NoneType___nonzero__() {
        return false;
    }

    public Object __tojava__(Class c) {
        //Danger here.  java.lang.Object gets null not None
        if (c == PyObject.class)
            return this;
        if (c.isPrimitive())
            return Py.NoConversion;
        return null;
    }

    public String toString() throws PyIgnoreMethodTag {
        return NoneType_toString();
    }

    final String NoneType_toString() {
        return "None";
    }

    public boolean isMappingType() { return false; }
    public boolean isSequenceType() { return false; }
    public boolean isNumberType() { return false; }
    public String asStringOrNull(int index) { return null; }
}
