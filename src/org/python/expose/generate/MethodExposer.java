package org.python.expose.generate;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.python.core.PyBuiltinMethod;
import org.python.core.PyBuiltinMethodNarrow;
import org.python.expose.ExposedMethod;
import org.python.expose.MethodType;

/**
 * Generates a class to call a given method with the {@link ExposedMethod}
 * annotation as a method on a builtin Python type.
 */
public class MethodExposer extends Exposer {

    public MethodExposer(Type onType, int access, String methodName, String desc, String prefix) {
        this(onType,
             access,
             methodName,
             desc,
             prefix,
             new String[0],
             new String[0],
             MethodType.NORMAL);
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
        this.params = Type.getArgumentTypes(desc);
        this.returnType = Type.getReturnType(desc);
        this.prefix = prefix;
        this.asNames = asNames;
        this.defaults = defaults;
        this.type = type;
    }

    /**
     * @return the names this method will be exposed as. Must be at least length
     *         1.
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
        if(isWide(params)) {
            if(defaults.length > 0) {
                throw new IllegalStateException("Can't have defaults on a wide method");
            }
            generateWideCall();
        } else {
            for(int i = 0; i < defaults.length + 1; i++) {
                generateCall(i);
            }
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
        if(isWide(params)) {
            superConstructor(STRING);
        } else {
            mv.visitLdcInsn(params.length + 1 - defaults.length);
            mv.visitLdcInsn(params.length + 1);
            superConstructor(STRING, INT, INT);
        }
        endConstructor();
    }

    private void generateBind() {
        startMethod("bind", BUILTIN_FUNCTION, PYOBJ);
        instantiate(thisType, new Instantiator(PYOBJ, BUILTIN_INFO){
            public void pushArgs() {
                mv.visitVarInsn(ALOAD, 1);
                get("info", BUILTIN_INFO);
            }
        });
        mv.visitInsn(ARETURN);
        endMethod();
    }

    private void generateWideCall() {
        startMethod("__call__", PYOBJ, APYOBJ, ASTRING);
        get("self", PYOBJ);
        mv.visitTypeInsn(CHECKCAST, onType.getInternalName());
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL,
                           onType.getInternalName(),
                           methodName,
                           methodDesc(returnType, params));
        generateCallReturn();
    }

    private void generateCallReturn() {
        if(returnType.equals(VOID)) {
            pushNone();
        } else if(returnType.equals(STRING)) {
            mv.visitMethodInsn(INVOKESTATIC, PY.getInternalName(), "newString", methodDesc(PYSTR,
                                                                                           STRING));
        } else if(returnType.equals(BOOLEAN)) {
            mv.visitMethodInsn(INVOKESTATIC,
                               PY.getInternalName(),
                               "newBoolean",
                               methodDesc(PYBOOLEAN, BOOLEAN));
        } else if(returnType.equals(INT)) {
            mv.visitMethodInsn(INVOKESTATIC,
                               PY.getInternalName(),
                               "newInteger",
                               methodDesc(PYINTEGER, INT));
        }
        mv.visitInsn(ARETURN);
        endMethod();
    }
    
    private boolean hasDefault(int argIndex) {
        return defaults.length - params.length + argIndex >= 0;
    }
    
    private String getDefault(int argIndex) {
        return defaults[defaults.length - params.length + argIndex];
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
            if(params[i].equals(INT)) {
                mv.visitMethodInsn(INVOKEVIRTUAL, PYOBJ.getInternalName(), "asInt", "()I");
            } else if(params[i].equals(STRING)) {
                if(hasDefault(i) && getDefault(i).equals("null")) {
                    mv.visitMethodInsn(INVOKEVIRTUAL,
                                       PYOBJ.getInternalName(),
                                       "asStringOrNull",
                                       methodDesc(STRING));
                } else {
                    mv.visitMethodInsn(INVOKEVIRTUAL,
                                       PYOBJ.getInternalName(),
                                       "asString",
                                       methodDesc(STRING));
                }
            } else if(params[i].equals(BOOLEAN)) {
                mv.visitMethodInsn(INVOKEVIRTUAL,
                                   PYOBJ.getInternalName(),
                                   "__nonzero__",
                                   methodDesc(BOOLEAN));
            }
        }
        for(int i = args.length; i < params.length; i++) {
            String def = getDefault(i);
            if(def.equals("Py.None")) {
                pushNone();
            } else if(def.equals("null")) {
                mv.visitInsn(ACONST_NULL);
            } else if(params[i].equals(INT)) {
                // An int is required here, so parse the default as an Integer
                // and push it as a constant.
                // If the default isn't a valid integer, a NumberFormatException
                // will be raised.
                mv.visitLdcInsn(new Integer(def));
            } else if(params[i].equals(BOOLEAN)){
                mv.visitLdcInsn(Boolean.valueOf(def) ? 1 : 0);
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
            instantiate(STRING_BUILDER, new Instantiator(STRING){
                public void pushArgs(){
                    mv.visitLdcInsn(prefix + ".__cmp__(x,y) requires y to be '" + prefix + "', not a '");
                }
            });
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
        generateCallReturn();
    }

    private void pushNone() {
        mv.visitFieldInsn(GETSTATIC, PY.getInternalName(), "None", PYOBJ.getDescriptor());
    }
    
    private static boolean isWide(String methDescriptor) {
        return isWide(Type.getArgumentTypes(methDescriptor));
    }
    
    private static boolean isWide(Type[] args) {
        return args.length == 2 && args[0].equals(APYOBJ) && args[1].equals(ASTRING);
    }

    private String methodName;

    protected String[] asNames, defaults;

    private String prefix;

    private Type[] params;

    private Type onType, returnType;

    protected MethodType type;
}
