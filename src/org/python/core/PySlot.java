package org.python.core;

public class PySlot extends PyDescriptor {

    public PySlot(PyType dtype, String name, int index) {
        this.name = name;
        this.dtype = dtype;
        this.index = index;
    }

    public boolean implementsDescrSet() {
        return true;
    }

    public boolean isDataDescr() {
        return true;
    }

    public PyObject __get__(PyObject obj, PyObject type) {
        if(obj != null) {
            checkType((PyType)type);
            return ((Slotted)obj).getSlot(index);
        }
        return this;
    }

    public void __set__(PyObject obj, PyObject value) {
        checkType(obj.getType());
        ((Slotted)obj).setSlot(index, value);
    }

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
