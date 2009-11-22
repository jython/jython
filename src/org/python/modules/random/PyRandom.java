package org.python.modules.random;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Random;

import org.python.core.Py;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "_random.Random")
public class PyRandom extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyRandom.class);

    public PyRandom() {
        this(TYPE);
    }

    public PyRandom(PyType subType) {
        super(subType);
    }

    // added functions
    protected Random javaRandom = new Random();

    /**
     * Sets the seed of the internal number generated to seed. If seed is a PyInteger or PyLong, it
     * uses the value, else it uses the hash function of PyObject
     */
    @ExposedMethod(defaults = "null")
    final void Random_seed(PyObject seed) {
        long n;
        if (seed == null) {
            seed = new PyLong(System.currentTimeMillis());
        }
        if (seed instanceof PyLong) {
            PyLong max = new PyLong(Long.MAX_VALUE);
            n = seed.__mod__(max).asLong();
        } else if (seed instanceof PyInteger) {
            n = seed.asLong();
        } else {
            n = seed.hashCode();
        }
        this.javaRandom.setSeed(n);
    }

    @ExposedNew
    @ExposedMethod
    final void Random___init__(PyObject[] args, String[] keywords) {}

    @ExposedMethod
    final void Random_jumpahead(PyObject arg0) {
        if (!(arg0 instanceof PyInteger || arg0 instanceof PyLong)) {
            throw Py.TypeError(String.format("jumpahead requires an integer, not '%s'",
                                             arg0.getType().fastGetName()));
        }
        for (long i = arg0.asLong(); i > 0; i--) {
            this.javaRandom.nextInt();
        }
    }

    @ExposedMethod
    final void Random_setstate(PyObject arg0) {
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
    }

    @ExposedMethod
    final PyObject Random_getstate() {
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

    /**
     * Generate a random number on [0,1) with 53-bit resolution. Implementation lifted from
     * _randommodule.c:random_random(), but we use &gt;&gt;&gt; instead of &gt;&gt; to avoid sign
     * problems.
     */
    @ExposedMethod
    final PyObject Random_random() {
        long a=this.javaRandom.nextInt()>>>5;
        long b=this.javaRandom.nextInt()>>>6;
        double ret=(a*67108864.0+b)*(1.0/9007199254740992.0);
        return new PyFloat(ret);
    }
    
    @ExposedMethod
    final PyLong Random_getrandbits(int k) {
        return new PyLong(new BigInteger(k, javaRandom));
    }
}
