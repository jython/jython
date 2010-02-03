
package org.python.modules.jffi;

import java.util.List;
import org.python.core.Py;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedClassMethod;
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

        StructLayout layout = getStructLayout(subtype);
        Structure s = new Structure(subtype, layout);
        if (args.length > 0) {
            int n = args.length - keywords.length;
            List<StructLayout.Field> fields = layout.getFieldList();
            Memory m = s.getMemory();
            // First, do non-keyword args in order
            for (int i = 0; i < n; ++i) {
                StructLayout.Field f = fields.get(i);
                f.op.put(m, f.offset, args[i]);
            }

            // Now handle the keyworded args by looking up the field
            for (int i = n; i < args.length; ++i) {
                StructLayout.Field f = layout.getField(keywords[i - n]);
                f.op.put(m, f.offset, args[i]);
            }
        }
        return s;
    }

    static final StructLayout getStructLayout(PyType subtype) {
        PyObject jffi_type = subtype.__getattr__("_jffi_type");
        if (!(jffi_type instanceof StructLayout)) {
            throw Py.TypeError("invalid _jffi_type for " + subtype.fastGetName() + "; should be instance of jffi.StructLayout");
        }

        return (StructLayout) jffi_type;
    }

    @ExposedClassMethod(names= { "from_address" })
    public static final PyObject from_address(PyType subtype, PyObject address) {
        return new Structure(subtype, getStructLayout(subtype), Util.getMemoryForAddress(address));
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
