package org.python.expose.generate;

import org.python.objectweb.asm.Label;
import org.python.objectweb.asm.Type;
import org.python.core.PyBuiltinMethod;
import org.python.core.PyBuiltinMethodNarrow;
import org.python.expose.ExposedMethod;
import org.python.expose.MethodType;

/**
 * Generates a class to call a given method with the {@link ExposedMethod} annotation as a method on
 * a builtin Python type.
 */
public class InstanceMethodExposer extends MethodExposer {

    MethodType type;

    public InstanceMethodExposer(Type onType,
                                 int access,
                                 String methodName,
                                 String desc,
                                 String typeName) {
        this(onType,
             access,
             methodName,
             desc,
             typeName,
             new String[0],
             new String[0],
             MethodType.DEFAULT);
    }

    public InstanceMethodExposer(Type onType,
                                 int access,
                                 String methodName,
                                 String desc,
                                 String typeName,
                                 String[] asNames,
                                 String[] defaults,
                                 MethodType type) {
        super(onType,
              methodName,
              Type.getArgumentTypes(desc),
              Type.getReturnType(desc),
              typeName,
              asNames,
              defaults,
              isWide(desc) ? PyBuiltinMethod.class : PyBuiltinMethodNarrow.class);
        if ((access & ACC_STATIC) != 0) {
            throwInvalid("@ExposedMethod can't be applied to static methods");
        }
        if (isWide(args)) {
            if (defaults.length > 0) {
                throwInvalid("Can't have defaults on a method that takes PyObject[], String[]");
            }
        }
        this.type = type;
    }

    protected void checkSelf() {
        mv.visitTypeInsn(CHECKCAST, onType.getInternalName());
    }

    protected void makeCall() {
        // Actually call the exposed method
        call(onType, methodName, returnType, args);
        if (type == MethodType.BINARY) {
            checkBinaryResult();
        } else if (type == MethodType.CMP) {
            checkCmpResult();
        }
    }

    /** Throw NotImplemented if a binary method returned null. */
    private void checkBinaryResult() {
        // If this is a binary method,
        mv.visitInsn(DUP);
        Label regularReturn = new Label();
        mv.visitJumpInsn(IFNONNULL, regularReturn);
        getStatic(PY, "NotImplemented", PYOBJ);
        mv.visitInsn(ARETURN);
        mv.visitLabel(regularReturn);
    }

    /** Throw a type error if a cmp method returned -2. */
    private void checkCmpResult() {
        mv.visitInsn(DUP);
        mv.visitIntInsn(BIPUSH, -2);
        Label regularReturn = new Label();
        mv.visitJumpInsn(IF_ICMPNE, regularReturn);
        // tediously build an error message based on the type name
        instantiate(STRING_BUILDER, new Instantiator(STRING) {

            public void pushArgs() {
                mv.visitLdcInsn(typeName + ".__cmp__(x,y) requires y to be '" + typeName
                        + "', not a '");
            }
        });
        mv.visitVarInsn(ALOAD, 1);
        call(PYOBJ, "getType", PYTYPE);
        call(PYTYPE, "fastGetName", STRING);
        call(STRING_BUILDER, "append", STRING_BUILDER, STRING);
        mv.visitLdcInsn("'");
        call(STRING_BUILDER, "append", STRING_BUILDER, STRING);
        call(STRING_BUILDER, "toString", STRING);
        // throw a type error with our excellent message since this was of the wrong type.
        callStatic(PY, "TypeError", PYEXCEPTION, STRING);
        mv.visitInsn(ATHROW);
        mv.visitLabel(regularReturn);
    }

    private static boolean isWide(String methDescriptor) {
        return isWide(Type.getArgumentTypes(methDescriptor));
    }
}
