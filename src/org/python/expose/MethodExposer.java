package org.python.expose;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.python.core.PyBuiltinMethodNarrow;

import com.sun.mirror.util.Types;

/**
 * Generates a class to call a given method with the {@link ExposedMethod}
 * annotation as a method on a builtin Python type.
 */
public class MethodExposer extends Exposer {

    public MethodExposer(Method method) {
        this(method, "");
    }

    /**
     * @param type -
     *            the type the method is on. If no explicit name is given in the
     *            annotation, type + '_' will be stripped from method names to
     *            make the exposed name.
     */
    public MethodExposer(Method method, String type) {
        this(Type.getType(method.getDeclaringClass()),
             method.getName(),
             Type.getMethodDescriptor(method),
             type,
             getExp(method).names(),
             getExp(method).defaults(),
             getExp(method).type());
    }

    public static ExposedMethod getExp(Method m) {
        ExposedMethod exp = m.getAnnotation(ExposedMethod.class);
        if(exp == null) {
            throw new IllegalArgumentException(m + " doesn't have the @ExposedMethod annotation");
        }
        return exp;
    }

    public MethodExposer(Type onType,
                         String methodName,
                         String desc,
                         String prefix,
                         String[] asNames,
                         String[] defaults,
                         MethodType type) {
        super(PyBuiltinMethodNarrow.class, onType.getClassName() + "_" + methodName + "_exposer");
        this.onType = onType;
        this.methodName = methodName;
        this.params = Type.getArgumentTypes(desc);
        this.returnType = Type.getReturnType(desc);
        this.prefix = prefix;
        this.asNames = asNames;
        this.defaults = defaults;
        this.type = type;
    }

    public String[] getNames() {
        if(asNames.length == 0) {
            if(methodName.startsWith(prefix + "_")) {
                methodName = methodName.substring((prefix + "_").length());
            }
            return new String[] {methodName};
        }
        return asNames;
    }

    protected void generate() {
        generateNamedConstructor();
        generateFullConstructor();
        generateBind();
        for(int i = 0; i < defaults.length + 1; i++) {
            generateCall(i);
        }
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
        mv.visitLdcInsn(params.length + 1 - defaults.length);
        mv.visitLdcInsn(params.length + 1);
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

    private void generateCall(int numDefaults) {
        int usedLocals = 1;// We always have one used local for this
        Type[] args = new Type[params.length - numDefaults];
        for(int i = 0; i < args.length; i++) {
            args[i] = PYOBJ;
        }
        startMethod("__call__", PYOBJ, args);
        get("self", PYOBJ);
        mv.visitTypeInsn(CHECKCAST, onType.getInternalName());
        for(int i = 0; i < args.length; i++) {
            mv.visitVarInsn(ALOAD, usedLocals++);
        }
        for(int i = 0; i < numDefaults; i++) {
            String def = defaults[i];
            if(def.equals("Py.None")) {
                pushNone();
            } else if(def.equals("null")) {
                mv.visitInsn(ACONST_NULL);
            }
        }
        mv.visitMethodInsn(INVOKEVIRTUAL,
                           onType.getInternalName(),
                           methodName,
                           methodDesc(returnType, params));
        if(type == MethodType.BINARY) {
            mv.visitInsn(DUP);
            Label regularReturn = new Label();
            mv.visitJumpInsn(IFNONNULL, regularReturn);
            mv.visitFieldInsn(GETSTATIC,
                              PY.getInternalName(),
                              "NotImplemented",
                              PYOBJ.getDescriptor());
            mv.visitInsn(ARETURN);
            mv.visitLabel(regularReturn);
        } else if(type == MethodType.CMP) {
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
        if(returnType.equals(Type.VOID_TYPE)) {
            pushNone();
        } else if(returnType.equals(STRING)) {
            mv.visitMethodInsn(INVOKESTATIC, PY.getInternalName(), "newString", methodDesc(PYSTR,
                                                                                           STRING));
        } else if(returnType.equals(Type.BOOLEAN_TYPE)) {
            mv.visitMethodInsn(INVOKESTATIC,
                               PY.getInternalName(),
                               "newBoolean",
                               methodDesc(PYBOOLEAN, BOOLEAN));
        } else if(returnType.equals(Type.INT_TYPE)) {
            mv.visitMethodInsn(INVOKESTATIC,
                               PY.getInternalName(),
                               "newInteger",
                               methodDesc(PYINTEGER, INT));
        }
        mv.visitInsn(ARETURN);
        endMethod();
    }

    private void pushNone() {
        mv.visitFieldInsn(GETSTATIC, PY.getInternalName(), "None", PYOBJ.getDescriptor());
    }

    private String methodName;

    private String[] asNames, defaults;

    private String prefix;

    private Type[] params;

    private Type onType, returnType;

    private MethodType type;
}
