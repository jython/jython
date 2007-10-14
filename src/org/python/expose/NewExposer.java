package org.python.expose;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.objectweb.asm.Type;
import org.python.core.PyBuiltinMethodNarrow;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;

public class NewExposer extends Exposer {

    public NewExposer(Method method) {
        super(PyNewWrapper.class, method.getDeclaringClass().getName() + "$exposed___new__");
        if((method.getModifiers() & Modifier.STATIC) == 0) {
            throw new IllegalArgumentException(method
                    + " isn't static, but it must be to be exposed as __new__");
        }
        if(!method.getReturnType().equals(PyObject.class)) {
            throw new IllegalArgumentException(method
                    + " must return PyObject to be exposed as __new__");
        }
        this.method = method;
    }

    public NewExposer(Class<?> cls, String name) {
        this(getNewImpl(cls, name));
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
        mv.visitMethodInsn(INVOKESTATIC,
                           Type.getType(getMethodClass()).getInternalName(),
                           method.getName(),
                           methodDesc(PYOBJ, PYNEWWRAPPER, BOOLEAN, PYTYPE, APYOBJ, ASTRING));
        mv.visitInsn(ARETURN);
        endMethod();
    }

    public Class getMethodClass() {
        return method.getDeclaringClass();
    }

    private Method method;
}
