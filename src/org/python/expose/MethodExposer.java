package org.python.expose;

import java.lang.reflect.Method;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.python.core.PyBuiltinMethodNarrow;

/**
 * Generates a class to call a given method with the {@link ExposedMethod}
 * annotation as a method on a builtin Python type.
 */
public class MethodExposer extends Exposer {

    public MethodExposer(Method method) {
        super(PyBuiltinMethodNarrow.class, method.getDeclaringClass().getName() + "$exposed_"
                + method.getName());
        this.method = method;
        exp = method.getAnnotation(ExposedMethod.class);
        if(exp == null) {
            throw new IllegalArgumentException(method
                    + " doesn't have the @ExposedMethod annotation");
        }
    }

    /**
     * @param type -
     *            the type the method is on. If no explicit name is given in the
     *            annotation, type + '_' will be stripped from method names to
     *            make the exposed name.
     */
    public MethodExposer(Method method, String type) {
        this(method);
        this.prefix = type;
    }

    public Class getMethodClass() {
        return method.getDeclaringClass();
    }

    public String[] getNames() {
        String[] names = exp.names();
        if(names.length == 0) {
            String name = method.getName();
            if(name.startsWith(prefix + "_")) {
                name = name.substring((prefix + "_").length());
            }
            return new String[] {name};
        }
        return names;
    }

    protected void generate() {
        generateNamedConstructor();
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

    private void generateNamedConstructor() {
        startConstructor(STRING);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
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
        int usedLocals = 1;// We always have one used local for this
        Type[] args = new Type[method.getParameterTypes().length];
        for(int i = 0; i < args.length; i++) {
            args[i] = PYOBJ;
        }
        startMethod("__call__", PYOBJ, args);
        Type methType = Type.getType(getMethodClass());
        get("self", PYOBJ);
        mv.visitTypeInsn(CHECKCAST, methType.getInternalName());
        for(int i = 0; i < args.length; i++) {
            mv.visitVarInsn(ALOAD, usedLocals++);
        }
        mv.visitMethodInsn(INVOKEVIRTUAL,
                           methType.getInternalName(),
                           method.getName(),
                           Type.getMethodDescriptor(method));
        if(exp.type() == MethodType.BINARY) {
            mv.visitInsn(DUP);
            Label regularReturn = new Label();
            mv.visitJumpInsn(IFNONNULL, regularReturn);
            mv.visitFieldInsn(GETSTATIC,
                              PY.getInternalName(),
                              "NotImplemented",
                              PYOBJ.getDescriptor());
            mv.visitInsn(ARETURN);
            mv.visitLabel(regularReturn);
        } else if(exp.type() == MethodType.CMP) {
            mv.visitInsn(DUP);
            mv.visitIntInsn(BIPUSH, -2);
            Label regularReturn = new Label();
            mv.visitJumpInsn(IF_ICMPNE, regularReturn);
            mv.visitTypeInsn(NEW, STRING_BUILDER.getInternalName());
            mv.visitInsn(DUP);
            mv.visitLdcInsn(prefix + ".__cmp__(x,y) requires y to be '" + prefix + "', not a '");
            callConstructor(STRING_BUILDER, STRING);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                               PYOBJ.getInternalName(),
                               "getType",
                               methodDesc(PYTYPE));
            mv.visitMethodInsn(INVOKEVIRTUAL,
                               PYTYPE.getInternalName(),
                               "fastGetName",
                               methodDesc(STRING));
            mv.visitMethodInsn(INVOKEVIRTUAL,
                               STRING_BUILDER.getInternalName(),
                               "append",
                               methodDesc(STRING_BUILDER, STRING));
            mv.visitLdcInsn("'");
            mv.visitMethodInsn(INVOKEVIRTUAL,
                               STRING_BUILDER.getInternalName(),
                               "append",
                               methodDesc(STRING_BUILDER, STRING));
            mv.visitMethodInsn(INVOKEVIRTUAL,
                               STRING_BUILDER.getInternalName(),
                               "toString",
                               methodDesc(STRING));
            mv.visitMethodInsn(INVOKESTATIC,
                               PY.getInternalName(),
                               "TypeError",
                               methodDesc(PYEXCEPTION, STRING));
            mv.visitInsn(ATHROW);
            mv.visitLabel(regularReturn);
        }
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
        } else if(ret == Integer.TYPE) {
            mv.visitMethodInsn(INVOKESTATIC,
                               PY.getInternalName(),
                               "newInteger",
                               methodDesc(PYINTEGER, INT));
        }
        mv.visitInsn(ARETURN);
        endMethod();
    }

    private ExposedMethod exp;

    private Method method;

    private String prefix = "";
}
