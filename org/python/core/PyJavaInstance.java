// Copyright © Corporation for National Research Initiatives
package org.python.core;
import java.lang.reflect.Modifier;

/**
 * A wrapper around a java instance.
 */

public class PyJavaInstance
    extends PyInstance
    implements java.io.Externalizable
{
    public PyJavaInstance() {
        super();
        //javaProxies = new Object[1];
    }

    public PyJavaInstance(PyJavaClass iclass) {
        super(iclass, null);
    }

    public PyJavaInstance(Object proxy) {
        super(PyJavaClass.lookup(proxy.getClass()), null);
        javaProxy = proxy;
    }

    public void readExternal(java.io.ObjectInput in)
        throws java.io.IOException, ClassNotFoundException
    {
        Object o = in.readObject();
        javaProxy = o;
        __class__ = PyJavaClass.lookup(o.getClass());
    }


    public void writeExternal(java.io.ObjectOutput out)
        throws java.io.IOException
    {
        //System.out.println("writing java instance");
        out.writeObject(javaProxy);
    }


    public void __init__(PyObject[] args, String[] keywords) {
        //javaProxies = new Object[1];

        Class pc = __class__.proxyClass;
        if (pc != null) {
            int mods = pc.getModifiers();
            if (Modifier.isInterface(mods)) {
                throw Py.TypeError("can't instantiate interface ("+
                                   __class__.__name__+")");
            }
            else if (Modifier.isAbstract(mods)) {
                throw Py.TypeError("can't instantiate abstract class ("+
                                   __class__.__name__+")");
            }
        }

        PyReflectedConstructor init = ((PyJavaClass)__class__).__init__;
        if (init == null) {
            throw Py.TypeError("no public constructors for "+
                               __class__.__name__);
        }
        init.__call__(this, args, keywords);
    }

    protected void noField(String name, PyObject value) {
        throw Py.TypeError("can't set arbitrary attribute in java instance: "+
                           name);
    }

    protected void unassignableField(String name, PyObject value) {
        throw Py.TypeError("can't assign to this attribute in java " +
                           "instance: " + name);
    }

    public int hashCode() {
        if (javaProxy != null) {
            return javaProxy.hashCode();
        } else {
            return super.hashCode();
        }
    }

    public PyObject _is(PyObject o) {
        if (o instanceof PyJavaInstance) {
            return javaProxy == ((PyJavaInstance)o).javaProxy
                ? Py.One : Py.Zero;
        }
        return Py.Zero;
    }

    public PyObject _isnot(PyObject o) {
        return _is(o).__not__();
    }

    public int __cmp__(PyObject o) {
        if (!(o instanceof PyJavaInstance))
            return -2;
        PyJavaInstance i = (PyJavaInstance)o;
        if (javaProxy.equals(i.javaProxy))
            return 0;
        return -2;
    }

    public PyString __str__() {
        return new PyString(javaProxy.toString());
    }

    public PyString __repr__() {
        return __str__();
    }

    public void __delattr__(String attr) {
        throw Py.TypeError("can't delete attr from java instance: "+attr);
    }
}
