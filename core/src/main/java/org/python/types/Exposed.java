// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.types;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.python.core.PyAttributeError;
import org.python.core.PyBaseException;
import org.python.core.PyDict;
import org.python.core.PyTuple;

/**
 * Annotations that may be placed on elements of a Java class intended
 * as the implementation of a Python type, and that the type system will
 * look for during the definition of a Python type.
 */
public interface Exposed {

    /**
     * Identify a Python instance method of a type or module defined in Java
     * and exposed to Python. The signature must be a supported type for
     * which coercions can be found for its parameters.
     * <p>
     * When found in the classes that define a built-in type, this
     * annotation results in a method definition, then a descriptor in the
     * dictionary of the type. When found in the class that defines a
     * built-in module, this annotation results in a method definition in
     * the module specification, and a bound method in the dictionary of
     * each module instance created from it.
     * <p>
     * Annotations may appear on the parameters of a method annotated with
     * {@code PythonMethod}. These further describe the method, defining the
     * parameters as positional-only parameters, or providing default
     * values. A method may also be annotated with a documentation string
     * (in the Python sense), by means of the &#064;{@link DocString}
     * annotation.
     * <p>
     * In types that accept multiple implementations, more than one method
     * of the same name may be annotated {@code PythonMethod}. Only one may
     * be the primary definition (see {@link PythonMethod#primary()}, and
     * only in that one are the documentation string and parameter
     * annotations effective. (It need not be the first definition.) These
     * annotations on the primary definition define the signature that
     * Python sees.
     */
    @Documented
    @Retention(RUNTIME)
    @Target(METHOD)
    @interface PythonMethod {
        /**
         * Exposed name of the method if different from the declaration.
         *
         * @return name of the method
         */
        String value() default "";

        /**
         * The element {@code primary=false} is used to indicate that the
         * annotated method is <b>not</b> the primary definition.
         *
         * @return {@code true} (the default) if and only if this is the primary
         *     definition of the method
         */
        boolean primary() default true;

        /**
         * The element {@code positionalOnly=false} is used to indicate that the
         * arguments in a call to the annotated method may be provided by
         * keyword. This provides the call with the semantics of a method
         * defined in Python, where <pre>
         * def g(a, b, c):
         *     print(a, b, c)
         * </pre> may be called as <pre>
         * &gt;&gt;&gt; g(b=2, c=3, a=1)
         * 1 2 3
         * &gt;&gt;&gt; g(**dict(b=2, c=3, a=1))
         * 1 2 3
         * </pre> It is as if we had annotated an imaginary parameter before the
         * first declared parameter (or {@code self}) with
         * &#064;{@link PositionalOnly}.
         * <p>
         * The default {@code positional=true} is the more frequent case for
         * built-in methods, although it is the opposite of the default for
         * methods defined in Python where it would have to be expressed as
         * {@code def g(a, b, c, /)}.
         *
         * @return {@code true} (the default) if and only if the parameters
         *     should be interpreted as positional from the start.
         */
        boolean positionalOnly() default true;
    }

    /**
     * Identify a Python static method of a type or module defined in Java
     * and exposed to Python. The signature must be a supported type for
     * which coercions can be found for its parameters.
     * <p>
     * When found in the classes that define a built-in type, this
     * annotation results in a method definition, then a
     * {@code staticmethod} object in the dictionary of the type. When found
     * in the class that defines a built-in module, this annotation results
     * in a method definition in the module specification, and an unbound
     * method in the dictionary of each module instance created from it.
     * <p>
     * Annotations may appear on the parameters of a method annotated with
     * {@code PythonStaticMethod}. These further describe the method,
     * defining the parameters as positional-only parameters, or providing
     * default values. A method may also be annotated with a documentation
     * string (in the Python sense), by means of the &#064;{@link DocString}
     * annotation.
     * <p>
     * Only one method of the given name, in a given class, may be annotated
     * as a {@code PythonStaticMethod}.
     */
    @Documented
    @Retention(RUNTIME)
    @Target(METHOD)
    @interface PythonStaticMethod {
        /**
         * Exposed name of the function if different from the declaration.
         *
         * @return name of the function
         */
        String value() default "";

        /**
         * The element {@code positionalOnly=false} is used to indicate that the
         * arguments in a call to the annotated method may be provided by
         * keyword. The semantics are the same as
         * {@link PythonMethod#positionalOnly()}
         *
         * @return {@code true} (the default) if and only if the parameters
         *     should be interpreted as positional from the start.
         */
        boolean positionalOnly() default true;
    }

    /**
     * Identify a Python {@code __new__} method of a type defined in Java
     * and exposed to Python. The signature must be a supported type for
     * which coercions can be found for its parameters.
     * <p>
     * When found in the classes that define a built-in type, this
     * annotation results in a {@code builtin_function_or_method} object in
     * the dictionary of the type. It is not valid in a class that defines a
     * built-in module.
     * <p>
     * Annotations may appear on the parameters of a method annotated with
     * {@code PythonNewMethod}. These further describe the method, defining
     * the parameters as positional-only parameters, or providing default
     * values. A method may also be annotated with a documentation string
     * (in the Python sense), by means of the &#064;{@link DocString}
     * annotation.
     * <p>
     * Only one method called {@code __new__} may be annotated as a
     * {@code PythonNewMethod} in a given class. The element
     * {@code positionalOnly=false} is not available on
     * {@code PythonNewMethod}: the type parameter must always be given
     * first, and cannot be given by keyword.
     */
    @Documented
    @Retention(RUNTIME)
    @Target(METHOD)
    @interface PythonNewMethod {
        /**
         * Exposed name of the function if different from the declaration.
         *
         * @return name of the function
         */
        String value() default "";
    }

    /**
     * Specify the documentation string ({@code __doc__}) for a method,
     * field, etc. defined in Java and exposed to Python. For reference, a
     * multi-line documentation string is correctly specified like
     * this:<pre>
     * &#64;DocString("""
     *     Return first index of value.
     *
     *     Raises ValueError if the value is not present.""")
     * </pre>
     */
    @Documented
    @Retention(RUNTIME)
    @Target({METHOD, FIELD, TYPE})
    @interface DocString {
        /** @return the argument given the annotation. */
        String value();
    }

    /**
     * Override the name of an parameter to a method defined in Java, as it
     * will appear to Python (in generated signatures and error messages).
     * It is preferable to use a name in Java that conventional for Python,
     * and is only necessary to annotate one when the conventional name is
     * impossible (e.g. "new").
     */
    @Documented
    @Retention(RUNTIME)
    @Target(PARAMETER)
    @interface Name {
        /** @return the argument given the annotation. */
        String value();
    }

    /**
     * Annotates the <b>last</b> positional-only parameter. This is
     * equivalent to following it with ", /" in a Python signature.
     */
    @Documented
    @Retention(RUNTIME)
    @Target(PARAMETER)
    @interface PositionalOnly {}

    /**
     * Annotates the <b>first</b> keyword-only parameter. This is equivalent
     * to preceding it with "*, " in a Python signature.
     */
    @Documented
    @Retention(RUNTIME)
    @Target(PARAMETER)
    @interface KeywordOnly {}

    /**
     * Provide default value for the annotated parameter. This is equivalent
     * to following it with "=" in a Python signature. Allowed values are a
     * small subset of the Python literals, presented as a {@code String}:
     * "None", an integer, or a single-quoted string. For example <pre>
     *  PyList split(
     *      &#64;Default("None") Object sep,
     *      &#64;Default("-1") int maxsplit
     *  )
     * </pre>
     */
    @Documented
    @Retention(RUNTIME)
    @Target(PARAMETER)
    @interface Default {
        /** @return the argument given the annotation. */
        String value();
    }

    /**
     * Annotates the collector for excess positional arguments. This is
     * equivalent to preceding the name with "*" in a Python signature. The
     * type must be {@link PyTuple}.
     * <p>
     * A {@code PositionalCollector} argument must be last in the Java
     * declaration, or followed by a {@link KeywordCollector} only, as it is
     * in a Python {@code frame}. It will appear in its natural position, as
     * {@code *name} between positional and keyword arguments, in the Python
     * method signature.
     */
    // XXX Try to place between positional and keywords in Java?
    @Documented
    @Retention(RUNTIME)
    @Target(PARAMETER)
    @interface PositionalCollector {}

    /**
     * Annotates the collector for excess keyword arguments. This is
     * equivalent to preceding the name with "**" in a Python signature. The
     * type must be {@link PyDict}.
     * <p>
     * A {@code KeywordCollector} argument must be last in the Java
     * declaration, as it is in a Python {@code frame}. It will appear in
     * its natural position, as {@code **name} after keyword arguments, in
     * the Python method signature.
     */
    @Documented
    @Retention(RUNTIME)
    @Target(PARAMETER)
    @interface KeywordCollector {}

    /**
     * Identify a field of a Python object as an exposed attribute. Get, set
     * and delete operations are provided automatically on a descriptor that
     * will be entered in the dictionary of the type being defined. If the
     * field is Java {@code final} it will be read-only.
     * <p>
     * Some primitive types and {@code String} receive special support for
     * conversion from Python objects. A field of type {@code Object} may
     * easily be made a member and will then receive any Python object.
     * <p>
     * The annotated field may have any Java reference type. In that case,
     * an attempt to assign a Python object of the wrong Java type will
     * raise a {@link PyBaseException TypeError}. This makes it possible to
     * declare an attribute of a specific Python type. For example one
     * enforces {@code tuple} values by declaring the field as a
     * {@link PyTuple}. The field would also accept Python sub-classes of
     * the attribute type, since they must be sub-classes in Java too.
     * <p>
     * This approach creates a limitation where the corresponding Python
     * type has multiple Java implementations not related by Java
     * inheritance and is not specially provided for (like {@code String}).
     * The set operation of the {@link Member} attribute will reject
     * instances that have the intended Python type but non-matching Java
     * type (with a confusing {@link PyBaseException TypeError} to boot). A
     * writable attribute of that type should be implemented as
     * {@code Object} or using explicit {@link Getter}, {@link Setter} and
     * {@link Deleter} methods.
     *
     */
    @Documented
    @Retention(RUNTIME)
    @Target(FIELD)
    @interface Member {

        /**
         * Exposed name of the member if different from the field.
         *
         * @return name of the attribute
         */
        String value() default "";

        /** @return true if read-only. */
        boolean readonly() default false;

        /**
         * A member may be {@code null} from Java or deleted from Python (if not
         * read-only). In this condition:
         * <ul>
         * <li>for a member annotated with {@code optional=true}, attempts to
         * {@code get} or {@code delete} the member will produce an
         * {@link PyAttributeError AttributeError}, until it is set again.</li>
         * <li>where {@code optional=false} (default), a {@code get} will return
         * {@code None} and {@code delete} will have no effect.</li>
         * </ul>
         *
         * @return true if access following delete will raise an error
         */
        boolean optional() default false;
    }

    /**
     * Identify a method as that to be called during a Python call to
     * {@code __getattribute__} naming an exposed attribute.
     * <p>
     * The signature must be {@code ()T} where {@code T} can be
     * {@code Object} if the implementor has no reason to do otherwise. (One
     * reason might be type safety when calling the same method from Java.)
     * The annotated method is responsible for converting to {@code T} from
     * however the attribute is represented internally to the type.
     */
    @Documented
    @Retention(RUNTIME)
    @Target(METHOD)
    @interface Getter {

        /**
         * Exposed name of the attribute, if different from the Java method
         * name.
         *
         * This name will relate the {@link Getter}, {@link Setter} and
         * {@link Deleter} in a single descriptor.
         *
         * @return name of the attribute
         */
        String value() default "";
    }

    /**
     * Identify a method as that to be called during a Python call to
     * {@code __setattr__} naming an exposed attribute.
     * <p>
     * The signature must be {@code (T)V} where {@code T} is often
     * {@code Object}. The annotated method is responsible for converting
     * this to the form in which the attribute is represented internally to
     * the type. If {@code T}is something more specific than {@code Object},
     * a cast occurs to this Java type during the descriptor call, which if
     * it fails will raise a Python {@link PyBaseException TypeError}.
     */
    @Documented
    @Retention(RUNTIME)
    @Target(METHOD)
    @interface Setter {

        /**
         * Exposed name of the attribute, if different from the Java method
         * name.
         *
         * This name will relate the {@link Getter}, {@link Setter} and
         * {@link Deleter} in a single descriptor.
         *
         * @return name of the attribute
         */
        String value() default "";
    }

    /**
     * Identify a method as that to be called during a Python call to
     * {@code __delattr__} naming an exposed attribute.
     * <p>
     * The signature must be {@code ()V}.
     */
    @Documented
    @Retention(RUNTIME)
    @Target(METHOD)
    @interface Deleter {

        /**
         * Exposed name of the attribute, if different from the Java method
         * name.
         *
         * This name will relate the {@link Getter}, {@link Setter} and
         * {@link Deleter} in a single descriptor.
         *
         * @return name of the attribute
         */
        String value() default "";
    }
}
