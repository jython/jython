package org.python.expose.generate;

import org.objectweb.asm.Type;
import org.python.expose.MethodType;

public abstract class MethodExposer extends Exposer {

    protected String[] defaults;
    
    protected final String[] asNames;

    protected final String prefix, typeName;

    protected final Type[] args;

    protected final String methodName;

    protected final Type onType, returnType;

    public MethodExposer(Type onType,
                         String methodName,
                         Type[] args,
                         Type returnType,
                         String typeName,
                         String[] asNames,
                         String[] defaults,
                         Class superClass) {
        super(superClass, onType.getClassName() + "$" + methodName + "_exposer");
        this.onType = onType;
        this.methodName = methodName;
        this.args = args;
        this.typeName = typeName;
        String prefix = typeName;
        int lastDot = prefix.lastIndexOf('.');
        if (lastDot != -1) {
            prefix = prefix.substring(lastDot + 1);
        }
        this.prefix = prefix;
        this.asNames = asNames;
        this.returnType = returnType;
        this.defaults = defaults;
        for(String name : getNames()) {
            if(name.equals("__new__")) {
                throwInvalid("@ExposedNew must be used to create __new__, not @ExposedMethod");
            }
        }
    }

    protected void throwInvalid(String msg) {
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
        checkSelf();
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        makeCall();
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
        int usedLocals = 1;// We always have one used local for self
        Type[] callArgs = new Type[args.length - numDefaults];
        for(int i = 0; i < callArgs.length; i++) {
            callArgs[i] = PYOBJ;
        }
        startMethod("__call__", PYOBJ, callArgs);
        // Push self on the stack so we can call it
        get("self", PYOBJ);
        checkSelf();
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
        makeCall();
        toPy(returnType);
        endMethod(ARETURN);
    }

    protected abstract void checkSelf();

    protected abstract void makeCall();

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

    protected static boolean isWide(Type[] args) {
        return args.length == 2 && args[0].equals(APYOBJ) && args[1].equals(ASTRING);
    }

}
