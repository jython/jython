// Copyright 2000 Samuele Pedroni
 
package org.python.core;

import java.lang.ref.*;
import java.util.*;

public class WeakInternalTables extends InternalTables2 {
        
    private ReferenceQueue queue = new ReferenceQueue();
    
    private class Ref extends WeakReference {
        Object key;
        short type;
        
        Ref(short type,Object key, Object obj) {
            super(obj,queue);
            this.type=type;
            this.key=key;
        }
    }

    private synchronized void cleanup() {
        if (keepstable >= GSTABLE)
         return;
        adapters.remove(null); // trick
        Ref ref;
        while ((ref = (Ref)queue.poll()) != null) {
            switch(ref.type) {
            case JCLASS:
                Class cl = (Class)ref.key;
                classes.remove(cl);
                classesDec(cl.getName());                
                break;
            case LAZY_JCLASS:
                lazyClasses.remove(ref.key);
                break;
            case ADAPTER_CLASS:
                adapterClasses.remove(ref.key);    
            }
        }
    }
    
    
    protected boolean queryCanonical(String name) {
        cleanup();
        return super.queryCanonical(name);
    }
    
    protected PyJavaClass getCanonical(Class c) {
        cleanup();
        Ref ref = (Ref)classesGet(c);
        if (ref == null) return null;
        return (PyJavaClass)ref.get();
    }
    
    protected PyJavaClass getLazyCanonical(String name) {
        cleanup();
        Ref ref = (Ref)lazyClasses.get(name);
        if (ref == null) return null;
        return (PyJavaClass)ref.get();
    }
    
    protected void putCanonical(Class c,PyJavaClass canonical) {
        cleanup();
        classesPut(c,new Ref(JCLASS,c,canonical));
    }
    
    protected void putLazyCanonical(String name,PyJavaClass canonical) {
        cleanup();
        lazyClasses.put(name,new Ref(LAZY_JCLASS,name,canonical));
    }

    protected Class getAdapterClass(Class c) {
        cleanup();
        Ref ref = (Ref)adapterClasses.get(c);
        if (ref == null) return null;
        return (Class)ref.get();
    }
    
    protected void putAdapterClass(Class c,Class ac) {
        cleanup();
        adapterClasses.put(c,new Ref(ADAPTER_CLASS,c,ac));
    }

    protected Object getAdapter(Object o,String evc) {
        cleanup();
        return super.getAdapter(o,evc);
    }
    
    protected void putAdapter(Object o,String evc,Object ad) {
        cleanup();
        super.putAdapter(o,evc,ad);
    }

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
                    PyJavaClass jc = (PyJavaClass)((Ref)cur).get();
                    if (jc == null ) continue;
                    cur = jc;
                    return jc;
                case LAZY_JCLASS:
                    PyJavaClass lazy = (PyJavaClass)((Ref)cur).get();
                    if (lazy == null) continue;
                    return new _LazyRep(lazy.__name__,lazy.__mgr__);
                case ADAPTER_CLASS:
                    Map.Entry entry = (Map.Entry)cur;
                    if (((Ref)entry.getValue()).get() == null ) continue;
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