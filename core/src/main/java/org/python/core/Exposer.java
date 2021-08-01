package org.python.core;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.python.base.InterpreterError;

/**
 * An object for tabulating the attributes of classes that define
 * Python types or modules.
 */
abstract class Exposer {

    /**
     * The index of intermediate descriptions by name, in which we may
     * locate specifications already made or begun, using the name.
     */
    final Map<String, Spec> specs;

    /** Construct the base with its table of entries. */
    protected Exposer() {
        this.specs = new HashMap<>();
    }

    /** @return which {@link ScopeKind} of {@code Exposer} is this? */
    abstract ScopeKind kind();

    /**
     * On behalf of the given type defined in Java, build a description
     * of the attributes discovered by introspection of the class (or
     * classes) provided.
     * <p>
     * Special methods are identified by their reserved name, while
     * other attributes are identified by annotations. (See
     * {@code Exposed}.)
     * <p>
     * In those attributes that reference their defining Python type
     * (descriptors), the {@code type} object will be referenced (as
     * {@link Descriptor#objclass}). It is not otherwise accessed, since
     * it is (necessarily) incomplete at this time.
     *
     * @param type to which these attributes apply
     * @param definingClass to introspect for members
     * @param methodClass additional class to introspect for members (or
     *     {@code null})
     * @return a type exposer able to deliver the attributes
     * @throws InterpreterError on errors of definition
     */
    static TypeExposer exposeType(PyType type, Class<?> definingClass, Class<?> methodClass)
            throws InterpreterError {

        // Create an instance of Exposer to hold specs, type.
        TypeExposer exposer = new TypeExposer(type);

        // Scan the defining class for definitions
        exposer.expose(definingClass);

        // Scan the supplementary class for definitions
        if (methodClass != null) {
            exposer.expose(methodClass);
        }

        // For each definition we found, add the attribute
        return exposer;
    }

    /**
     * Create an exception with a message along the lines "'NAME',
     * already exposed as SPEC, cannot be NEW_SPEC" where the
     * place-holders are filled from the corresponding arguments (or
     * their names or type names).
     *
     * @param name being defined
     * @param member field or method annotated
     * @param newSpec of the new entry apparently requested
     * @param priorSpec of the inconsistent, existing entry
     * @return the required error
     */
    static InterpreterError duplicateError(String name, Member member, Spec newSpec,
            Spec priorSpec) {
        String memberName = member.getName();
        String memberString = memberName == name ? "" : " (called '" + memberName + "' in source)";
        String priorSpecType = priorSpec.annoClassName();
        String newSpecType = newSpec.annoClassName();
        if (priorSpecType.equals(newSpecType)) {
            newSpecType = "redefined";
        }
        return new InterpreterError(ALREADY_EXPOSED, name, memberString, priorSpecType,
                newSpecType);
    }

    private static final String ALREADY_EXPOSED = "'%s'%s, already exposed as %s, cannot be %s";

    /**
     * A helper that avoids repeating nearly the same code for adding
     * each particular sub-class of {@link Spec} when a method is
     * encountered. The implementation finds or creates a {@code Spec}
     * by the given name or method name. It then adds this {@code Spec}
     * to {@link #specs}. The caller provides a factory method, in case
     * a new {@code Spec} is needed, a method for adding the Spec to a
     * type-specific list, and a method for adding the method to the
     * {@code Spec}.
     *
     * @param <MS> the type of {@link Spec} being added or added to.
     * @param m the method being adding to the {@code MS}
     * @param name specified in the annotation or {@code null}
     * @param cast to the {@code MS} if possible or {@code null}
     * @param makeSpec constructor for an {@code MS}
     * @param addSpec function to add the {@code MS} to the proper list
     * @param addMethod function to update the {@code MS} with a method
     */
    <MS extends BaseMethodSpec> void addSpec(Method m, String name, Function<Spec, MS> cast, //
            Function<String, MS> makeSpec, //
            Consumer<MS> addSpec, //
            BiConsumer<MS, Method> addMethod) {

        // The name is as annotated or the "natural" one
        if (name == null || name.length() == 0)
            name = m.getName();

        // Find any existing definition
        Spec spec = specs.get(name);
        MS entry;
        if (spec == null) {
            // A new entry is needed
            entry = makeSpec.apply(name);
            specs.put(entry.name, entry);
            addSpec.accept(entry);
            addMethod.accept(entry, m);
        } else if ((entry = cast.apply(spec)) != null) {
            // Existing entry will be updated
            addMethod.accept(entry, m);
        } else {
            /*
             * Existing entry is not compatible, but make a loose entry on which
             * to base the error message.
             */
            entry = makeSpec.apply(name);
            addMethod.accept(entry, m);
            throw duplicateError(name, m, entry, spec);
        }
    }

    /**
     * The base of classes that describe a named, built-in object,
     * during the exposure process. Instances of {@code Exposer.Spec}
     * are created, and added to a collection held by the exposer, as
     * each definition is encountered in a defining class. The
     * annotation and other factors determine the particular subclass of
     * {@code Exposer.Spec} produced.
     * <p>
     * In cases where more than one Java definition contributes to a
     * single exposed attribute, {@code Spec}s are updated as successive
     * definitions are encountered.
     * <p>
     * When exposing attributes of a Python type, the actual object to
     * be entered in a dictionary of a type or module is obtained by a
     * call to {@link #asAttribute(PyType, Lookup)}.
     */
    abstract static class Spec implements Comparable<Spec> {

        /** The Python name of the method being defined. */
        final String name;

        /** The kind of scope (type or module) being defined. */
        final ScopeKind scopeKind;

        /** Documentation string for the (eventual) descriptor. */
        String doc = null;

        /**
         * @param name of member
         * @param scopeKind module or type?
         */
        Spec(String name, ScopeKind scopeKind) {
            this.name = name;
            this.scopeKind = scopeKind;
        }

        /**
         * Create an attribute for the type being defined (suitable as an
         * entry in its dictionary).
         *
         * @param objclass defining type
         * @param lookup authorisation to access methods or fields
         * @return attribute to add
         * @throws InterpreterError on specification errors
         */
        abstract Object asAttribute(PyType objclass, Lookup lookup) throws InterpreterError;

        /** @return the documentation string (or {@code null}) */
        String getDoc() { return doc; }

        /**
         * Name the built-in being defined from a Java perspective, mostly
         * for use in messages regarding errors in definition.
         *
         * @return the Java name
         */
        abstract String getJavaName();

        /**
         * The class of annotation that creates a specification of this
         * type. This is primarily for creating for error messages that
         * direct the author of an exposed class to annotations being used
         * incompatibly. {@code WrapperSpec}s return {@code null} as special
         * methods are not identified by an annotation.
         *
         * @return type of thing exposed.
         */
        abstract Class<? extends Annotation> annoClass();

        /**
         * String version of the kind of specification this is, expressed as
         * the the type of annotation that gave rise to it.
         *
         * @return annotation type name
         */
        protected String annoClassName() {
            Class<? extends Annotation> ac = annoClass();
            if (ac == Annotation.class) {
                // Special methods recognised by name, so no annotation
                return "special method";
            } else {
                return ac.getSimpleName();
            }
        }

        @Override
        public int compareTo(Spec o) { return name.compareTo(o.name); }
    }

    /**
     * A specialisation of {@link Spec} to describe, through one or more
     * Java methods, a named, built-in method-like object, during the
     * exposure process.
     */
    static abstract class BaseMethodSpec extends Spec {

        /** Collects the methods declared (often just one). */
        final List<Method> methods;

        BaseMethodSpec(String name, ScopeKind scopeKind) {
            super(name, scopeKind);
            this.methods = new ArrayList<>(1);
        }

        /**
         * Add a method implementation to the collection.
         *
         * @param method to add to {@link #methods}
         */
        void add(Method method) { methods.add(method); }

        /** @return a name designating the method */
        @Override
        String getJavaName() {
            StringBuilder b = new StringBuilder(64);
            if (!methods.isEmpty()) {
                // It shouldn't matter, but take the last added
                Method method = methods.get(methods.size() - 1);
                b.append(method.getDeclaringClass().getSimpleName());
                b.append('.');
                b.append(method.getName());
            } else {
                // Take the name from the Spec instead
                b.append(name);
            }
            return b.toString();
        }

        @Override
        public String toString() {
            return String.format("%s(%s[%d])", getClass().getSimpleName(), name, methods.size());
        }

        /**
         * Insert a {@code MethodHandle h} into a list, such that every
         * handle in the list, of which the first parameter type is
         * assignable from the first parameter type of {@code h}, will
         * appear after {@code h} in the list. If there are none such,
         * {@code h} is added at the end. The resulting list is partially
         * ordered, and has the property that, in a forward search for a
         * handle applicable to a given class, the most specific match is
         * found first.
         *
         * @param list to add h into
         * @param h to insert/add
         */
        protected static void addOrdered(LinkedList<MethodHandle> list, MethodHandle h) {
            // Type of first parameter of h
            Class<?> c = h.type().parameterType(0);
            // We'll scan until a more general type is found
            ListIterator<MethodHandle> iter = list.listIterator(0);
            while (iter.hasNext()) {
                MethodHandle i = iter.next();
                Class<?> d = i.type().parameterType(0);
                if (d.isAssignableFrom(c)) {
                    /*
                     * d is more general than c (i is more general than h): back up and
                     * position just before i.
                     */
                    iter.previous();
                    break;
                }
            }
            // Insert h where the iterator stopped. Could be the end.
            iter.add(h);
        }

        /**
         * Convenience function to compose error when creating a descriptor
         * or method definition, when the un-reflecting to a method handle
         * fails.
         *
         * @param m method we were working on
         * @param e what went wrong
         * @return an exception to throw
         */
        protected static InterpreterError cannotGetHandle(Method m, IllegalAccessException e) {
            return new InterpreterError(e, CANNOT_GET_HANDLE, m.getName(), m.getDeclaringClass());
        }

        private static final String CANNOT_GET_HANDLE = "cannot get method handle for '%s' in '%s'";

        /**
         * Convenience function to compose error when creating a descriptor
         * or method definition and the arguments of the method handle are
         * unexpected number in type or number.
         *
         * @param type being exposed
         * @param mh handle from reflected method
         * @return an exception to throw
         */
        protected InterpreterError methodSignatureError(PyType type, MethodHandle mh) {
            return new InterpreterError(UNSUPPORTED_SIG, name, type.getName(), mh.type(),
                    annoClassName());
        }

        private static final String UNSUPPORTED_SIG =
                "method %.50s in '%.50s' has wrong signature %.100s for %.100s";
    }
}
