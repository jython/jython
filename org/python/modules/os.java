// Copyright © Corporation for National Research Initiatives
package org.python.modules;

import org.python.core.*;

public class os implements InitModule {
    public static String[] __depends__ = new String[] {"javaos", };
    
    public void initModule(PyObject dict) {
        // Fake from javaos import *
        PyFrame frame = new PyFrame(null, dict, dict, null);
        imp.importAll("javaos", frame);
    }
}
