package org.python.expose;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Type;
import org.python.core.BytecodeLoader;
import org.python.core.PyBuiltinFunction;
import org.python.core.PyMethodDescr;
import org.python.core.PyObject;
import org.python.core.PyStringMap;

/**
 * Generates a subclass of TypeBuilder to expose a class with the
 * {@link ExposedType} annotation as a builtin Python type.
 */
public class TypeExposer extends Exposer {

    public TypeExposer(Class<?> cls) {
        super(BaseTypeBuilder.class, getExposedName(cls));
        this.cls = cls;
        exp = cls.getAnnotation(ExposedType.class);
        if(exp == null) {
            throw new IllegalArgumentException(cls + " doesn't have the @Exposed annotation");
        }
    }

    public TypeBuilder makeBuilder() {
        BytecodeLoader.Loader l = new BytecodeLoader.Loader();
        for(Method m : findMethods()) {
            new MethodExposer(m, getName() + "_").load(l);
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
    
    /**
     * @return - the name of the class that would be generated to expose cls.
     */
    public static String getExposedName(Class cls) {
        return cls.getName() + "$PyExposer";
    }

    /**
     * @return - the methods on the exposed class that should be exposed.
     */
    public List<Method> findMethods() {
        List<Method> exposedMethods = new ArrayList<Method>();
        for(Method m : cls.getMethods()) {
            if(m.getAnnotation(ExposedMethod.class) != null) {
                exposedMethods.add(m);
            }
        }
        return exposedMethods;
    }

    public String getName() {
        String name = exp.name();
        if(name.equals("")) {
            return cls.getSimpleName();
        }
        return name;
    }

    protected void generate() {
        startConstructor();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(getName());
        mv.visitLdcInsn(Type.getType(cls));
        List<Method> methods = findMethods();
        List<MethodExposer> exposers = new ArrayList<MethodExposer>(methods.size());
        int numNames = 0;
        for(Method method : methods) {
            MethodExposer me = new MethodExposer(method, getName());
            exposers.add(me);
            numNames += me.getNames().length;
        }
        mv.visitLdcInsn(numNames);
        mv.visitTypeInsn(ANEWARRAY, BUILTIN_FUNCTION.getInternalName());
        mv.visitVarInsn(ASTORE, 1);
        int i = 0;
        for(MethodExposer exposer : exposers) {
            for(int j = 0; j < exposer.getNames().length; j++) {
                mv.visitVarInsn(ALOAD, 1);
                mv.visitLdcInsn(i++);
                mv.visitTypeInsn(NEW, exposer.getInternalName());
                mv.visitInsn(DUP);
                mv.visitLdcInsn(exposer.getNames()[j]);
                callConstructor(exposer.getGeneratedType(), STRING);
                mv.visitInsn(AASTORE);
            }
        }
        mv.visitVarInsn(ALOAD, 1);
        superConstructor(STRING, CLASS, ABUILTIN_FUNCTION);
        endConstructor();
    }

    protected static class BaseTypeBuilder implements TypeBuilder {

        public BaseTypeBuilder(String name, Class typeClass, PyBuiltinFunction[] funcs) {
            this.typeClass = typeClass;
            this.name = name;
            this.funcs = funcs;
        }

        public PyObject getDict() {
            PyObject dict = new PyStringMap();
            for(PyBuiltinFunction func : funcs) {
                PyMethodDescr pmd = new PyMethodDescr(typeClass, func);
                dict.__setitem__(pmd.getName(), pmd);
            }
            return dict;
        }

        public String getName() {
            return name;
        }

        public Class getTypeClass() {
            return typeClass;
        }
        

        private PyBuiltinFunction[] funcs;
        private Class typeClass;

        private String name;
    }

    private ExposedType exp;

    private Class cls;
}
