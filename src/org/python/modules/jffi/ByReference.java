
package org.python.modules.jffi;

import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedType;

@ExposedType(name = "jffi.ByReference", base = PyObject.class)
public final class ByReference extends PyObject implements Pointer {
    public static final PyType TYPE = PyType.fromClass(ByReference.class);

    private final DirectMemory memory;

    ByReference(CType componentType, DirectMemory memory) {
        super(TYPE);
        this.memory = memory;
    }

    public final DirectMemory getMemory() {
        return memory;
    }

    @Override
    public boolean __nonzero__() {
        return !getMemory().isNull();
    }
}
