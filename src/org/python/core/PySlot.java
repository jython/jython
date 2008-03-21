package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name = "member_descriptor", base = PyObject.class)
public class PySlot extends PyDescriptor {

    public PySlot(PyType dtype, String name, int index) {
        this.name = name;
        this.dtype = dtype;
        this.index = index;
    }

    @Override
    public boolean implementsDescrSet() {
        return true;
    }

    @Override
    public boolean isDataDescr() {
        return true;
    }

    @Override
    public PyObject __get__(PyObject obj, PyObject type) {
        return member_descriptor___get__(obj, type);
    }

    @ExposedMethod
    public PyObject member_descriptor___get__(PyObject obj, PyObject type) {
        if(obj != null) {
            checkType((PyType)type);
            return ((Slotted)obj).getSlot(index);
        }
        return this;
    }

    @Override
    public void __set__(PyObject obj, PyObject value) {
        member_descriptor___set__(obj, value);
    }

    @ExposedMethod
    public void member_descriptor___set__(PyObject obj, PyObject value) {
        checkType(obj.getType());
        ((Slotted)obj).setSlot(index, value);
    }

    @Override
    public void __delete__(PyObject obj) {
        member_descriptor___delete__(obj);
    }

    @ExposedMethod
    public void member_descriptor___delete__(PyObject obj) {
        checkType(obj.getType());
        ((Slotted)obj).setSlot(index, null);
    }

    @Override
    public String toString() {
        return "<member '" + name + "' of '" + dtype.fastGetName()
                + "' objects>";
    }

    private void checkType(PyType type) {
        if(type != dtype && !type.isSubType(dtype))
            throw get_wrongtype(type);
    }

    private int index;
}
