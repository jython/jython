package org.python.expose.generate;

import java.lang.reflect.Method;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;

public class NewExposer extends Exposer {

    private Type onType;

    private String name;

    public NewExposer(Type onType, int access, String methodName, String desc, String[] exceptions) {
        super(PyNewWrapper.class, onType.getClassName() + "$exposed___new__");
        this.onType = onType;
        this.name = methodName;
        if((access & Opcodes.ACC_STATIC) == 0) {
            throwInvalid("Full methods for @ExposedNew must be static");
        }
        if(!Type.getReturnType(desc).equals(PYOBJ)) {
            throwInvalid("@ExposedNew methods must return PyObject");
        }
        if(exceptions != null && exceptions.length > 0) {
            throwInvalid("@ExposedNew methods may not throw checked exceptions");
        }
    }

    private void throwInvalid(String msg) {
        throw new InvalidExposingException(msg + "[method=" + onType.getClassName() + "." + name
                + "]");
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
        endMethod(ARETURN);
    }

    public static final String NEW_DESCRIPTOR = Type.getMethodDescriptor(PYOBJ,
                                                                         new Type[] {PYNEWWRAPPER,
                                                                                     BOOLEAN,
                                                                                     PYTYPE,
                                                                                     APYOBJ,
                                                                                     ASTRING});
}
