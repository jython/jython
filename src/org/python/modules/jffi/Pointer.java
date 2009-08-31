
package org.python.modules.jffi;

import org.python.core.Py;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

@ExposedType(name = "jffi.Pointer", base = PyObject.class)
public class Pointer extends PyObject {
    public static final PyType TYPE = PyType.fromClass(Pointer.class);

    @ExposedGet
    public final long address;

    final Memory memory;
    final MemoryOp componentMemoryOp;

    public Pointer(PyType type, long address, Memory memory) {
        super(type);
        this.address = address;
        this.memory = memory;
        this.componentMemoryOp = MemoryOp.INVALID;
    }

    public Pointer(long address, Memory memory) {
        this.address = address;
        this.memory = memory;
        this.componentMemoryOp = MemoryOp.INVALID;
    }

    Pointer(DirectMemory memory, MemoryOp componentMemoryOp) {
        this(TYPE, memory, componentMemoryOp);
    }

    Pointer(PyType subtype, DirectMemory memory, MemoryOp componentMemoryOp) {
        super(subtype);
        this.address = memory.getAddress();
        this.memory = memory;
        this.componentMemoryOp = componentMemoryOp;
    }

    @ExposedNew
    public static PyObject Pointer_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {

        PyObject jffi_type = subtype.__getattr__("_jffi_type");

        if (!(jffi_type instanceof CType.Pointer)) {
            throw Py.TypeError("invalid _jffi_type for " + subtype.getName());
        }

        CType.Pointer type = (CType.Pointer) jffi_type;

        if (args.length == 0) {
            return new Pointer(subtype, NullMemory.INSTANCE, type.componentMemoryOp);
        }
        DirectMemory contents = AllocatedNativeMemory.allocate(type.componentType.size(), false);
        type.componentMemoryOp.put(contents, 0, args[0]);

        return new Pointer(subtype, contents, type.componentMemoryOp);
    }

    @ExposedGet(name="contents")
    public PyObject contents() {
        return componentMemoryOp.get(memory, 0);
    }

    @ExposedSet(name="contents")
    public void contents(PyObject value) {
        componentMemoryOp.put(memory, 0, value);
    }

}
