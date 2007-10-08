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

    /**
     * @param prefix -
     *            a prefix that will be stripped from method names if no
     *            explicit name is given in the Exposed annotation
     */
    public MethodExposer(Method method, String prefix) {
        this(method);
        this.prefix = prefix;
    }

    public Class getMethodClass() {
        return method.getDeclaringClass();
    }

    public String getName() {
        String name = exp.name();
        if(name.equals("")) {
            name = method.getName();
            if(name.startsWith(prefix)) {
                return name.substring(prefix.length());
            }
            return name;
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
        mv.visitLdcInsn(method.getParameterTypes().length + 1);
        mv.visitLdcInsn(method.getParameterTypes().length + 1);
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
        Type[] args = new Type[method.getParameterTypes().length];
        for(int i = 0; i < args.length; i++) {
            args[i] = PYOBJ;
        }
        startMethod("__call__", PYOBJ, args);
        Type methType = Type.getType(getMethodClass());
        mv.visitVarInsn(ALOAD, 0);
        get("self", PYOBJ);
        mv.visitTypeInsn(CHECKCAST, methType.getInternalName());
        for(int i = 0; i < args.length; i++) {
            mv.visitVarInsn(ALOAD, i + 1);
        }
        mv.visitMethodInsn(INVOKEVIRTUAL,
                           methType.getInternalName(),
                           method.getName(),
                           Type.getMethodDescriptor(method));
        Class ret = method.getReturnType();
        if(ret == Void.TYPE) {
            mv.visitFieldInsn(GETSTATIC, PY.getInternalName(), "None", PYOBJ.getDescriptor());
        } else if(ret == String.class) {
            mv.visitMethodInsn(INVOKESTATIC, PY.getInternalName(), "newString", methodDesc(PYSTR,
                                                                                           STRING));
        } else if(ret == Boolean.TYPE) {
            mv.visitMethodInsn(INVOKESTATIC,
                               PY.getInternalName(),
                               "newBoolean",
                               methodDesc(PYBOOLEAN, BOOLEAN));
        }
        mv.visitInsn(ARETURN);
        endMethod();
    }

    private Exposed exp;

    private Method method;

    private String prefix = "";
}
