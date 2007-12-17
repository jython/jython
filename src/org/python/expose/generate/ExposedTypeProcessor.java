package org.python.expose.generate;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.python.expose.ExposedType;

/**
 * Processes the bytecode of a Java class that has the {@link ExposedType}
 * annotation on it and generates new bytecode for it containing the inner
 * classes Jython needs to expose it as a type.
 */
public class ExposedTypeProcessor implements Opcodes, PyTypes {

    /**
     * @param in -
     *            an InputStream to bytecode of an ExposedType
     * @throws IllegalArgumentException -
     *             if the class doesn't have an annotation, or if one of the
     *             method annotations is malformed
     */
    public ExposedTypeProcessor(InputStream in) throws IOException {
        ClassReader cr = new ClassReader(in);
        cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
        cr.accept(new TypeProcessor(cw), ClassWriter.COMPUTE_FRAMES);
    }

    /**
     * @return the processed bytecode
     */
    public byte[] getBytecode() {
        return cw.toByteArray();
    }

    /**
     * @return MethodExposers for each method that needs to be exposed
     */
    public Collection<MethodExposer> getMethodExposers() {
        return methodExposers;
    }

    public Collection<DescriptorExposer> getDescriptorExposers() {
        return descExposers.values();
    }

    /**
     * @return The NewExposer for this type. Can be null if the type isn't
     *         instantiable.
     */
    public Exposer getNewExposer() {
        return newExposer;
    }

    /**
     * @return the name of the exposed type.
     */
    public String getName() {
        return typeName;
    }

    public TypeExposer getTypeExposer() {
        return typeExposer;
    }

    /**
     * @return the name of the class being processed
     */
    public String getExposedClassName() {
        return onType.getClassName();
    }

    protected DescriptorExposer getDescriptorExposer(String descName) {
        if(!descExposers.containsKey(descName)) {
            descExposers.put(descName, new DescriptorExposer(onType, descName));
        }
        return descExposers.get(descName);
    }

    private List<MethodExposer> methodExposers = new ArrayList<MethodExposer>();

    private Map<String, DescriptorExposer> descExposers = new HashMap<String, DescriptorExposer>();

    private Exposer newExposer;

    private TypeExposer typeExposer;

    private ClassWriter cw;

    private String typeName;

    private Type onType;

    /**
     * The actual visitor that runs over the bytecode and figures out what to
     * expose.
     */
    private final class TypeProcessor extends ClassAdapter {

        private TypeProcessor(ClassVisitor cv) {
            super(cv);
        }

        @Override
        public void visit(int version,
                          int access,
                          String name,
                          String signature,
                          String superName,
                          String[] interfaces) {
            onType = Type.getType("L" + name + ";");
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if(desc.equals(EXPOSED_TYPE.getDescriptor())) {
                return new ExposedTypeVisitor(onType) {

                    @Override
                    public void handleResult(String name) {
                        typeName = name;
                    }

                    @Override
                    public void handleResult(Type base) {
                        baseType = base;
                    }
                };
            }
            return super.visitAnnotation(desc, visible);
        }

        @Override
        public void visitEnd() {
            // typeName is set by the ExposedTypeVisitor in visitAnnotation, if
            // the ExposedType annotation is found.
            if(typeName == null) {
                throw new IllegalArgumentException("A class given to InnerClassExposer must have the ExposedType annotation");
            }
            typeExposer = new TypeExposer(onType,
                                          baseType,
                                          getName(),
                                          methodExposers,
                                          descExposers.values(),
                                          newExposer);
            for(MethodExposer exposer : methodExposers) {
                addInnerClass(exposer.getGeneratedType());
            }
            for(DescriptorExposer exposer : descExposers.values()) {
                addInnerClass(exposer.getGeneratedType());
            }
            if(newExposer != null) {
                addInnerClass(newExposer.getGeneratedType());
            }
            addInnerClass(typeExposer.getGeneratedType());
            // Create the builder and add it to PyType's map in a static block
            // if we haven't already added it to a preexisting static block
            if(!generatedStaticBlock) {
                MethodVisitor mv = visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
                mv.visitCode();
                mv.visitInsn(RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
            super.visitEnd();
        }

        private void generateAddBuilder(MethodVisitor mv) {
            mv.visitLdcInsn(onType);
            Type typeExposerType = Type.getObjectType(TypeExposer.makeGeneratedName(onType)
                    .replace('.', '/'));
            mv.visitTypeInsn(NEW, typeExposerType.getInternalName());
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, typeExposerType.getInternalName(), "<init>", "()V");
            mv.visitMethodInsn(INVOKESTATIC,
                               PYTYPE.getInternalName(),
                               "addBuilder",
                               Type.getMethodDescriptor(VOID, new Type[] {CLASS, TYPEBUILDER}));
        }

        /** Adds an inner class reference to inner from the class being visited. */
        private void addInnerClass(Type inner) {
            super.visitInnerClass(inner.getInternalName(),
                                  onType.getInternalName(),
                                  inner.getClassName().substring(inner.getClassName()
                                          .lastIndexOf('$') + 1),
                                  ACC_PRIVATE | ACC_STATIC);
        }

        @Override
        public MethodVisitor visitMethod(int access,
                                         final String name,
                                         final String desc,
                                         String signature,
                                         String[] exceptions) {
            if(name.equals("<clinit>")) {
                // If the class already has a static block, we add our builder
                // adding code at the beginning of it.
                generatedStaticBlock = true;
                final MethodVisitor passthroughVisitor = super.visitMethod(access,
                                                                           name,
                                                                           desc,
                                                                           signature,
                                                                           exceptions);
                return new MethodAdapter(passthroughVisitor) {

                    @Override
                    public void visitCode() {
                        super.visitCode();
                        generateAddBuilder(passthroughVisitor);
                    }
                };
            } else {
                // Otherwise we check each method for exposed annotations.
                MethodVisitor passthroughVisitor = super.visitMethod(access,
                                                                     name,
                                                                     desc,
                                                                     signature,
                                                                     exceptions);
                return new ExposedMethodFinder(getName(),
                                               onType,
                                               access,
                                               name,
                                               desc,
                                               exceptions,
                                               passthroughVisitor) {

                    @Override
                    public void handleResult(MethodExposer exposer) {
                        methodExposers.add(exposer);
                    }

                    @Override
                    public void handleNewExposer(Exposer exposer) {
                        newExposer = exposer;
                    }

                    @Override
                    public void exposeAsGetDescriptor(String descName) {
                        getDescriptorExposer(descName).addMethodGetter(name, desc);
                    }

                    @Override
                    public void exposeAsSetDescriptor(String descName) {
                        getDescriptorExposer(descName).addMethodSetter(name, desc);
                    }

                    @Override
                    public void exposeAsDeleteDescriptor(String descName) {
                        getDescriptorExposer(descName).addMethodDeleter(name, desc);
                    }
                };
            }
        }

        @Override
        public FieldVisitor visitField(int access,
                                       final String fieldName,
                                       final String desc,
                                       String signature,
                                       Object value) {
            FieldVisitor passthroughVisitor = super.visitField(access,
                                                               fieldName,
                                                               desc,
                                                               signature,
                                                               value);
            return new ExposedFieldFinder(fieldName, passthroughVisitor) {

                @Override
                public void exposeAsGet(String name) {
                    getDescriptorExposer(name).addFieldGetter(fieldName, Type.getType(desc));
                }

                @Override
                public void exposeAsSet(String name) {
                    getDescriptorExposer(name).addFieldSetter(fieldName, Type.getType(desc));
                }
            };
        }
        
        private Type baseType = OBJECT;

        private boolean generatedStaticBlock;
    }
}
