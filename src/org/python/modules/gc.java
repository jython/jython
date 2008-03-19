package org.python.modules;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyInteger;
import org.python.core.PyObject;

public class gc {

    public static final String __doc__ =
            "This module provides access to the garbage collector.\n" +
            "\n" +
            "enable() -- Enable automatic garbage collection (does nothing).\n" +
            "isenabled() -- Returns True because Java garbage collection cannot be disabled.\n" +
            "collect() -- Trigger a Java garbage collection (potentially expensive).\n" +
            "get_debug() -- Get debugging flags (returns 0).\n" +
            "\n" +
            "Other functions raise NotImplementedError because they do not apply to Java.\n";
    
    public static final String __name__ = "gc";

    public static void enable() {}
    public static void disable() {
        throw Py.NotImplementedError("can't disable Java GC");
    }
    public static boolean isenabled() { return true; }
    
    public static void collect() {
        System.gc();
    }
    
    public static PyObject get_count() {
        throw Py.NotImplementedError("not applicable to Java GC");
    }
    
    public static void set_debug() {
        throw Py.NotImplementedError("not applicable to Java GC");
    }
    public static int get_debug() { return 0; }
        
    public static void set_threshold() {
        throw Py.NotImplementedError("not applicable to Java GC");
    }
    public static PyObject get_threshold() {
        throw Py.NotImplementedError("not applicable to Java GC");
    }
    
    public static PyObject get_objects() {
        throw Py.NotImplementedError("not applicable to Java GC");
    }
    public static PyObject get_referrers() {
        throw Py.NotImplementedError("not applicable to Java GC");
    }
    public static PyObject get_referents() {
        throw Py.NotImplementedError("not applicable to Java GC");
    }

}
