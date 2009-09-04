
package org.python.modules.jffi;

import org.python.core.Py;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "jffi.Structure", base = CData.class)
public class Structure extends CData implements Pointer {
    public static final PyType TYPE = PyType.fromClass(Structure.class);

    private final StructLayout layout;
    private final MemoryOp memoryOp;

    Structure(PyType pyType, StructLayout layout) {
        this(pyType, layout, AllocatedNativeMemory.allocate(layout.size(), true));
    }

    Structure(PyType pyType, StructLayout layout, Memory m) {
        super(pyType, layout);
        this.layout = layout;
        this.memoryOp = new MemoryOp.StructOp(pyType, layout);
        setReferenceMemory(m);
    }

    @ExposedNew
    public static PyObject Structure_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {

        PyObject layout = subtype.__getattr__("_jffi_type");
        if (!(layout instanceof StructLayout)) {
            throw Py.TypeError("invalid _jffi_type for " + subtype.fastGetName() + "; should be instance of jffi.StructLayout");
        }

        return new Structure(subtype, (StructLayout) layout);
    }

    protected final void initReferenceMemory(Memory m) {
        throw Py.RuntimeError("reference memory already initialized");
    }

    @Override
    MemoryOp getMemoryOp() {
        return memoryOp;
    }

    StructLayout.Field getField(PyObject key) {
        StructLayout.Field f = layout.getField(key);
        if (f == null) {
            throw Py.NameError(String.format("struct %s has no field '%s'", getType().fastGetName(), key.toString()));
        }
        return f;
    }

    @Override
    public PyObject __getitem__(PyObject key) {
        StructLayout.Field f = getField(key);
        return f.op.get(getReferenceMemory(), f.offset);
    }

    @Override
    public void __setitem__(PyObject key, PyObject value) {
        StructLayout.Field f = getField(key);
        f.op.put(getReferenceMemory(), f.offset, value);
    }

    public DirectMemory getMemory() {
        return getReferenceMemory();
    }

}
