// Copyright © Corporation for National Research Initiatives
package org.python.modules;

import org.python.core.*;

public class os implements ClassDictInit {
    public static String[] __depends__ = new String[] {"javaos", };

    // An ugly hack, but it keeps the site.py from CPython2.0 happy

    public static String __file__;

    public static void classDictInit(PyObject dict) {
        String prefix = Py.getSystemState().prefix;
        if (prefix != null)
            __file__ = prefix + "/Lib/javaos.py";

        // Fake from javaos import *
        PyFrame frame = new PyFrame(null, dict, dict, null);
        org.python.core.imp.importAll("javaos", frame);
    }
}
