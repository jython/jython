// Copyright (c) Corporation for National Research Initiatives
package org.python.modules;

import java.io.File;

import jnr.constants.platform.Errno;
import org.python.core.Py;
import org.python.core.PyList;
import org.python.core.PyString;
import org.python.core.PySystemState;

public class _py_compile {
    public static PyList __all__ = new PyList(new PyString[] { new PyString("compile") });

    /**
     * Java wrapper on the module compiler in support of of py_compile.compile. Filenames here will
     * be interpreted as Unicode if they are PyUnicode, and as byte-encoded names if they only
     * PyString.
     *
     * @param fileName actual source file name
     * @param compiledName compiled filename
     * @param displayName displayed source filename, only used for error messages (and not resolved)
     * @return true if successful
     */
    public static boolean compile(PyString fileName, PyString compiledName, PyString displayName) {
        // Resolve source path and check it exists
        PySystemState sys = Py.getSystemState();
        String file = sys.getPath(Py.fileSystemDecode(fileName));
        File f = new File(file);
        if (!f.exists()) {
            throw Py.IOError(Errno.ENOENT, file);
        }

        // Convert file in which to put the byte code and display name (each may be null)
        String c = (compiledName == null) ? null : sys.getPath(Py.fileSystemDecode(compiledName));
        String d = (displayName == null) ? null : Py.fileSystemDecode(displayName);
        byte[] bytes = org.python.core.imp.compileSource(getModuleName(f), f, d);
        org.python.core.imp.cacheCompiledSource(file, c, bytes);
        return bytes.length > 0;
    }

    public static final String getModuleName(File f) {
        String name = f.getName();
        int dot = name.lastIndexOf('.');
        if (dot != -1) {
            name = name.substring(0, dot);
        }

        // name the __init__ module after its package
        File dir = f.getParentFile();
        if (name.equals("__init__")) {
            name = dir.getName();
            dir = dir.getParentFile();
        }

        // Make the compiled classfile's name fully qualified with a package by walking up the
        // directory tree looking for __init__.py files. Don't check for __init__$py.class since
        // we're compiling source here and the existence of a class file without corresponding
        // source probably doesn't indicate a package.
        while (dir != null && (new File(dir, "__init__.py").exists())) {
            name = dir.getName() + "." + name;
            dir = dir.getParentFile();
        }
        return name;
    }
}
