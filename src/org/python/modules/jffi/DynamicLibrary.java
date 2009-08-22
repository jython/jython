
package org.python.modules.jffi;

import com.kenai.jffi.Library;
import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposeAsSuperclass;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name = "jffi.DynamicLibrary", base = PyObject.class)
public class DynamicLibrary extends PyObject {

    public static final PyType TYPE = PyType.fromClass(DynamicLibrary.class);

    @ExposedGet
    public final String name;
    private final com.kenai.jffi.Library lib;
    
    DynamicLibrary(String libname, int mode) {
        this.name = libname;
        lib = Library.getCachedInstance(libname, mode);

        if (lib == null) {
            throw Py.RuntimeError("Could not open " 
                    + libname != null ? libname : "[current process]"
                    + " " + Library.getLastError());
        }
    }

    private long findSymbol(PyObject name) {
        long address = lib.getSymbolAddress(name.asString());
        if (address == 0) {
            throw Py.NameError("Could not locate symbol '" + name.asString() + "' in " + this.name);
        }

        return address;
    }

    @ExposedMethod
    public final PyObject find_symbol(PyObject name) {
        long address = findSymbol(name);
        return new Symbol(this, name.asString(), address, new NativeMemory(address));
    }

    @ExposedMethod
    public final PyObject find_function(PyObject name) {
        return new TextSymbol(this, name.asString(), findSymbol(name));
    }

    @ExposedMethod
    public final PyObject find_variable(PyObject name) {
        return new DataSymbol(this, name.asString(), findSymbol(name));
    }

    @ExposedType(name = "jffi.DynamicLibrary.Symbol", base = Pointer.class)
    public static class Symbol extends Pointer {
        public static final PyType TYPE = PyType.fromClass(Symbol.class);

        final DynamicLibrary library;

        @ExposedGet
        public final String name;

        public Symbol(DynamicLibrary library, String name, long address, DirectMemory memory) {
            super(address, memory);
            this.library = library;
            this.name = name;
        }
    }
    
    public static class TextSymbol extends Symbol implements ExposeAsSuperclass {
        public TextSymbol(DynamicLibrary lib, String name, long address) {
            super(lib, name, address, new NativeMemory(address));
        }
    }

    public static class DataSymbol extends Symbol implements ExposeAsSuperclass {
        public DataSymbol(DynamicLibrary lib, String name, long address) {
            super(lib, name, address, new NativeMemory(address));
        }
    }
}
