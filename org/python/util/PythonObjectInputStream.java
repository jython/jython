// Copyright 2000 Finn Bock

package org.python.util;

import java.io.*;
import org.python.core.*;

public class PythonObjectInputStream extends ObjectInputStream {
    public PythonObjectInputStream(InputStream istr) throws IOException {
        super(istr);
    }

    protected Class resolveClass(ObjectStreamClass v)
                      throws IOException, ClassNotFoundException {
        String clsName = v.getName();
        //System.out.println(clsName);
        if (clsName.startsWith("org.python.proxies")) {
            int idx = clsName.lastIndexOf('$');
            if (idx > 19)
               clsName = clsName.substring(19, idx);
            //System.out.println("new:" + clsName);

            idx = clsName.indexOf('$');
            if (idx >= 0) {
                String mod = clsName.substring(0, idx);
                clsName = clsName.substring(idx+1);

                PyObject module = importModule(mod);
                PyObject pycls = module.__getattr__(clsName.intern());
                Object cls = pycls.__tojava__(Class.class);

                if (cls != null && cls != Py.NoConversion)
                    return (Class) cls;
            }
        }
        try {
            return super.resolveClass(v);
        } catch (ClassNotFoundException exc) {
            PyObject m = importModule(clsName);
            //System.out.println("m:" + m);
            Object cls = m.__tojava__(Class.class);
            //System.out.println("cls:" + cls);
            if (cls != null && cls != Py.NoConversion)
                return (Class) cls;
            throw exc;
        }
    }


    private static PyObject importModule(String name) {
        PyObject silly_list = new PyTuple(new PyString[] {
            Py.newString("__doc__"),
        });
        return __builtin__.__import__(name, null, null, silly_list);
    }
}
