// Copyright 2000 Samuele Pedroni

package org.python.core;

import java.util.*;


public class InternalTables1 extends InternalTables {

    protected static interface Table {
        public Object put(Object key,Object obj);
        public Object get(Object key);
        public Object remove(Object key);
        public void clear();
    }

    private static class TableProvid1 extends Hashtable implements Table {
    }

    final protected static short JCLASS=0;
    final protected static short LAZY_JCLASS=1;
    final protected static short ADAPTER_CLASS=2;
    final protected static short ADAPTER = 3;

    protected Table classes;
    protected Table temp;
    protected Table counters;
    protected Table lazyClasses;

    protected Table adapterClasses;

    protected final short GSTABLE=1;
    protected final short JCSTABLE=2;

    protected short keepstable;

    protected void beginStable(short lvl) {
        keepstable = lvl;
    }

    protected void classesPut(Class c,Object jc) {
        if (keepstable == JCSTABLE) {
            temp.put(c,jc);
            // System.err.println("temp-defer-canonical: "+c.getName());
        } else {
            classes.put(c,jc);
        }
        String name = c.getName();
        Integer cnt = (Integer)counters.get(name);
        if (cnt == null) {
            counters.put(name,new Integer(1));
            lazyClasses.remove(name);
        } else {
            counters.put(name,new Integer(cnt.intValue()+1));
        }
    }

    protected Object classesGet(Class c) {
        Object o = classes.get(c);
        if (o != null || keepstable != JCSTABLE) return o;
        return temp.get(c);
    }

    protected void endStable() {
        if (keepstable == JCSTABLE)
          commitTemp();
        keepstable = 0;
    }

    protected void classesDec(String name) {
        int c = ((Integer)counters.get(name)).intValue();
        if (c == 1)
         counters.remove(name);
        else
         counters.put(name,new Integer(c-1));
    }

    protected void commitTemp() {
        for(Enumeration e=((Hashtable)temp).keys();e.hasMoreElements();) {
            Object c = e.nextElement();
            classes.put(c,temp.get(c));
        }
        temp.clear();
    }

    protected boolean queryCanonical(String name) {
        return counters.get(name) != null || lazyClasses.get(name) != null;
    }

    protected PyJavaClass getCanonical(Class c) {
        return (PyJavaClass)classesGet(c);
    }

    protected PyJavaClass getLazyCanonical(String name) {
        return (PyJavaClass)lazyClasses.get(name);
    }

    protected void putCanonical(Class c,PyJavaClass canonical) {
        classesPut(c,canonical);
    }

    protected void putLazyCanonical(String name,PyJavaClass canonical) {
        lazyClasses.put(name,canonical);
    }

    protected Class getAdapterClass(Class c) {
        return (Class)adapterClasses.get(c);
    }

    protected void putAdapterClass(Class c,Class ac) {
        adapterClasses.put(c,ac);
    }

    private Hashtable adapters;

    protected Object getAdapter(Object o,String evc) {
        return adapters.get(evc+'$'+System.identityHashCode(o));
    }

    protected void putAdapter(Object o,String evc,Object ad) {
        adapters.put(evc+'$'+System.identityHashCode(o),ad);
    }

    protected short iterType;
    protected Object cur;

    private Enumeration enumm;
    private Hashtable enumTable;

    public void _beginCanonical() {
        beginStable(JCSTABLE);
        enumm = ((TableProvid1)classes).keys();
        enumTable = (TableProvid1)classes;
        iterType = JCLASS;
    }

    public void _beginLazyCanonical() {
        enumm = ((TableProvid1)lazyClasses).keys();
        enumTable = (TableProvid1)lazyClasses;
        iterType = LAZY_JCLASS;
    }

    public void _beginOverAdapterClasses() {
        enumm = ((TableProvid1)adapterClasses).keys();
        enumTable = (TableProvid1)adapterClasses;
        iterType = ADAPTER_CLASS;

    }

    public void _beginOverAdapters() {
        enumm = adapters.keys();
        enumTable = adapters;
        iterType = ADAPTER;
    }

    public Object _next() {
        if(enumm.hasMoreElements()) {
            cur = enumm.nextElement();
            switch(iterType) {
            case JCLASS:
                return (PyJavaClass)classes.get(cur);
            case LAZY_JCLASS:
                PyJavaClass lazy = (PyJavaClass)lazyClasses.get(cur);
                return new _LazyRep(lazy.__name__,lazy.__mgr__);
            case ADAPTER_CLASS:
                return cur;
            case ADAPTER:
                return adapters.get(cur).getClass().getInterfaces()[0];
            }
        }
        cur = null;
        enumm = null;
        endStable();
        return null;
    }

    public void _flushCurrent() {
       enumTable.remove(cur);
       if (iterType == JCLASS) classesDec(((Class)cur).getName());
    }

    public void _flush(PyJavaClass jc) {
        Class c = jc.proxyClass;
        if (c == null) {
            lazyClasses.remove(jc.__name__);
        } else {
            classes.remove(c);
            classesDec(jc.__name__);
        }
    }

    protected InternalTables1(boolean fake) {
    }

    public InternalTables1() {
        classes = new TableProvid1();
        temp = new TableProvid1();
        counters = new TableProvid1();
        lazyClasses = new TableProvid1();

        adapterClasses = new TableProvid1();

        adapters = new Hashtable();
    }
}

