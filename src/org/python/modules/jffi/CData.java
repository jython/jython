
package org.python.modules.jffi;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

@ExposedType(name = "jffi.CData", base = PyObject.class)
public abstract class CData extends PyObject {
    public static final PyType TYPE = PyType.fromClass(CData.class);

    final MemoryOp memoryOp;
    final CType type;

    private Memory contentMemory;
    private PyObject value;

    CData(PyType subtype, CType type, MemoryOp memoryOp) {
        super(subtype);
        this.type = type;
        this.memoryOp = memoryOp;
        this.value = Py.None;
        this.contentMemory = null;
    }

    @ExposedGet(name = "value")
    public PyObject getValue() {
        // If native memory has been allocated, read the value from there
        if (contentMemory != null) {
            return memoryOp.get(contentMemory, 0);
        }

        return value;
    }


    @ExposedSet(name = "value")
    public void setValue(PyObject value) {
        this.value = value;
        // If native memory has been allocated, sync the value to memory
        if (contentMemory != null) {
            memoryOp.put(contentMemory, 0, value);
        }
    }

    @ExposedMethod(names= { "byref", "pointer" })
    public PyObject byref() {
        return new Pointer((DirectMemory) getContentMemory(), memoryOp);
    }

    boolean hasValueMemory() {
        return contentMemory != null;
    }

    void setContentMemory(Memory memory) {
        if (!(memory instanceof DirectMemory)) {
            throw Py.TypeError("invalid memory");
        }
        this.contentMemory = memory;
    }

    Memory getContentMemory() {
        if (contentMemory != null) {
            return contentMemory;
        }

        return allocateDirect();
    }

    private DirectMemory allocateDirect() {
        DirectMemory m = AllocatedNativeMemory.allocate(type.size(), false);
        memoryOp.put(m, 0, value);
        contentMemory = m;
        return m;
    }
}
