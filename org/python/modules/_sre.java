
package org.python.modules;

import java.util.*;
import org.python.core.*;
import org.python.modules.sre.*;


public class _sre {

    public static PatternObject compile(PyString pattern, int flags, PyObject code,
            int groups, PyObject groupindex, PyObject indexgroup) {
        char[] ccode = null;
        if (code instanceof PyList) {
            int n = code.__len__();
            ccode = new char[n];
            for (int i = 0; i < n; i++) 
                ccode[i] = (char) code.__getitem__(i).__int__().getValue();
        } else {
            throw Py.TypeError("Expected list");
        }

        PatternObject po = new PatternObject(pattern, flags, ccode, groups, groupindex, indexgroup);
        return po;
    }



    public static int getcodesize() {
        return 2;
    }


    public static int getlower(int ch, int flags) {
        return SRE_STATE.getlower(ch, flags);
    }
}