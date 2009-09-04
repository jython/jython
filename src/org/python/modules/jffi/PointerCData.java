
package org.python.modules.jffi;

import org.python.core.Py;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedClassMethod;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

@ExposedType(name = "jffi.PointerCData", base = CData.class)
public class PointerCData extends AbstractMemoryCData implements Pointer {
    public static final PyType TYPE = PyType.fromClass(PointerCData.class);

    final MemoryOp componentMemoryOp;

    PointerCData(PyType subtype, CType type, DirectMemory memory, MemoryOp componentMemoryOp) {
        super(subtype, type, memory);
        this.componentMemoryOp = componentMemoryOp;
    }
    
    @ExposedNew
    public static PyObject PointerCData_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {

        CType.Pointer pointerType = getPointerType(subtype);

        // No args == create NULL pointer
        if (args.length == 0) {
            return new PointerCData(subtype, pointerType, NullMemory.INSTANCE, pointerType.componentMemoryOp);
        }

        PyObject value = args[0];
        if (value instanceof CData && value.getType().isSubType(pointerType.pyComponentType)) {

            return new PointerCData(subtype, pointerType, ((CData) value).getReferenceMemory(), pointerType.componentMemoryOp);

        } else {
            throw Py.TypeError("expected " + pointerType.pyComponentType.getName() + " instead of " + value.getType().getName());
        }
    }

    static final CType.Pointer getPointerType(PyType subtype) {
        PyObject jffi_type = subtype.__getattr__("_jffi_type");

        if (!(jffi_type instanceof CType.Pointer)) {
            throw Py.TypeError("invalid _jffi_type for " + subtype.getName());
        }

        return (CType.Pointer) jffi_type;
    }

    @ExposedClassMethod(names= { "from_address" })
    public static final PyObject from_address(PyType subtype, PyObject address) {

        CType.Pointer pointerType = getPointerType(subtype);
        DirectMemory m = Util.getMemoryForAddress(address);
        PointerCData cdata = new PointerCData(subtype, pointerType, m.getMemory(0), pointerType.componentMemoryOp);
        cdata.setReferenceMemory(m);

        return cdata;
    }
    
    @ExposedGet(name="contents")
    public PyObject getContents() {
        return componentMemoryOp.get(getMemory(), 0);
    }

    @ExposedSet(name="contents")
    public void setContents(PyObject value) {
        componentMemoryOp.put(getMemory(), 0, value);
    }

}
