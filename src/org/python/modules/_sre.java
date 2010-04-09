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

import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.modules.sre.PatternObject;
import org.python.modules.sre.SRE_STATE;

public class _sre {
    public static int MAGIC = SRE_STATE.SRE_MAGIC;

    // workaround the fact that H, I types are unsigned, but we are not really using them as such
    // XXX: May not be the right size, but I suspect it is -- see sre_compile.py
    public static int CODESIZE = 4;

    public static PatternObject compile(PyString pattern, int flags, PyObject code, int groups,
                                        PyObject groupindex, PyObject indexgroup) {
        int[] ccode = new int[code.__len__()];
        int i = 0;
        for (PyObject item : code.asIterable()) {
            ccode[i++] = (int)item.asLong();
        }
        return new PatternObject(pattern, flags, ccode, groups, groupindex, indexgroup);
    }

    public static int getcodesize() {
        return CODESIZE;
    }

    public static int getlower(int ch, int flags) {
        return SRE_STATE.getlower(ch, flags);
    }
}
