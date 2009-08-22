
package org.python.modules.jffi;

import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedType;

@ExposedType(name = "jffi.Pointer", base = PyObject.class)
public class Pointer extends PyObject {
    public static final PyType TYPE = PyType.fromClass(Pointer.class);

    @ExposedGet
    public final long address;

    final Memory memory;

    public Pointer(PyType type, long address, Memory memory) {
        super(type);
        this.address = address;
        this.memory = memory;
    }

    public Pointer(long address, Memory memory) {
        this.address = address;
        this.memory = memory;
    }
}
