// Copyright 2000 Samuele Pedroni
 
package org.python.core;

import java.util.*;

public class InternalTables2 extends InternalTables1 {
    
    protected static class TableProvid2 extends HashMap implements Table {
    }

    protected void commitTemp() {
        ((TableProvid2)classes).putAll((TableProvid2)temp);
        temp.clear();
    }    

    protected WeakHashMap adapters;
    
    protected Object getAdapter(Object o,String evc) {
        HashMap ads = (HashMap)adapters.get(o);
        if (ads == null) return null;
        return ads.get(evc);
    }
    
    protected void putAdapter(Object o,String evc,Object ad) {
        HashMap ads = (HashMap)adapters.get(o);
        if (ads == null) {
            ads = new HashMap();
            adapters.put(o,ads);
        }
        ads.put(evc,ad);
    }

    protected Iterator iter;    
    protected Iterator grand;
    
    public void _beginCanonical() {
        beginStable(JCSTABLE);
        iter = ((TableProvid2)classes).values().iterator();
        iterType = JCLASS;
    }
    
    public void _beginLazyCanonical() {
        beginStable(GSTABLE);
        iter = ((TableProvid2)lazyClasses).values().iterator();
        iterType = LAZY_JCLASS;
    }
    
    public void _beginOverAdapterClasses() {
        beginStable(GSTABLE);
        iter = ((TableProvid2)adapterClasses).entrySet().iterator();
        iterType = ADAPTER_CLASS;
        
    }
    
    public void _beginOverAdapters() {
        beginStable((short)0);
        grand = adapters.values().iterator();
        iter = null;
        iterType = ADAPTER;
    }

    public Object _next() {
        if (iterType == ADAPTER) {
           if (iter==null || !iter.hasNext() ) {
            if (grand.hasNext()) {
                cur = grand.next();
                iter = ((HashMap)cur).values().iterator();
            } else iter = null;
           }
           if (iter != null) {
            return iter.next().getClass().getInterfaces()[0];
           }
           grand = null;
        }
        else if (iter.hasNext()) {
            cur = iter.next();
            switch(iterType) {
            case JCLASS:
                return (PyJavaClass)cur;
            case LAZY_JCLASS:
                PyJavaClass lazy = (PyJavaClass)cur;
                return new _LazyRep(lazy.__name__,lazy.__mgr__);
            case ADAPTER_CLASS:
                Map.Entry entry = (Map.Entry)cur;
                return entry.getKey();
            }
        }
        cur = null;
        endStable();
        iter = null;
        return null;
    }
    
    public void _flushCurrent() {
       iter.remove();
       switch(iterType) {
       case JCLASS:
           classesDec(((PyJavaClass)cur).__name__);
           break;
       case ADAPTER:
           if (((HashMap)cur).size() == 0) grand.remove();
       }
    }
       
    public InternalTables2() {
        super(true);
        
        classes = new TableProvid2();
        temp = new TableProvid2();
        counters = new TableProvid2();
        lazyClasses = new TableProvid2();
       
        adapterClasses = new TableProvid2();
        
        adapters = new WeakHashMap();
    }
}