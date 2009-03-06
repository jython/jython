package org.python.expose;

import org.python.core.PyBuiltinMethod;
import org.python.core.PyDataDescr;
import org.python.core.PyMethodDescr;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyStringMap;
import org.python.core.PyType;

public class BaseTypeBuilder implements TypeBuilder {

    private PyNewWrapper newWrapper;

    private PyBuiltinMethod[] meths;

    private PyDataDescr[] descrs;

    private Class<?> typeClass;

    private Class<?> baseClass;

    private String name;

    private boolean isBaseType;

    public BaseTypeBuilder(String name,
                           Class<?> typeClass,
                           Class<?> baseClass,
                           boolean isBaseType,
                           PyBuiltinMethod[] meths,
                           PyDataDescr[] descrs,
                           PyNewWrapper newWrapper) {
        this.typeClass = typeClass;
        this.baseClass = baseClass;
        this.isBaseType = isBaseType;
        this.name = name;
        this.descrs = descrs;
        this.meths = meths;
        this.newWrapper = newWrapper;
    }

    public PyObject getDict(PyType type) {
        PyObject dict = new PyStringMap();
        for(PyBuiltinMethod func : meths) {
            PyMethodDescr pmd = func.makeDescriptor(type);
            dict.__setitem__(pmd.getName(), pmd);
        }
        for(PyDataDescr descr : descrs) {
            descr.setType(type);
            dict.__setitem__(descr.getName(), descr);
        }
        if (newWrapper != null) {
            dict.__setitem__("__new__", newWrapper);
            newWrapper.setWrappedType(type);
        }
        return dict;
    }

    public String getName() {
        return name;
    }

    public Class<?> getTypeClass() {
        return typeClass;
    }

    public Class<?> getBase() {
        return baseClass;
    }

    public boolean getIsBaseType() {
        return isBaseType;
    }
}
