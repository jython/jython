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
        boolean needsThreadState = needsThreadState(args);
        int offset = needsThreadState ? 1 : 0;

        if (args.length == offset || !args[offset].equals(PYTYPE)) {
            String msg = String.format("ExposedClassMethod's first argument %smust be "
                                       + "PyType[method=%s.%s]",
                                       needsThreadState ? "(following ThreadState) " : "",
                                       onType.getClassName(), methodName);
            throw new InvalidExposingException(msg);
        }

        // Remove PyType from the exposed __call__'s args, it'll be already bound as self
        Type[] filledInArgs = new Type[args.length - 1];
        if (needsThreadState) {
            // ThreadState precedes PyType
            filledInArgs[0] = args[0];
            System.arraycopy(args, 2, filledInArgs, 1, filledInArgs.length - 1);
        } else {
            System.arraycopy(args, 1, filledInArgs, 0, filledInArgs.length);
        }
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

    @Override
    protected void loadSelfAndThreadState() {
        // ThreadState precedes self for ClassMethods, load it first if necessary
        loadThreadState();
        // Push self on the stack so we can call it
        get("self", PYOBJ);
        checkSelf();
    }
}
