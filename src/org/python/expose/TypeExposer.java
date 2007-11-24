package org.python.expose;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Type;
import org.python.core.BytecodeLoader;
import org.python.core.PyBuiltinFunction;
import org.python.core.PyMethodDescr;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyStringMap;
import org.python.core.PyType;

/**
 * Generates a subclass of TypeBuilder to expose a class with the
 * {@link ExposedType} annotation as a builtin Python type.
 */
public class TypeExposer extends Exposer {

    public TypeExposer(Class<?> cls) {
        this(Type.getType(cls),
             makeName(cls),
             getMethodExposers(cls, makeName(cls)),
             getNewExposer(cls));
    }
    
    public static List<MethodExposer> getMethodExposers(Class<?> cls, String name){
        List<Method> methods = findMethods(cls);
        List<MethodExposer> exposers = new ArrayList<MethodExposer>(methods.size());
        for(Method method : methods) {
            MethodExposer me = new MethodExposer(method, name);
            exposers.add(me);
        }
        return exposers;
    }
    
    public static NewExposer getNewExposer(Class<?> cls) {
        if(!getExp(cls).constructor().equals("")) {
            return new NewExposer(cls, getExp(cls).constructor());
        }
        return null;
    }

    public static ExposedType getExp(Class<?> m) {
        ExposedType exp = m.getAnnotation(ExposedType.class);
        if(exp == null) {
            throw new IllegalArgumentException(m + " doesn't have the @ExposedType annotation");
        }
        return exp;
    }
    
    public static String makeName(Class<?> cls) {
        if(getExp(cls).name().equals("")) {
            return cls.getSimpleName();
        }
        return getExp(cls).name();
    }
    
    public TypeExposer(Type onType, String name, List<MethodExposer> exposers, NewExposer ne) {
        super(BaseTypeBuilder.class, onType.getClassName() + "$PyExposer");
        this.onType = onType;
        this.name = name;
        this.exposers = exposers;
        for(MethodExposer method : exposers) {
            numNames += method.getNames().length;
        }
        this.ne = ne;
    }

    public TypeBuilder makeBuilder() {
        BytecodeLoader.Loader l = new BytecodeLoader.Loader();
        if(ne != null) {
            ne.load(l);
        }
        for(MethodExposer me : exposers) {
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
    
    /**
     * @return - the name of the class that would be generated to expose cls.
     */
    public static String getExposedName(Class cls) {
        return cls.getName() + "$PyExposer";
    }

    /**
     * @return - the methods on the exposed class that should be exposed.
     */
    public static List<Method> findMethods(Class cls) {
        List<Method> exposedMethods = new ArrayList<Method>();
        for(Method m : cls.getDeclaredMethods()) {
            if(m.getAnnotation(ExposedMethod.class) != null) {
                exposedMethods.add(m);
            }
        }
        return exposedMethods;
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
        if(ne != null) {
            mv.visitTypeInsn(NEW, ne.getInternalName());
            mv.visitInsn(DUP);
            callConstructor(ne.getGeneratedType());
        } else {
            mv.visitInsn(ACONST_NULL);
        }
        superConstructor(STRING, CLASS, ABUILTIN_FUNCTION, PYNEWWRAPPER);
        endConstructor();
    }

    protected static class BaseTypeBuilder implements TypeBuilder {

        public BaseTypeBuilder(String name, Class typeClass, PyBuiltinFunction[] funcs, PyNewWrapper newWrapper) {
            this.typeClass = typeClass;
            this.name = name;
            this.funcs = funcs;
            this.newWrapper = newWrapper;
        }

        public PyObject getDict(PyType type) {
            PyObject dict = new PyStringMap();
            for(PyBuiltinFunction func : funcs) {
                PyMethodDescr pmd = new PyMethodDescr(typeClass, func);
                dict.__setitem__(pmd.getName(), pmd);
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
        private Class typeClass;

        private String name;
    }

    private Type onType;

    private String name;

    private List<MethodExposer> exposers = new ArrayList<MethodExposer>();

    private int numNames;

    private NewExposer ne;
}
