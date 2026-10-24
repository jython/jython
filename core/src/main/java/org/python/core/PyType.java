// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.util.EnumSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.python.kernel.Representation;
import org.python.kernel.TypeFactory.Clash;
import org.python.types.FastCall;
import org.python.types.Feature;
import org.python.types.Constructible;
import org.python.types.TypeFlag;
import org.python.types.TypeSpec;
import org.python.types.WithClass;

/**
 * Each Python {@code type} object is implemented by an <i>instance</i>
 * in Java of {@code PyType}. As {@code PyType} is {@code abstract},
 * that means each is an instance of a subclass of {@code PyType}. Only
 * {@code PyType} is public API. The built-ins {@code object},
 * {@code type}, {@code str}, {@code int}, etc. and any class defined in
 * Python, are instances in Java of a subclass of {@code PyType}.
 * <p>
 * {@code PyType} also offers type object lookup and creation methods,
 * for example {@link PyType#fromSpec(TypeSpec)}. For this purpose it
 * holds the single static instance of the Python type factory, which
 * comes into being upon first use of the {@code PyType} class.
 */
public interface PyType extends Constructible, WithClass, FastCall {

    /** Logger for (the public face of) the type system. */
    static final Logger logger = LoggerFactory.getLogger(PyType.class);

    /**
     * Return the name of the type. Equivalent to getting the type's
     * {@code __name__} attribute, which may be changed by assignment in a
     * mutable type.
     *
     * @return the name of the type
     */
    String getName();

    /**
     * Return the qualified name of the type. Equivalent to getting the
     * type's {@code __qualname__} attribute, which may be changed by
     * assignment in a mutable type.
     *
     * @return the qualified name of the type
     */
    String getQualName();

    /**
     * A copy of the sequence of bases specified for the type, essentially
     * {@code __bases__}.
     *
     * @return the sequence of bases
     */
    PyType[] getBases();

    /**
     * Return the (best) base type of this type, essentially
     * {@code __base__}. In the case of single inheritance, the choice is
     * obvious. In multiple inheritance, Python makes a somewhat subtle
     * choice. requires a particular care.
     *
     * @return the "best base" type of this type
     */
    PyType getBase();

    /**
     * {@inheritDoc} The type of every type object is {@code type}, except
     * for those created by a <i>metatype</i>, where it is that metatype.
     * (Even the type of {@code type} is {@code type}.)
     */
    @Override
    PyType getType();

    /**
     * Return a copy of the MRO of this type.
     *
     * @return a copy of the MRO of this type
     */
    PyType[] getMRO();

    // C-API Equivalents ---------------------------------------------

    /*
     * Java API that is roughly equivalent to the C-API as might be used in
     * the creation of extension types, Python modules in Java, or
     * applications that embed Python requiring more than an encapsulated
     * interpreter.
     */

    /**
     * {@code true} iff the type of {@code o} is a Python sub-type of
     * {@code this} (including exactly {@code this} type). This is likely to
     * be used in the form:<pre>
     * if(!PyUnicode.TYPE.check(oName)) throw ...
     * </pre>
     *
     * @param o object to test
     * @return {@code true} iff {@code o} is of a sub-type of this type
     */
    boolean check(Object o);

    /**
     * {@code true} iff the Python type of {@code o} is exactly {@code this}
     * type. This is likely to be used in the form:<pre>
     * if(!PyUnicode.TYPE.checkExact(oName)) throw ...
     * </pre>
     *
     * @param o object to test
     * @return {@code true} iff {@code o} is exactly of this type
     */
    boolean checkExact(Object o);

    /**
     * Look for a name, returning the entry directly from the first
     * dictionary along the MRO containing key {@code name}. This may be a
     * descriptor, but no {@code __get__} takes place on it: the descriptor
     * itself will be returned. This method does not throw an exception if
     * the name is not found, but returns {@code null} like a
     * {@code Map.get}
     *
     * @param name to look up, must be exactly a {@code str}
     * @return dictionary entry or {@code null} if not found
     */
    // Compare CPython _PyType_Lookup in typeobject.c
    // and find_name_in_mro in typeobject.c
    Object lookup(String name);

    /**
     * The features of this type as a set.
     *
     * @return features of this type
     */
    // Compare CPython PyType_GetFlags
    EnumSet<TypeFlag> getFeatures();

    /**
     * Test for possession of a specified feature.
     *
     * @param feature to check for
     * @return whether present on the type
     */
    boolean hasFeature(TypeFlag feature);

    /**
     * Return true if and only if this is a mutable type. The attributes of
     * a mutable type may be changed, although it will manage that change
     * according to rules of its own. An immutable type object does not
     * allow attribute assignment: the value of an attribute once observed
     * remains valid for the lifetime of the run time system.
     *
     * @return {@code true} iff this is a mutable type
     */
    boolean isMutable();

    /**
     * Fast check that an object of this type is a sequence, that is, its
     * type declares itself to have {@link Feature#SEQUENCE_PROTOCOL} in a
     * Java definition. It will also define {@code __getitem__} that should
     * accept and index or slice.
     *
     * @return target is a sequence
     */
    // Compare CPython PySequence_Check (on instance) in abstract.c
    boolean isSequence();

    /**
     * Fast check that an object of this type is a mapping, that is, it
     * defines {@code __getitem__} and does not declare itself to have
     * {@link Feature#SEQUENCE_PROTOCOL} in a Java definition.
     *
     * @return target is a mapping
     */
    // Compare CPython PyMapping_Check (on instance) in abstract.c
    boolean isMapping();

    /**
     * Fast check that an object of this type is iterable (defines
     * {@code __iter__}).
     *
     * @return target is a iterable with {@code __iter__}
     */
    boolean isIterable();

    /**
     * Fast check that an object of this type is an iterator (defines
     * {@code __next__}).
     *
     * @return target is a an iterator
     */
    boolean isIterator();

    /**
     * Fast check that an object of this type is a descriptor (defines
     * {@code __get__}).
     *
     * @return target is a descriptor
     */
    boolean isDescr();

    /**
     * Fast check that an object of this type is a data descriptor (defines
     * {@code __set__} or {@code __delete__}).
     *
     * @return target is a data descriptor
     */
    boolean isDataDescr();

    /**
     * Fast check that instances of the type are a method descriptors,
     * meaning that they take a {@code self} argument that may be supplied
     * "loose" when calling them as a method. Types defined in Java may
     * declare this in their specification.
     * <p>
     * This method is equivalent to reading the flag
     * {@code Py_TPFLAGS_METHOD_DESCRIPTOR} described in <a
     * href=https://peps.python.org/pep-0590/#descriptor-behavior>
     * PEP-590</a>. If {@code isMethodDescr()} returns {@code true} for
     * {@code type(func)}, then:
     * <ul>
     * <li>{@code func.__get__(obj, cls)(*args, **kwds)} (with {@code {@code
     * obj}} not None) must be equivalent to
     * {@code func(obj, *args, **kwds)}.</li>
     * <li>{@code func.__get__(None, cls)(*args, **kwds)} must be equivalent
     * to {@code func(*args, **kwds)}.</li>
     * </ul>
     *
     * @return target is a method descriptor
     */
    boolean isMethodDescr();

    /**
     * Determine if this type is a Python sub-type of {@code b} (if
     * {@code b} is on the MRO of this type).
     *
     * @param b to test
     * @return {@code true} if {@code this} is a sub-type of {@code b}
     */
    // Compare CPython PyType_IsSubtype in typeobject.c
    boolean isSubTypeOf(PyType b);

    // Java representations ------------------------------------------

    // XXX Are these too much oriented towards our implementation?
    /*
     * Does the relationship to a Java class have relevance outside the
     * run-time? Probably.
     */

    /**
     * The primary Java representation class of the type. The primary class
     * is {@link #selfClasses()}{@code [0]}.
     * <p>
     * In many cases, the primary class is the base class in Java of all
     * representations of the type and its subclasses in Python, and the
     * only entry in {@code selfClasses()} but it is not safe to assume that
     * always.
     *
     * @return primary class of the type
     */
    Class<?> javaClass();

    /**
     * A particular subclass (in Java) of the primary representation class
     * that is to be used as the base of representations of subclasses of
     * the type in Python. That is, the canonical class is a subclass of
     * {@link #selfClasses()}{@code [0]}.
     * <p>
     * In many cases, the canonical class is exactly the primary (and only)
     * representation class, but it is not safe to assume so always. For
     * {@code type} itself, the canonical class is a kernel class called
     * {@code ReplaceableType}, and for subclasses defined in Python it may
     * be the canonical representation of an ancestor Python class.
     *
     * @return the canonical Java representation class of {@code self}
     */
    Class<?> canonicalClass();

    /**
     * An immutable list of every Java class that was named as primary,
     * adopted or accepted in the specification of this type, in order.
     * These are the base Java classes of objects that can legitimately be
     * presented as {@code self} to methods of the type.
     *
     * @return the bases of classes allowed as {@code self}
     */
    List<Class<?>> selfClasses();

    // static methods -----------------------------------------------

    /**
     * Return the Python type of {@code type} objects.
     *
     * @return {@code type}
     */
    public static PyType TYPE() {
        /*
         * The object "type" is created deep in the type system, during
         * bootstrapping. The next statement will block any thread other than
         * the one currently bootstrapping the type system, until that process
         * is over.
         */
        return TypeSystem.TYPE_type;
    }

    /**
     * Determine (or create if necessary) the Python type for the given
     * object.
     *
     * @param o for which a type is required
     * @return the type
     */
    public static PyType of(Object o) {
        Representation rep = TypeSystem.registry.get(o.getClass());
        return rep.pythonType(o);
    }

    /**
     * Create a Python type according to the specification. This is the
     * normal way to create any Python type that is defined in Java: the
     * Python built-ins or user-defined types. The minimal idiom is:<pre>
     * class MyType {
     *     static PyType TYPE = PyType.fromSpec(
     *         new TypeSpec("mypackage.mytype",
     *                      MethodHandles.lookup());
     * }
     * </pre> The type system will add descriptors for the members of the
     * class identified by annotation or reserved names for exposure to
     * Python.
     * <p>
     * Exceptions thrown during the creation of the type propagate out to
     * the caller. If possible they are presented as Python exceptions. In
     * that case, there will usually be no change to the state of the type
     * system.
     *
     * @param spec specifying the new type
     * @return the new type
     * @throws PyBaseException from specification errors or failures during
     *     construction
     */
    public static PyType fromSpec(TypeSpec spec) throws PyBaseException {
        try {
            return TypeSystem.factory.fromSpec(spec);
        } catch (Clash clash) {
            // Clash is an internal type: translate to TypeError
            logger.atInfo().log(clash.toString());
            throw PyErr.format(PyExc.TypeError, clash.getMessage());
        }
    }
}
