// Copyright 2000 Samuele Pedroni
 
package org.python.core;

public abstract class InternalTables {
    
    // x__ --> org.python.core.X__InternalTables
    // (x|X)__> --> org.python.core.X__InternalTables
    // >(x|X)__ --> org.python.core.InternalTablesX__
    // other (X__|__.__) --> other
    static private InternalTables tryImpl(String id) {
        try {
            if(id.indexOf('.') < 0) {
                boolean glue = true;
                boolean front = true;
                if (id.charAt(0) == '>') {
                    id = id.substring(1);
                    front = false;
                } else if (id.charAt(id.length()-1)=='>') {
                    id = id.substring(0,id.length()-1);
                } else if (!Character.isLowerCase(id.charAt(0))) glue = false;
                if (glue) {
                    StringBuffer buf = new StringBuffer("org.python.core.");
                    if (!front) buf.append("InternalTables");
                    if (Character.isLowerCase(id.charAt(0))) {
                        buf.append(Character.toUpperCase(id.charAt(0)));
                        buf.append(id.substring(1));
                    } else
                      buf.append(id);
                    if (front) buf.append("InternalTables");
                    id = buf.toString();
                }
            }
            // System.err.println("*InternalTables*-create-try: "+id); // ??dbg
            return (InternalTables)Class.forName(id).newInstance();
        } 
        catch(Throwable e) {
            // System.err.println(" exc: "+e); // ??dbg
            return null;
        }
    }
    
    static InternalTables createInternalTables() {
        String cands = PySystemState.registry.getProperty("python.options.internalTablesImpl");
        if (cands == null)
            cands = ">2:>1";
        else
            cands = cands + ":>2:>1";
        java.util.StringTokenizer candEnum = new java.util.StringTokenizer(cands,":");
        while (candEnum.hasMoreTokens()) {
            InternalTables tbl = tryImpl(candEnum.nextToken().trim());
            if (tbl != null) return tbl;
        }
        return null; // never reached
    }
    
    protected abstract boolean queryCanonical(String name);
    protected abstract PyJavaClass getCanonical(Class c);
    protected abstract PyJavaClass getLazyCanonical(String name);
    
    protected abstract void putCanonical(Class c,PyJavaClass canonical);
    protected abstract void putLazyCanonical(String name,PyJavaClass canonical);

    protected abstract Class getAdapterClass(Class c);
    protected abstract void putAdapterClass(Class c,Class ac);

    protected abstract Object getAdapter(Object o,String evc);
    protected abstract void putAdapter(Object o,String evc,Object ad);
    
    public boolean _doesSomeAutoUnload() { return false; }
    
    public void _forceCleanup() {}

    public abstract void _beginCanonical();
    public abstract void _beginLazyCanonical();
    
    public abstract void _beginOverAdapterClasses();
    public abstract void _beginOverAdapters();
    
    public abstract Object _next();
    public abstract void _flushCurrent();
    
    public abstract void _flush(PyJavaClass jc);
    

    static public class _LazyRep {
        public String name;
        public PackageManager mgr;
        
        _LazyRep(String name, PackageManager mgr) {
            this.name = name;
            this.mgr = mgr;
        }
    }
    
}