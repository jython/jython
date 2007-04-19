// Copyright (c) Corporation for National Research Initiatives
package org.python.modules;

import java.io.File;

import org.python.core.PyList;
import org.python.core.PyString;

public class py_compile {
    public static PyList __all__ = new PyList(new PyString[] { new PyString("compile") });

    public static boolean compile(String filename, String cfile) {
        return compile(filename, cfile, null);
    }

    public static boolean compile(String filename) {
        return compile(filename, null, null);
    }

    public static boolean compile(String filename, String cfile, String dfile) {
        File file = new File(filename);
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot != -1) {
            name = name.substring(0, dot);
        }
        byte[] bytes = org.python.core.imp.compileSource(name, file, dfile, cfile);
        org.python.core.imp.cacheCompiledSource(filename, null, bytes);

        return bytes.length > 0;
    }
}
