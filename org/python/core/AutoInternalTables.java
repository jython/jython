// Copyright 2000 Samuele Pedroni

package org.python.core;

import java.lang.ref.*;
import java.util.*;

public abstract class AutoInternalTables extends InternalTables2 {

    protected ReferenceQueue queue = new ReferenceQueue();

    protected abstract Reference newAutoRef(short type, Object key,
                                            Object obj);
    protected abstract short getAutoRefType(Reference ref);
    protected abstract Object getAutoRefKey(Reference ref);

    private synchronized void cleanup() {
        if (keepstable >= GSTABLE)
         return;
        adapters.remove(null); // trick
        Reference ref;
        while ((ref = queue.poll()) != null) {
            Object key = getAutoRefKey(ref);
            switch(getAutoRefType(ref)) {
            case JCLASS:
                Class cl = (Class)key;
                classes.remove(cl);
                classesDec(cl.getName());
                break;
            case LAZY_JCLASS:
                lazyClasses.remove(key);
                break;
            case ADAPTER_CLASS:
                adapterClasses.remove(key);
            }
        }
    }


    protected boolean queryCanonical(String name) {
        cleanup();
        return super.queryCanonical(name);
    }

    protected PyJavaClass getCanonical(Class c) {
        cleanup();
        Reference ref = (Reference)classesGet(c);
        if (ref == null) return null;
        return (PyJavaClass)ref.get();
    }

    protected PyJavaClass getLazyCanonical(String name) {
        cleanup();
        Reference ref = (Reference)lazyClasses.get(name);
        if (ref == null) return null;
        return (PyJavaClass)ref.get();
    }

    protected void putCanonical(Class c,PyJavaClass canonical) {
        cleanup();
        classesPut(c,newAutoRef(JCLASS,c,canonical));
    }

    protected void putLazyCanonical(String name,PyJavaClass canonical) {
        cleanup();
        lazyClasses.put(name,newAutoRef(LAZY_JCLASS,name,canonical));
    }

    protected Class getAdapterClass(Class c) {
        cleanup();
        Reference ref = (Reference)adapterClasses.get(c);
        if (ref == null) return null;
        return (Class)ref.get();
    }

    protected void putAdapterClass(Class c,Class ac) {
        cleanup();
        adapterClasses.put(c,newAutoRef(ADAPTER_CLASS,c,ac));
    }

    protected Object getAdapter(Object o,String evc) {
        cleanup();
        return super.getAdapter(o,evc);
    }

    protected void putAdapter(Object o,String evc,Object ad) {
        cleanup();
        super.putAdapter(o,evc,ad);
    }


    public boolean _doesSomeAutoUnload() { return true; }

    public void _forceCleanup() { cleanup(); }

    public void _beginCanonical() {
        cleanup();
        super._beginCanonical();
    }

    public void _beginLazyCanonical() {
        cleanup();
        super._beginLazyCanonical();
    }

    public void _beginOverAdapterClasses() {
        cleanup();
        super._beginOverAdapterClasses();

    }

    public void _beginOverAdapters() {
        cleanup();
        super._beginOverAdapters();
    }

    public Object _next() {
        if (iterType == ADAPTER) {
            Object ret = super._next();
            if (ret != null) return ret;
        }  else {
            while(iter.hasNext()) {
                cur = iter.next();
                switch(iterType) {
                case JCLASS:
                    PyJavaClass jc = (PyJavaClass)((Reference)cur).get();
                    if (jc == null ) continue;
                    cur = jc;
                    return jc;
                case LAZY_JCLASS:
                    PyJavaClass lazy = (PyJavaClass)((Reference)cur).get();
                    if (lazy == null) continue;
                    return new _LazyRep(lazy.__name__,lazy.__mgr__);
                case ADAPTER_CLASS:
                    Map.Entry entry = (Map.Entry)cur;
                    if (((Reference)entry.getValue()).get() == null )
                        continue;
                    return entry.getKey();
                }
            }
            cur = null;
            iter = null;
            endStable();
        }
        cleanup();
        return null;
    }

    public void _flush(PyJavaClass jc) {
        cleanup();
        super._flush(jc);
    }

}
