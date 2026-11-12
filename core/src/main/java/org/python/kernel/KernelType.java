// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.kernel;

import java.util.EnumSet;
import java.util.List;

import org.python.core.PyType;
import org.python.types.TypeFlag;
import org.python.types.TypeSpec;

/**
 * A base class for Python type objects.
 * <p>
 * In the layered architecture of the Python type object, this class
 * takes responsibility for the basic properties of a type towards use
 * from Java. It contains the apparatus to make the type "Java-ready".
 */
public abstract class KernelType extends Representation implements PyType {

    /**
     * Feature flags collecting various boolean traits of this type, such as
     * immutability or being a subclass of {@code int}. Some of these come
     * fairly directly from the {@link TypeSpec} (where used to define the
     * type) and others are observed during construction of the type.
     */
    // Compare CPython tp_flags in object.h
    protected final EnumSet<TypeFlag> features = EnumSet.noneOf(TypeFlag.class);

    /**
     * Kernel feature flags collecting various traits of this type that are
     * private to the implementation, such as defining a certain special
     * method.
     */
    // Compare CPython tp_flags in object.h
    protected final EnumSet<KernelTypeFlag> kernelFeatures = EnumSet.noneOf(KernelTypeFlag.class);

    /**
     * The {@code __base__} of this type. The {@code __base__} is a type
     * from the {@code __bases__}, but its choice is determined by
     * implementation details.
     * <p>
     * It is the type earliest on the MRO after the current type, whose
     * implementation contains all the members necessary to implement the
     * current type. It is {@code null} (exposed as {@code None}) only on
     * the type object for {@code object}.
     */
    protected BaseType base;

    /**
     * The {@code __bases__} of this type, which are the types named in
     * heading of the Python {@code class} definition, or just
     * {@code object} if none are named, or an empty array in the special
     * case of {@code object} itself.
     */
    protected BaseType[] bases;

    /**
     * Constructor used by (permitted) subclasses of {@code PyType}.
     *
     * @param javaClass implementing Python instances of the type
     * @param bases of the new type
     */
    protected KernelType(Class<?> javaClass, BaseType[] bases) {
        super(javaClass);
        /*
         * These assertions mainly check our assumptions about the needs of
         * sub-types. They are retained only in testing.
         */
        assert javaClass != null || this instanceof AdoptiveType;
        assert bases != null;

        this.bases = bases;
        this.base = bases.length > 0 ? bases[0] : null;
    }

    /**
     * A copy of the sequence of bases specified for the type, essentially
     * {@code __bases__}.
     *
     * @return the sequence of bases
     */
    @Override
    public PyType[] getBases() { return bases.clone(); }

    /**
     * Return the (best) base type of this type, essentially
     * {@code __base__}. In the case of single inheritance, the choice is
     * obvious. In multiple inheritance, Python makes a somewhat subtle
     * choice. requires a particular care.
     *
     * @return the "best base" type of this type
     */
    @Override
    public PyType getBase() { return base; }

    @Override
    public PyType getType() { return typeType(); }

    /**
     * Return a copy of the MRO of this type.
     *
     * @return a copy of the MRO of this type
     */
    @Override
    public abstract PyType[] getMRO();

    @Override
    public String toString() { return "<class '" + getName() + "'>"; }

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
    @Override
    public abstract Object lookup(String name);

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
    @Override
    public boolean check(Object o) {
        PyType t = PyType.of(o);
        return t == this || t.isSubTypeOf(this);
    }

    /**
     * {@code true} iff the Python type of {@code o} is exactly {@code this}
     * type. This is likely to be used in the form:<pre>
     * if(!PyUnicode.TYPE.checkExact(oName)) throw ...
     * </pre>
     *
     * @param o object to test
     * @return {@code true} iff {@code o} is exactly of this type
     */
    @Override
    public boolean checkExact(Object o) { return PyType.of(o) == this; }

    @Override
    public final boolean hasFeature(TypeFlag feature) { return features.contains(feature); }

    /**
     * Test for possession of a specified kernel feature. Kernel features
     * are not public API.
     *
     * @param feature to check for
     * @return whether present
     */
    public final boolean hasFeature(KernelTypeFlag feature) {
        return kernelFeatures.contains(feature);
    }

    @Override
    public boolean hasFeature(Object x, KernelTypeFlag feature) {
        return kernelFeatures.contains(feature);
    }

    /**
     * Return true if and only if this is a mutable type. The attributes of
     * a mutable type may be changed, although it will manage that change
     * according to rules of its own. An immutable type object does not
     * allow attribute assignment: the value of an attribute once observed
     * remains valid for the lifetime of the run time system.
     *
     * @return {@code true} iff this is a mutable type
     */
    @Override
    public abstract boolean isMutable();

    @Override
    public boolean isSequence() { return features.contains(TypeFlag.SEQUENCE_PROTOCOL); }

    @Override
    public boolean isMapping() {
        return kernelFeatures.contains(KernelTypeFlag.HAS_GETITEM)
                && !features.contains(TypeFlag.SEQUENCE_PROTOCOL);
    }

    /**
     * Fast check that an object of this type is iterable (defines
     * {@code __iter__}).
     *
     * @return target is a iterable with {@code __iter__}
     */
    @Override
    public boolean isIterable() { return kernelFeatures.contains(KernelTypeFlag.HAS_ITER); }

    /**
     * Fast check that an object of this type is an iterator (defines
     * {@code __next__}).
     *
     * @return target is a an iterator
     */
    @Override
    public boolean isIterator() { return kernelFeatures.contains(KernelTypeFlag.HAS_NEXT); }

    /**
     * Fast check that an object of this type is a descriptor (defines
     * {@code __get__}).
     *
     * @return target is a descriptor
     */
    @Override
    public boolean isDescr() { return kernelFeatures.contains(KernelTypeFlag.HAS_GET); }

    /**
     * Fast check that an object of this type is a data descriptor (defines
     * {@code __set__} or {@code __delete__}).
     *
     * @return target is a data descriptor
     */
    @Override
    public boolean isDataDescr() {
        return kernelFeatures.contains(KernelTypeFlag.HAS_SET)
                || kernelFeatures.contains(KernelTypeFlag.HAS_DELETE);
    }

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
    @Override
    public boolean isMethodDescr() { return features.contains(TypeFlag.METHOD_DESCR); }

    /**
     * Check that this type is Python-ready, that is, it has been marked as
     * fit to be handled outside the type system. An immutable type becomes
     * ready at the end of construction and stays that way. A mutable type
     * may go through periods when it is not Python-ready, while it is
     * internally inconsistent, or inconsistent with related data. Correct
     * synchronisation will ensure it does not escape in that condition.
     * <p>
     * Ideally, this method only appears in {@code assert} statements and
     * unit tests. Operations on types that are not Python-ready should
     * block until they are. If we have to consult this as a basis for
     * run-time decisions, something is amiss with the design.
     *
     * @return target is a Python-ready
     */
    // Compare CPython PySequence_Check (on instance) in abstract.c
    public boolean isReady() { return kernelFeatures.contains(KernelTypeFlag.READY); }

    /**
     * An immutable list of the {@link Representation}s of this type. These
     * are the representations of the primary or adopted classes in the
     * specification of this type, in order.
     *
     * @return the representations of {@code self}
     */
    public abstract List<Representation> representations();

    /**
     * An immutable list of every Java class that was named as primary,
     * adopted or accepted in the specification of this type, in order.
     * These are the base Java classes of objects that can legitimately be
     * presented as {@code self} to methods of the type.
     * <p>
     * These are also the classes of the {@link #representations()}, in
     * order, except that the non-representation classes also accepted as
     * {@code self} (if any) are appended. A method descriptor in an
     * adoptive type uses this list to ensure it has an implementation for
     * each self class.
     *
     * @return the bases of classes allowed as {@code self}
     */
    @Override
    public abstract List<Class<?>> selfClasses();

    /**
     * A particular subclass (in Java) of the primary representation class
     * that is to be used as the base of representations of subclasses in
     * Python. That is, the canonical class is a subclass of
     * {@link #selfClasses()}{@code [0]}.
     * <p>
     * In many cases, the canonical class is exactly the primary (and only)
     * representation class, but it is not safe to assume so always. For
     * {@code type} itself, the canonical class is called
     * {@code SimpleType}, and for subclasses defined in Python it may be
     * the canonical representation of one of an ancestor class.
     *
     * @return the canonical Java representation class of {@code self}
     */
    @Override
    public abstract Class<?> canonicalClass();

    /**
     * Find the index in the self-classes of this type, of a Java class that
     * is assignment-compatible with that of the argument. This method is
     * used by descriptors when they are called with a {@code self} argument
     * that is not of the Python type that defined the descriptor, but is
     * found to be a sub-type of it.
     * <p>
     * In these circumstances it is only necessary to try the primary and
     * accepted (not adopted) classes because adopted classes take a fast
     * path. It returns 0 in all cases where there are no such accepted
     * representations, even if selfClasses()[0] is not assignment
     * compatible.
     *
     * @param selfClass to seek
     * @return index in {@link #selfClasses()}
     */
    @SuppressWarnings("static-method")
    public int getSubclassIndex(Class<?> selfClass) { return 0; }
}
