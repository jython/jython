
package org.python.modules.jffi;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name = "jffi.CData", base = PyObject.class)
public abstract class CData extends PyObject {
    public static final PyType TYPE = PyType.fromClass(CData.class);
    
    private final CType ctype;

    private DirectMemory referenceMemory;

    CData(PyType subtype, CType ctype) {
        this(subtype, ctype, null);
    }

    CData(PyType subtype, CType ctype, DirectMemory memory) {
        super(subtype);
        this.ctype = ctype;
        this.referenceMemory = memory;
    }

    /**
     * Wraps up this object in a pointer that can be passed to native code.
     * The byref() return value cannot be used as anything other than a parameter.
     *
     * @return A ByReference instance pointing to this object's native memory.
     */
    @ExposedMethod(names= { "byref" })
    public PyObject byref(PyObject offset) {
        return new ByReference(ctype, (DirectMemory) getReferenceMemory().slice(offset.asInt()));
    }

    @ExposedMethod(names= { "pointer" })
    public PyObject pointer(PyObject pytype) {
        if (!(pytype instanceof PyType)) {
            throw Py.TypeError("expected type");
        }

        return new PointerCData((PyType) pytype, CType.typeOf(pytype), getReferenceMemory(), getMemoryOp());
    }

    @ExposedMethod(names = { "address" })
    public PyObject address() {
        return Py.newInteger(getReferenceMemory().getAddress());
    }

    final CType getCType() {
        return ctype;
    }

    MemoryOp getMemoryOp() {
        return getCType().getMemoryOp();
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
        DirectMemory m = AllocatedNativeMemory.allocate(getCType().size(), false);
        initReferenceMemory(m);
        this.referenceMemory = m;
        return m;
    }

    public Memory getContentMemory() {
        return getReferenceMemory();
    }

    protected abstract void initReferenceMemory(Memory m);

    

    protected static final DirectMemory findInDll(PyObject lib, PyObject name) {
        if (!(lib instanceof DynamicLibrary)) {
            throw Py.TypeError("expected library, not " + lib.getType().fastGetName());
        }
        DynamicLibrary.Symbol sym = (DynamicLibrary.Symbol) ((DynamicLibrary) lib).find_symbol(name);
        
        return sym.getMemory();
    }
}
