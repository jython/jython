package org.python.expose.generate;

import java.io.PrintWriter;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;
import org.python.core.BytecodeLoader;

/**
 * Base class that handles the basics of generating a single class with asm.
 * Subclass to supply the actual functionality of the generated class.
 * 
 */
public abstract class Exposer implements Opcodes, PyTypes {

    /**
     * @param superClass -
     *            the super class of the generated class
     * @param generatedName -
     *            the name of the class to generate
     */
    public Exposer(Class superClass, String generatedName) {
        superType = Type.getType(superClass);
        thisType = Type.getType("L" + generatedName.replace('.', '/') + ";");
    }

    /**
     * Implemented by subclasses to fill in the actual implementation of the
     * class. cv is set to the ClassVisitor to be used when this is called.
     */
    protected abstract void generate();

    /**
     * Generates this Exposer and loads it into the given Loader.
     */
    protected Class load(BytecodeLoader.Loader l) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        generate(cw);
//        ClassVisitor cv = new CheckClassAdapter(cw);
//        cv = new TraceClassVisitor(cv, new PrintWriter(System.out));
//        generate(cv);
        return l.loadClassFromBytes(getClassName(), cw.toByteArray());
    }

    protected Type getGeneratedType() {
        return thisType;
    }

    public String getClassName() {
        return thisType.getClassName();
    }

    public String getInternalName() {
        return thisType.getInternalName();
    }

    /**
     * Will call the methods on visitor to generate this class. Only one call to
     * generate may be active at a time on a single instance of Exposer. The
     * ClassVisitor is assumed to have been constructed with COMPUTE_FRAMES.
     */
    public void generate(ClassVisitor visitor) {
        assert cv == null;
        cv = visitor;
        cv.visit(V1_5,
                 ACC_PUBLIC,
                 getInternalName(),
                 null,
                 superType.getInternalName(),
                 new String[] {});
        generate();
        assert mv == null;
        cv.visitEnd();
        cv = null;
    }

    /** Calls the constructor on the super class with the given args. */
    protected void superConstructor(Type... args) {
        callConstructor(superType, args);
    }
    
    class Instantiator {
        public Instantiator(Type... types) {
            this.types = types;
        }

        /** 
         * Push args onto the stack corresponding to the types passed to the constructor.
         */
        public void pushArgs() {
            if(types.length > 0) {
                throw new IllegalStateException("If the constuctor takes types as indicated by "
                        + "passing their types to Instantiator, pushArgs must be overriden to put "
                        + "those args on the stack before the call");
            }
        };

        public Type[] getTypes() {
            return types;
        }

        private Type[] types;
    }
    
    /** Instantiates ofType using its no-arg constructor */
    protected void instantiate(Type ofType) {
        instantiate(ofType, new Instantiator());
    }
    
    /**
     * Instantiates ofType with its constructor that takes the types returned by
     * inst.getTypes(). inst should override pushArgs to put arguments of those
     * types on the stack for the call.
     */
    protected void instantiate(Type ofType, Instantiator inst){
        mv.visitTypeInsn(NEW, ofType.getInternalName());
        mv.visitInsn(DUP);
        inst.pushArgs();
        callConstructor(ofType, inst.getTypes());
    }

    /** Calls the constructor on onType with the given args. */
    protected void callConstructor(Type onType, Type... args) {
        mv.visitMethodInsn(INVOKESPECIAL,
                           onType.getInternalName(),
                           "<init>",
                           methodDesc(VOID, args));
    }

    /** Produces a method descriptor with ret as its return type that takes args. */
    protected String methodDesc(Type ret, Type... args) {
        return Type.getMethodDescriptor(ret, args);
    }

    /**
     * Starts building a constructor in the class. Must be followed by a call to
     * endConstructor before startConstructor or startMethod may be called.
     */
    protected void startConstructor(Type... args) {
        startMethod("<init>", VOID, args);
    }

    /** Closes the constructor begun by startConstructor. */
    protected void endConstructor() {
        mv.visitInsn(RETURN);
        endMethod();
    }

    /**
     * Starts building a method in the class being generated. Must be followed
     * by a call to endMethod before startMethod or startConstructor may be
     * called.
     */
    protected void startMethod(String name, Type ret, Type... args) {
        assert mv == null;
        mv = cv.visitMethod(ACC_PUBLIC, name, methodDesc(ret, args), null, null);
        mv.visitCode();
    }

    /** Closes the method under construction. */
    protected void endMethod() {
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        mv = null;
    }

    /** Loads a field on the type under construction of ofType onto the stack */
    protected void get(String fieldName, Type ofType) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, getInternalName(), fieldName, ofType.getDescriptor());
    }

    /** The current method under construction or null. */
    protected MethodVisitor mv;

    /** The current class under construction. */
    protected ClassVisitor cv;

    /** The super class of the type that will be generated. */
    private Type superType;

    /** The type that will be generated. */
    protected Type thisType;
}
