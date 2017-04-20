/* Maintains semantics of NullImporter objects in CPython - unique objects,
  but does not retain the path used. */

package org.python.core;

import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@Untraversable
@ExposedType(name = "NullImporter", isBaseType = false)
public class PyNullImporter extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyNullImporter.class);
    static {
        TYPE.setName("imp.NullImporter");
    }

    public PyNullImporter(PyObject pathObj) {
        super();
        String pathStr = Py.fileSystemDecode(pathObj);
        if (pathStr.equals("")) {
            throw Py.ImportError("empty pathname");
        }
        if (isDir(pathStr)) {
            throw Py.ImportError("existing directory: " + pathStr);
        }
    }

    public PyObject find_module(String fullname) {
        return Py.None;
    }

    public PyObject find_module(String fullname, String path) {
        return Py.None;
    }

    @ExposedMethod(defaults = "null")
    final PyObject NullImporter_find_module(String fullname, String path) {
        return Py.None;
    }

    private static boolean isDir(String pathStr) {
        if (pathStr.equals("")) {
            return false;
        }
        try {
            Path path = FileSystems.getDefault().getPath(pathStr);
            if (!path.isAbsolute()) {
                path = FileSystems.getDefault().getPath(Py.getSystemState().getCurrentWorkingDir(), pathStr);
            }
            return path.toFile().isDirectory();
        } catch (java.nio.file.InvalidPathException ex) {
            return false;
        }
    }

}
