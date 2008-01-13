package org.python.expose.generate;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.python.core.PyBuiltinMethod;
import org.python.core.PyBuiltinMethodNarrow;
import org.python.expose.ExposedMethod;
import org.python.expose.MethodType;

/**
 * Generates a class to call a given method with the {@link ExposedMethod} annotation as a method on
 * a builtin Python type.
 */
public class MethodExposer extends Exposer {

    private String methodName;

    protected String[] asNames, defaults;

    private String prefix;

    private Type[] args;

    private Type onType, returnType;

    protected MethodType type;

    public MethodExposer(Type onType, int access, String methodName, String desc, String prefix) {
        this(onType,
             access,
             methodName,
             desc,
             prefix,
             new String[0],
             new String[0],
             MethodType.DEFAULT);
    }

    public MethodExposer(Type onType,
                         int access,
                         String methodName,
                         String desc,
                         String prefix,
                         String[] asNames,
                         String[] defaults,
                         MethodType type) {
        super(isWide(desc) ? PyBuiltinMethod.class : PyBuiltinMethodNarrow.class,
              onType.getClassName() + "$" + methodName + "_exposer");
        this.onType = onType;
        this.methodName = methodName;
        if((access & ACC_STATIC) != 0) {
            throwInvalid("@ExposedMethod can't be applied to static methods");
        }
        this.args = Type.getArgumentTypes(desc);
        if(isWide(args)) {
            if(defaults.length > 0) {
                throwInvalid("Can't have defaults on a method that takes PyObject[], String[]");
            }
        }
        this.returnType = Type.getReturnType(desc);
        this.prefix = prefix;
        this.asNames = asNames;
        for(String name : getNames()) {
            if(name.equals("__new__")) {
                throwInvalid("@ExposedNew must be used to create __new__, not @ExposedMethod");
            }
        }
        this.defaults = defaults;
        this.type = type;
    }

    private void throwInvalid(String msg) {
        throw new InvalidExposingException(msg + "[method=" + onType.getClassName() + "."
                + methodName + "]");
    }

    /**
     * @return the names this method will be exposed as. Must be at least length 1.
     */
    public String[] getNames() {
        if(asNames.length == 0) {
            String name = methodName;
            if(name.startsWith(prefix + "_")) {
                name = methodName.substring((prefix + "_").length());
            }
            return new String[] {name};
        }
        return asNames;
    }

    /**
     * @return the default values for the later params, if any.
     */
    String[] getDefaults() {
        return defaults;
    }

    protected void generate() {
        generateNamedConstructor();
        generateFullConstructor();
        generateBind();
        if(isWide(args)) {
            generateWideCall();
        } else {
            for(int i = 0; i < defaults.length + 1; i++) {
                generateCall(i);
            }
        }
    }

    private void generateFullConstructor() {
        startConstructor(PYTYPE, PYOBJ, BUILTIN_INFO);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ALOAD, 3);
        superConstructor(PYTYPE, PYOBJ, BUILTIN_INFO);
        endConstructor();
    }

    private void generateNamedConstructor() {
        startConstructor(STRING);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        if(isWide(args)) {
            superConstructor(STRING);
        } else {
            mv.visitLdcInsn(args.length + 1 - defaults.length);
            mv.visitLdcInsn(args.length + 1);
            superConstructor(STRING, INT, INT);
        }
        endConstructor();
    }

    private void generateBind() {
        startMethod("bind", BUILTIN_FUNCTION, PYOBJ);
        instantiate(thisType, new Instantiator(PYTYPE, PYOBJ, BUILTIN_INFO) {

            public void pushArgs() {
                mv.visitVarInsn(ALOAD, 0);
                call(thisType, "getType", PYTYPE);
                mv.visitVarInsn(ALOAD, 1);
                get("info", BUILTIN_INFO);
            }
        });
        endMethod(ARETURN);
    }

    private void generateWideCall() {
        startMethod("__call__", PYOBJ, APYOBJ, ASTRING);
        get("self", PYOBJ);
        mv.visitTypeInsn(CHECKCAST, onType.getInternalName());
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        call(onType, methodName, returnType, args);
        toPy(returnType);
        endMethod(ARETURN);
    }

    private boolean hasDefault(int argIndex) {
        return defaults.length - args.length + argIndex >= 0;
    }

    private String getDefault(int argIndex) {
        return defaults[defaults.length - args.length + argIndex];
    }

    private void generateCall(int numDefaults) {
        int usedLocals = 1;// We always have one used local for 'this'
        Type[] callArgs = new Type[args.length - numDefaults];
        for(int i = 0; i < callArgs.length; i++) {
            callArgs[i] = PYOBJ;
        }
        startMethod("__call__", PYOBJ, callArgs);
        // Push self on the stack so we can call it
        get("self", PYOBJ);
        mv.visitTypeInsn(CHECKCAST, onType.getInternalName());
        // Push the passed in callArgs onto the stack, and convert them if necessary
        for(int i = 0; i < callArgs.length; i++) {
            mv.visitVarInsn(ALOAD, usedLocals++);
            if(PRIMITIVES.containsKey(args[i])) {
                callStatic(PY, "py2" + args[i].getClassName(), args[i], PYOBJ);
            } else if(args[i].equals(STRING)) {
                if(hasDefault(i) && getDefault(i).equals("null")) {
                    call(PYOBJ, "asStringOrNull", STRING);
                } else {
                    call(PYOBJ, "asString", STRING);
                }
            }
        }
        // Push the defaults onto the stack
        for(int i = callArgs.length; i < args.length; i++) {
            pushDefault(getDefault(i), args[i]);
        }
        // Actually call the exposed method
        call(onType, methodName, returnType, args);
        if(type == MethodType.BINARY) {
            checkBinaryResult();
        } else if(type == MethodType.CMP) {
            checkCmpResult();
        }
        toPy(returnType);
        endMethod(ARETURN);
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
                mv.visitLdcInsn(prefix + ".__cmp__(x,y) requires y to be '" + prefix + "', not a '");
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

    private void pushDefault(String def, Type arg) {
        if(def.equals("Py.None")) {
            getStatic(PY, "None", PYOBJ);
        } else if(def.equals("null")) {
            mv.visitInsn(ACONST_NULL);
        } else if(arg.equals(Type.LONG_TYPE)) {
            // For primitive types, parse using the Java wrapper for that type and push onto the
            // stack as a constant. If the default is malformed, a NumberFormatException will be
            // raised.
            mv.visitLdcInsn(new Long(def));
        } else if(arg.equals(INT)) {
            mv.visitLdcInsn(new Integer(def));
        } else if(arg.equals(BYTE)) {
            // byte, char, boolean and short go as int constants onto the stack, so convert them
            // to ints to get the right type
            mv.visitLdcInsn(new Byte(def).intValue());
        } else if(arg.equals(SHORT)) {
            mv.visitLdcInsn(new Short(def).intValue());
        } else if(arg.equals(CHAR)) {
            if(def.length() != 1) {
                throwInvalid("A default for a char argument must be one character in length");
            }
            mv.visitLdcInsn((int)new Character(def.charAt(0)).charValue());
        } else if(arg.equals(BOOLEAN)) {
            mv.visitLdcInsn(Boolean.valueOf(def) ? 1 : 0);
        } else if(arg.equals(Type.FLOAT_TYPE)) {
            mv.visitLdcInsn(new Float(def));
        } else if(arg.equals(Type.DOUBLE_TYPE)) {
            mv.visitLdcInsn(new Double(def));
        }
    }

    private static boolean isWide(String methDescriptor) {
        return isWide(Type.getArgumentTypes(methDescriptor));
    }

    private static boolean isWide(Type[] args) {
        return args.length == 2 && args[0].equals(APYOBJ) && args[1].equals(ASTRING);
    }
}
