/*
 * Copyright 2000 Finn Bock
 *
 * This program contains material copyrighted by:
 * Copyright (c) 1997-2000 by Secret Labs AB.  All rights reserved.
 *
 * This version of the SRE library can be redistributed under CNRI's
 * Python 1.6 license.  For any other use, please contact Secret Labs
 * AB (info@pythonware.com).
 *
 * Portions of this engine have been developed in cooperation with
 * CNRI.  Hewlett-Packard provided funding for 1.6 integration and
 * other compatibility work.
 */

package org.python.modules;

import java.util.*;
import org.python.core.*;
import org.python.modules.sre.*;


public class _sre {
    // update when constants are added or removed
    public static int MAGIC = 20010320;


    public static PatternObject compile(PyString pattern, int flags,
                                        PyObject code, int groups,
                                        PyObject groupindex,
                                        PyObject indexgroup) {
        char[] ccode = null;
        if (code instanceof PyList) {
            int n = code.__len__();
            ccode = new char[n];
            for (int i = 0; i < n; i++)
                ccode[i] = (char) code.__getitem__(i).__int__().getValue();
        } else {
            throw Py.TypeError("Expected list");
        }

        PatternObject po = new PatternObject(pattern,
                                             flags,
                                             ccode,
                                             groups,
                                             groupindex,
                                             indexgroup);
        return po;
    }



    public static int getcodesize() {
        return 2;
    }


    public static int getlower(int ch, int flags) {
        return SRE_STATE.getlower(ch, flags);
    }
}
