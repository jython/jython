
package org.python.modules.jffi;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name = "jffi.CData", base = PyObject.class)
public abstract class CData extends PyObject {
    public static final PyType TYPE = PyType.fromClass(CData.class);

    final MemoryOp memoryOp;
    final CType type;

    private DirectMemory referenceMemory;

    CData(PyType subtype, CType type, MemoryOp memoryOp) {
        super(subtype);
        this.type = type;
        this.memoryOp = memoryOp;
        this.referenceMemory = null;
    }
    
    @ExposedMethod(names= { "byref", "pointer" })
    public PyObject byref() {
        return new PointerCData(type, getReferenceMemory(), memoryOp);
    }

    final boolean hasReferenceMemory() {
        return referenceMemory != null;
    }

    final void setReferenceMemory(Memory memory) {
        if (!(memory instanceof DirectMemory)) {
            throw Py.TypeError("invalid memory");
        }
        this.referenceMemory = (DirectMemory) memory;
    }

    /**
     * Returns the memory used when creating a reference to this instance.
     * e.g. via byref(obj)
     *
     * @return The reference memory for this object
     */
    public final DirectMemory getReferenceMemory() {
        if (referenceMemory != null) {
            return referenceMemory;
        }

        return allocateReferenceMemory();
    }

    protected DirectMemory allocateReferenceMemory() {
        DirectMemory m = AllocatedNativeMemory.allocate(type.size(), false);
        initReferenceMemory(m);
        this.referenceMemory = m;
        return m;
    }

    public Memory getContentMemory() {
        return getReferenceMemory();
    }

    protected abstract void initReferenceMemory(Memory m);
}
