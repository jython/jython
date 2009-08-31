
package org.python.modules.jffi;

import org.python.core.Py;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

@ExposedType(name = "jffi.PointerCData", base = CData.class)
public class PointerCData extends CData implements Pointer {
    public static final PyType TYPE = PyType.fromClass(PointerCData.class);

    private final DirectMemory memory;
    final MemoryOp componentMemoryOp;

    PointerCData(PyType subtype, CType type, DirectMemory memory, MemoryOp componentMemoryOp) {
        super(subtype, type, type.getMemoryOp());
        this.memory = memory;
        this.componentMemoryOp = componentMemoryOp;
    }

    PointerCData(CType type, DirectMemory memory, MemoryOp componentMemoryOp) {
        this(TYPE, type, memory, componentMemoryOp);
    }


    @ExposedNew
    public static PyObject PointerCData_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {

        PyObject jffi_type = subtype.__getattr__("_jffi_type");

        if (!(jffi_type instanceof CType.Pointer)) {
            throw Py.TypeError("invalid _jffi_type for " + subtype.getName());
        }

        CType.Pointer type = (CType.Pointer) jffi_type;

        if (args.length == 0) {
            return new PointerCData(subtype, type, NullMemory.INSTANCE, type.componentMemoryOp);
        }
        PyObject value = args[0];
        if (value instanceof CData && value.getType().isSubType(type.pyComponentType)) {

            return new PointerCData(subtype, type, ((CData) value).getReferenceMemory(), type.componentMemoryOp);

        } else {
            throw Py.TypeError("expected " + type.pyComponentType.getName() + " instead of " + value.getType().getName());
        }
    }

    @ExposedGet(name="contents")
    public PyObject getContents() {
        return componentMemoryOp.get(getMemory(), 0);
    }

    @ExposedSet(name="contents")
    public void setContents(PyObject value) {
        componentMemoryOp.put(getMemory(), 0, value);
    }

    @Override
    public boolean __nonzero__() {
        return !getMemory().isNull();
    }


    protected void initReferenceMemory(Memory m) {
        m.putAddress(0, memory);
    }

    public final long getAddress() {
        return getMemory().getAddress();
    }

    public final DirectMemory getMemory() {
        return hasReferenceMemory() ? getReferenceMemory().getMemory(0) : memory;
    }
    
}
