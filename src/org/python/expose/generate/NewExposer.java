package org.python.expose.generate;

import java.lang.reflect.Method;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;

public class NewExposer extends Exposer {

    public NewExposer(Class<?> cls, String name) {
        this(getNewImpl(cls, name));
    }

    public NewExposer(Method method) {
        this(Type.getType(method.getDeclaringClass()),
             method.getModifiers(),
             method.getName(),
             Type.getMethodDescriptor(method),
             asInternals(method.getExceptionTypes()));
    }

    private static String[] asInternals(Class<?>[] exceptionTypes) {
        String[] internals = new String[exceptionTypes.length];
        for(int i = 0; i < internals.length; i++) {
            internals[i] = Type.getInternalName(exceptionTypes[i]);
        }
        return internals;
    }

    public NewExposer(Type onType, int access, String methodName, String desc, String[] exceptions) {
        super(PyNewWrapper.class, onType.getClassName() + "$exposed___new__");
        String fullName = onType.getClassName() + "." + methodName;
        if((access & Opcodes.ACC_STATIC) == 0) {
            throw new IllegalArgumentException(fullName
                    + " isn't static, but it must be to be exposed as __new__");
        }
        if(!Type.getReturnType(desc).equals(PYOBJ)) {
            throw new IllegalArgumentException(fullName
                    + " must return PyObject to be exposed as __new__");
        }
        if(exceptions != null && exceptions.length > 0) {
            throw new IllegalArgumentException(fullName
                    + " may not throw any exceptions if it is to be exposed as __new__");
        }
        this.onType = onType;
        this.name = methodName;
    }

    private static Method getNewImpl(Class<?> cls, String name) {
        try {
            return cls.getDeclaredMethod(name,
                                         PyNewWrapper.class,
                                         Boolean.TYPE,
                                         PyType.class,
                                         PyObject[].class,
                                         String[].class);
        } catch(SecurityException e) {
            throw new RuntimeException(e);
        } catch(NoSuchMethodException e) {
            throw new IllegalArgumentException("There is no method named " + name + " on " + cls
                    + " with the arguments for new_impl");
        }
    }

    @Override
    protected void generate() {
        generateConstructor();
        generateNewImpl();
    }

    private void generateConstructor() {
        startConstructor();
        mv.visitVarInsn(ALOAD, 0);
        superConstructor();
        endConstructor();
    }

    private void generateNewImpl() {
        startMethod("new_impl", PYOBJ, BOOLEAN, PYTYPE, APYOBJ, ASTRING);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitMethodInsn(INVOKESTATIC, onType.getInternalName(), name, NEW_DESCRIPTOR);
        mv.visitInsn(ARETURN);
        endMethod();
    }

    public static final String NEW_DESCRIPTOR = Type.getMethodDescriptor(PYOBJ,
                                                                         new Type[] {PYNEWWRAPPER,
                                                                                     BOOLEAN,
                                                                                     PYTYPE,
                                                                                     APYOBJ,
                                                                                     ASTRING});

    private Type onType;

    private String name;
}
