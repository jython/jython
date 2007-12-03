package org.python.expose.generate;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Type;
import org.python.core.BytecodeLoader;
import org.python.core.PyBuiltinFunction;
import org.python.core.PyDataDescr;
import org.python.core.PyMethodDescr;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyStringMap;
import org.python.core.PyType;
import org.python.expose.ExposedDelete;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;
import org.python.expose.TypeBuilder;

/**
 * Generates a subclass of TypeBuilder to expose a class with the
 * {@link ExposedType} annotation as a builtin Python type.
 */
public class TypeExposer extends Exposer {

    public TypeExposer(Type onType,
                       String name,
                       Collection<MethodExposer> methods,
                       Collection<DescriptorExposer> descriptors,
                       NewExposer ne) {
        super(BaseTypeBuilder.class, makeGeneratedName(onType));
        this.onType = onType;
        this.name = name;
        this.methods = methods;
        this.descriptors = descriptors;
        for(MethodExposer method : methods) {
            numNames += method.getNames().length;
        }
        this.ne = ne;
    }

    public static String makeGeneratedName(Type onType) {
        return onType.getClassName() + "$PyExposer";
    }

    public TypeBuilder makeBuilder() {
        BytecodeLoader.Loader l = new BytecodeLoader.Loader();
        if(ne != null) {
            ne.load(l);
        }
        for(DescriptorExposer de : descriptors) {
            de.load(l);
        }
        for(MethodExposer me : methods) {
            me.load(l);
        }
        Class descriptor = load(l);
        try {
            return (TypeBuilder)descriptor.newInstance();
        } catch(Exception e) {
            // If we're unable to create the generated class, the process is
            // definitely ill, but that shouldn't be the case most of the time
            // so make this a runtime exception
            throw new RuntimeException("Unable to create generated builder", e);
        }
    }

    public String getName() {
        return name;
    }

    protected void generate() {
        startConstructor();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(getName());
        mv.visitLdcInsn(onType);
        mv.visitLdcInsn(numNames);
        mv.visitTypeInsn(ANEWARRAY, BUILTIN_FUNCTION.getInternalName());
        mv.visitVarInsn(ASTORE, 1);
        int i = 0;
        for(MethodExposer exposer : methods) {
            for(String name : exposer.getNames()) {
                mv.visitVarInsn(ALOAD, 1);
                mv.visitLdcInsn(i++);
                mv.visitTypeInsn(NEW, exposer.getInternalName());
                mv.visitInsn(DUP);
                mv.visitLdcInsn(name);
                callConstructor(exposer.getGeneratedType(), STRING);
                mv.visitInsn(AASTORE);
            }
        }
        mv.visitVarInsn(ALOAD, 1);
        mv.visitLdcInsn(descriptors.size());
        mv.visitTypeInsn(ANEWARRAY, DATA_DESCR.getInternalName());
        mv.visitVarInsn(ASTORE, 2);
        i = 0;
        for(DescriptorExposer desc : descriptors) {
            mv.visitVarInsn(ALOAD, 2);
            mv.visitLdcInsn(i++);
            mv.visitTypeInsn(NEW, desc.getInternalName());
            mv.visitInsn(DUP);
            callConstructor(desc.getGeneratedType());
            mv.visitInsn(AASTORE);
        }
        mv.visitVarInsn(ALOAD, 2);
        if(ne != null) {
            mv.visitTypeInsn(NEW, ne.getInternalName());
            mv.visitInsn(DUP);
            callConstructor(ne.getGeneratedType());
        } else {
            mv.visitInsn(ACONST_NULL);
        }
        superConstructor(STRING, CLASS, ABUILTIN_FUNCTION, ADATA_DESCR, PYNEWWRAPPER);
        endConstructor();
    }

    protected static class BaseTypeBuilder implements TypeBuilder {

        public BaseTypeBuilder(String name,
                               Class typeClass,
                               PyBuiltinFunction[] funcs,
                               PyDataDescr[] descrs,
                               PyNewWrapper newWrapper) {
            this.typeClass = typeClass;
            this.name = name;
            this.descrs = descrs;
            this.funcs = funcs;
            this.newWrapper = newWrapper;
        }

        public PyObject getDict(PyType type) {
            PyObject dict = new PyStringMap();
            for(PyBuiltinFunction func : funcs) {
                PyMethodDescr pmd = new PyMethodDescr(typeClass, func);
                dict.__setitem__(pmd.getName(), pmd);
            }
            for(PyDataDescr descr : descrs) {
                dict.__setitem__(descr.getName(), descr);
            }
            if(newWrapper != null) {
                dict.__setitem__("__new__", newWrapper);
                newWrapper.setWrappedType(type);
            }
            return dict;
        }

        public String getName() {
            return name;
        }

        public Class getTypeClass() {
            return typeClass;
        }

        private PyNewWrapper newWrapper;

        private PyBuiltinFunction[] funcs;

        private PyDataDescr[] descrs;

        private Class typeClass;

        private String name;
    }

    private Type onType;

    private String name;

    private Collection<MethodExposer> methods;

    private Collection<DescriptorExposer> descriptors;

    private int numNames;

    private NewExposer ne;
}
