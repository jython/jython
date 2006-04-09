// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.Serializable;

/**
 * A class representing the singleton None object,
 */
public class PyNone extends PyObject implements Serializable
{
    
    /* type info */

    public static final String exposed_name="NoneType";

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        class exposed___repr__ extends PyBuiltinFunctionNarrow {

            private PyNone self;

            public PyObject getSelf() {
                return self;
            }

            exposed___repr__(PyNone self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___repr__((PyNone)self,info);
            }

            public PyObject __call__() {
                return new PyString(self.NoneType_toString());
            }

            public PyObject inst_call(PyObject gself) {
                PyNone self=(PyNone)gself;
                return new PyString(self.NoneType_toString());
            }

        }
        dict.__setitem__("__repr__",new PyMethodDescr("__repr__",PyNone.class,0,0,new exposed___repr__(null,null)));

        class exposed___nonzero__ extends PyBuiltinFunctionNarrow {

            private PyNone self;

            public PyObject getSelf() {
                return self;
            }

            exposed___nonzero__(PyNone self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___nonzero__((PyNone)self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(self.NoneType___nonzero__());
            }

            public PyObject inst_call(PyObject gself) {
                PyNone self=(PyNone)gself;
                return Py.newBoolean(self.NoneType___nonzero__());
            }

        }
        dict.__setitem__("__nonzero__",new PyMethodDescr("__nonzero__",PyNone.class,0,0,new exposed___nonzero__(null,null)));
    }
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
