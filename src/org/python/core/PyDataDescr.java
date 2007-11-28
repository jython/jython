package org.python.core;

/**
 * Implements type checking and return type coercion for a data descriptor. A
 * subclass must at least implement invokeGet which is called in __get__
 * operations. If the descriptor supports setting and deleting, the subclass
 * must also override invokeSet and invokeDel respectively. When implementing
 * those methods, their respective implementsDescr* methods should be overriden
 * as well.
 */
public class PyDataDescr extends PyDescriptor {

    /**
     * @param onType -
     *            the type the descriptor belongs to
     * @param name -
     *            the name of the descriptor on descriptor type
     * @param ofType -
     *            the type returned by the descriptor
     */
    public PyDataDescr(Class onType, String name, Class ofType) {
        this(PyType.fromClass(onType), name, ofType);
    }

    /**
     * @param onType -
     *            the type the descriptor belongs to
     * @param name -
     *            the name of the descriptor on descriptor type
     * @param ofType -
     *            the type returned by the descriptor
     */
    public PyDataDescr(PyType onType, String name, Class ofType) {
        this.dtype = onType;
        this.name = name;
        this.ofType = ofType;
    }

    /**
     * @return - the name this descriptor is exposed as
     */
    public String getName() {
        return name;
    }

    @Override
    public PyObject __get__(PyObject obj, PyObject type) {
        if(obj != null) {
            PyType objtype = obj.getType();
            if(objtype != dtype && !objtype.isSubType(dtype))
                throw get_wrongtype(objtype);
            Object v = invokeGet(obj);
            if(v == null) {
                obj.noAttributeError(name);
            }
            return Py.java2py(v);
        }
        return this;
    }

    public Object invokeGet(PyObject obj) {
        throw new UnsupportedOperationException("Must be overriden by a subclass");
    }

    @Override
    public void __set__(PyObject obj, PyObject value) {
        PyType objtype = obj.getType();
        if(objtype != dtype && !objtype.isSubType(dtype))
            throw get_wrongtype(objtype);
        Object converted = value.__tojava__(ofType);
        if(converted == Py.NoConversion) {
            throw Py.TypeError(""); // xxx
        }
        invokeSet(obj, converted);
    }

    public void invokeSet(PyObject obj, Object converted) {
        throw new UnsupportedOperationException("Must be overriden by a subclass");
    }

    @Override
    public void __delete__(PyObject obj) {
        if(obj != null) {
            PyType objtype = obj.getType();
            if(objtype != dtype && !objtype.isSubType(dtype))
                throw get_wrongtype(objtype);
            invokeDelete(obj);
        }
    }

    public void invokeDelete(PyObject obj) {
        throw new UnsupportedOperationException("Must be overriden by a subclass");
    }

    @Override
    public boolean isDataDescr() {
        return true;
    }

    @Override
    public String toString() {
        return "<member '" + name + "' of '" + dtype.fastGetName() + "' objects>";
    }

    protected Class ofType;
}
