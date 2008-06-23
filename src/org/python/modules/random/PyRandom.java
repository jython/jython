package org.python.modules.random;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
    final PyObject Random_seed(PyObject seed) {
        if (seed == null) {
            seed = new PyLong(System.currentTimeMillis());
        }
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

    @ExposedNew
    @ExposedMethod
    public void Random___init__(PyObject[] args, String[] keywords) {}

    @ExposedMethod
    public PyObject Random_jumpahead(PyObject arg0) {
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

    @ExposedMethod
    public PyObject Random_setstate(PyObject arg0) {
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

    @ExposedMethod
    public PyObject Random_getstate() {
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
    public PyObject Random_random() {
        long a=this.javaRandom.nextInt()>>>5;
        long b=this.javaRandom.nextInt()>>>6;
        double ret=(a*67108864.0+b)*(1.0/9007199254740992.0);
        return new PyFloat(ret);
    }
}
