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
        // Make the compiled classfile's name the fully qualified with a package by
        // walking up the directory tree looking for __init__.py files. Don't
        // check for __init__$py.class since we're compiling source here and the
        // existence of a class file without corresponding source probably doesn't
        // indicate a package.
        File dir = file.getParentFile();
        while (dir != null && (new File(dir, "__init__.py").exists())) {
            name = dir.getName() + "." + name;
            dir = dir.getParentFile();
        }
        byte[] bytes = org.python.core.imp.compileSource(name, file, dfile, cfile);
        org.python.core.imp.cacheCompiledSource(filename, cfile, bytes);

        return bytes.length > 0;
    }
    
}
