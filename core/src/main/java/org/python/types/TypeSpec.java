package org.python.types;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import org.python.core.Py;
import org.python.core.PyBaseObject;
import org.python.core.PyType;
import org.python.support.InterpreterError;

/**
 * A specification for a Python type. A Java class intended as the
 * implementation of a Python object creates one of these data
 * structures during static initialisation, and configures it using
 * the mutators. A fluent interface makes this configuration
 * readable as a single, long statement.
 */
public class TypeSpec {

    /** Name of the class being specified. */
    public final String name;

    /** Delegated authorisation to resolve names. */
    public final Lookup lookup;

    /**
     * The defining class for the type being specified, in which the
     * {@code Spec.lookup} was created.
     */
    public final Class<?> definingClass;

    /**
     * Additional class in which to look up method names or
     * {@code null}. See {@link #methods(Class)}
     */
    public Class<?> methodClass;

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
    public EnumSet<TypeFlag> flags = TypeSpec.getDefaultFlags();

    /**
     * Create (begin) a specification for a {@link PyType} based on a
     * specified implementation class.
     * <p>
     * {@link PyType#fromSpec(TypeSpec)} will interrogate the implementation
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
    public TypeSpec(String name, Class<?> definingClass, Lookup lookup) {
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
     * {@link PyType#fromSpec(TypeSpec)} will interrogate the implementation
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
    public TypeSpec(String name, Lookup lookup) { this(name, lookup.lookupClass(), lookup); }

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
    public TypeSpec(String name, Class<?> implClass) {
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
    public TypeSpec canonical(Class<?> impl) {
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
    public TypeSpec adopt(Class<?>... classes) {
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
    public TypeSpec accept(Class<?>... classes) {
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
    public TypeSpec operand(Class<?>... classes) {
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
    public int adoptedCount() { return adoptedCount; }

    /**
     * The number of classes specified as canonical, adopted or accepted
     * as {@code self} for the Python type being specified.
     *
     * @return number of accepted classes
     */
    public int acceptedCount() { return acceptedCount; }

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
    public TypeSpec base(PyType base) {
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
    static EnumSet<TypeFlag> getDefaultFlags() { return EnumSet.of(TypeFlag.BASETYPE); }

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
    TypeSpec flag(TypeFlag f) {
        flags.add(f);
        return this;
    }

    /**
     * Specify a characteristic (type flag), or several, to be added.
     *
     * @param f to add to the current flags
     * @return {@code this}
     */
    public TypeSpec flag(TypeFlag... f) {
        for (TypeFlag x : f) { flags.add(x); }
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
    public TypeSpec flagNot(TypeFlag f) {
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
    public TypeSpec metaclass(PyType metaclass) {
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
    public TypeSpec methods(Class<?> methodClass) {
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
    TypeSpec binops(Class<?> binopClass) {
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
    public Class<?>[] getClasses() { return classes.toArray(new Class<?>[classes.size()]); }

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
                return PyType.ONLY_OBJECT;         // Normally
            else
                return PyType.EMPTY_TYPE_ARRAY;    // For 'object'
        } else
            return bases.toArray(new PyType[bases.size()]);
    }

    /**
     * Return the meta-class of the type being created. If none was set,
     * it is {@link PyType#TYPE}..
     *
     * @return the proper meta-class
     */
    public PyType getMetaclass() { return metaclass != null ? metaclass : PyType.TYPE; }

    // Something helpful in debugging (__repr__ is different)
    @Override
    public String toString() {
        String fmt = "'%s' %s, flags=%s def=%s";
        return String.format(fmt, name, bases, flags, definingClass().getSimpleName());
    }
}