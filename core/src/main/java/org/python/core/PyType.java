// Copyright (c)2022 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.python.base.InterpreterError;
import org.python.core.Exposed.Getter;
import org.python.core.Slot.Signature;

/**
 * The Python {@code type} object. Type objects are normally created
 * (when created from Java) by a call to
 * {@link PyType#fromSpec(Spec)}.
 */
public class PyType extends Operations implements DictPyObject {
    /*
     * The static initialisation of PyType is a delicate business, since
     * it occurs early in the initialisation of the run-time system. The
     * objective is simple: we must bring into existence type objects
     * for both PyBaseObject ('object') and PyType ('type'), and then
     * the descriptor types that will populate the dictionaries of all
     * types including their own.
     *
     * This last fact makes it necessary to Java-initialise the classes
     * that represent these objects, and afterwards return to build
     * their dictionaries. This done, all subsequent type objects may be
     * built in the obvious sequence.
     */

    // *** The order of these initialisations is critical

    /**
     * Classes for which the type system has to prepare {@code PyType}
     * objects in two stages, deferring the filling of the dictionary of
     * the type until all classes in this set have completed their
     * static initialisation in Java and built a {@code PyType}.
     * Generally, this is because these types are necessary to create
     * entries in the dictionary of any type.
     */
    // Use an ordered list so we have full control over sequence.
    static final Map<Class<?>, BootstrapTask> bootstrapTasks = new LinkedHashMap<>();
    static {
        /*
         * Name the classes needing this bootstrap treatment in the order
         * they should be processed.
         */
        Class<?>[] bootstrapClasses = {
                // Really special cases
                PyBaseObject.class, //
                PyType.class,
                // The entries are descriptors so defer those
                // PyMemberDescr.class, //
                // PyGetSetDescr.class, //
                PyWrapperDescr.class, //
                PyMethodDescr.class, //
                // And sometimes things go wrong :(
                BaseException.class, //
                // Types with multiple/adopted implementations
                PyUnicode.class, //
                PyLong.class, //
                PyBool.class, //
                PyFloat.class, //
        };
        // Fill the map from the list.
        for (Class<?> c : bootstrapClasses) { bootstrapTasks.put(c, new BootstrapTask()); }
    }

    /** An empty array of type objects */
    static final PyType[] EMPTY_TYPE_ARRAY = new PyType[0];
    /** Lookup object on {@code PyType}. */
    private static Lookup LOOKUP = MethodHandles.lookup();
    /** The type object of {@code type} objects. */
    public static final PyType TYPE = new PyType();
    /** The type object of {@code object} objects. */
    static final PyType OBJECT_TYPE = TYPE.base;
    /** An array containing only 'object', the bases of many types. */
    private static final PyType[] ONLY_OBJECT = new PyType[] {OBJECT_TYPE};

    static {
        // For each bootstrap class: ensure static initialisation
        for (Class<?> c : bootstrapTasks.keySet()) {
            String name = c.getName();
            try {
                Class.forName(name);
            } catch (ClassNotFoundException e) {
                throw new InterpreterError("failed to initialise bootstrap class %s",
                        c.getSimpleName());
            }
        }
    }

    // *** End critically ordered section

    /**
     * Particular type of this {@code PyType}. Why is this not always
     * {@link #TYPE}? Because there may be subclasses of type
     * (meta-classes) and objects having those as their {@code type}.
     */
    private final PyType type;

    /** Name of the type. */
    final String name;

    /** The Java class defining operations on instances of the type. */
    final Class<?> definingClass;

    /**
     * Handle arrays for in which to look up binary class-specific
     * methods when these are provided as a supplementary implementation
     * class. {@code null} such a class is not provided in the
     * specification. See {@link Spec#binops(Class)}.
     */
    final Map<Slot, BinopGrid> binopTable;

    /**
     * The Java classes appearing as operands in the operations and
     * methods of the type.
     * <ol>
     * <li>[0] is the canonical implementation class</li>
     * <li>[1:implCount] are the adopted implementation classes</li>
     * <li>[:acceptedCount] are the classes acceptable as
     * {@code self}</li>
     * <li>[:] (entire array) are the classes provided for as the
     * "other" argument of binary operations</li>
     * </ol>
     */
    final Class<?>[] classes;

    /**
     * The number of {@link #classes} in {@link #classes} recognised by
     * the run-time as implementations of the type.
     */
    final int implCount;

    /**
     * The number of Java classes in {@link #classes} that are
     * acceptable as {@code self} in methods.
     */
    final int acceptedCount;

    /**
     * Characteristics of the type, to determine behaviours (such as
     * mutability) of instances or the type itself, or to provide quick
     * answers to frequent questions such as "are instances data
     * descriptors".
     */
    EnumSet<Flag> flags;

    // Support for class hierarchy

    /**
     * The {@code __bases__} of this type, which are the types named in
     * heading of the Python {@code class} definition, or just
     * {@code object} if none are named, or an empty array in the
     * special case of {@code object} itself.
     */
    private PyType[] bases;
    /**
     * The {@code __base__} of this type. The {@code __base__} is a type
     * from the {@code __bases__}, but its choice is determined by
     * implementation details.
     * <p>
     * It is the type earliest on the MRO after the current type, whose
     * implementation contains all the members necessary to implement
     * the current type.
     */
    private PyType base;
    /**
     * The {@code __mro__} of this type, that is, the method resolution
     * order, as defined for Python and constructed by the {@code mro()}
     * method (which may be overridden), by analysis of the
     * {@code __bases__}.
     */
    private PyType[] mro;

    /**
     * The dictionary of the type is always an ordered {@code Map}. It
     * is only accessible (outside the core) through a
     * {@code mappingproxy} that renders it a read-only
     * {@code dict}-like object. Internally names are stored as
     * {@code String} for speed and accessed via
     * {@link #lookup(String)}.
     */
    private final Map<String, Object> dict = new LinkedHashMap<>();

    /**
     * Partially construct a {@code type} object for {@code type}, and
     * by side-effect the type object of its base {@code object}. The
     * special constructor solves the problem that each of these has to
     * exist in order properly to create the other. This constructor is
     * <b>only used once</b>, during the static initialisation of
     * {@code PyType}, after which these objects are constants.
     */
    private PyType() {
        /*
         * We are creating the PyType for "type". We need a specification
         * too, because there's nothing more bootstrappy than type. :)
         */
        Spec spec = new Spec("type", LOOKUP).metaclass(this);
        /*
         * We cannot use fromSpec here, because we are already in a
         * constructor and it needs TYPE, which we haven't set.
         */
        this.type = this;
        this.name = spec.name;
        this.definingClass = spec.definingClass();
        this.binopTable = Collections.emptyMap();
        this.implCount = spec.adoptedCount();
        this.acceptedCount = spec.acceptedCount();
        this.classes = spec.getClasses();
        this.flags = spec.flags;

        /*
         * Break off to construct the type object for "object", which we
         * need as the base. Again, we need the spec.
         */
        Spec objectSpec = new Spec("object", PyBaseObject.class, LOOKUP).metaclass(this)
                .canonical(Object.class);
        /*
         * This time the constructor will work, as long as we supply the
         * metatype. For consistency, take values from objectSpec.
         */
        PyType objectType = new PyType(objectSpec);

        // The only base of type is object
        this.base = objectType;
        this.bases = new PyType[] {objectType};
        this.mro = new PyType[] {this, objectType};

        // Defer filling the dictionary for both types we made
        BootstrapTask.shelve(objectSpec, objectType);
        BootstrapTask.shelve(spec, this);
    }

    /**
     * Partially construct a type from a type specification. This
     * implements only the basic object creation, short of filling the
     * dictionary, for example. It is intended to be used with or as
     * part of {@link #fromSpec(Spec)}.
     *
     * @param spec specification for the type
     */
    private PyType(Spec spec) {
        this.type = spec.getMetaclass();
        this.name = spec.name;
        this.definingClass = spec.definingClass();
        this.classes = spec.getClasses();
        this.implCount = spec.adoptedCount() + 1;
        this.acceptedCount = spec.acceptedCount();
        // in case original changes
        this.flags = EnumSet.copyOf(spec.flags);
        // Sets base as well as bases
        this.setBases(spec.getBases());
        // Fix-up base and MRO from bases array
        this.setMROfromBases();
        // Create the binary operations table (none for now)
        this.binopTable = Collections.emptyMap();
    }

    @Override
    public PyType getType() { return type; }

    /**
     * Construct a type from the given specification. This approach is
     * preferred to the direct constructor. The type object does not
     * retain a reference to the specification, once constructed, so
     * that subsequent alterations have no effect on the {@code PyType}.
     *
     * @param spec specification
     * @return the constructed {@code PyType}
     */
    public static PyType fromSpec(Spec spec) {

        // Construct a type with an empty dictionary
        PyType type;

        if (spec.getMetaclass() == TYPE) {
            type = new PyType(spec);
        } else {
            throw new InterpreterError("Metaclasses not supported.");
        }

        /*
         * The next step for this type is to populate the dictionary from
         * the information gathered in the specification. We can only do
         * this if all the bootstrap types have also reached at least this
         * stage (are no longer on the waiting list).
         */
        if (bootstrapTasks.isEmpty()) {
            // The bootstrap types have all completed. Make descriptors.
            try {
                type.fillDictionary(spec);
            } catch (Clash clash) {
                /*
                 * Another thread beat us to the construction of an operations
                 * object for (one of) the implementing classes, or perhaps we're
                 * repeating ourselves.
                 */
                Operations ops = clash.existing;
                if (ops instanceof PyType && Arrays.equals(type.classes, ((PyType)ops).classes))
                    // Graciously accept it as the result. (I think.)
                    type = (PyType)ops;
                else
                    // Something bad is happening. -> SystemError?
                    throw new InterpreterError(clash, "constructing %s", type);
            }

        } else {
            /*
             * Some bootstrap types are waiting for their dictionaries. It is
             * not safe to create descriptors in the dictionary).
             */
            BootstrapTask.shelve(spec, type);

            /*
             * However, the current type may be the last bootstrap type we were
             * waiting for.
             */
            if (BootstrapTask.allReady()) {
                /*
                 * Complete the types we had to shelve. Doing so may create new
                 * types, so we empty the waiting list into a private copy.
                 */
                List<BootstrapTask> tasks = new ArrayList<>(bootstrapTasks.values());
                bootstrapTasks.clear();

                for (BootstrapTask task : tasks) {
                    try {
                        task.type.fillDictionary(task.spec);
                    } catch (Clash clash) {
                        // We're trying to repeat ourselves?
                        throw new InterpreterError(clash, "constructing %s", task.type);
                    }
                }

                /*
                 * Bootstrapping is over: the type we return will be
                 * fully-functional as a Python type object after all.
                 */
            }
        }

        return type;
    }

    /**
     * Get the Python type corresponding to the given Java class. The
     * method will find, or if necessary cause the creation of, a
     * {@link PyType} that represents the type of instances of that Java
     * class. The Java class given will be initialised, if it has not
     * been already.
     * <p>
     * This is not always a meaningful enquiry: if the given class is
     * not the implementation of exactly one Python type, an error will
     * be thrown. This also applies when the call causes creation of one
     * or more {@link PyType}s and {@link Operations} objects.
     *
     * @param klass to inspect
     * @return the Python type of {@code klass}
     */
    static PyType fromClass(Class<?> klass) { return Operations.fromClass(klass).uniqueType(); }

    /**
     * A record used to defer the completion of a particular type
     * object. When so deferred, {@link PyType#fromSpec(Spec)} will
     * return a type object without filling the dictionary of the type.
     * The type and the implementation class can be available to Java,
     * but will not yet function properly as a Python object.
     * <p>
     * This only happens while starting up the run-time. The purpose is
     * to allow Java class initialisation to complete for all of the
     * types needed to populate type dictionaries ("bootstrap types").
     * Other classes that request a type object during this time will be
     * caught up temporarily in the same process.
     * <p>
     * A {@code BootstrapTask} stores the {@link PyType} object and the
     * {@link Spec} for a given type. All the waiting types are
     * completed as soon as the last of them becomes available.
     */
    private static class BootstrapTask {

        Spec spec;
        PyType type;

        /**
         * Place a partially-completed {@code type} on the
         * {@link PyType#bootstrapTasks} list.
         *
         * @param spec specification for the type
         * @param type corresponding (partial) type object
         */
        static void shelve(Spec spec, PyType type) {
            Class<?> key = spec.definingClass();
            BootstrapTask t = bootstrapTasks.get(key);
            if (t == null)
                // Not present: add an entry.
                bootstrapTasks.put(key, t = new BootstrapTask());
            else if (t.spec != null)
                throw new InterpreterError(REPEAT_CLASS, key);
            // Fill the entry as partially initialised.
            t.spec = spec;
            t.type = type;
        }

        /**
         * Check to see if all {@link PyType#bootstrapTasks} have reached
         * partially complete (are awaiting a dictionary).
         *
         * @return true iff all are ready
         */
        static boolean allReady() {
            for (BootstrapTask t : bootstrapTasks.values()) {
                if (t.spec == null) { return false; }
            }
            return true;
        }

        @Override
        public String toString() { return String.format("BootstrapTask[%s]", spec); }

        private static final String REPEAT_CLASS =
                "PyType bootstrapping: class %s encountered twice";
    }

    /**
     * Load the dictionary of this type with attributes discovered
     * through the specification.
     *
     * @param spec to apply
     * @throws Clash when an implementation class is already registered
     */
    private void fillDictionary(Spec spec) throws Clash {

        // Fill slots from implClass or bases
        addDefinitions(spec);
        // XXX Possibly belong distinct from fillDictionary
        defineOperations(spec);
        deduceFlags();
    }

    /**
     * Define the Operations objects for this type, posting them to the
     * registry.
     *
     * @throws Clash when an implementation class is already registered
     */
    private void defineOperations(Spec spec) throws Clash {

        setAllSlots();

        int n = spec.adoptedCount();

        if (n == 1) {
            // Simple case: one class and the Operations is the PyType
            Operations.register(classes[0], this);

        } else {
            // Multiple implementations must stand or fall together
            Class<?>[] cls = Arrays.copyOf(classes, n);
            Operations[] ops = new Operations[n];

            // The first Operations object is this PyType
            cls[0] = classes[0];
            ops[0] = this;

            // Create an Operations for each adopted implementation
            for (int i = 1; i < n; i++) {
                // Creating the operations object sets its slots
                ops[i] = new Operations.Accepted(this, i);
                cls[i] = classes[i];
            }

            // Register these pairings as a batch
            Operations.register(cls, ops);
        }
    }

    /**
     * Add methods, get-sets, members and special functions as
     * attributes to this type, as discovered through the specification.
     *
     * @param spec to apply
     */
    private void addDefinitions(Spec spec) {
        // Add definitions found in the defining class to the type
        TypeExposer exposer = Exposer.exposeType(this, spec.definingClass, spec.methodClass);
        exposer.populate(dict, spec.lookup);
    }

    /**
     * The {@link #flags} field caches many characteristics of the type
     * that we need to consult: we deduce them here.
     */
    private void deduceFlags() {
        if (Slot.op_get.isDefinedFor(this)) {
            // It's a descriptor
            flags.add(Flag.IS_DESCR);
        }
        if (Slot.op_set.isDefinedFor(this) || Slot.op_delete.isDefinedFor(this)) {
            // It's a data descriptor
            flags.add(Flag.IS_DESCR);
            flags.add(Flag.IS_DATA_DESCR);
        }
    }

    /**
     * Get the Python type of the given object {@code obj}. The Java
     * class of {@code obj} will normally have been initialised, since
     * an instance exists.
     *
     * @param obj to inspect
     * @return the Python type of {@code obj}
     */
    public static PyType of(Object obj) {
        return Operations.fromClass(obj.getClass()).type(obj);
    }

    @Override
    PyType type(Object x) { return this; }

    @Override
    PyType uniqueType() { return this; }

    /**
     * Get the (canonical) Java implementation class of this
     * {@code PyType} object.
     */
    @Override
    Class<?> getJavaClass() { return classes[0]; }

    /**
     * Set {@link #bases} and deduce {@link #base}.
     *
     * @param bases to set
     */
    private void setBases(PyType bases[]) {
        this.bases = bases;
        this.base = bestBase(bases);
    }

    /** Set the MRO, but at present only single base. */
    // XXX note may retain a reference todeclaredBases
    private void setMROfromBases() {

        int n = bases.length;

        if (n == 0) {
            // Special case of 'object'
            this.mro = new PyType[] {this};

        } else if (n == 1) {
            // Just one base: short-cut: mro = (this,) + this.base.mro
            PyType[] baseMRO = base.getMRO();
            int m = baseMRO.length;
            PyType[] mro = new PyType[m + 1];
            mro[0] = this;
            System.arraycopy(baseMRO, 0, mro, 1, m);
            this.mro = mro;

        } else { // n >= 2
            // Need the proper C3 algorithm to set MRO
            String fmt = "multiple inheritance not supported yet (type `%s`)";
            throw new InterpreterError(fmt, name);
        }
    }

    /**
     * Set all the slots ({@code op_*}) from the entries in the
     * dictionaries of this type and its bases.
     */
    private void setAllSlots() {
        for (Slot s : Slot.values()) {
            Object def = lookup(s.methodName);
            s.setDefinition(this, def);
        }
    }

    /** A name has the form __A__ where A is one or more characters. */
    private static boolean isDunderName(String n) {
        final int L = n.length();
        return L > 4 && n.charAt(1) == '_' && n.charAt(0) == '_' && n.charAt(L - 2) == '_'
                && n.charAt(L - 1) == '_';
    }

    /**
     * Called from {@link #__setattr__(String, Object)} after an
     * attribute has been set or deleted. This gives the type the
     * opportunity to recompute slots and perform any other actions.
     *
     * @param name of the attribute modified
     */
    protected void updateAfterSetAttr(String name) {

        // XXX What if a slot-wrapper is removed, not replaced?
        // XXX Should also visit sub-classes

        // If the update is a slot wrapper change, slots must follow.
        Slot s = Slot.forMethodName(name);
        if (s != null) {
            Object def = dict.get(name);
            for (Class<?> impl : classes) {
                Operations ops = Operations.fromClass(impl);
                s.setDefinition(ops, def);
            }
        }
    }

    @Override
    public String toString() { return "<class '" + name + "'>"; }

    /**
     * The name of this type.
     *
     * @return name of this type
     */
    @Getter("__name__")
    public String getName() { return name; }

    /**
     * Find the index of the given class in the accepted classes for
     * this type. There is a match if the found class is a assignable
     * from the given class. In the case that more than one matches, the
     * first qualifying index is returned. The "accepted" classes
     * consist of:
     * <ol>
     * <li>the canonical class (index zero)</li>
     * <li>other adopted implementations (like {@code Double} for
     * {@code float})</li>
     * <li>the accepted implementations of any sub-classes that are not
     * assignable to the canonical or adopted</li>
     * </ol>
     *
     * @param c a class matching one of the accepted classes
     * @return its index or -1
     */
    int indexAccepted(Class<?> c) {
        // Try the non-canonical accepted classes first (if any)
        for (int i = 1; i < acceptedCount; i++) {
            if (classes[i].isAssignableFrom(c)) { return i; }
        }
        // Try the canonical class last
        return classes[0].isAssignableFrom(c) ? 0 : -1;
    }

    /**
     * Find the index of the given class in the known operand classes
     * for this type. There is a match if the found class is a
     * assignable from the given class. In the case that more than one
     * matches, the first qualifying index is returned.
     *
     * @param c a class matching one of the operand classes
     * @return its index or -1
     */
    int indexOperand(Class<?> c) {
        // Try the non-canonical known operand classes first (if any)
        for (int i = 1; i < classes.length; i++) {
            if (classes[i].isAssignableFrom(c)) { return i; }
        }
        // Try the canonical class last
        return classes[0].isAssignableFrom(c) ? 0 : -1;
    }

    /**
     * {@code true} iff the type of {@code o} is a Python sub-type of
     * {@code this} (including exactly {@code this} type). This is
     * likely to be used in the form:<pre>
     * if(!PyUnicode.TYPE.check(oName)) throw ...
     * </pre>
     *
     * @param o object to test
     * @return {@code true} iff {@code o} is of a sub-type of this type
     */
    boolean check(Object o) {
        PyType t = PyType.of(o);
        return t == this || t.isSubTypeOf(this);
    }

    /**
     * {@code true} iff the Python type of {@code o} is exactly
     * {@code this}, not a Python sub-type of {@code this}, nor just any
     * Java sub-class of {@code PyType}. This is likely to be used in
     * the form:<pre>
     * if(!PyUnicode.TYPE.checkExact(oName)) throw ...
     * </pre>
     *
     * @param o object to test
     * @return {@code true} iff {@code o} is exactly of this type
     */
    public boolean checkExact(Object o) { return PyType.of(o) == this; }

    /**
     * Determine if this type is a Python sub-type of {@code b} (if
     * {@code b} is on the MRO of this type).
     *
     * @param b to test
     * @return {@code true} if {@code this} is a sub-type of {@code b}
     */
    // Compare CPython PyType_IsSubtype in typeobject.c
    boolean isSubTypeOf(PyType b) {
        if (mro != null) {
            /*
             * Deal with multiple inheritance without recursion by walking the
             * MRO tuple
             */
            for (PyType base : mro) {
                if (base == b)
                    return true;
            }
            return false;
        } else
            // a is not completely initialised yet; follow base
            return type_is_subtype_base_chain(b);
    }

    /**
     * Determine if this type is a Python sub-type of {@code b} by
     * chaining through the {@link #base} property. (This is a fall-back
     * when {@link #mro} is not valid.)
     *
     * @param b to test
     * @return {@code true} if {@code this} is a sub-type of {@code b}
     */
    // Compare CPython type_is_subtype_base_chain in typeobject.c
    private boolean type_is_subtype_base_chain(PyType b) {
        PyType t = this;
        while (t != b) {
            t = t.base;
            if (t == null) { return b == OBJECT_TYPE; }
        }
        return true;
    }

    /**
     * Return whether special methods in this type may be assigned new
     * meanings after type creation (or may be safely cached).
     *
     * @return whether a data descriptor
     */
    final boolean isMutable() { return flags.contains(Flag.MUTABLE); }

    /**
     * Return whether an instance of this type is a data descriptor
     * (defines {@code __get__} and at least one of {@code __set__} or
     * {@code __delete__}.
     *
     * @return whether a data descriptor
     */
    @Override
    final boolean isDataDescr() { return flags.contains(Flag.IS_DATA_DESCR); }

    /**
     * Return whether an instance of this type defines {@code __get__}
     * participates in the optimised call pattern supported by
     * {@link Opcode311#LOAD_METHOD}.
     *
     * @return whether a method descriptor
     */
    @Override
    final boolean isMethodDescr() { return flags.contains(Flag.IS_METHOD_DESCR); }

    /**
     * Return whether an instance of this type is a descriptor (defines
     * {@code __get__}).
     *
     * @return whether a descriptor
     */
    final boolean isDescr() { return flags.contains(Flag.IS_DESCR); }

    /**
     * Return whether this type uses object.__getattribute__ from .
     *
     * @return whether a descriptor
     */
    final boolean hasGenericGetAttr() {
        return op_getattribute == PyBaseObject.TYPE.op_getattribute;
    }

    /**
     * Get the {@code __base__} of this type. The {@code __base__} is a
     * type from the MRO, but its choice is determined by implementation
     * details.
     * <p>
     * It is the type earliest on the MRO after the current type, whose
     * implementation contains all the members necessary to implement
     * the current type.
     *
     * @return the base (core use only).
     */
    PyType getBase() { return base; }

    /** @return the bases as an array (core use only). */
    PyType[] getBases() { return bases; }

    /** @return the MRO as an array (core use only). */
    PyType[] getMRO() { return mro; }

    /**
     * The dictionary of a {@code type} in a read-only view.
     */
    @Override
    public final Map<Object, Object> getDict() { return Collections.unmodifiableMap(dict); }

    /**
     * Look for a name, returning the entry directly from the first
     * dictionary along the MRO containing key {@code name}. This may be
     * a descriptor, but no {@code __get__} takes place on it: the
     * descriptor itself will be returned. This method does not throw an
     * exception if the name is not found, but returns {@code null} like
     * a {@code Map.get}
     *
     * @param name to look up, must be exactly a {@code str}
     * @return dictionary entry or null
     */
    // Compare CPython _PyType_Lookup in typeobject.c
    // and find_name_in_mro in typeobject.c
    Object lookup(String name) {

        /*
         * CPython wraps this in a cache keyed by (type, name) and sensitive
         * to the "version" of this type. (Version changes when any change
         * occurs, even in a super-class, that would alter the result of a
         * look-up.) We do not reproduce that at present.
         */

        // Look in dictionaries of types in MRO
        PyType[] mro = getMRO();

        // CPython checks here to see in this type is "ready".
        // Could we be "not ready" in some loop of types?

        for (PyType base : mro) {
            Object res;
            if ((res = base.dict.get(name)) != null)
                return res;
        }
        return null;
    }

    /**
     * Equivalent to {@link #lookup(String)}, accepting
     * {@link PyUnicode}.
     *
     * @param name to look up, must be exactly a {@code str}
     * @return dictionary entry or null
     */
    Object lookup(PyUnicode name) { return lookup(name.asString()); }

    /**
     * Enumeration of the characteristics of a type. These are the
     * members that appear appear in the {@link PyType#flags} to
     * determine behaviours or provide quick answers to frequent
     * questions such as "are you a data descriptor".
     */
    public enum Flag {
        /**
         * Special methods may be assigned new meanings in the {@code type},
         * after creation.
         */
        MUTABLE,
        /**
         * An object of this type can change to another type (within
         * "layout" constraints).
         */
        VARIABLE,
        /**
         * This type the type allows sub-classing (is acceptable as a base).
         */
        BASETYPE,
        /**
         * An object of this type is a descriptor (defines {@code __get__}).
         */
        IS_DESCR,
        /**
         * An object of this type is a data descriptor (defines
         * {@code __get__} and at least one of {@code __set__} or
         * {@code __delete__}).
         */
        IS_DATA_DESCR,
        /**
         * An object of this type is a method descriptor (participates in an
         * optimised call pattern supported by {@link Opcode#LOAD_METHOD}).
         */
        IS_METHOD_DESCR,
    }

    /**
     * A specification for a Python type. A Java class intended as the
     * implementation of a Python object creates one of these data
     * structures during static initialisation, and configures it using
     * the mutators. A fluent interface makes this configuration
     * readable as a single, long statement.
     */
    public static class Spec {

        /** Name of the class being specified. */
        final String name;

        /** Delegated authorisation to resolve names. */
        final Lookup lookup;

        /**
         * The defining class for the type being specified, in which the
         * {@code Spec.lookup} was created.
         */
        private final Class<?> definingClass;

        /**
         * Additional class in which to look up method names or
         * {@code null}. See {@link #methods(Class)}
         */
        private Class<?> methodClass;

        /**
         * Additional class in which to look up binary class-specific
         * methods or {@code null}. See {@link #binops(Class)}.
         */
        private Class<?> binopClass;

        /**
         * The canonical and adopted implementations of the Python type,
         * classes acceptable as {@code self}, and other known operand
         * classes will be collected here.
         */
        private ArrayList<Class<?>> classes = new ArrayList<>(1);

        /**
         * The number of adopted implementations of the Python type,
         * including the canonical one. Increment for each adopted class
         * added.
         */
        private int adoptedCount;

        /**
         * The number of classes, including the (canonical and) adopted
         * classes, that are accepted as instances of the Python type.
         * Increment for each adopted or accepted class added. See
         * {@link #accept(Class...)}.
         */
        private int acceptedCount;

        /**
         * The Python type being specified may be represented by a Python
         * sub-class of {@code type}, i.e. something other than
         * {@link PyType#TYPE}. This will be represented by a sub-class of
         * {@link PyType}.
         */
        private PyType metaclass;

        /** Python types that are bases of the type being specified. */
        // Must allow null element, needed when defining 'object'
        private final List<PyType> bases = new LinkedList<>();

        /** Characteristics of the type being specified. */
        EnumSet<Flag> flags = Spec.getDefaultFlags();

        /**
         * Create (begin) a specification for a {@link PyType} based on a
         * specified implementation class.
         * <p>
         * {@link PyType#fromSpec(Spec)} will interrogate the implementation
         * class reflectively to discover attributes the type should have,
         * and will form type dictionary entries with {@link MethodHandle}s
         * or {@link VarHandle}s on qualifying members. The caller supplies
         * a {@link Lookup} object to make this possible. An implementation
         * class may declare methods and fields as {@code private}, and
         * annotate them to be exposed to Python, as long as the lookup
         * object provided to the {@code Spec} confers the right to access
         * them.
         * <p>
         * A {@code Spec} given private or package access to members should
         * not be passed to untrusted code. {@code PyType} does not hold
         * onto the {@code Spec} after completing the type object.
         * <p>
         * Additional classes may be given containing the implementation and
         * the lookup classes (see {code Lookup.lookupClass()}) to be
         * different from the caller. Usually they are the same.
         *
         * @param name of the type
         * @param definingClass in which operations are defined
         * @param lookup authorisation to access {@code implClass}
         *
         * @deprecated Use {@link #Spec(String, Lookup)} instead
         */
        @Deprecated
        Spec(String name, Class<?> definingClass, Lookup lookup) {
            this.name = name;
            this.definingClass = definingClass;
            this.lookup = lookup;
            this.methodClass = this.binopClass = null;
            this.adopt(definingClass);
        }

        /**
         * Create (begin) a specification for a {@link PyType} based on the
         * caller as the implementation class. This is the beginning
         * normally made by built-in classes in their static initialisation.
         * <p>
         * The caller supplies a {@link Lookup} object which must have been
         * created by the implementation class.
         * {@link PyType#fromSpec(Spec)} will interrogate the implementation
         * class reflectively to discover attributes the type should have,
         * and will form type dictionary entries with {@link MethodHandle}s
         * or {@link VarHandle}s on qualifying members. An implementation
         * class may declare methods and fields as {@code private}, and
         * annotate them to be exposed to Python, as long as the lookup
         * object provided to the {@code Spec} confers the right to access
         * them.
         * <p>
         * A {@code Spec} given private or package access to members should
         * not be passed to untrusted code. {@code PyType} does not hold
         * onto the {@code Spec} after completing the type object.
         * <p>
         * Additional classes may be given containing the implementation and
         * the lookup classes (see {code Lookup.lookupClass()}) to be
         * different from the caller. Usually they are the same.
         *
         * @param name of the type
         * @param lookup authorisation to access {@code implClass}
         */
        public Spec(String name, Lookup lookup) { this(name, lookup.lookupClass(), lookup); }

        /**
         * Create (begin) a specification for a {@link PyType} representing
         * a sub-class of a built-in type. The same implementation class may
         * be used to specify any number of Python types, instances of which
         * are able to migrate between these types by {@code __class__}
         * assignment. The {@link Operations} object of the implementation
         * class will be an {@code Operations.}{@link Derived}.
         *
         * @param name of the type
         * @param implClass in which operations are defined
         */
        public Spec(String name, Class<? extends DerivedPyObject> implClass) {
            this(name, implClass, null);
        }

        /**
         * Specify the canonical implementation class for the type. By
         * default, if {@link #canonical(Class)} is not called, the
         * canonical implementation is the lookup class given in the
         * constructor. This method makes it possible to have that lookup
         * class not be an implementation.
         * <p>
         * This is the case for the Python {@code object}, for which the
         * canonical implementation is Java {@code java.lang.Object} while
         * operations are defined elsewhere. Also {@code PyBool} makes
         * {@code java.lang.Boolean} canonical for Python {@code bool}).
         *
         * @param impl replacement canonical implementation class
         * @return {@code this}
         */
        public Spec canonical(Class<?> impl) {
            classes.set(0, impl);
            return this;
        }

        /**
         * Specify adopted implementation classes for the type. The adopted
         * implementations are those that will be identified by the run-time
         * as having the Python type of this {@code Spec}. Successive calls
         * are cumulative.
         * <p>
         * The note in {@link #accept(Class...)} about the availability of
         * method definitions applies.
         *
         * @param classes classes to treat as adopted implementations
         * @return {@code this}
         */
        Spec adopt(Class<?>... classes) {
            for (Class<?> c : classes) {
                // Add at the end of the adopted classes
                this.classes.add(adoptedCount, c);
                adoptedCount++;
                acceptedCount++;
            }
            return this;
        }

        /**
         * Specify Java classes to be accepted as "self" arguments for the
         * type, in addition to the canonical and adopted implementations.
         * The use for this is to ensure that the implementations of Python
         * sub-types of the type being specified are acceptable as "self",
         * when defined by unrelated Java classes. As an example, consider
         * that operations on a Python {@code int} must have a Java
         * implementation that accepts a Java {@code Boolean} (Python
         * {@code bool}).
         * <p>
         * Successive calls are cumulative. Classes assignable to existing
         * accepted classes are ignored.
         * <p>
         *
         * @apiNote For every instance method {@code m} (including special
         *     methods) on a Python object, and for for every adopted or
         *     accepted class {@code C}, there must be an implementation
         *     {@code m(D self, ...)} where the "self" (first) argument type
         *     {@code D} is assignable from {@code C}.
         *     <p>
         *     Note that this criterion could be satisfied by defining just
         *     one {@code m(Object self, ...} or by a series of specialised
         *     implementations, or any combination. When it selects an
         *     implementation, the run-time chooses the most specialised
         *     match.
         *
         * @param classes to append to the list
         * @return {@code this}
         */
        Spec accept(Class<?>... classes) {
            for (Class<?> c : classes) {
                if (indexOf(c) < 0) {
                    // Add at the end of the accepted classes
                    this.classes.add(acceptedCount, c);
                    acceptedCount++;
                }
            }
            return this;
        }

        /**
         * Specify Java classes accepted as the second operand in binary
         * operations. Successive calls are cumulative. Classes assignable
         * to existing accepted classes are ignored.
         *
         * @param classes to append to the list
         * @return {@code this}
         */
        Spec operand(Class<?>... classes) {
            for (Class<?> c : classes) {
                if (indexOf(c) < 0) {
                    // Add at the very end
                    this.classes.add(c);
                }
            }
            return this;
        }

        /**
         * The number of classes specified as adopted implementations of the
         * Python type being specified, including the canonical
         * implementation.
         *
         * @return number of adopted classes
         */
        int adoptedCount() { return adoptedCount; }

        /**
         * The number of classes specified as canonical, adopted or accepted
         * as {@code self} for the Python type being specified.
         *
         * @return number of accepted classes
         */
        int acceptedCount() { return acceptedCount; }

        /**
         * The number of classes specified as canonical, adopted, accepted
         * {@code self} or as operands for the Python type being specified.
         *
         * @return number of all classes to be treated as operands
         */
        int classesCount() { return classes.size(); }

        /**
         * Find c in the known operand classes.
         *
         * @param c class to find
         * @return index of {@code c} in accepted
         */
        private int indexOf(Class<?> c) {
            for (int i = classes.size(); --i >= 0;) {
                if (classes.get(i).isAssignableFrom(c)) { return i; }
            }
            return -1;
        }

        /**
         * Specify a base for the type. Successive bases given are
         * cumulative and ordered.
         *
         * @param base to append to the bases
         * @return {@code this}
         */
        public Spec base(PyType base) {
            if (base == null)
                throw new InterpreterError("null base specified for %s. (Base not ready?)", name);
            bases.add(base);
            return this;
        }

        /**
         * A new set of flags with the default values for a type defined in
         * Java.
         *
         * @return new default flags
         */
        static EnumSet<Flag> getDefaultFlags() { return EnumSet.of(Flag.BASETYPE); }

        /**
         * Specify a characteristic (type flag) to be added.
         *
         * @param f to add to the current flags
         * @return {@code this}
         */
        /*
         * XXX Better encapsulation to have methods for things we want to
         * set/unset. Most PyType.flags members should not be manipulated
         * through the Spec and are derived in construction, or as a side
         * effect of setting something else.
         */
        Spec flag(Flag f) {
            flags.add(f);
            return this;
        }

        /**
         * Specify a characteristic (type flag), or several, to be added.
         *
         * @param f to add to the current flags
         * @return {@code this}
         */
        Spec flag(Flag... f) {
            for (Flag x : f) { flags.add(x); }
            return this;
        }

        /**
         * Specify a characteristic (type flag) to be removed.
         *
         * @param f to remove from the current flags
         * @return {@code this}
         */
        // XXX mostly used as flagNot(BASETYPE). Consider specific call.
        // XXX Consider also reversing that default.
        public Spec flagNot(Flag f) {
            flags.remove(f);
            return this;
        }

        /**
         * Specify that the Python type being specified will be represented
         * by a an instance of this Python sub-class of {@code type}, i.e.
         * something other than {@link PyType#TYPE}.
         *
         * @param metaclass to specify (or null for {@code type}
         * @return {@code this}
         */
        public Spec metaclass(PyType metaclass) {
            this.metaclass = metaclass;
            return this;
        }

        /**
         * Get the defining class for the type. This is often, and is by
         * default, the canonical implementation class, but it doesn't have
         * to be.
         *
         * @return the defining class for the type
         */
        public Class<?> definingClass() { return definingClass; }

        /**
         * Set the class additionally defining methods for the type. This
         * class will be consulted when filling the dictionary of the type.
         * A separate class is useful when the method definitions are
         * generated by a script, as for types that admit multiple
         * realisations in Java.
         *
         * @param methodClass class with additional methods
         * @return {@code this}
         */
        Spec methods(Class<?> methodClass) {
            this.methodClass = methodClass;
            return this;
        }

        /**
         * Get the class additionally defining methods for the type. See
         * {@link #methods(Class)}.
         *
         * @return class additionally defining methods for the type
         */
        Class<?> methodClass() { return methodClass; }

        /**
         * Set the class in which to look up binary class-specific
         * operations, for example {@code __rsub__(MyObject, Integer)}. Such
         * signatures are used in call sites.
         * <p>
         * Types may ignore this technique if the designer is content with a
         * {@code __rsub__(MyObject, Object)} that coerces its right-hand
         * argument on each call. (This method has to exist to satisfy the
         * Python data model.) The method may be defined in the
         * {@link #definingClass()}, or {@link #methodClass()}
         * <p>
         * A separate class is necessary since the method definition for
         * {@code __rsub__(MyObject, Object)} must sometimes return
         * {@link Py#NotImplemented}, and we should like to avoid checking
         * for that in the call site. Rather, the absence of a definition
         * should indicate that he operation is not defined for a given pair
         * of types Certain built-ins use the technique to speed up call
         * sites in JVM byte code compiled from Python. (The class may be
         * generated by a script.)
         *
         * @param binopClass class with binary class-specific methods
         * @return {@code this}
         */
        Spec binops(Class<?> binopClass) {
            this.binopClass = binopClass;
            return this;
        }

        /**
         * Get the class defining binary class-specific operations for the
         * type. See {@link #binops(Class)}. {@code null} if there isn't
         * one.
         *
         * @return class defining binary class-specific operations (or
         *     {@code null})
         */
        Class<?> binopClass() { return binopClass; }

        /**
         * Get all the operand classes for the type, in order, the canonical
         * at index 0, adopted, accepted and operand classes following.
         *
         * @return a copy of all the operand classes
         */
        Class<?>[] getClasses() { return classes.toArray(new Class<?>[classes.size()]); }

        /**
         * Return the accumulated list of bases. If no bases were added, the
         * result is just {@code [object]}, except when we do this for
         * object itself, for which it is a zero-length array.
         *
         * @return array of the bases of this type
         */
        public PyType[] getBases() {
            if (bases.isEmpty()) {
                /*
                 * No bases specified: that means 'object' is the implicit base,
                 * unless that's us.
                 */
                if (definingClass() != PyBaseObject.class)
                    return ONLY_OBJECT;         // Normally
                else
                    return EMPTY_TYPE_ARRAY;    // For 'object'
            } else
                return bases.toArray(new PyType[bases.size()]);
        }

        /**
         * Return the meta-class of the type being created. If none was set,
         * it is {@link PyType#TYPE}..
         *
         * @return the proper meta-class
         */
        public PyType getMetaclass() { return metaclass != null ? metaclass : TYPE; }

        // Something helpful in debugging (__repr__ is different)
        @Override
        public String toString() {
            String fmt = "'%s' %s, flags=%s def=%s";
            return String.format(fmt, name, bases, flags, definingClass().getSimpleName());
        }
    }

    // Special methods -----------------------------------------------

    protected Object __repr__() throws Throwable { return String.format("<class '%s'>", name); }

    /**
     * {@link Slot#op_getattribute} has signature
     * {@link Signature#GETATTR} and provides attribute read access on
     * this type object and its metatype. This is very like
     * {@code object.__getattribute__}
     * ({@link PyBaseObject#__getattribute__(Object, String)}), but the
     * instance is replaced by a type object, and that object's type is
     * a meta-type (which is also a {@code type}).
     * <p>
     * The behavioural difference is that in looking for attributes on a
     * type:
     * <ul>
     * <li>we use {@link #lookup(String)} to search along along the MRO,
     * and</li>
     * <li>if we find a descriptor, we use it.
     * ({@code object.__getattribute__} does not check for descriptors
     * on the instance.)</li>
     * </ul>
     * <p>
     * The following order of precedence applies when looking for the
     * value of an attribute:
     * <ol>
     * <li>a data descriptor from the dictionary of the meta-type</li>
     * <li>a descriptor or value in the dictionary of {@code type}</li>
     * <li>a non-data descriptor or value from dictionary of the meta
     * type</li>
     * </ol>
     *
     * @param name of the attribute
     * @return attribute value
     * @throws AttributeError if no such attribute
     * @throws Throwable on other errors, typically from the descriptor
     */
    // Compare CPython type_getattro in typeobject.c
    protected Object __getattribute__(String name) throws AttributeError, Throwable {

        PyType metatype = getType();
        MethodHandle descrGet = null;

        // Look up the name in the type (null if not found).
        Object metaAttr = metatype.lookup(name);
        if (metaAttr != null) {
            // Found in the metatype, it might be a descriptor
            Operations metaAttrOps = Operations.of(metaAttr);
            descrGet = metaAttrOps.op_get;
            if (metaAttrOps.isDataDescr()) {
                // metaAttr is a data descriptor so call its __get__.
                try {
                    // Note the cast of 'this', to match op_get
                    return descrGet.invokeExact(metaAttr, (Object)this, metatype);
                } catch (Slot.EmptyException e) {
                    /*
                     * Only __set__ or __delete__ was defined. We do not catch
                     * AttributeError: it's definitive. Suppress trying __get__ again.
                     */
                    descrGet = null;
                }
            }
        }

        /*
         * At this stage: metaAttr is the value from the meta-type, or a
         * non-data descriptor, or null if the attribute was not found. It's
         * time to give the type's instance dictionary a chance.
         */
        Object attr = lookup(name);
        if (attr != null) {
            // Found in this type. Try it as a descriptor.
            try {
                /*
                 * Note the args are (null, this): we respect descriptors in this
                 * step, but have not forgotten we are dereferencing a type.
                 */
                return Operations.of(attr).op_get.invokeExact(attr, (Object)null, this);
            } catch (Slot.EmptyException e) {
                // We do not catch AttributeError: it's definitive.
                // Not a descriptor: the attribute itself.
                return attr;
            }
        }

        /*
         * The name wasn't in the type dictionary. metaAttr is now the
         * result of look-up on the meta-type: a value, a non-data
         * descriptor, or null if the attribute was not found.
         */
        if (descrGet != null) {
            // metaAttr may be a non-data descriptor: call __get__.
            try {
                return descrGet.invokeExact(metaAttr, (Object)this, metatype);
            } catch (Slot.EmptyException e) {}
        }

        if (metaAttr != null) {
            /*
             * The attribute obtained from the meta-type, and that turned out
             * not to be a descriptor, is the return value.
             */
            return metaAttr;
        }

        // All the look-ups and descriptors came to nothing :(
        throw Abstract.noAttributeError(this, name);
    }

    /**
     * {@link Slot#op_setattr} has signature {@link Signature#SETATTR}
     * and provides attribute write access on this type object. The
     * behaviour is very like the default {@code object.__setattr__}
     * except that it has write access to the type dictionary that is
     * denied through {@link #getDict()}.
     *
     * @param name of the attribute
     * @param value to give the attribute
     * @throws AttributeError if no such attribute or it is read-only
     * @throws Throwable on other errors, typically from the descriptor
     */
    // Compare CPython type_setattro in typeobject.c
    protected void __setattr__(String name, Object value) throws AttributeError, Throwable {

        // Accommodate CPython idiom that set null means delete.
        if (value == null) {
            // Do this to help porting. Really this is an error.
            __delattr__(name);
            return;
        }

        // Trap immutable types
        if (!flags.contains(Flag.MUTABLE))
            throw Abstract.cantSetAttributeError(this);

        // Check to see if this is a special name
        boolean special = isDunderName(name);

        // Look up the name in the meta-type (null if not found).
        Object metaAttr = getType().lookup(name);
        if (metaAttr != null) {
            // Found in the meta-type, it might be a descriptor.
            Operations metaAttrOps = Operations.of(metaAttr);
            if (metaAttrOps.isDataDescr()) {
                // Try descriptor __set__
                try {
                    metaAttrOps.op_set.invokeExact(metaAttr, (Object)this, value);
                    if (special) { updateAfterSetAttr(name); }
                    return;
                } catch (Slot.EmptyException e) {
                    // We do not catch AttributeError: it's definitive.
                    // Descriptor but no __set__: do not fall through.
                    throw Abstract.readonlyAttributeError(this, name);
                }
            }
        }

        /*
         * There was no data descriptor, so we will place the value in the
         * object instance dictionary directly.
         */
        // Use the privileged put
        dict.put(name, value);
        if (special) { updateAfterSetAttr(name); }
    }

    /**
     * {@link Slot#op_delattr} has signature {@link Signature#DELATTR}
     * and provides attribute deletion on this type object. The
     * behaviour is very like the default {@code object.__delattr__}
     * except that it has write access to the type dictionary that is
     * denied through {@link #getDict()}.
     *
     * @param name of the attribute
     * @throws AttributeError if no such attribute or it is read-only
     * @throws Throwable on other errors, typically from the descriptor
     */
    // Compare CPython type_setattro in typeobject.c
    protected void __delattr__(String name) throws AttributeError, Throwable {

        // Trap immutable types
        if (!flags.contains(Flag.MUTABLE))
            throw Abstract.cantSetAttributeError(this);

        // Check to see if this is a special name
        boolean special = isDunderName(name);

        // Look up the name in the meta-type (null if not found).
        Object metaAttr = getType().lookup(name);
        if (metaAttr != null) {
            // Found in the meta-type, it might be a descriptor.
            Operations metaAttrOps = Operations.of(metaAttr);
            if (metaAttrOps.isDataDescr()) {
                // Try descriptor __delete__
                try {
                    metaAttrOps.op_delete.invokeExact(metaAttr, (Object)this);
                    if (special) { updateAfterSetAttr(name); }
                    return;
                } catch (Slot.EmptyException e) {
                    // We do not catch AttributeError: it's definitive.
                    // Data descriptor but no __delete__.
                    throw Abstract.mandatoryAttributeError(this, name);
                }
            }
        }

        /*
         * There was no data descriptor, so it's time to give the type
         * instance dictionary a chance to receive. A type always has a
         * dictionary so this.dict can't be null.
         */
        // Use the privileged remove
        Object previous = dict.remove(name);
        if (previous == null) {
            // A null return implies it didn't exist
            throw Abstract.noAttributeError(this, name);
        }

        if (special) { updateAfterSetAttr(name); }
        return;
    }

    // plumbing --------------------------------------------------

    /**
     * Given the bases of a new class, choose the {@code type} on which
     * a sub-class should be implemented.
     * <p>
     * When a sub-class is defined in Python, it may have several bases,
     * each with their own Java implementation. What Java class should
     * implement the new sub-class? This chosen Java class must be
     * acceptable as {@code self} to a method (slot functions,
     * descriptors) inherited from any base. The methods of
     * {@link PyBaseObject} accept any {@link Object}, but all other
     * implementation classes require an instance of their own type to
     * be presented.
     * <p>
     * A method will accept any Java sub-type of the type of its
     * declared parameter. We ensure compatibility by choosing that the
     * implementation Java class of the new sub-type is a Java sub-class
     * of the implementation types of all the bases (excluding those
     * implemented on {@link PyBaseObject}).
     * <p>
     * This imposes a constraint on the bases, except for those
     * implemented by PyBaseObject, that their implementations have a
     * common Java descendant. (The equivalent constraint in CPython is
     * that the layout of the {@code struct} that represents an instance
     * of every base should match a truncation of the one chosen.)
     *
     * @param bases sub-classed by the new type
     * @return the acceptable base
     */
    // Compare CPython best_base in typeobject.c
    private static PyType bestBase(PyType[] bases) {
        // XXX This is a stop-gap answer: revisit in due course.
        /*
         * Follow the logic of CPython typeobject.c, but adapted to a Java
         * context.
         */
        if (bases.length == 0)
            return OBJECT_TYPE;
        else {
            return bases[0];
        }
    }

    // Compare CPython _PyType_GetDocFromInternalDoc
    // in typeobject.c
    // XXX Consider implementing in ArgParser instead
    static Object getDocFromInternalDoc(String name, String doc) {
        // TODO Auto-generated method stub
        return null;
    }

    // Compare CPython: PyType_GetTextSignatureFromInternalDoc
    // in typeobject.c
    // XXX Consider implementing in ArgParser instead
    static Object getTextSignatureFromInternalDoc(String name, String doc) {
        // TODO Auto-generated method stub
        return null;
    }
}
