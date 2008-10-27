// Copyright (c) Corporation for National Research Initiatives
package org.python.core;
import java.lang.reflect.Modifier;

/**
 * A wrapper around a java instance.
 */

public class PyJavaInstance extends PyInstance
    implements java.io.Externalizable
{
    public PyJavaInstance() {
    }

    public PyJavaInstance(PyJavaClass iclass) {
        super(iclass, null);
    }

    public PyJavaInstance(Object proxy) {
        super(PyJavaClass.lookup(proxy.getClass()), null);
        javaProxy = proxy;
    }

    /**
     * Implementation of the Externalizable interface.
     * @param in the input stream.
     * @exception java.io.IOException
     * @exception ClassNotFoundException
     */
    public void readExternal(java.io.ObjectInput in)
        throws java.io.IOException, ClassNotFoundException
    {
        Object o = in.readObject();
        javaProxy = o;
        instclass = PyJavaClass.lookup(o.getClass());
    }

    /**
     * Implementation of the Externalizable interface.
     * @param out the output stream.
     * @exception java.io.IOException
     */
    public void writeExternal(java.io.ObjectOutput out)
        throws java.io.IOException
    {
        //System.out.println("writing java instance");
        out.writeObject(javaProxy);
    }


    public void __init__(PyObject[] args, String[] keywords) {
        //javaProxies = new Object[1];

        Class pc = instclass.proxyClass;
        if (pc != null) {
            int mods = pc.getModifiers();
            if (Modifier.isInterface(mods)) {
                throw Py.TypeError("can't instantiate interface ("+
                                   instclass.__name__+")");
            }
            else if (Modifier.isAbstract(mods)) {
                throw Py.TypeError("can't instantiate abstract class ("+
                                   instclass.__name__+")");
            }
        }

        PyReflectedConstructor init = ((PyJavaClass)instclass).__init__;
        if (init == null) {
            throw Py.TypeError("no public constructors for " + instclass.__name__);
        }
        init.__call__(this, args, keywords);
    }

    protected void noField(String name, PyObject value) {
        throw Py.TypeError("can't set arbitrary attribute in java instance: "+
                           name);
    }

    protected void unassignableField(String name, PyObject value) {
        throw Py.TypeError("can't assign to this attribute in java instance: "
            + name);
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
                ? Py.True : Py.False;
        }
        return Py.False;
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

    protected PyString makeDefaultRepr() {
        return new PyString(javaProxy.toString());
    }

    public void __delattr__(String attr) {
        throw Py.TypeError("can't delete attr from java instance: "+attr);
    }
}
