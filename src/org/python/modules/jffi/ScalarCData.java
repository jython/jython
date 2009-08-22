
package org.python.modules.jffi;

import org.python.core.Py;
import org.python.core.PyFloat;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyObject.ConversionException;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

@ExposedType(name = "jffi.ScalarCData", base = PyObject.class)
public class ScalarCData extends PyObject {
    public static final PyType TYPE = PyType.fromClass(ScalarCData.class);

    final Type.Builtin type;
    private PyObject value;
    private Memory memory;
    
    
    @ExposedNew
    public static PyObject ScalarCData_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        
        PyObject jffi_type = subtype.__getattr__("_jffi_type");

        if (!(jffi_type instanceof Type.Builtin)) {
            throw Py.TypeError("invalid _jffi_type for " + subtype.getName());
        }
        
        ScalarCData cdata = new ScalarCData(subtype, (Type.Builtin) jffi_type);

        // If an initial value was supplied, use it, else default to zero
        cdata.setValue(args.length > 0 ? args[0] : Py.newInteger(0));

        return cdata;
    }

    ScalarCData(PyType pyType, Type.Builtin type) {
        super(pyType);
        this.type = type;
        this.memory = null;
    }

    @ExposedGet(name = "value")
    public PyObject getValue() {
        // If native memory has been allocated, read the value from there
        if (memory != null) {
            return type.getMemoryOp().get(memory, 0);
        }

        return value;
    }


    @ExposedSet(name = "value")
    public void setValue(PyObject value) {
        this.value = value;
        // If native memory has been allocated, sync the value to memory
        if (memory != null) {
            type.getMemoryOp().put(memory, 0, value);
        }
    }

    @Override
    public int asInt() {
        return getValue().asInt();
    }

    @Override
    public long asLong(int index) throws ConversionException {
        return getValue().asLong(index);
    }

    @ExposedMethod
    @Override
    public PyObject __int__() {
        return getValue().__int__();
    }

    @ExposedMethod
    @Override
    public PyObject __long__() {
        return getValue().__long__();
    }

    @ExposedMethod
    @Override
    public PyFloat __float__() {
        return getValue().__float__();
    }

    @Override
    public final String toString() {
        return getType().getName() + "(" + getValue().toString() + ")";
    }
}
