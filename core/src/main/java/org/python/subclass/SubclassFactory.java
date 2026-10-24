// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.subclass;

import static org.objectweb.asm.Opcodes.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.python.core.PyBaseException;
import org.python.core.PyDict;
import org.python.core.PyType;
import org.python.internal.Util;
import org.python.kernel.TypeFactory;
import org.python.support.InterpreterError;
import org.python.types.WithClassAssignment;
import org.python.types.WithDictAssignment;

/**
 * A helper to the {@link TypeFactory} that is responsible for the
 * synthetic classes we use to represent subclasses defined in Python.
 */
public class SubclassFactory {

    /** Write generated classes as files. (Dump with {@code javap}.) */
    // TODO Make this (or destination path) a property
    private static boolean DEBUG_CLASSFILES = false;

    /** Logger for the subclass factory. */
    final Logger logger = LoggerFactory.getLogger(SubclassFactory.class);

    /**
     * Lookup object allowing package-level access to generated classes.
     */
    static final Lookup LOOKUP = MethodHandles.lookup();

    /** A prefix used in {@link SubclassBuilder#begin()}. */
    private final String subclassPkg;
    /** A name template used in {@link SubclassBuilder#begin()}. */
    private final String subclassNameTemplate;

    /**
     * The classes (actually, the lookup objects of those classes) created
     * in this factory from a specification. We do not use a weak reference
     * here because the loader (which is part of class identity) keeps the
     * classes alive anyway.
     */
    private final Map<SubclassSpec, Lookup> subclasses;

    /** Minimum number of distinct specifications to expect. */
    private static final int CLASSES_MAP_SIZE = 100;

    /**
     * Create a factory that creates classes in the {@code subclasses}
     * package, specifying a string format for generating class names,
     * requiring one string and one integer (like {@code "JY$%s$%d"}). When
     * creating a class, the package will be a subpackage ".subclasses" and
     * the name generated from the simple name of the base class.
     *
     * @param subclassNameTemplate format of class names
     */
    public SubclassFactory(String subclassNameTemplate) {
        // Convert package name for ASM: org/python/runtime/subclasses/
        String[] parts = getClass().getPackageName().split("\\.");
        this.subclassPkg = String.join("/", parts) + "/";
        // Pattern for class names
        this.subclassNameTemplate = subclassNameTemplate;
        // Cache for classes we made already
        this.subclasses = new HashMap<>(CLASSES_MAP_SIZE * 2);
        logger.atInfo().setMessage("Subclass factory created for {}").addArgument(subclassPkg)
                .log();
    }

    /**
     * We name each class we synthesise after its Java base type, with a
     * one-up number. This table must only be accessed when holding a lock
     * on this instance of {@code SubclassFactory}.
     */
    private final Map<String, AtomicInteger> unique = new HashMap<>();

    /**
     * Find a lookup object (created by a previous call), or create a one
     * now, that matches the specification for base, interfaces, slots and
     * possession of {@code __dict__} and managed {@code __class__} members.
     *
     * @param spec of the required class
     * @return lookup on the Java representation class
     */
    public synchronized Lookup findOrCreateSubclass(SubclassSpec spec) {
        Lookup lookup = subclasses.get(spec); // hash calls freeze()
        if (lookup == null) {
            // Make a class
            SubclassBuilder sw = new SubclassBuilder(spec);
            sw.build();
            byte[] b = sw.toByteArray();

            if (DEBUG_CLASSFILES) {
                // Write so we can dump it later.
                String fn = spec.getName() + ".class";
                logger.atTrace().setMessage("Writing {}").addArgument(fn).log();
                try (OutputStream f = new FileOutputStream(fn)) {
                    f.write(b);
                } catch (IOException e) {
                    throw new InterpreterError(e, "writing class file");
                }
            }

            // Create a representation class according to this spec
            try {
                lookup = LOOKUP.defineHiddenClass(b, true);
            } catch (IllegalAccessException e) {
                throw new InterpreterError("Failed to create subclass specified by %s", spec);
            }

            /*
             * Cache the new representation so that an identical spec will lead to
             * re-use of the same representation.
             */
            subclasses.put(spec, lookup);

            /*
             * Create a second spec citing the new representation as a Java base but
             * requiring no additional __dict__ and __class__ members. This is a
             * specification that may result when a Python class with the new
             * representation is sub-classed, and the subclass adds no members. It
             * can and should be satisfied by re-use of the same representation.
             */
            SubclassSpec spec2 = new SubclassSpec(spec.getName() + "$sub", lookup.lookupClass());
            subclasses.put(spec2, lookup);
        }
        return lookup;
    }

    /**
     * We name each class we synthesise after its Java base type, with a
     * one-up number.
     *
     * @param baseName name of the base (to extend)
     * @return chosen unique name
     */
    synchronized String uniqueName(String baseName) {
        AtomicInteger id = unique.get(baseName);
        if (id == null) { id = new AtomicInteger(); unique.put(baseName, id); }
        int n = id.incrementAndGet();
        return String.format(subclassNameTemplate, baseName, n);
    }

    /** Description of a field with elements in internal format. */
    static class FieldDescr {
        /** Access mode (private, static, etc.). */
        final int access;
        /** Name of field. */
        final String name;
        /** Descriptor of field in internal format. */
        final String descr;

        /**
         * Create from name and type.
         *
         * @param access access flags of the fields
         * @param name of field
         * @param type of field
         */
        FieldDescr(int access, String name, Class<?> type) {
            this.access = access;
            this.name = name;
            this.descr = Type.getType(type).getDescriptor();
        }

        /**
         * Create private instance field from name and type.
         *
         * @param name of field
         * @param type of field
         */
        FieldDescr(String name, Class<?> type) { this(ACC_PRIVATE, name, type); }
    }

    /** Description of a method with elements in internal format. */
    static class MethodDescr {
        /** Name of method. */
        final String name;
        /** Descriptor of method in internal format. */
        final String descr;

        /**
         * Create from name and type.
         *
         * @param c class in which named method is found
         * @param name of method
         * @param args types of arguments
         */
        MethodDescr(Class<?> c, String name, Class<?>... args) {
            this.name = name;
            try {
                this.descr = Type.getType(c.getMethod(name, args)).getDescriptor();
            } catch (NoSuchMethodException | SecurityException e) {
                // Should never happen.
                throw new InterpreterError(e, "reflecting %s.%s", c.getName(), name);
            }
        }
    }

    // Field for __class__ and methods from WithClassAssignment.
    private static final FieldDescr TYPE_FIELD = new FieldDescr("$type", PyType.class);
    private static final MethodDescr GET_TYPE =
            new MethodDescr(WithClassAssignment.class, "getType");
    private static final MethodDescr SET_TYPE =
            new MethodDescr(WithClassAssignment.class, "setType", Object.class);
    private static final MethodDescr CHECK_TYPE =
            new MethodDescr(WithClassAssignment.class, "checkClassAssignment", Object.class);

    // Field for __dict__ and methods from WithDictAssignment.
    private static final FieldDescr DICT_FIELD = new FieldDescr("$dict", PyDict.class);
    private static final MethodDescr GET_DICT =
            new MethodDescr(WithDictAssignment.class, "getDict");
    private static final MethodDescr SET_DICT =
            new MethodDescr(WithDictAssignment.class, "setDict", Object.class);
    private static final MethodDescr CHECK_DICT =
            new MethodDescr(WithDictAssignment.class, "checkDictAssignment", Object.class);

    // ASM Types we use repeatedly
    private static final Type PY_TYPE_TYPE = Type.getType(PyType.class);
    private static final Type WITH_CLASS_ASSIGNMENT_TYPE = Type.getType(WithClassAssignment.class);
    private static final Type WITH_DICT_ASSIGNMENT_TYPE = Type.getType(WithDictAssignment.class);
    private static final Type PY_EXCEPTION_TYPE = Type.getType(PyBaseException.class);

    /** A builder object for one subclass from a given specification. */
    class SubclassBuilder {

        private final SubclassSpec spec;
        private final ClassNode cn;
        private final Type baseClass;
        private final String baseName;
        private final String[] exceptions;

        // private final MethodNode staticInit;

        /**
         * Create builder from specification.
         *
         * @param spec to follow
         */
        SubclassBuilder(SubclassSpec spec) {
            this.spec = spec.freeze();
            this.cn = new ClassNode();
            // Make it easier to use the (canonical) base
            Class<?> base = spec.getBase();
            this.baseClass = Type.getType(base);
            this.baseName = baseClass.getInternalName();

            // Constructors and setType may throw:
            this.exceptions = new String[] {PY_EXCEPTION_TYPE.getInternalName()};
            // Get a new unique class name according to factory + spec
            String name = uniqueName(base.getSimpleName());
            // OK to set after the freeze as not part of identity.
            spec.setName(name);
            logger.atDebug().setMessage("Creating class for spec {}").addArgument(spec).log();
            // We seem never to need a static initialisation section
            // this.staticInit = new MethodNode(ACC_STATIC, "<clinit>",
            // "()V", null, null);
            // this.cn.methods.add(this.staticInit);
        }

        /**
         * Build the internal representation of the class being specified.
         */
        void build() {
            begin();
            /*
             * Constructors as required in the specification. These will have been
             * deduced by the type object from the Java base.
             */
            addConstructors();
            // Add the methods to manage object type.
            if (spec.manageClassAssignment()) {
                addGetSet(TYPE_FIELD, GET_TYPE, SET_TYPE, CHECK_TYPE);
            }
            // Add the methods to manage an assignable dictionary.
            if (spec.manageDictAssignment()) {
                addGetSet(DICT_FIELD, GET_DICT, SET_DICT, CHECK_DICT);
            }
            // Add an attribute for each name in __slots__.
            for (String name : spec.getSlots()) { addObjectAttr(name); }

            end();
        }

        /**
         * Get the class definition as a JVM byte code file.
         *
         * @return the class definition
         */
        byte[] toByteArray() {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            cn.accept(cw);
            return cw.toByteArray();
        }

        /**
         * Begin the class definition with a version and a name based on the
         * base Java class that this class extends. This base class will be the
         * canonical representation of the "solid base".
         */
        void begin() {
            cn.version = V17;
            cn.access = 0;
            cn.name = subclassPkg + spec.getName();
            cn.superName = baseClass.getInternalName();
            for (Class<?> c : spec.getInterfaces()) { cn.interfaces.add(Type.getInternalName(c)); }
            // Possibly add replaceable dictionary
            if (spec.manageDictAssignment()) {
                cn.interfaces.add(WITH_DICT_ASSIGNMENT_TYPE.getInternalName());
            }
            // Add __class__ access if base is not already doing it
            if (spec.manageClassAssignment())
                cn.interfaces.add(WITH_CLASS_ASSIGNMENT_TYPE.getInternalName());
        }

        /**
         * For each base class constructor in the specification, synthesise a
         * constructor in this class.
         */
        void addConstructors() {
            // Now process the list of constructors from spec
            for (Constructor<?> c : spec.getConstructors()) { addConstructorWrapping(c); }
        }

        /**
         * Synthesise a constructor in this class that has {@code PyType} as its
         * first parameter and calls the given superclass constructor.
         * <p>
         * If the superclass manages the type ({@code __class__} attribute),
         * each constructor has {@code PyType} as its first parameter. The new
         * constructor has exactly the same parameters and all the arguments are
         * passed on to the superclass constructor. We require that the
         * superclass constructor check that the type is acceptable for the
         * actual class.
         * <p>
         * If the superclass does not manage type, the constructor does not have
         * {@code PyType} as its first parameter, we synthesise a constructor
         * that does. In the body we check and store the type in the
         * {@code __class__} attribute and pass on the remaining same
         * parameters.
         *
         * @param baseConstructor constructor of the superclass.
         */
        void addConstructorWrapping(Constructor<?> baseConstructor) {

            // This is the super constructor
            String baseDescr = Type.getConstructorDescriptor(baseConstructor);
            Type[] baseParams = Type.getArgumentTypes(baseDescr);

            // Work out the parameters of the new constructor
            String descr;
            if (spec.manageClassAssignment()) {
                /*
                 * The base does not hold the actual type of the object. Its constructor
                 * does not have the actual PyType as its first parameter, but ours
                 * must.
                 */
                Type[] params = Util.prepend(PY_TYPE_TYPE, baseParams);
                descr = Type.getMethodType(Type.VOID_TYPE, params).getDescriptor();
            } else {
                /*
                 * The base holds the actual type of the object. Its constructor has a
                 * PyType parameter, and so must the synthesised constructor.
                 */
                assert baseParams.length > 0;
                assert PY_TYPE_TYPE.equals(baseParams[0]);
                descr = baseDescr;
            }

            MethodNode mn = new MethodNode(ACC_PUBLIC, "<init>", descr, null, exceptions);

            // Build the constructor code T(actualType, args...)
            InsnList ins = mn.instructions;

            if (spec.manageClassAssignment()) {
                // Call super(args...). args begin at frame[2].
                addCallSuperConstructor(ins, baseName, baseDescr, baseParams, 2);

                // $type = check(actualType);
                ins.add(new VarInsnNode(ALOAD, 0));
                ins.add(new VarInsnNode(ALOAD, 0));
                ins.add(new VarInsnNode(ALOAD, 1));
                MethodDescr check = CHECK_TYPE;
                ins.add(new MethodInsnNode(INVOKEVIRTUAL, cn.name, check.name, check.descr));

                FieldDescr field = TYPE_FIELD;
                ins.add(new FieldInsnNode(PUTFIELD, cn.name, field.name, field.descr));

            } else {
                // Call super(actualType, args...).
                // Arguments begin at frame[1].
                addCallSuperConstructor(ins, baseName, baseDescr, baseParams, 1);
            }

            ins.add(new InsnNode(RETURN));

            cn.methods.add(mn);
        }

        /**
         * Emit instructions to call a constructor with parameters of the given
         * ASM types, which will be taken from local variables (assumed
         * parameters of the enclosing constructor method) starting at the given
         * offset. We support all primitive and object parameter types.
         *
         * @param ins to which to add new instructions
         * @param base_name of the constructor
         * @param base_descr descriptor of the super
         * @param base_params types expected by chosen super()
         * @param offset of first parameter
         * @throws InterpreterError if a parameter type is bad
         */
        private void addCallSuperConstructor(InsnList ins, String base_name, String base_descr,
                Type[] base_params, int offset) throws InterpreterError {
            // Before any parameter comes 'this'
            ins.add(new VarInsnNode(ALOAD, 0));
            for (Type p : base_params) {
                // Generate a
                int sort = p.getSort();
                switch (sort) {
                    case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.INT, Type.SHORT:
                        ins.add(new VarInsnNode(ILOAD, offset++));
                        break;
                    case Type.LONG:
                        ins.add(new VarInsnNode(LLOAD, offset));
                        offset += 2;
                        break;
                    case Type.FLOAT:
                        ins.add(new VarInsnNode(FLOAD, offset++));
                        break;
                    case Type.DOUBLE:
                        ins.add(new VarInsnNode(DLOAD, offset));
                        offset += 2;
                        break;
                    case Type.ARRAY, Type.OBJECT:
                        ins.add(new VarInsnNode(ALOAD, offset++));
                        break;
                    default:
                        throw new InterpreterError("Parameter of unsupported sort %d", sort);
                }
            }
            ins.add(new MethodInsnNode(INVOKESPECIAL, base_name, "<init>", base_descr));
        }

        /**
         * Add a field with get and set methods as described and a checked cast
         * used when setting.
         *
         * @param field describing the field to create
         * @param getMethod describing the getter
         * @param setMethod describing the setter
         * @param checkMethod describing the checked cast
         */
        void addGetSet(FieldDescr field, MethodDescr getMethod, MethodDescr setMethod,
                MethodDescr checkMethod) {
            // Add a field
            addField(field);
            cn.methods.add(getter(field, getMethod));
            cn.methods.add(setter(field, setMethod, checkMethod));
        }

        /**
         * Add a field according to the specification given.
         *
         * @param field specifying the field
         */
        void addField(FieldDescr field) {
            cn.fields.add(new FieldNode(field.access, field.name, field.descr, null, null));
        }

        /**
         * Add a getter for the named field.
         *
         * @param field to get
         * @param method descriptor for the method to create
         * @return method for the get operation
         */
        MethodNode getter(FieldDescr field, MethodDescr method) {
            MethodNode mn = new MethodNode(ACC_PUBLIC, method.name, method.descr, null, null);
            InsnList ins = mn.instructions;
            // Return the attribute
            ins.add(new VarInsnNode(ALOAD, 0));
            ins.add(new FieldInsnNode(GETFIELD, cn.name, field.name, field.descr));
            ins.add(new InsnNode(ARETURN));
            return mn;
        }

        /**
         * Add a setter for the named field, using the checked cast method to
         * disallow values that would make this not a valid representation class
         * for the clique of mutually-replaceable types.
         *
         * @param field to set
         * @param method descriptor for the method to create
         * @param check descriptor for the checked cast method
         * @return method for the set operation
         */
        MethodNode setter(FieldDescr field, MethodDescr method, MethodDescr check) {
            MethodNode mn = new MethodNode(ACC_PUBLIC, method.name, method.descr, null, null);
            InsnList ins = mn.instructions;
            ins.add(new VarInsnNode(ALOAD, 0));
            ins.add(new VarInsnNode(ALOAD, 0));
            // Get the purported new value for the field
            ins.add(new VarInsnNode(ALOAD, 1));
            // Apply the check method (may throw)
            ins.add(new MethodInsnNode(INVOKEVIRTUAL, cn.name, check.name, check.descr));
            // Assign to the attribute
            ins.add(new FieldInsnNode(PUTFIELD, cn.name, field.name, field.descr));
            ins.add(new InsnNode(RETURN));
            return mn;
        }

        /** */
        void addWithDictInterface() {

        }

        /**
         * Add a field with {@code Object} type as needed for a slot (named in
         * {@code __slots__}). The field has default (package) access so that a
         * package-level lookup may be used to generate handles.
         *
         * @param name of the field to create
         */
        void addObjectAttr(String name) {
            // Add a field (with package access)
            FieldDescr field = new FieldDescr(name, Object.class);
            cn.fields.add(new FieldNode(0, field.name, field.descr, null, null));
        }

        /** */
        void end() {
            // Finish static section
            // InsnList ins = staticInit.instructions;
            // ins.add(new InsnNode(RETURN));
        }
    }
}
