package org.python.modules.random;

import java.util.Random;
import java.lang.System;
import java.lang.Math;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;


import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.core.PyLong;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyTuple;

import org.python.core.Py;
import org.python.core.PyNewWrapper;
import org.python.core.PyMethodDescr;
import org.python.core.PyBuiltinFunction;
import org.python.core.PyBuiltinMethod;
import org.python.core.PyBuiltinMethodNarrow;


public class PyRandom extends PyObject {
    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */

    public static final String exposed_name="random";

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        class exposed_random extends PyBuiltinMethodNarrow {

            exposed_random(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_random(self,info);
            }

            public PyObject __call__() {
                return((PyRandom)self).random_random();
            }

        }
        dict.__setitem__("random",new PyMethodDescr("random",PyRandom.class,0,0,new exposed_random(null,null)));
        class exposed_seed extends PyBuiltinMethodNarrow {

            exposed_seed(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_seed(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PyRandom)self).random_seed(arg0);
            }

            public PyObject __call__() {
                return((PyRandom)self).random_seed();
            }

        }
        dict.__setitem__("seed",new PyMethodDescr("seed",PyRandom.class,0,1,new exposed_seed(null,null)));
        class exposed_getstate extends PyBuiltinMethodNarrow {

            exposed_getstate(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_getstate(self,info);
            }

            public PyObject __call__() {
                return((PyRandom)self).random_getstate();
            }

        }
        dict.__setitem__("getstate",new PyMethodDescr("getstate",PyRandom.class,0,0,new exposed_getstate(null,null)));
        class exposed_setstate extends PyBuiltinMethodNarrow {

            exposed_setstate(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_setstate(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PyRandom)self).random_setstate(arg0);
            }

        }
        dict.__setitem__("setstate",new PyMethodDescr("setstate",PyRandom.class,1,1,new exposed_setstate(null,null)));
        class exposed_jumpahead extends PyBuiltinMethodNarrow {

            exposed_jumpahead(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_jumpahead(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PyRandom)self).random_jumpahead(arg0);
            }

        }
        dict.__setitem__("jumpahead",new PyMethodDescr("jumpahead",PyRandom.class,1,1,new exposed_jumpahead(null,null)));
        class exposed___init__ extends PyBuiltinMethod {

            exposed___init__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___init__(self,info);
            }

            public PyObject __call__(PyObject[]args) {
                return __call__(args,Py.NoKeywords);
            }

            public PyObject __call__(PyObject[]args,String[]keywords) {
                ((PyRandom)self).random_init(args,keywords);
                return Py.None;
            }

        }
        dict.__setitem__("__init__",new PyMethodDescr("__init__",PyRandom.class,-1,-1,new exposed___init__(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(PyRandom.class,"__new__",-1,-1) {

                                                                                        public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
                                                                                            PyRandom newobj;
                                                                                            if (for_type==subtype) {
                                                                                                newobj=new PyRandom();
                                                                                                if (init)
                                                                                                    newobj.random_init(args,keywords);
                                                                                            } else {
                                                                                                newobj=new PyRandomDerived(subtype);
                                                                                            }
                                                                                            return newobj;
                                                                                        }

                                                                                    });
    }
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py

    private static final PyType RANDOMTYPE = PyType.fromClass(PyRandom.class);

    public PyRandom() {
        this(RANDOMTYPE);
    }

    public PyRandom(PyType subType) {
        super(subType);
    }

    // added functions
    protected java.util.Random javaRandom=new java.util.Random();

    public PyObject random_seed() {
        return this.random_seed(new PyLong(System.currentTimeMillis()));
    }

    /** Sets the seed of the internal number generated to seed.  If seed
     * is a PyInteger or PyLong, it uses the value, else it uses the
     * hash function of PyObject
     */
    public PyObject random_seed(PyObject seed) {
        if (seed instanceof PyLong) {
            this.javaRandom.setSeed(((PyLong)seed).asLong(0));
        } else if (seed instanceof PyInteger) {
            this.javaRandom.setSeed(((PyInteger)seed).getValue());
        } else {
            this.javaRandom.setSeed(seed.hashCode());
        }

        // duplicating the _randommodule.c::init_by_array return
        return Py.None;
    }

    public void random_init(PyObject[] args, String[] keywords) { }

    public PyObject random_jumpahead(PyObject arg0) {
        long inc;
        if (arg0 instanceof PyLong) {
            inc=((PyLong)arg0).asLong(0);
        } else if (arg0 instanceof PyInteger) {
            inc=((PyInteger)arg0).getValue();
        } else {
            throw Py.TypeError("jumpahead requires an integer");
        }
        for(int i=0;i<inc;i++) {
            this.javaRandom.nextInt();
        }
        return Py.None;
    }

    public PyObject random_setstate(PyObject arg0) {
        if (!(arg0 instanceof PyTuple)) {
            throw Py.TypeError("state vector must be a tuple");
        }

        try {
            Object arr[]=((PyTuple)arg0).toArray();
            byte b[]=new byte[arr.length];
            for(int i=0;i<arr.length;i++) {
                if (arr[i] instanceof Integer) {
                    b[i]=((Integer)arr[i]).byteValue();
                } else {
                    throw Py.TypeError("state vector of unexpected type: "+
                            arr[i].getClass());
                }
            }
            ByteArrayInputStream bin=new ByteArrayInputStream(b);
            ObjectInputStream oin=new ObjectInputStream(bin);
            this.javaRandom=(java.util.Random)oin.readObject();
        } catch (IOException e) {
            throw Py.SystemError("state vector invalid: "+e.getMessage());
        } catch (ClassNotFoundException e) {
            throw Py.SystemError("state vector invalid: "+e.getMessage());
        }

        // duplicating the _randommodule.c::random_setstate return
        return Py.None;
    }

    public PyObject random_getstate() {
        try {
            ByteArrayOutputStream bout=new ByteArrayOutputStream();
            ObjectOutputStream oout=new ObjectOutputStream(bout);
            oout.writeObject(this.javaRandom);
            byte b[]=bout.toByteArray();
            PyInteger retarr[]=new PyInteger[b.length];
            for (int i=0;i<b.length;i++) {
                retarr[i]=new PyInteger(b[i]);
            }
            PyTuple ret=new PyTuple(retarr);
            return ret;
        } catch (IOException e) {
            throw Py.SystemError("creation of state vector failed: "+
                    e.getMessage());
        }
    }

    /** Generate a random number on [0,1) with 53-bit resolution.
     * Implementation lifted from _randommodule.c:random_random(), but we
     * use &gt;&gt;&gt; instead of &gt;&gt; to avoid sign problems.
     */
    public PyObject random_random() {
        long a=this.javaRandom.nextInt()>>>5;
        long b=this.javaRandom.nextInt()>>>6;
        double ret=(a*67108864.0+b)*(1.0/9007199254740992.0);
        return new PyFloat(ret);
    }
}
