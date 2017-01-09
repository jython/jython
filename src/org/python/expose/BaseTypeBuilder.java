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

    private Class<? extends PyObject> typeClass;

    private Class<?> baseClass;

    private String name;

    private boolean isBaseType;

    private String doc;

    public BaseTypeBuilder(String name,
                           Class<? extends PyObject> typeClass,
                           Class<?> baseClass,
                           boolean isBaseType,
                           String doc,
                           PyBuiltinMethod[] meths,
                           PyDataDescr[] descrs,
                           PyNewWrapper newWrapper) {
        this.typeClass = typeClass;
        this.baseClass = baseClass;
        this.isBaseType = isBaseType;
        this.doc = doc;
        this.name = name;
        this.descrs = descrs;
        this.meths = meths;
        this.newWrapper = newWrapper;
    }

    @Override
    public PyObject getDict(PyType type) {
        PyStringMap dict = new PyStringMap();
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

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<? extends PyObject> getTypeClass() {
        return typeClass;
    }

    @Override
    public Class<?> getBase() {
        return baseClass;
    }

    @Override
    public boolean getIsBaseType() {
        return isBaseType;
    }

    @Override
    public String getDoc() {
        return doc;
    }
}
