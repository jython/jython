// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.kernel;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.python.core.PyErr;
import org.python.core.PyExc;
import org.python.core.PyFloat;
import org.python.core.PyLong;
import org.python.core.PyType;
import org.python.kernel.SpecialMethod.Signature;
import org.python.types.Constructible;
import org.python.types.TypeFlag;
import org.python.types.WithClass;

/**
 * A {@code Representation} provides Python behaviour to a Java object
 * by linking its Java class to essential type information. Every Java
 * class has a {@code Representation} that may be found from the type
 * system, even if it must be created to satisfy the enquiry. Java
 * classes may share a representation if they are all subclasses of the
 * same base in Java.
 * <p>
 * In many cases, the Java class alone determines the {@link PyType
 * Python type} and behaviour. In cases where instances of the Java
 * class can represent objects with different (or mutable) Python type,
 * the object itself identifies the actual type by implementing
 * {@link WithClass}.
 * <p>
 * The run-time system will form a mapping from each Java class to an
 * instance of (a specific sub-class of) {@code Representation}. Apart
 * from a small collection of bootstrap classes (all of them built-in
 * types), this mapping will be developed as instances of the classes
 * are encountered in Python code.
 */
public abstract class Representation implements Constructible {

    /** Logger for representation object activity in the kernel. */
    protected static final Logger logger = LoggerFactory.getLogger(Representation.class);

    /**
     * The type factory to which the entire run-time system goes for type
     * objects. Do not use this object until {@code core.TypeSystem} has
     * completed static initialisation.
     */
    public static final TypeFactory factory;

    /** The {@code TypeRegistry} in use. */
    protected static final TypeRegistry registry;

    /*
     * We create the singleton type factory for the kernel. This is a one
     * time action during the creation of the type system.
     */
    static {
        // This may be the first thing the run-time system does.
        logger.debug("Representation system is waking up.");

        @SuppressWarnings("deprecation")
        TypeFactory f = new TypeFactory();

        // Unsafe to create type objects yet as they extend this class

        factory = f;
        registry = f.getRegistry();
    }

    /**
     * Get the {@code Representation} of the class of an object {@code o},
     * and hence access to its Python type and behaviour. This call may
     * result in the creation of a {@code Representation} for that class.
     * <p>
     * Because this uses the type factory, beware of using it before the
     * static initialisation of {@code core.TypeSystem} is complete.
     *
     * @param o for which a representation of the class is needed
     * @return the representation
     */
    static Representation get(Object o) { return registry.get(o.getClass()); }

    /**
     * Get the Python type object {@code type} directly from the factory.
     *
     * @return the Python type object {@code type}
     */
    static final SimpleType typeType() { return factory.typeType(); }

    /**
     * Get the Python type object {@code object} directly from the factory.
     *
     * @return the Python type object {@code object}
     */
    static final SimpleType objectType() { return factory.objectType(); }

    /*
     * Give SpecialMethod access to private members (so it may write the
     * method handle caches), and to the core package in general. It looks
     * clunky, but seems the only way to supply the necessary access and
     * keep the module API clean.
     */
    static {
        SpecialMethod.SMUtil.provideAccess(
                // Anonymously implement the requirement
                new SpecialMethod.Required() {
                    // private lookup to access caches
                    private static final Lookup LOOKUP = MethodHandles.lookup();

                    @Override
                    public Lookup getLookup() { return LOOKUP; }
                });
    }

    /**
     * The common type (class or interface) of Java classes representing
     * instances of the related Python {@code type}, and registered for this
     * {@code Representation}. If more than one Java class is registered to
     * this representation, then they must all inherit (extend or implement
     * in Java) the type recorded here.
     * <p>
     * Classes representing instances of the related Python {@code type} and
     * <i>not</i> related by inheritance must be described in separate
     * {@code Representation}s.
     */
    protected final Class<?> javaClass;

    /**
     * Holds the information necessary to synthesise and call a constructor
     * for a Java subclass of the {@link #javaClass()} of this
     * {@code Representation}.
     */
    private Map<MethodType, ConstructorAndHandle> constructorLookup = Collections.emptyMap();

    /**
     * Create a {@code Representation} relating a (base) Java class to a
     * type. Creation of a {@code Representation} does not register the
     * association.
     *
     * @param javaClass the base of classes represented
     */
    protected Representation(Class<?> javaClass) { this.javaClass = javaClass; }

    /**
     * Get the Python type of the object <i>given that</i> this is the
     * representation object for it. The argument {@code x} is only used
     * when this is a {@code SharedRepresentation} representation. A shared
     * representation is not associated with a unique type, so in that case
     * {@code x} is consulted for the type, while a {@code null} returns a
     * {@code null} result.
     *
     * @implSpec If this object is also a type object, it will answer that
     *     it itself is that type. An {@code AdoptedRepresentation} knows
     *     its {@link AdoptiveType} directly, while a
     *     {@code SharedRepresentation} must consult the object {@code x}.
     *
     * @param x subject of the enquiry
     * @return {@code type(x)}
     */
    public abstract BaseType pythonType(Object x);

    /**
     * Fast check that a particular object, for which this is the
     * representation, is a data descriptor (defines {@code __set__} or
     * {@code __delete__}).
     *
     * @param x subject of the enquiry
     * @return {@code x} is a data descriptor
     */
    public boolean isDataDescr(Object x) {
        BaseType type = pythonType(x);
        return type.hasFeature(KernelTypeFlag.HAS_SET)
                || type.hasFeature(KernelTypeFlag.HAS_DELETE);
    }

    /**
     * Fast check that a particular object, for which this is the
     * representation, has the specified feature.
     *
     * @param x subject of the enquiry
     * @param feature to check for
     * @return {@code x} is a data descriptor
     */
    // Avoid a call to pythonType(x), when possible, by overriding.
    public boolean hasFeature(Object x, TypeFlag feature) {
        return pythonType(x).hasFeature(feature);
    }

    /**
     * Fast check that a particular object, for which this is the
     * representation, has a specified kernel feature.
     *
     * @param x subject of the enquiry
     * @param feature to check for
     * @return {@code x} is a data descriptor
     */
    // Avoid a call to pythonType(x), when possible, by overriding.
    public boolean hasFeature(Object x, KernelTypeFlag feature) {
        return pythonType(x).hasFeature(feature);
    }

    /**
     * Fast check that an object having this as its Representation is
     * exactly a Python {@code int}. We can do this without reference to the
     * object itself, just from the representation.
     *
     * @implNote The result may be incorrect during type system bootstrap.
     *
     * @return target is exactly a Python {@code int}
     */
    public boolean isIntExact() { return this == PyLong.TYPE; }

    /**
     * Fast check that an object having this as its Representation is
     * exactly a Python {@code float}. We can do this without reference to
     * the object itself, just from the representation.
     *
     * @implNote The result may be incorrect during type system bootstrap.
     *
     * @return target is exactly a Python {@code float}
     */
    public boolean isFloatExact() { return this == PyFloat.TYPE; }

    /**
     * A base Java class representing instances of the related Python
     * {@code type} associated with this {@code Representation}. If there is
     * more than one Java class <i>associated to this representation</i>,
     * they must all be subclasses in Java of the class returned here.
     *
     * @return base class of the representation
     */
    public Class<?> javaClass() { return javaClass; }

    /**
     * Return the index of this {@code Representation} in the associated
     * type. Adoptive types support multiple Java representation classes for
     * their instances, and each Representation holds the index for the
     * class(es) associated to it in the registry. For other
     * representations, the index is zero.
     * <p>
     * Each descriptor in the dictionary of that type is able to provide an
     * implementation of the method that applies to a {@code self} with Java
     * class equal to (or a subclass of) {@link #javaClass} of the
     * representation.
     * <p>
     * When a type accepts, as self-classes, the representations of some
     * other type (as when {@code int} accepts {@code Boolean}), there is no
     * {@code Representation} of it in the accepting type. The correct index
     * must be determined by finding a compatible class in
     * {@link AdoptiveType#selfClasses()}.
     *
     * @implSpec Override this in the {@code Representation}s attached to
     *     adoptive types. The default implementation returns zero.
     *
     * @return index in the type (0 if canonical)
     */
    @SuppressWarnings("static-method")
    public int getIndex() { return 0; }

    // Support for __new__ -------------------------------------------

    /**
     * {@inheritDoc} This is set by a call to
     */
    @Override
    public Map<MethodType, ConstructorAndHandle> constructorLookup() { return constructorLookup; }

    @Override
    public ConstructorAndHandle constructor(Class<?>... param) {
        // Neutralise the actual return type
        MethodType mt = MethodType.methodType(Object.class, param);
        ConstructorAndHandle ch = constructorLookup.get(mt);
        if (ch == null) {
            // TODO Improve communicating this error
            /*
             * This method is here for __new__ implementations, and has to be public
             * because they could be in any package. So, stuff can go wrong. But how
             * to explain to the hapless caller when something is probably a Java
             * API error in the synthetic class?
             */
            throw PyErr.format(PyExc.TypeError, "No public Java constructor of '%s' matches %s",
                    this, mt);
        }
        return ch;
    }

    /**
     * Provide the information necessary to synthesise and call a
     * constructor for a Java subclass of the {@link #javaClass()} of this
     * {@code Representation}.
     *
     * @param table of constructor signatures and method handles
     */
    protected void setConstructorLookup(Map<MethodType, ConstructorAndHandle> table) {
        // Freeze the table as the constructor lookup
        constructorLookup = Collections.unmodifiableMap(table);
    }

    // Getters for special methods -----------------------------------
    /*
     * There is one of these methods for each member of the SpecialMethod
     * enum. Each returns a method handle, of the appropriate signature for
     * the special method, that designates an implementation applicable to
     * an object that has this Representation, according to its class and
     * the type registry. We treat these return values roughly as CPython
     * does the "slots" of its type object.
     *
     * Some of these come from values cached on the Representation itself,
     * while others return a generic handle that performs a lookup in the
     * dictionary of the type when invoked. We can choose between the
     * strategies to trade speed and size of Representation objects. To
     * switch a "generic" special method into a caching one, define a cache
     * variable and change the method to return it. The corresponding
     * SpecialMethod will discover the cache automatically and the type
     * system will use it.
     *
     * Those accessors that return only a generic handle do not depend on
     * the Representation (this) and could be declared static. (The IDE may
     * inform you of this.) Client code should then change to call the
     * accessor as a static method in Representation, without calling
     * PyType.getRepresentation(). That would be an improvement, but is
     * premature until the choice of what to cache settles down.
     */

    /**
     * Return a matching implementation of {@code __repr__} with signature
     * {@link Signature#UNARY}, supporting built-in {@code repr()}.
     *
     * @return handle on {@code __repr__} with signature
     *     {@link Signature#UNARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_repr() { return SpecialMethod.op_repr.generic; }

    /**
     * Return a matching implementation of {@code __hash__} with signature
     * {@link Signature#LEN}, supporting object hashing and the built-in
     * {@code hash()}.
     *
     * @return handle on {@code __hash__} with signature
     *     {@link Signature#LEN}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_hash() { return SpecialMethod.op_hash.generic; }

    /**
     * Return a matching implementation of {@code __call__} with signature
     * {@link Signature#CALL}, which supports calling an object.
     *
     * @return handle on {@code __call__} with signature
     *     {@link Signature#CALL}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_call() { return SpecialMethod.op_call.generic; }

    /**
     * Return a matching implementation of {@code __str__} with signature
     * {@link Signature#UNARY}, supporting built-in {@code str()}.
     *
     * @return handle on {@code __str__} with signature
     *     {@link Signature#UNARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_str() { return SpecialMethod.op_str.generic; }

    /**
     * Return a matching implementation of {@code __getattribute__} with
     * signature {@link Signature#GETATTR}, which implements attribute get.
     *
     * @return handle on {@code __getattribute__} with signature
     *     {@link Signature#GETATTR}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_getattribute() { return SpecialMethod.op_getattribute.generic; }

    /**
     * Return a matching implementation of {@code __getattr__} with
     * signature {@link Signature#GETATTR}, the fall-back attribute get.
     *
     * @return handle on {@code __getattr__} with signature
     *     {@link Signature#GETATTR}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_getattr() { return SpecialMethod.op_getattr.generic; }

    /**
     * Return a matching implementation of {@code __setattr__} with
     * signature {@link Signature#SETATTR}, which implements attribute set.
     *
     * @return handle on {@code __setattr__} with signature
     *     {@link Signature#SETATTR}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_setattr() { return SpecialMethod.op_setattr.generic; }

    /**
     * Return a matching implementation of {@code __delattr__} with
     * signature {@link Signature#DELATTR}, which implements attribute
     * deletion.
     *
     * @return handle on {@code __delattr__} with signature
     *     {@link Signature#DELATTR}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_delattr() { return SpecialMethod.op_delattr.generic; }

    /**
     * Return a matching implementation of {@code __lt__} with signature
     * {@link Signature#BINARY}, the {@code <} operation.
     *
     * @return handle on {@code __lt__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_lt() { return SpecialMethod.op_lt.generic; }

    /**
     * Return a matching implementation of {@code __le__} with signature
     * {@link Signature#BINARY}, the {@code <=} operation.
     *
     * @return handle on {@code __le__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_le() { return SpecialMethod.op_le.generic; }

    /**
     * Return a matching implementation of {@code __eq__} with signature
     * {@link Signature#BINARY}, the {@code ==} operation.
     *
     * @return handle on {@code __eq__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_eq() { return SpecialMethod.op_eq.generic; }

    /**
     * Return a matching implementation of {@code __ne__} with signature
     * {@link Signature#BINARY}, the {@code !=} operation.
     *
     * @return handle on {@code __ne__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_ne() { return SpecialMethod.op_ne.generic; }

    /**
     * Return a matching implementation of {@code __gt__} with signature
     * {@link Signature#BINARY}, the {@code >} operation.
     *
     * @return handle on {@code __gt__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_gt() { return SpecialMethod.op_gt.generic; }

    /**
     * Return a matching implementation of {@code __ge__} with signature
     * {@link Signature#BINARY}, the {@code >=} operation.
     *
     * @return handle on {@code __ge__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_ge() { return SpecialMethod.op_ge.generic; }

    /**
     * Return a matching implementation of {@code __iter__} with signature
     * {@link Signature#UNARY}, get an iterator, supporting built-in
     * {@code iter()}.
     *
     * @return handle on {@code __iter__} with signature
     *     {@link Signature#UNARY}, get an iterator.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_iter() { return SpecialMethod.op_iter.generic; }

    /**
     * Return a matching implementation of {@code __next__} with signature
     * {@link Signature#UNARY}, advance an iterator, supporting built-in
     * {@code next()}.
     *
     * @return handle on {@code __next__} with signature
     *     {@link Signature#UNARY}, advance an iterator.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_next() { return SpecialMethod.op_next.generic; }

    /**
     * Return a matching implementation of {@code __get__} with signature
     * {@link Signature#DESCRGET}, which implements descriptor
     * {@code __get__}.
     *
     * @return handle on {@code __get__} with signature
     *     {@link Signature#DESCRGET}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_get() { return SpecialMethod.op_get.generic; }

    /**
     * Return a matching implementation of {@code __set__} with signature
     * {@link Signature#SETITEM}, which implements descriptor
     * {@code __set__}.
     *
     * @return handle on {@code __set__} with signature
     *     {@link Signature#SETITEM}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_set() { return SpecialMethod.op_set.generic; }

    /**
     * Return a matching implementation of {@code __delete__} with signature
     * {@link Signature#DELITEM}, which implements descriptor
     * {@code __delete__}.
     *
     * @return handle on {@code __delete__} with signature
     *     {@link Signature#DELITEM}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_delete() { return SpecialMethod.op_delete.generic; }

    /**
     * Return a matching implementation of {@code __init__} with signature
     * {@link Signature#INIT}, which initialises an object after
     * {@code __new__}.
     *
     * @return handle on {@code __init__} with signature
     *     {@link Signature#INIT}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_init() { return SpecialMethod.op_init.generic; }

    // __new__ is not enumerated here (not instance method)
    // __del__ is not in our implementation

    /**
     * Return a matching implementation of {@code __await__} with signature
     * {@link Signature#UNARY}.
     *
     * @return handle on {@code __await__} with signature
     *     {@link Signature#UNARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_await() { return SpecialMethod.op_await.generic; }

    /**
     * Return a matching implementation of {@code __aiter__} with signature
     * {@link Signature#UNARY}.
     *
     * @return handle on {@code __aiter__} with signature
     *     {@link Signature#UNARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_aiter() { return SpecialMethod.op_aiter.generic; }

    /**
     * Return a matching implementation of {@code __anext__} with signature
     * {@link Signature#UNARY}.
     *
     * @return handle on {@code __anext__} with signature
     *     {@link Signature#UNARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_anext() { return SpecialMethod.op_anext.generic; }

    /**
     * Return a matching implementation of {@code __radd__} with signature
     * {@link Signature#BINARY}, the reflected {@code +} operation.
     *
     * @return handle on {@code __radd__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_radd() { return SpecialMethod.op_radd.generic; }

    /**
     * Return a matching implementation of {@code __add__} with signature
     * {@link Signature#BINARY}, the {@code +} operation.
     *
     * @return handle on {@code __add__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_add() { return SpecialMethod.op_add.generic; }

    /**
     * Return a matching implementation of {@code __rsub__} with signature
     * {@link Signature#BINARY}, the reflected {@code -} operation.
     *
     * @return handle on {@code __rsub__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_rsub() { return SpecialMethod.op_rsub.generic; }

    /**
     * Return a matching implementation of {@code __sub__} with signature
     * {@link Signature#BINARY}, the {@code -} operation.
     *
     * @return handle on {@code __sub__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_sub() { return SpecialMethod.op_sub.generic; }

    /**
     * Return a matching implementation of {@code __rmul__} with signature
     * {@link Signature#BINARY}, the reflected {@code *} operation.
     *
     * @return handle on {@code __rmul__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_rmul() { return SpecialMethod.op_rmul.generic; }

    /**
     * Return a matching implementation of {@code __mul__} with signature
     * {@link Signature#BINARY}, the {@code *} operation.
     *
     * @return handle on {@code __mul__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_mul() { return SpecialMethod.op_mul.generic; }

    /**
     * Return a matching implementation of {@code __rmod__} with signature
     * {@link Signature#BINARY}, the reflected {@code %} operation.
     *
     * @return handle on {@code __rmod__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_rmod() { return SpecialMethod.op_rmod.generic; }

    /**
     * Return a matching implementation of {@code __mod__} with signature
     * {@link Signature#BINARY}, the {@code %} operation.
     *
     * @return handle on {@code __mod__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_mod() { return SpecialMethod.op_mod.generic; }

    /**
     * Return a matching implementation of {@code __rdivmod__} with
     * signature {@link Signature#BINARY}, the reflected {@code divmod}
     * operation.
     *
     * @return handle on {@code __rdivmod__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_rdivmod() { return SpecialMethod.op_rdivmod.generic; }

    /**
     * Return a matching implementation of {@code __divmod__} with signature
     * {@link Signature#BINARY}, the {@code divmod} operation.
     *
     * @return handle on {@code __divmod__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_divmod() { return SpecialMethod.op_divmod.generic; }

    /**
     * Return a matching implementation of {@code __rpow__} with signature
     * {@link Signature#BINARY}, the reflected {@code pow} operation. (The
     * signature is not not {@link Signature#TERNARY} like {@code #op_pow}
     * since only an infix operation can be reflected).
     *
     * @return handle on {@code __rpow__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_rpow() { return SpecialMethod.op_rpow.generic; }

    /**
     * Return a matching implementation of {@code __pow__} with signature
     * {@link Signature#TERNARY}, the {@code **} operation and built-in
     * {@code pow()}.
     *
     * @return handle on {@code __pow__} with signature
     *     {@link Signature#TERNARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_pow() { return SpecialMethod.op_pow.generic; }

    /**
     * Return a matching implementation of {@code __neg__} with signature
     * {@link Signature#UNARY}, the unary {@code -} operation.
     *
     * @return handle on {@code __neg__} with signature
     *     {@link Signature#UNARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_neg() { return SpecialMethod.op_neg.generic; }

    /**
     * Return a matching implementation of {@code __pos__} with signature
     * {@link Signature#UNARY}, the unary {@code +} operation.
     *
     * @return handle on {@code __pos__} with signature
     *     {@link Signature#UNARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_pos() { return SpecialMethod.op_pos.generic; }

    /**
     * Return a matching implementation of {@code __abs__} with signature
     * {@link Signature#UNARY}, supporting built-in {@code abs()}.
     *
     * @return handle on {@code __abs__} with signature
     *     {@link Signature#UNARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_abs() { return SpecialMethod.op_abs.generic; }

    /**
     * Return a matching implementation of {@code __bool__} with signature
     * {@link Signature#PREDICATE}, conversion to a truth value.
     *
     * @return handle on {@code __bool__} with signature
     *     {@link Signature#PREDICATE}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_bool() { return SpecialMethod.op_bool.generic; }

    /**
     * Return a matching implementation of {@code __invert__} with signature
     * {@link Signature#UNARY}, the unary {@code ~} operation.
     *
     * @return handle on {@code __invert__} with signature
     *     {@link Signature#UNARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_invert() { return SpecialMethod.op_invert.generic; }

    /**
     * Return a matching implementation of {@code __rlshift__} with
     * signature {@link Signature#BINARY}, the reflected {@code <<}
     * operation.
     *
     * @return handle on {@code __rlshift__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_rlshift() { return SpecialMethod.op_rlshift.generic; }

    /**
     * Return a matching implementation of {@code __lshift__} with signature
     * {@link Signature#BINARY}, the {@code <<} operation.
     *
     * @return handle on {@code __lshift__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_lshift() { return SpecialMethod.op_lshift.generic; }

    /**
     * Return a matching implementation of {@code __rrshift__} with
     * signature {@link Signature#BINARY}, the reflected {@code >>}
     * operation.
     *
     * @return handle on {@code __rrshift__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_rrshift() { return SpecialMethod.op_rrshift.generic; }

    /**
     * Return a matching implementation of {@code __rshift__} with signature
     * {@link Signature#BINARY}, the {@code >>} operation.
     *
     * @return handle on {@code __rshift__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_rshift() { return SpecialMethod.op_rshift.generic; }

    /**
     * Return a matching implementation of {@code __rand__} with signature
     * {@link Signature#BINARY}, the reflected {@code &} operation.
     *
     * @return handle on {@code __rand__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_rand() { return SpecialMethod.op_rand.generic; }

    /**
     * Return a matching implementation of {@code __and__} with signature
     * {@link Signature#BINARY}, the {@code &} operation.
     *
     * @return handle on {@code __and__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_and() { return SpecialMethod.op_and.generic; }

    /**
     * Return a matching implementation of {@code __rxor__} with signature
     * {@link Signature#BINARY}, the reflected {@code ^} operation.
     *
     * @return handle on {@code __rxor__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_rxor() { return SpecialMethod.op_rxor.generic; }

    /**
     * Return a matching implementation of {@code __xor__} with signature
     * {@link Signature#BINARY}, the {@code ^} operation.
     *
     * @return handle on {@code __xor__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_xor() { return SpecialMethod.op_xor.generic; }

    /**
     * Return a matching implementation of {@code __ror__} with signature
     * {@link Signature#BINARY}, the reflected {@code |} operation.
     *
     * @return handle on {@code __ror__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_ror() { return SpecialMethod.op_ror.generic; }

    /**
     * Return a matching implementation of {@code __or__} with signature
     * {@link Signature#BINARY}, the {@code |} operation.
     *
     * @return handle on {@code __or__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_or() { return SpecialMethod.op_or.generic; }

    /**
     * Return a matching implementation of {@code __int__} with signature
     * {@link Signature#UNARY}, conversion to an integer value.
     *
     * @return handle on {@code __int__} with signature
     *     {@link Signature#UNARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_int() { return SpecialMethod.op_int.generic; }

    /**
     * Return a matching implementation of {@code __float__} with signature
     * {@link Signature#UNARY}, conversion to a {@code float} value.
     *
     * @return handle on {@code __float__} with signature
     *     {@link Signature#UNARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_float() { return SpecialMethod.op_float.generic; }

    // in-place: unexplored territory

    /**
     * Return a matching implementation of {@code __iadd__} with signature
     * {@link Signature#BINARY}, the {@code +=} operation.
     *
     * @return handle on {@code __iadd__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_iadd() { return SpecialMethod.op_iadd.generic; }

    /**
     * Return a matching implementation of {@code __isub__} with signature
     * {@link Signature#BINARY}, the {@code -=} operation.
     *
     * @return handle on {@code __isub__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_isub() { return SpecialMethod.op_isub.generic; }

    /**
     * Return a matching implementation of {@code __imul__} with signature
     * {@link Signature#BINARY}, the {@code *=} operation.
     *
     * @return handle on {@code __imul__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_imul() { return SpecialMethod.op_imul.generic; }

    /**
     * Return a matching implementation of {@code __imod__} with signature
     * {@link Signature#BINARY}, the {@code %=} operation.
     *
     * @return handle on {@code __imod__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_imod() { return SpecialMethod.op_imod.generic; }

    /**
     * Return a matching implementation of {@code __iand__} with signature
     * {@link Signature#BINARY}, the {@code &=} operation.
     *
     * @return handle on {@code __iand__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_iand() { return SpecialMethod.op_iand.generic; }

    /**
     * Return a matching implementation of {@code __ixor__} with signature
     * {@link Signature#BINARY}, the {@code ^=} operation.
     *
     * @return handle on {@code __ixor__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_ixor() { return SpecialMethod.op_ixor.generic; }

    /**
     * Return a matching implementation of {@code __ior__} with signature
     * {@link Signature#BINARY}, the {@code |=} operation.
     *
     * @return handle on {@code __ior__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_ior() { return SpecialMethod.op_ior.generic; }

    /**
     * Return a matching implementation of {@code __rfloordiv__} with
     * signature {@link Signature#BINARY}, the reflected {@code //}
     * operation.
     *
     * @return handle on {@code __rfloordiv__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_rfloordiv() { return SpecialMethod.op_rfloordiv.generic; }

    /**
     * Return a matching implementation of {@code __floordiv__} with
     * signature {@link Signature#BINARY}, the {@code //} operation.
     *
     * @return handle on {@code __floordiv__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_floordiv() { return SpecialMethod.op_floordiv.generic; }

    /**
     * Return a matching implementation of {@code __rtruediv__} with
     * signature {@link Signature#BINARY}, the reflected {@code /}
     * operation.
     *
     * @return handle on {@code __rtruediv__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_rtruediv() { return SpecialMethod.op_rtruediv.generic; }

    /**
     * Return a matching implementation of {@code __truediv__} with
     * signature {@link Signature#BINARY}, the {@code /} operation.
     *
     * @return handle on {@code __truediv__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_truediv() { return SpecialMethod.op_truediv.generic; }

    /**
     * Return a matching implementation of {@code __ifloordiv__} with
     * signature {@link Signature#BINARY}, the {@code //=} operation.
     *
     * @return handle on {@code __ifloordiv__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_ifloordiv() { return SpecialMethod.op_ifloordiv.generic; }

    /**
     * Return a matching implementation of {@code __itruediv__} with
     * signature {@link Signature#BINARY}, the {@code /=} operation.
     *
     * @return handle on {@code __itruediv__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_itruediv() { return SpecialMethod.op_itruediv.generic; }

    /**
     * Return a matching implementation of {@code __index__} with signature
     * {@link Signature#UNARY}, implementing lossless conversion to a Python
     * {@code int}.
     *
     * @return handle on {@code __index__} with signature
     *     {@link Signature#UNARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_index() { return SpecialMethod.op_index.generic; }

    /**
     * Return a matching implementation of {@code __rmatmul__} with
     * signature {@link Signature#BINARY}, the reflected {@code @}
     * operation.
     *
     * @return handle on {@code __rmatmul__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_rmatmul() { return SpecialMethod.op_rmatmul.generic; }

    /**
     * Return a matching implementation of {@code __matmul__} with signature
     * {@link Signature#BINARY}, the {@code @} (matrix multiply) operation.
     *
     * @return handle on {@code __matmul__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_matmul() { return SpecialMethod.op_matmul.generic; }

    /**
     * Return a matching implementation of {@code __imatmul__} with
     * signature {@link Signature#BINARY}, the {@code @=} (matrix multiply
     * in place) operation.
     *
     * @return handle on {@code __imatmul__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_imatmul() { return SpecialMethod.op_imatmul.generic; }

    /*
     * Note that CPython repeats for "mappings" the following "sequence"
     * slots, and slots for __add_ and __mul__, __iadd_ and __imul__, but
     * that we do not need to.
     */
    /**
     * Return a matching implementation of {@code __len__} with signature
     * {@link Signature#LEN}, supporting built-in {@code len()}.
     *
     * @return handle on {@code __len__} with signature
     *     {@link Signature#LEN}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_len() { return SpecialMethod.op_len.generic; }

    /**
     * Return a matching implementation of {@code __getitem__} with
     * signature {@link Signature#BINARY}, get object at index.
     *
     * @return handle on {@code __getitem__} with signature
     *     {@link Signature#BINARY}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_getitem() { return SpecialMethod.op_getitem.generic; }

    /**
     * Return a matching implementation of {@code __setitem__} with
     * signature {@link Signature#SETITEM}, set object at index.
     *
     * @return handle on {@code __setitem__} with signature
     *     {@link Signature#SETITEM}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_setitem() { return SpecialMethod.op_setitem.generic; }

    /**
     * Return a matching implementation of {@code __delitem__} with
     * signature {@link Signature#DELITEM}, delete object from index.
     *
     * @return handle on {@code __delitem__} with signature
     *     {@link Signature#DELITEM}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_delitem() { return SpecialMethod.op_delitem.generic; }

    /**
     * Return a matching implementation of {@code __contains__} with
     * signature {@link Signature#BINARY_PREDICATE}, implementing keyword
     * {@code in}.
     *
     * @return handle on {@code __contains__} with signature
     *     {@link Signature#BINARY_PREDICATE}.
     */
    @SuppressWarnings("static-method")
    public MethodHandle op_contains() { return SpecialMethod.op_contains.generic; }
}
