// Copyright 2000 Samuele Pedroni

package org.python.core;

import java.util.*;

/**
 * 
 * @deprecated Java1 no longer supported.
 *
 */
public class InternalTables1 extends InternalTables {

    protected static interface Table {
        public Object put(Object key, Object obj);

        public Object get(Object key);

        public Object remove(Object key);

        public void clear();
    }

    private static class TableProvid1 extends Hashtable implements Table {
    }

    final protected static short JCLASS = 0;

    final protected static short LAZY_JCLASS = 1;

    final protected static short ADAPTER_CLASS = 2;

    final protected static short ADAPTER = 3;

    protected Table classes;

    protected Table temp;

    protected Table counters;

    protected Table lazyClasses;

    protected Table adapterClasses;

    protected final short GSTABLE = 1;

    protected final short JCSTABLE = 2;

    protected short keepstable;

    protected void beginStable(short lvl) {
        this.keepstable = lvl;
    }

    protected void classesPut(Class c, Object jc) {
        if (this.keepstable == this.JCSTABLE) {
            this.temp.put(c, jc);
            // System.err.println("temp-defer-canonical: "+c.getName());
        } else {
            this.classes.put(c, jc);
        }
        String name = c.getName();
        Integer cnt = (Integer) this.counters.get(name);
        if (cnt == null) {
            this.counters.put(name, new Integer(1));
            this.lazyClasses.remove(name);
        } else {
            this.counters.put(name, new Integer(cnt.intValue() + 1));
        }
    }

    protected Object classesGet(Class c) {
        Object o = this.classes.get(c);
        if (o != null || this.keepstable != this.JCSTABLE)
            return o;
        return this.temp.get(c);
    }

    protected void endStable() {
        if (this.keepstable == this.JCSTABLE)
            commitTemp();
        this.keepstable = 0;
    }

    protected void classesDec(String name) {
        int c = ((Integer) this.counters.get(name)).intValue();
        if (c == 1)
            this.counters.remove(name);
        else
            this.counters.put(name, new Integer(c - 1));
    }

    protected void commitTemp() {
        for (Enumeration e = ((Hashtable) this.temp).keys(); e
                .hasMoreElements();) {
            Object c = e.nextElement();
            this.classes.put(c, this.temp.get(c));
        }
        this.temp.clear();
    }

    protected boolean queryCanonical(String name) {
        return this.counters.get(name) != null
                || this.lazyClasses.get(name) != null;
    }

    protected PyJavaClass getCanonical(Class c) {
        return (PyJavaClass) classesGet(c);
    }

    protected PyJavaClass getLazyCanonical(String name) {
        return (PyJavaClass) this.lazyClasses.get(name);
    }

    protected void putCanonical(Class c, PyJavaClass canonical) {
        classesPut(c, canonical);
    }

    protected void putLazyCanonical(String name, PyJavaClass canonical) {
        this.lazyClasses.put(name, canonical);
    }

    protected Class getAdapterClass(Class c) {
        return (Class) this.adapterClasses.get(c);
    }

    protected void putAdapterClass(Class c, Class ac) {
        this.adapterClasses.put(c, ac);
    }

    private Hashtable adapters;

    protected Object getAdapter(Object o, String evc) {
        return this.adapters.get(evc + '$' + System.identityHashCode(o));
    }

    protected void putAdapter(Object o, String evc, Object ad) {
        this.adapters.put(evc + '$' + System.identityHashCode(o), ad);
    }

    protected short iterType;

    protected Object cur;

    private Enumeration enumm;

    private Hashtable enumTable;

    public void _beginCanonical() {
        beginStable(this.JCSTABLE);
        this.enumm = ((TableProvid1) this.classes).keys();
        this.enumTable = (TableProvid1) this.classes;
        this.iterType = JCLASS;
    }

    public void _beginLazyCanonical() {
        this.enumm = ((TableProvid1) this.lazyClasses).keys();
        this.enumTable = (TableProvid1) this.lazyClasses;
        this.iterType = LAZY_JCLASS;
    }

    public void _beginOverAdapterClasses() {
        this.enumm = ((TableProvid1) this.adapterClasses).keys();
        this.enumTable = (TableProvid1) this.adapterClasses;
        this.iterType = ADAPTER_CLASS;

    }

    public void _beginOverAdapters() {
        this.enumm = this.adapters.keys();
        this.enumTable = this.adapters;
        this.iterType = ADAPTER;
    }

    public Object _next() {
        if (this.enumm.hasMoreElements()) {
            this.cur = this.enumm.nextElement();
            switch (this.iterType) {
            case JCLASS:
                return (PyJavaClass) this.classes.get(this.cur);
            case LAZY_JCLASS:
                PyJavaClass lazy = (PyJavaClass) this.lazyClasses.get(this.cur);
                return new _LazyRep(lazy.__name__, lazy.__mgr__);
            case ADAPTER_CLASS:
                return this.cur;
            case ADAPTER:
                return this.adapters.get(this.cur).getClass().getInterfaces()[0];
            }
        }
        this.cur = null;
        this.enumm = null;
        endStable();
        return null;
    }

    public void _flushCurrent() {
        this.enumTable.remove(this.cur);
        if (this.iterType == JCLASS)
            classesDec(((Class) this.cur).getName());
    }

    public void _flush(PyJavaClass jc) {
        Class c = jc.proxyClass;
        if (c == null) {
            this.lazyClasses.remove(jc.__name__);
        } else {
            this.classes.remove(c);
            classesDec(jc.__name__);
        }
    }

    protected InternalTables1(boolean fake) {
    }

    public InternalTables1() {
        this.classes = new TableProvid1();
        this.temp = new TableProvid1();
        this.counters = new TableProvid1();
        this.lazyClasses = new TableProvid1();

        this.adapterClasses = new TableProvid1();

        this.adapters = new Hashtable();
    }
}
