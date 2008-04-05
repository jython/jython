// Copyright 2001 Finn Bock

package org.python.modules;

import java.lang.ref.*;
import java.util.*;
import org.python.core.*;


public class _weakref implements ClassDictInit {
    static ReferenceQueue referenceQueue = new ReferenceQueue();

    static RefReaperThread reaperThread;
    static Map objects = new HashMap();

    public static PyObject ReferenceError = null;

    static {
        reaperThread = new RefReaperThread();
        reaperThread.setDaemon(true);
        reaperThread.start();
    }

    /** <i>Internal use only. Do not call this method explicit.</i> */
    public static void classDictInit(PyObject dict)
        throws PyIgnoreMethodTag
    {
        ReferenceError = Py.makeClass("ReferenceError", Py.RuntimeError,
                                      new PyStringMap() {{
                                          __setitem__("__module__", Py.newString("_weakref"));
                                      }});
        dict.__setitem__("ReferenceError", ReferenceError);
    }

    public static ReferenceType ref(PyObject object)  {
        GlobalRef gref = mkGlobal(object);
        ReferenceType ret = (ReferenceType)gref.find(ReferenceType.class);
        if (ret != null) {
            return ret;
        }
        return new ReferenceType(mkGlobal(object), null);
    }

    public static ReferenceType ref(PyObject object, PyObject callback) {
        return new ReferenceType(mkGlobal(object), callback);
    }

    public static ProxyType proxy(PyObject object)  {
        GlobalRef gref = mkGlobal(object);
        ProxyType ret = (ProxyType)gref.find(ProxyType.class);
        if (ret != null) {
            return ret;
        }
        if (object.isCallable()) {
            return new CallableProxyType(mkGlobal(object), null);
        } else {
            return new ProxyType(mkGlobal(object), null);
        }
    }

    public static ProxyType proxy(PyObject object, PyObject callback) {
        if (object.isCallable()) {
            return new CallableProxyType(mkGlobal(object), callback);
        } else {
            return new ProxyType(mkGlobal(object), callback);
        }
    }

    public static int getweakrefcount(PyObject o) {
        GlobalRef ref = (GlobalRef) objects.get(new GlobalRef(o));
        if (ref == null)
            return 0;
        return ref.count();
    }

    public static PyList getweakrefs(PyObject o) {
        GlobalRef ref = (GlobalRef) objects.get(new GlobalRef(o));
        if (ref == null)
            return new PyList();
        return ref.refs();
    }

    private static GlobalRef mkGlobal(PyObject object) {
        GlobalRef ref = (GlobalRef) objects.get(new GlobalRef(object));
        if (ref == null) {
            ref = new GlobalRef(object, referenceQueue);
            objects.put(ref, ref);
        }
        return ref;
    }

    static class RefReaperThread extends Thread {
        RefReaperThread() {
            super("weakref reaper");
        }

        public void collect() throws InterruptedException {
            GlobalRef gr = (GlobalRef) referenceQueue.remove();
            gr.call();
            objects.remove(gr);
            gr = null;
        }

        public void run() {
            while (true) {
                try {
                    collect();
                } catch (InterruptedException exc) { }
             }
        }
    }


    public static class GlobalRef extends WeakReference {
        private Vector references = new Vector();
        private int hash;
        private boolean realHash; // If the hash value was calculated by the underlying object
        
        public GlobalRef(PyObject object) {
            super(object);
            calcHash(object);
        }

        public GlobalRef(PyObject object, ReferenceQueue queue) {
            super(object, queue);
            calcHash(object);
        }
        
        /**
         * Calculate a hash code to use for this object.  If the PyObject we're
         * referencing implements hashCode, we use that value.  If not, we use
         * System.identityHashCode(refedObject).  This allows this object to be 
         * used in a Map while allowing Python ref objects to tell if the 
         * hashCode is actually valid for the object.
         */
        private void calcHash (PyObject object) {
            try {
                hash = object.hashCode();
                realHash = true;
            } catch (PyException e) {
                if (Py.matchException(e, Py.TypeError)) {
                    hash = System.identityHashCode(object);
                } else {
                    throw e;
                }
            }
        }

        public synchronized void add(AbstractReference ref) {
            Reference r = new WeakReference(ref);
            references.addElement(r);
        }

        private final AbstractReference getReferenceAt(int idx) {
            WeakReference wref = (WeakReference) references.elementAt(idx);
            return (AbstractReference) wref.get();
        }

        /**
         * Search for a reusable refrence. To be reused, it must be of the
         * same class and it must not have a callback.
         */
        synchronized AbstractReference find(Class cls) {
            for (int i = references.size() - 1; i >= 0; i--) {
                AbstractReference r = getReferenceAt(i);
                if (r == null) {
                    references.removeElementAt(i);
                } else if (r.callback == null && r.getClass() == cls) {
                    return r;
                }
            }
            return null;
        }

        /**
         * Call each of the registered references.
         */
        synchronized void call() {
            for (int i = references.size() - 1; i >= 0; i--) {
                AbstractReference r = getReferenceAt(i);
                if (r == null)
                    references.removeElementAt(i);
                else
                    r.call();
            }
        }

        synchronized public int count() {
            for (int i = references.size() - 1; i >= 0; i--) {
                AbstractReference r = getReferenceAt(i);
                if (r == null) {
                    references.removeElementAt(i);
                }
            }
            return references.size();
        }

        synchronized public PyList refs() {
            Vector list = new Vector();
            for (int i = references.size() - 1; i >= 0; i--) {
                AbstractReference r = getReferenceAt(i);
                if (r == null)
                    references.removeElementAt(i);
                else
                    list.addElement(r);
            }
            return new PyList(list);
        }

        /**
         * Allow GlobalRef's to be used as hashtable keys.
         */
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GlobalRef)) return false;
            Object t = this.get();
            Object u = ((GlobalRef)o).get();
            if ((t == null) || (u == null)) return false;
            if (t == u) return true;
            return t.equals(u);
        }

        /**
         * Allow GlobalRef's to be used as hashtable keys.
         */
        public int hashCode() {
            return hash;
        }
    }


    public static abstract class AbstractReference extends PyObject {
        PyObject callback;
        protected GlobalRef gref;

        public AbstractReference(GlobalRef gref, PyObject callback) {
            this.gref = gref;
            this.callback = callback;
            gref.add(this);
        }

        void call() {
            if (callback == null)
                return;
            try {
                callback.__call__(this);
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }

        protected PyObject py() {
            PyObject o = (PyObject) gref.get();
            if (o == null) {
                throw new PyException(ReferenceError,
                              "weakly-referenced object no longer exists");
            }
            return o;
        }

        public int hashCode() {
            if (gref.realHash) {
                return gref.hash;
            }
            throw Py.TypeError("unhashable instance");
        }

        public PyObject __eq__(PyObject other) {
            if (other.getClass() != getClass())
                return null;
            PyObject pythis = (PyObject) gref.get();
            PyObject pyother = (PyObject) ((AbstractReference) other).
                                                            gref.get();
            if (pythis == null || pyother == null)
                return this == other ? Py.True : Py.False;
            return  pythis._eq(pyother);
        }

    }


    public static class ReferenceType extends AbstractReference {
        ReferenceType(GlobalRef gref, PyObject callback) {
            super(gref, callback);
        }

        public PyObject __call__() {
            return Py.java2py(gref.get());
        }

        public String toString() {
            String ret = "<weakref " +
                    Py.idstr(this) +";";
            PyObject obj = (PyObject) gref.get();
            if (obj != null)
                ret += " to " + obj.getType().fastGetName() + ">";
            else
                ret += " dead>";
            return ret;
        }
    }

    public static class ProxyType extends AbstractReference {
        ProxyType(GlobalRef ref, PyObject callback) {
            super(ref, callback);
        }

        public PyObject __findattr__(String name) {
            return py().__findattr__(name);
        }

        public void __setattr__(String name, PyObject value) {
            py().__setattr__(name, value);
        }

        public void __delattr__(String name) {
            py().__delattr__(name);
        }

        public PyString __str__() { return py().__str__(); }
        public PyString __hex__() { return py().__hex__(); }
        public PyString __oct__() { return py().__oct__(); }
        public PyObject __int__() { return py().__int__(); }
        public PyFloat __float__() { return py().__float__(); }
        public PyLong __long__() { return py().__long__(); }
        public PyComplex __complex__() { return py().__complex__(); }
        public PyObject __pos__() { return py().__pos__(); }
        public PyObject __neg__() { return py().__neg__(); }
        public PyObject __abs__() { return py().__abs__(); }
        public PyObject __invert__() { return py().__invert__(); }

        public PyObject __add__(PyObject o) { return py().__add__(o); }
        public PyObject __radd__(PyObject o) { return py().__radd__(o); }
        public PyObject __iadd__(PyObject o) { return py().__iadd__(o); }
        public PyObject __sub__(PyObject o) { return py().__sub__(o); }
        public PyObject __rsub__(PyObject o) { return py().__rsub__(o); }
        public PyObject __isub__(PyObject o) { return py().__isub__(o); }
        public PyObject __mul__(PyObject o) { return py().__mul__(o); }
        public PyObject __rmul__(PyObject o) { return py().__rmul__(o); }
        public PyObject __imul__(PyObject o) { return py().__imul__(o); }
        public PyObject __div__(PyObject o) { return py().__div__(o); }
        public PyObject __rdiv__(PyObject o) { return py().__rdiv__(o); }
        public PyObject __idiv__(PyObject o) { return py().__idiv__(o); }
        public PyObject __mod__(PyObject o) { return py().__mod__(o); }
        public PyObject __rmod__(PyObject o) { return py().__rmod__(o); }
        public PyObject __imod__(PyObject o) { return py().__imod__(o); }
        public PyObject __divmod__(PyObject o) { return py().__divmod__(o); }
        public PyObject __rdivmod__(PyObject o) { return py().__rdivmod__(o);}
        public PyObject __pow__(PyObject o) { return py().__pow__(o); }
        public PyObject __rpow__(PyObject o) { return py().__rpow__(o); }
        public PyObject __ipow__(PyObject o) { return py().__ipow__(o); }
        public PyObject __lshift__(PyObject o) { return py().__lshift__(o); }
        public PyObject __rlshift__(PyObject o) { return py().__rlshift__(o);}
        public PyObject __ilshift__(PyObject o) { return py().__ilshift__(o);}

        public PyObject __rshift__(PyObject o) { return py().__rshift__(o); }
        public PyObject __rrshift__(PyObject o) { return py().__rrshift__(o);}
        public PyObject __irshift__(PyObject o) { return py().__irshift__(o);}
        public PyObject __and__(PyObject o) { return py().__and__(o); }
        public PyObject __rand__(PyObject o) { return py().__rand__(o); }
        public PyObject __iand__(PyObject o) { return py().__iand__(o); }
        public PyObject __or__(PyObject o) { return py().__or__(o); }
        public PyObject __ror__(PyObject o) { return py().__ror__(o); }
        public PyObject __ior__(PyObject o) { return py().__ior__(o); }
        public PyObject __xor__(PyObject o) { return py().__xor__(o); }
        public PyObject __rxor__(PyObject o) { return py().__rxor__(o); }
        public PyObject __ixor__(PyObject o) { return py().__ixor__(o); }

        public String toString() {
            String ret = "<weakref " +Py.idstr(this);
            PyObject obj = (PyObject) gref.get();
            if (obj == null)
                obj = Py.None;
            ret += " to " + obj.getType().fastGetName() + " "+
                    Py.idstr(obj) + ">";
            return ret;
        }
    }

    public static class CallableProxyType extends ProxyType {
        CallableProxyType(GlobalRef ref, PyObject callback) {
            super(ref, callback);
        }
        public PyObject __call__(PyObject[] args, String[] kws) {
            return py().__call__(args, kws);
        }
    }


}


