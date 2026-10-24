// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles.Lookup;

import org.python.kernel.BaseType;
import org.python.kernel.Representation;
import org.python.kernel.SimpleType;
import org.python.kernel.TypeFactory;
import org.python.kernel.TypeFactory.Clash;
import org.python.kernel.TypeRegistry;
import org.python.support.InterpreterError;
import org.python.types.Feature;
import org.python.types.TypeSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code TypeSystem} is the nexus of type object lookup and creation
 * methods in the run-time system, and publishes (to this package) a
 * static reference to the single instance of the Python type factory.
 * The type factory comes into being with the static initialisation of
 * this class.
 * <p>
 * All use of the type system is funnelled through this class in order
 * to ensure this initialisation is complete before types are used. The
 * JVM guarantees that competing threads are made to wait while the
 * first thread to use the type system completes the initialisation.
 * Careful design of the run-time avoids re-entrant use by the
 * initialising thread.
 */
class TypeSystem {
    /*
     * Design intent: the creation of any Python type object or
     * Representation should only take place after (or in selected cases
     * during) the creation of the single TypeFactory instance by the static
     * initialisation of this class. that is static initialisation of this
     * class. The TypeSystem class must also publish that instance to the
     * kernel package, by means that avoid it becoming module API.
     *
     * The class is in this package so that it can have package level access
     * to object implementations and the exposer, granting that access to
     * the TypeFactory through objects it passes when that is created.
     *
     * Could this class safely be made public API? Possibly, but not the
     * factory or registry objects, which are in the kernel for a reason.
     * Even if it were safe to export them, putting that much implementation
     * detail into the API constrains future change.
     */

    private TypeSystem() {} // no instances and static members only

    /** Logger for (the public face of) the type system. */
    static final Logger logger = LoggerFactory.getLogger(TypeSystem.class);

    /*
     * The static initialisation of this class brings the type system into
     * existence in the *only* way it should be allowed to happen.
     */

    /**
     * The type factory to which the run-time system goes for all type
     * objects.
     */
    static final TypeFactory factory;

    /**
     * The type registry to which this run-time system goes for all class
     * look-ups.
     */
    static final TypeRegistry registry;

    /*
     * We use these in corresponding TYPE fields avoid a static
     * initialisation deadlock during bootstrap.
     */
    /** The type object of {@code type} objects. */
    static final PyType TYPE_type;
    /** The type object of {@code getset_descriptor}. */
    static final PyType TYPE_getset_descriptor;
    /** The type object of {@code builtin_function_or_method}. */
    static final PyType TYPE_builtin_function_or_method;
    /** The type object of {@code member_descriptor} objects. */
    static final PyType TYPE_member_descriptor;
    /** The type object of {@code method_descriptor} objects. */
    static final PyType TYPE_method_descriptor;
    /** The type object of {@code method-wrapper} objects. */
    static final PyType TYPE_method_wrapper;
    /** The type object of {@code wrapper_descriptor} objects. */
    static final PyType TYPE_wrapper_descriptor;

    /** The type object of {@code int} objects. */
    static final PyType TYPE_int;
    /** The type object of {@code bool} objects. */
    static final PyType TYPE_bool;
    /** The type object of {@code str} objects. */
    static final PyType TYPE_str;
    /** The type object of {@code float} objects. */
    static final PyType TYPE_float;
    /** The type object of the {@code None} singleton. */
    static final PyType TYPE_NoneType;

    /**
     * High-resolution time (the result of {@link System#nanoTime()}) at
     * which the type system began static initialisation. This is used in
     * tests.
     */
    static final long bootstrapNanoTime;

    /**
     * High-resolution time (the result of {@link System#nanoTime()}) at
     * which the type system completed static initialisation. This is used
     * in tests and to give the "ready" message a time.
     */
    static final long readyNanoTime;

    /*
     * The next block intends to make all the bootstrap types Java ready,
     * then Python ready, before any type object becomes visible to another
     * thread. For this it relies on the protection a JVM gives to a class
     * during static initialisation, on which the well-known thread-safe
     * lazy singleton pattern is based. This class is the holder class for
     * the entire type system.
     */
    static {
        // This should be the first thing the run-time system does.
        logger.info("Type system is waking up.");
        bootstrapNanoTime = System.nanoTime();

        // Bring the type machinery to life.
        TypeFactory f = Representation.factory;

        /*
         * It is only safe to create our first types now that the Representation
         * class has completed static initialisation.
         */
        @SuppressWarnings("deprecation")
        SimpleType t = Representation.factory.createTypeForType();

        try {
            /*
             * At this point, type objects 'type' and 'object' exist "Java ready"
             * forms, but they are not "Python ready". We let them leak out so that
             * the bootstrap process itself may use them. No *other* thread can get
             * to them until this thread leaves the static initialisation of
             * TypeSystem.
             */
            TYPE_type = t;
            factory = f;
            registry = f.getRegistry();

            /*
             * Create partial types for all the bootstrap types. All the calls to
             * f.fromSpec() here count as "re-entrant", so the type objects they
             * create are not published, and will not be visible to any other thread
             * yet. The process of making them Python ready is deferred to
             * publishBootstrapTypes, and they sit in the workshop until we have
             * defined the full set.
             */

            /*
             * Many types can use the idiom 'static TYPE = PyType.fromSpec(...)' in
             * the static initialisation of the defining class. This is not safe for
             * types needed in the construction of type objects themselves. The
             * reason is that a competing thread could already have seized control
             * of the static initialisation of such a class, which would block the
             * bootstrap. Instead, each defining class contains a package-visible
             * Spec class, that provides the specification, even in those
             * circumstances.
             */

            /*
             * The first types needing this consideration are the descriptors.
             */
            TYPE_getset_descriptor = f.fromSpec(PyGetSetDescr.Spec.get());
            TYPE_builtin_function_or_method = f.fromSpec(PyJavaFunction.Spec.get());
            TYPE_member_descriptor = f.fromSpec(PyMemberDescr.Spec.get());
            TYPE_method_descriptor = f.fromSpec(PyMethodDescr.Spec.get());
            TYPE_method_wrapper = f.fromSpec(PyMethodWrapper.Spec.get());
            TYPE_wrapper_descriptor = f.fromSpec(PyWrapperDescr.Spec.get());

            /*
             * The second group of types is those with adopted representation Java
             * classes. A client that, for example, calls PyType.of(1) will
             * otherwise accidentally create a "discovered" type for Integer.
             */

            TYPE_str = f.fromSpec(PyUnicode.Spec.get());
            TYPE_float = f.fromSpec(PyFloat.Spec.get());
            TYPE_int = f.fromSpec(PyLong.Spec.get());
            TYPE_bool = f.fromSpec(PyBool.Spec.get(TYPE_int));

            /*
             * In order to create MethodHandles when exposing types (see static
             * initialisation of Clinic), we also need:
             */
            TYPE_NoneType = f.fromSpec(PyNone.Spec.get());

            /*
             * We complete and publish the bootstrap types, for which the type
             * factory needs to create exposers.
             */
            f.publishBootstrapTypes(TypeExposerImplementation::new);

        } catch (Clash clash) {
            // Maybe a bootstrap type was used prematurely?
            throw new InterpreterError(clash);
        }

        /*
         * We like to know how long this took. It is also used in BootstrapTest
         * to verify that the initialisation thread finished before any other
         * could access the type system.
         */
        readyNanoTime = System.nanoTime();

        logger.atInfo().setMessage("Type system is ready after {} seconds")
                .addArgument(
                        () -> String.format("%.3f", 1e-9 * (readyNanoTime - bootstrapNanoTime)))
                .log();
    }

    /**
     * A specification of a bootstrap a type object. We do this as a
     * shorthand for use in the specification of the bootstrap types (e.g.
     * {@link PyFloat.Spec#get()}, as we find the same mutators have to be
     * added.
     */
    static class BootstrapSpec extends TypeSpec {
        /**
         * Create a specification using name and primary class. The lookup
         * object used to expose methods will be a generic one supplied by the
         * run-time package, that does not have access to {@code private}
         * members of the {@code primary} class. For this reason, the special
         * and other exposed methods of a bootstrap type must be visible at
         * package scope in the run-time package.
         *
         * @param name of type
         * @param lookup to access the implementation classes
         * @param primary used to represent and define methods
         */
        BootstrapSpec(String name, Lookup lookup, Class<?> primary) {
            super(name, lookup, false);
            this.primary(primary).add(Feature.IMMUTABLE);
        }
    }

    /**
     * Weak test that the type system has completed its bootstrap. This does
     * not guarantee that type objects, outside the bootstrap set, are safe
     * to use. A thread that has triggered type system creation can use this
     * as a check that it has finished (and certain operations are valid).
     * Any other thread calling this method will either cause the type
     * system bootstrap or wait for it to complete.
     *
     * @return type {@code true} iff system is ready for use.
     */
    static boolean systemReady() { return readyNanoTime != 0L; }

    /**
     * Determine (or create if necessary) the {@link Representation} for the
     * given object. The representation is found (in the type registry) from
     * the Java class of the argument.
     *
     * @param o for which a {@code Representation} is required
     * @return the {@code Representation}
     */
    static Representation getRepresentation(Object o) { return registry.get(o.getClass()); }

    /**
     * Determine (or create if necessary) the Python type for the given
     * object.
     *
     * @param o for which a type is required
     * @return the type
     */
    static BaseType typeOf(Object o) { return registry.get(o.getClass()).pythonType(o); }

    /**
     * Determine the Python type for the given class, or fail if it the
     * class is a representation of more than one type.
     *
     * @param klass for which a type is required
     * @return the corresponding unique type
     * @throws InterpreterError when not unique
     */
    static BaseType typeForClass(Class<?> klass) throws InterpreterError {
        Representation rep = registry.get(klass);
        if (rep instanceof BaseType bt) {
            return bt;
        } else {
            throw new InterpreterError("Python type of %s was expected to be unique",
                    klass.getSimpleName());
        }
    }

    /**
     * Create a Python type according to the specification. This exists to
     * back {@link PyType#fromSpec(TypeSpec)}. Within the run-time system
     * package, we might call:<pre>
     * class MyType {
     *     BaseType bt = TypeSystem.typeFromSpec(
     *         new TypeSpec("mypackage.mytype",
     *                      MethodHandles.lookup());
     * }
     * </pre> The type system will add descriptors for the members of the
     * class identified by annotation or reserved names for exposure to
     * Python.
     *
     * @param spec specifying the new type
     * @return the new type
     */
    // FIXME Avoid throwing InterpreterError(clash). If used at all.
    static BaseType typeFromSpec(TypeSpec spec) {
        try {
            return factory.fromSpec(spec);
        } catch (Clash clash) {
            logger.atError().log(clash.toString());
            throw new InterpreterError(clash);
        }
    }
}
