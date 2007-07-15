// Copyright 2000 Samuele Pedroni

package org.python.core;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.WeakHashMap;

import org.python.core.packagecache.PackageManager;

public class InternalTables {

    // x__ --> org.python.core.X__InternalTables
    // (x|X)__> --> org.python.core.X__InternalTables
    // >(x|X)__ --> org.python.core.InternalTablesX__
    // other (X__|__.__) --> other
    //
    static private InternalTables tryImpl(String id) {
        try {
            if (id.indexOf('.') < 0) {
                boolean glue = true;
                boolean front = true;
                if (id.charAt(0) == '>') {
                    id = id.substring(1);
                    front = false;
                } else if (id.charAt(id.length() - 1) == '>') {
                    id = id.substring(0, id.length() - 1);
                } else if (!Character.isLowerCase(id.charAt(0)))
                    glue = false;
                if (glue) {
                    StringBuffer buf = new StringBuffer("org.python.core.");
                    if (!front) {
                        buf.append("InternalTables");
                    }
                    if (Character.isLowerCase(id.charAt(0))) {
                        buf.append(Character.toUpperCase(id.charAt(0)));
                        buf.append(id.substring(1));
                    } else {
                        buf.append(id);
                    }
                    if (front) {
                        buf.append("InternalTables");
                    }
                    id = buf.toString();
                }
            }
            // System.err.println("*InternalTables*-create-try: "+id);
            return (InternalTables) Class.forName(id).newInstance();
        } catch (Throwable e) {
            // System.err.println(" exc: "+e); // ??dbg
            return null;
        }
    }

    static InternalTables createInternalTables() {
        java.util.Properties registry = PySystemState.registry;
        if (registry == null) {
            throw new java.lang.IllegalStateException(
                    "Jython interpreter state not initialized. "
                            + "You need to call PySystemState.initialize or "
                            + "PythonInterpreter.initialize.");
        }
        String cands = registry.getProperty("python.options.internalTablesImpl");
        if (cands == null) {
            return new InternalTables();
        } 
        StringTokenizer candEnum = new StringTokenizer(cands, ":");
        while (candEnum.hasMoreTokens()) {
            InternalTables tbl = tryImpl(candEnum.nextToken().trim());
            if (tbl != null) {
                return tbl;
            }
        }
        return new InternalTables();
    }
    
    final protected static short JCLASS = 0;

    final protected static short LAZY_JCLASS = 1;

    final protected static short ADAPTER_CLASS = 2;

    final protected static short ADAPTER = 3;

    protected Map classes = new HashMap();

    protected Map temp = new HashMap();;

    protected Map counters = new HashMap();;

    protected Map lazyClasses = new HashMap();;

    protected Map adapterClasses = new HashMap();;

    protected final short GSTABLE = 1;

    protected final short JCSTABLE = 2;

    protected short keepstable;

    protected void commitTemp() {
        this.classes.putAll(this.temp);
        this.temp.clear();
    }

    protected WeakHashMap adapters = new WeakHashMap();;

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
    protected short iterType;
    protected Object cur;

    protected void beginStable(short lvl) {
        this.keepstable = lvl;
    }

    protected void endStable() {
        if (this.keepstable == this.JCSTABLE)
            commitTemp();
        this.keepstable = 0;
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

    public void _beginCanonical() {
        beginStable(this.JCSTABLE);
        this.iter = this.classes.values().iterator();
        this.iterType = JCLASS;
    }

    public void _beginLazyCanonical() {
        beginStable(this.GSTABLE);
        this.iter = this.lazyClasses.values().iterator();
        this.iterType = LAZY_JCLASS;
    }

    public void _beginOverAdapterClasses() {
        beginStable(this.GSTABLE);
        this.iter = this.adapterClasses.entrySet().iterator();
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
                return this.cur;
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

    public void _flush(PyJavaClass jc) {
        Class c = jc.proxyClass;
        if (c == null) {
            this.lazyClasses.remove(jc.__name__);
        } else {
            this.classes.remove(c);
            classesDec(jc.__name__);
        }
    }

    protected Class getAdapterClass(Class c) {
        return (Class) this.adapterClasses.get(c);
    }

    protected PyJavaClass getCanonical(Class c) {
        return (PyJavaClass) classesGet(c);
    }

    protected PyJavaClass getLazyCanonical(String name) {
        return (PyJavaClass) this.lazyClasses.get(name);
    }

    protected void putAdapterClass(Class c, Class ac) {
        this.adapterClasses.put(c, ac);
    }

    protected void putCanonical(Class c, PyJavaClass canonical) {
        classesPut(c, canonical);
    }

    protected void putLazyCanonical(String name, PyJavaClass canonical) {
        this.lazyClasses.put(name, canonical);
    }

    protected boolean queryCanonical(String name) {
        return this.counters.get(name) != null
                || this.lazyClasses.get(name) != null;
    }

    protected void classesDec(String name) {
        int c = ((Integer) this.counters.get(name)).intValue();
        if (c == 1)
            this.counters.remove(name);
        else
            this.counters.put(name, new Integer(c - 1));
    }

    static public class _LazyRep {
        public String name;

        public PackageManager mgr;

        _LazyRep(String name, PackageManager mgr) {
            this.name = name;
            this.mgr = mgr;
        }
    }

}
