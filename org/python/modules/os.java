// Copyright © Corporation for National Research Initiatives
package org.python.modules;

import org.python.core.*;

public class os implements ClassDictInit {
    public static String[] __depends__ = new String[] {"javaos", };
    
    public static void classDictInit(PyObject dict) {
        // Fake from javaos import *
        PyFrame frame = new PyFrame(null, dict, dict, null);
        imp.importAll("javaos", frame);
    }
}
