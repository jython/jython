// Copyright 2000 Samuele Pedroni

package jxxload_help;

import org.python.core.PyJavaClass;

public class DiscardHelp extends Object {

  private DiscardHelp() {
  }

  private static boolean check(Class c,ClassLoader loader,java.util.Vector interfaces) {
    try {
        Class s = c;
        do {
            if (s.getClassLoader() == loader) return true;
            s = s.getSuperclass();
        } while (s != null);
        
        for(java.util.Enumeration enumm=interfaces.elements(); enumm.hasMoreElements();) {
            Class intf = (Class)enumm.nextElement();
            if (intf.isAssignableFrom(c)) return true;
        }
        
    } catch(SecurityException e) {
    }
    return false;
  }

  // clearly not thread safe
  public static void discard(ClassLoader loader,java.util.Vector interfaces) {
    
    org.python.core.InternalTables tbl = PyJavaClass.getInternalTables();
    
    tbl._beginCanonical();
    
    PyJavaClass jc;
    while ((jc = (PyJavaClass)tbl._next()) != null ) {
        Class c = (Class)jc.__tojava__(Class.class);
        if(check(c,loader,interfaces)) tbl._flushCurrent();    
    }
    
    tbl._beginOverAdapterClasses();
    
    Class c;
    
    while ((c = (Class)tbl._next()) != null) {
        if(interfaces.contains(c)) tbl._flushCurrent();
    }
    
    tbl._beginOverAdapters();
    
    while ((c = (Class)tbl._next()) != null) {
        if(interfaces.contains(c)) tbl._flushCurrent();
    }
    
  }

}
