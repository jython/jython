
package org.python.modules.jffi;

import com.kenai.jffi.Library;
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
        return new Symbol(this, name.asString(), new NativeMemory(address));
    }

    @ExposedMethod
    public final PyObject find_function(PyObject name) {
        return new TextSymbol(this, name.asString(), findSymbol(name));
    }

    @ExposedMethod
    public final PyObject find_variable(PyObject name) {
        return new DataSymbol(this, name.asString(), findSymbol(name));
    }

    @ExposedType(name = "jffi.DynamicLibrary.Symbol", base = PyObject.class)
    public static class Symbol extends BasePointer {
        public static final PyType TYPE = PyType.fromClass(Symbol.class);

        final DynamicLibrary library;
        final DirectMemory memory;

        @ExposedGet
        public final String name;

        public Symbol(DynamicLibrary library, String name, DirectMemory memory) {
            super(TYPE);
            this.library = library;
            this.name = name;
            this.memory = memory;
        }

        public final DirectMemory getMemory() {
            return memory;
        }
    }
    
    public static final class TextSymbol extends Symbol implements ExposeAsSuperclass {
        public TextSymbol(DynamicLibrary lib, String name, long address) {
            super(lib, name, new SymbolMemory(lib, address));
        }
    }

    public static final class DataSymbol extends Symbol implements ExposeAsSuperclass {
        public DataSymbol(DynamicLibrary lib, String name, long address) {
            super(lib, name, new SymbolMemory(lib, address));
        }
    }

    private static final class SymbolMemory extends NativeMemory {
        private final DynamicLibrary library; // backlink to keep library alive

        public SymbolMemory(DynamicLibrary library, long address) {
            super(address);
            this.library = library;
        }
    }
}
