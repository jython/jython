// Copyright 2000 Samuele Pedroni

package org.python.core;

import java.util.*;
import java.lang.ref.*;

public class InternalTables2 extends InternalTables1 {

    protected static class TableProvid2 extends HashMap implements Table {
    }

    protected void commitTemp() {
        ((TableProvid2)this.classes).putAll((TableProvid2)this.temp);
        this.temp.clear();
    }

    protected WeakHashMap adapters;

    protected Object getAdapter(Object o,String evc) {
        HashMap ads = (HashMap)this.adapters.get(o);
        if (ads == null) {
            return null;
        }
        WeakReference adw = (WeakReference) ads.get(evc);
        if (adw == null){
            return null;
        }
        return adw.get();
    }

    protected void putAdapter(Object o,String evc,Object ad) {
        HashMap ads = (HashMap)this.adapters.get(o);
        if (ads == null) {
            ads = new HashMap();
            this.adapters.put(o,ads);
        }
        ads.put(evc,new WeakReference(ad));
    }

    protected Iterator iter;
    protected Iterator grand;

    public void _beginCanonical() {
        beginStable(this.JCSTABLE);
        this.iter = ((TableProvid2)this.classes).values().iterator();
        this.iterType = JCLASS;
    }

    public void _beginLazyCanonical() {
        beginStable(this.GSTABLE);
        this.iter = ((TableProvid2)this.lazyClasses).values().iterator();
        this.iterType = LAZY_JCLASS;
    }

    public void _beginOverAdapterClasses() {
        beginStable(this.GSTABLE);
        this.iter = ((TableProvid2)this.adapterClasses).entrySet().iterator();
        this.iterType = ADAPTER_CLASS;

    }

    public void _beginOverAdapters() {
        beginStable((short)0);
        this.grand = this.adapters.values().iterator();
        this.iter = null;
        this.iterType = ADAPTER;
    }

    public Object _next() {
        if (this.iterType == ADAPTER) {
            for(;;) {
                if (this.iter==null || !this.iter.hasNext() ) {
                    if (this.grand.hasNext()) {
                        this.cur = this.grand.next();
                        this.iter = ((HashMap)this.cur).values().iterator();
                    } else {
                        this.iter = null;
                    }
                }
                if (this.iter != null) {
                    WeakReference adw = (WeakReference)this.iter.next();
                    Object ad = adw.get();
                    if (ad != null) {
                        return ad.getClass().getInterfaces()[0];
                    }
                    else { 
                        continue;
                    }
                }
                this.grand = null;
                break;
            }
        }
        else if (this.iter.hasNext()) {
            this.cur = this.iter.next();
            switch(this.iterType) {
            case JCLASS:
                return (PyJavaClass)this.cur;
            case LAZY_JCLASS:
                PyJavaClass lazy = (PyJavaClass)this.cur;
                return new _LazyRep(lazy.__name__,lazy.__mgr__);
            case ADAPTER_CLASS:
                Map.Entry entry = (Map.Entry)this.cur;
                return entry.getKey();
            }
        }
        this.cur = null;
        endStable();
        this.iter = null;
        return null;
    }

    public void _flushCurrent() {
       this.iter.remove();
       switch(this.iterType) {
       case JCLASS:
           classesDec(((PyJavaClass)this.cur).__name__);
           break;
       case ADAPTER:
           if (((HashMap)this.cur).size() == 0) this.grand.remove();
       }
    }

    public InternalTables2() {
        super(true);

        this.classes = new TableProvid2();
        this.temp = new TableProvid2();
        this.counters = new TableProvid2();
        this.lazyClasses = new TableProvid2();

        this.adapterClasses = new TableProvid2();

        this.adapters = new WeakHashMap();
    }
}