// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.subclass;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;

import org.python.core.PyType;
import org.python.internal.NamedSpec;
import org.python.types.Constructible.ConstructorAndHandle;
import org.python.types.WithClassAssignment;
import org.python.types.WithDict;

/**
 * A {@code SubclassSpec} is a specification for a Java class to
 * represent the instances of a type defined in Python. Such a
 * specification is computed from a Python class definition, or a
 * three-argument call to {@code type(name, bases, dict)}. It is not a
 * factory for the actual class object, creation of which requires more
 * of the caller's context.
 * <p>
 * When such a specification is computed, it may be identical in form to
 * one that was computed earlier in the lifetime of the type system. In
 * that case, instances of the Python class now being created will be
 * class-assignable to the previous class, and vice-versa. For this to
 * be possible in Jython, instances of each must have the same class in
 * Java, that is, the Python types must share a representation.
 * Therefore a {@code SubclassSpec} must be usable as a key in a cache
 * of existing Java representation classes, for which we defines
 * appropriate {@code equals()} and {@code hash()} methods. Only after
 * that fails, do we read the specification to create new {@code Class}
 * and {@code Representation} objects.
 */
public class SubclassSpec extends NamedSpec implements Cloneable {

    /** Base class the representation {@code extends}. */
    private final Class<?> base;
    /** Interfaces the representation {@code implements}. */
    private List<Class<?>> interfaces = EMPTY;
    /** Constructors on which to base those created here. */
    private List<Constructor<?>> constructors = new LinkedList<>();

    /** Whether the class manages a writable {@code __dict__}. */
    private boolean manageDict = false;
    /** Whether the class manages a writable {@code __class__}. */
    private boolean manageClass = false;

    /**
     * Names of slots to be added as members or {@link #NONAMES}. Note even
     * an empty {@code __slots__} ensures {@code slots != NONAMES}, so we
     * use this value as a marker. Python language rules applied in
     * {@link #freeze()} may change this from what was specified by the
     * client.
     */
    private List<String> slots = NONAMES;

    /**
     * Create (or begin) a specification. Note that the name given here is
     * provisional, reflecting the Python class (and useful for error
     * messages). The name of the Java class created according to the
     * specification will be different.
     *
     * @param name provisional name of the representation class.
     * @param base the representation {@code extends}.
     */
    public SubclassSpec(String name, Class<?> base) {
        super(name);
        this.base = base;
    }

    /**
     * Create (or begin) a specification. Note that the name given here is
     * provisional, reflecting the Python class (and useful for error
     * messages). The name of the Java class created according to the
     * specification will be different.
     *
     * @param name provisional name of the representation class.
     * @param base to determine the base representation class.
     */
    public SubclassSpec(String name, PyType base) { this(name, base.canonicalClass()); }

    /**
     * Adjust the state and ensure it cannot be further changed in ways
     * affecting sorting and lookup. Here we decide finally whether the Java
     * class has to handle class assignment and the instance dictionary.
     * <p>
     * The final value of the slot list (after {@link #freeze()}) is
     * modified from the slot names added during definition, by rules that
     * are part of Python (e.g. removal of {@code "__dict__"}, sorting). It
     * will be exactly this set of named fields that are added to the Java
     * class. The value of {@code __slots__} stored in the class definition
     * is not affected.
     */
    @Override
    protected final SubclassSpec freeze() {
        /*
         * If we are not frozen yet, it means we have yet to finalise the
         * interfaces and slots.
         */
        if (frozen == false) {
            // Compare CPython ctx->may_add_dict in typeobject.c
            boolean needDict = true;

            if (slots != NONAMES) {
                // We have slots. Lots of rules. Mostly:
                needDict = false;

                if (slots.remove("__dict__")) {
                    // __slots__ *and* __dict__
                    needDict = true;
                }

                // Sort and freeze the list.
                Collections.sort(slots);
                slots = Collections.unmodifiableList(slots);
            }

            // FIXME : (maybe order and) freeze interfaces.
            /*
             * Order of interfaces is *not* significant in Java and treating them as
             * ordered in the MRO is perhaps behind certain problems in Jython 2
             * such as #70 and #391. Yet we must (I think) acknowledge them when
             * looking for a representation, and probably as bases in Python.
             */

            /*
             * The specification calls for instances to have a __dict__ member. We
             * must manage __dict__ assignment in this class unless the base already
             * manages a __dict__.
             */
            manageDict = needDict && !WithDict.class.isAssignableFrom(base);

            /*
             * Instances must support class assignment (of compatible classes). We
             * must manage class assignment in this class unless the base already
             * does that for us.
             */
            manageClass = !WithClassAssignment.class.isAssignableFrom(base);

            // Prevent further change to the specification.
            frozen = true;
        }
        return this;
    }

    /**
     * Return the class the representation {@code extends}.
     *
     * @return class the representation {@code extends}
     */
    Class<?> getBase() { return base; }

    /**
     * Set the name of the class being created. The name is not part of the
     * hash (or test of equality) so it is not covered by {@link #freeze()}.
     *
     * @param name of class being created
     * @return {@code this}
     */
    SubclassSpec setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Add one interface (guarding against repetition).
     *
     * @param iface new interface
     * @return {@code this}
     */
    SubclassSpec addInterface(Class<?> iface) {
        checkNotFrozen();
        if (!iface.isInterface()) {
            String msg = String.format("%s is not an interface", iface.getTypeName());
            throw specError(msg);
        } else if (interfaces == EMPTY) {
            interfaces = new LinkedList<Class<?>>();
        } else if (interfaces.indexOf(iface) >= 0) { throw repeatError("addInterface", iface); }
        this.interfaces.add(iface);
        return this;
    }

    /**
     * Add multiple interfaces (guarding against repetition).
     *
     * @param ifaces new interfaces
     * @return {@code this}
     */
    public SubclassSpec addInterfaces(Collection<Class<?>> ifaces) {
        for (Class<?> iface : ifaces) { addInterface(iface); }
        return this;
    }

    /**
     * Add multiple interfaces (guarding against repetition).
     *
     * @param ifaces new interfaces
     * @return {@code this}
     */
    SubclassSpec addInterfaces(Class<?>... ifaces) { return addInterfaces(List.of(ifaces)); }

    /**
     * Return list of interfaces the representation {@code implements}
     *
     * @return interfaces the representation {@code implements}
     */
    List<Class<?>> getInterfaces() { return interfaces; }

    /**
     * Add multiple constructors on which to base those in the new
     * representation. This is a subset chosen by the super-type from its
     * canonical representation. The new representation will always have a
     * {@link PyType} first argument of its constructors.
     *
     * @param cons new constructors
     * @return {@code this}
     */
    public SubclassSpec addConstructors(List<Constructor<?>> cons) {
        checkNotFrozen();
        for (Constructor<?> c : cons) { constructors.add(c); }
        return this;
    }

    /**
     * Add all the published constructors of a particular Python type, on
     * which to base those in the new representation. The new representation
     * will always have a {@link PyType} first argument of its constructors.
     *
     * @param baseType from which to get constructors
     * @return {@code this}
     */
    public SubclassSpec addConstructors(PyType baseType) {
        checkNotFrozen();
        for (ConstructorAndHandle ch : baseType.constructorLookup().values()) {
            constructors.add(ch.constructor());
        }
        return this;
    }

    /**
     * Get the defined constructors.
     *
     * @return {@code this}
     */
    List<Constructor<?>> getConstructors() { return constructors; }

    /**
     * Add one named slot (guarding against repetition). If a specification
     * is created and no slots added before it is used, that is implicitly a
     * request to add a dictionary.
     *
     * @param slotName new slot
     * @return {@code this}
     */
    SubclassSpec addSlot(String slotName) {
        checkNotFrozen();
        if (slots == NONAMES) {
            slots = new LinkedList<>();
        } else if (slots.indexOf(slotName) >= 0) { throw repeatError("addSlot", "slotName"); }
        this.slots.add(slotName);
        return this;
    }

    /**
     * Add multiple named slots (guarding against repetition). Note that An
     * empty collection still counts as defining {@code __slots__}.
     *
     * @param slots new slot names
     * @return {@code this}
     */
    public SubclassSpec addSlots(Collection<String> slots) {
        // An empty collection still counts as defining __slots__
        if (slots == NONAMES) { slots = new LinkedList<>(); }
        for (String slot : slots) { addSlot(slot); }
        return this;
    }

    /**
     * Add multiple named slots (guarding against repetition).
     *
     * @param slots new slot names
     * @return {@code this}
     */
    SubclassSpec addSlots(String... slots) { return addSlots(List.of(slots)); }

    /**
     * Return names of {@code __slots__} to be added as members.
     *
     * @return names of {@code __slots__} to be added
     */
    List<String> getSlots() { return slots; }

    /**
     * Whether {@code __slots__} was defined. If {@code false},
     * {@link #getSlots()} will return an empty list.
     *
     * @return whether {@code __slots__} was defined.
     */
    boolean hasSlots() { return slots == NONAMES; }

    /**
     * Report whether the class being specified must add a field and
     * mechanisms to manage {@code __class__} assignment. Types specified
     * here all support class assignment, even e.g. meta-classes, but the
     * class need only add a field and mechanisms when the base class
     * doesn't already have them.
     * <p>
     * The result is not reliable until {@link #freeze()} is called.
     *
     * @return whether to add a {@code __class__} field.
     */
    boolean manageClassAssignment() { return manageClass; }

    /**
     * Report whether to create a {@code __dict__} member in instances. An
     * instance dictionary is required, in most classes defined in Python,
     * except as controlled by {@code __slots__}.
     * <p>
     * If an instance dictionary is required, the class need only add a
     * field and mechanisms when the base class doesn't already have them.
     * In that case, it will exist with its own access policies thanks to
     * the base class.
     * <p>
     * The result is not reliable until {@link #freeze()} is called.
     *
     * @return whether to add a {@code __dict__} field.
     */
    boolean manageDictAssignment() {
        freeze();
        return manageDict;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(name);
        b.append(" extends ").append(base.getName());
        if (interfaces != EMPTY) {
            StringJoiner sj = new StringJoiner(", ", " implements ", " ");
            for (Class<?> c : interfaces) { sj.add(c.getSimpleName()); }
            b.append(sj.toString());
        }
        if (manageDict) { b.append(" +dict"); }
        if (slots != NONAMES) {
            StringJoiner sj = new StringJoiner(", ", " +slots(", ") ");
            for (String s : slots) { sj.add(s); }
            b.append(sj.toString());
        }
        return b.toString();
    }

    @Override
    public boolean equals(Object obj) {
        freeze();
        if (obj instanceof SubclassSpec r) {
            r.freeze();
            return (getBase() == r.getBase()) && (manageDict == r.manageDict)
                    && (manageClass == r.manageClass) && getSlots().equals(r.getSlots())
                    && getInterfaces().equals(r.getInterfaces());
        }
        return false;
    }

    @Override
    public int hashCode() {
        freeze();
        int h = getBase().hashCode();
        if (manageDict) { h *= 9; }
        if (manageClass) { h *= 9; }
        // Need not hash hasSlots(), only names (not ordered).
        for (String s : getSlots()) { h += s.hashCode(); }
        // Interfaces hash contribution not ordered.
        for (Class<?> i : getInterfaces()) { h += i.hashCode(); }
        return h;
    }

    @Override
    public SubclassSpec clone() {
        SubclassSpec spec = new SubclassSpec(name, base);
        if (interfaces != EMPTY) { spec.addInterfaces(interfaces); }
        if (slots != NONAMES) { spec.addSlots(slots); }
        if (frozen) {
            /*
             * Slightly tricky, because freeze() modifies __slots__ as it computes
             * the missing values.
             */
            spec.freeze();
            spec.manageDict = manageDict;
            spec.manageClass = manageClass;
        }
        return spec;
    }
}
