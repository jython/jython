package org.python.expose.generate;

import org.objectweb.asm.Type;
import org.python.core.PyBuiltinClassMethodNarrow;

public class ClassMethodExposer extends MethodExposer {

    private final Type[] actualArgs;

    public ClassMethodExposer(Type onType,
                              int access,
                              String methodName,
                              String desc,
                              String typeName,
                              String[] asNames,
                              String[] defaults,
                              String doc) {
        super(onType,
              methodName,
              getArgs(onType, methodName, desc),
              Type.getReturnType(desc),
              typeName,
              asNames,
              defaults,
              PyBuiltinClassMethodNarrow.class,
              doc);
        actualArgs = Type.getArgumentTypes(desc);
    }

    private static Type[] getArgs(Type onType, String methodName, String desc) {
        Type[] args = Type.getArgumentTypes(desc);
        if (args.length == 0 || !args[0].equals(PYTYPE)) {
            throw new InvalidExposingException("The first argument to an ExposedClassMethod must be PyType[method="
                    + onType.getClassName() + "." + methodName + "]");
        }
        Type[] filledInArgs = new Type[args.length - 1];
        System.arraycopy(args, 1, filledInArgs, 0, filledInArgs.length);
        return filledInArgs;
    }

    @Override
    protected void makeCall() {
        callStatic(onType, methodName, returnType, actualArgs);
    }

    @Override
    protected void checkSelf() {
        mv.visitTypeInsn(CHECKCAST, PYTYPE.getInternalName());
    }
}
