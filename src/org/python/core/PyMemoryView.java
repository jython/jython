package org.python.core;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "memoryview", base = PyObject.class, isBaseType = false)
public class PyMemoryView extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyMemoryView.class);

    MemoryView backing;

    public PyMemoryView(MemoryViewProtocol obj) {
        backing = obj.getMemoryView();
    }

    @ExposedNew
    static PyObject memoryview_new(PyNewWrapper new_, boolean init, PyType subtype, PyObject[] args,
                              String[] keywords) {
        PyObject obj = args[0];
        if (obj instanceof MemoryViewProtocol) {
            return new PyMemoryView((MemoryViewProtocol)obj);
        }
        else throw Py.TypeError("cannot make memory view because object does not have the buffer interface");
    }

    @ExposedGet(name = "format")
    public String get_format() {
        return backing.get_format();
    }

    @ExposedGet(name = "itemsize")
    public int get_itemsize() {
        return backing.get_itemsize();
    }

    @ExposedGet(name = "shape")
    public PyTuple get_shape() {
        return backing.get_shape();
    }

    @ExposedGet(name = "ndim")
    public int get_ndim() {
        return backing.get_ndim();
    }

    @ExposedGet(name = "strides")
    public PyTuple get_strides() {
        return backing.get_strides();
    }

    @ExposedGet(name = "readonly")
    public boolean get_readonly() {
        return backing.get_readonly();
    }

}


