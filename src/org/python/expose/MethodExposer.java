package org.python.expose;

import java.lang.reflect.Method;

import org.objectweb.asm.Type;
import org.python.core.PyBuiltinMethodNarrow;

/**
 * Generates a class to call a given method with the {@link Exposed} annotation
 * as a method on a builtin Python type.
 */
public class MethodExposer extends Exposer {

    public MethodExposer(Method method) {
        super(PyBuiltinMethodNarrow.class, method.getDeclaringClass().getName() + "$exposed_"
                + method.getName());
        this.method = method;
        exp = method.getAnnotation(Exposed.class);
        if(exp == null) {
            throw new IllegalArgumentException(method + " doesn't have the @Exposed annotation");
        }
    }

    public Class getMethodClass() {
        return method.getDeclaringClass();
    }

    public String getName() {
        String name = exp.name();
        if(name.equals("")) {
            return method.getName();
        }
        return name;
    }

    protected void generate() {
        generateNoArgConstructor();
        generateFullConstructor();
        generateBind();
        generateCall();
    }

    private void generateFullConstructor() {
        startConstructor(PYOBJ, BUILTIN_INFO);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        superConstructor(PYOBJ, BUILTIN_INFO);
        endConstructor();
    }

    private void generateNoArgConstructor() {
        startConstructor();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(getName());
        mv.visitInsn(ICONST_1);
        mv.visitInsn(ICONST_1);
        superConstructor(STRING, INT, INT);
        endConstructor();
    }

    private void generateBind() {
        startMethod("bind", BUILTIN_FUNCTION, PYOBJ);
        mv.visitTypeInsn(NEW, getInternalName());
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 1);
        get("info", BUILTIN_INFO);
        callConstructor(thisType, PYOBJ, BUILTIN_INFO);
        mv.visitInsn(ARETURN);
        endMethod();
    }

    private void generateCall() {
        startMethod("__call__", PYOBJ);
        Type methType = Type.getType(getMethodClass());
        mv.visitVarInsn(ALOAD, 0);
        get("self", PYOBJ);
        mv.visitTypeInsn(CHECKCAST, methType.getInternalName());
        mv.visitMethodInsn(INVOKEVIRTUAL,
                           methType.getInternalName(),
                           method.getName(),
                           methodDesc(VOID));
        mv.visitFieldInsn(GETSTATIC, PY.getInternalName(), "None", PYOBJ.getDescriptor());
        mv.visitInsn(ARETURN);
        endMethod();
    }

    private Exposed exp;

    private Method method;
}
