package org.python.expose;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Documented;

/**
 * Can appear on two forms of methods to indicate a method should be part of the {@code __new__}
 * object creation. Can only appear once per exposed type.
 * <p>
 * In the first form, the method must be static and take the arguments
 * {@code PyNewWrapper new_, boolean init, PyType subtype, PyObject[] args, String[] keywords}.
 * In this case, the method has full responsibility for creating and initiating the object and
 * will be invoked for every subtype of this exposed type. Essentially it's for object
 * instantiation that must be called for every instance of that object.
 * See {@link org.python.core.PyInteger#int_new} for an example of this type of {@code ExposedNew}.
 * <p>
 * In the second form, the method must be an instance method that takes the standard Jython call
 * arguments, {@code PyObject[] args, String[] keywords}. In this case, the basic new
 * functionality is handled by {@link org.python.core.PyOverridableNew} and the method with
 * {@code ExposedNew} is called as {@code __init__} as part of that process.
 * This allows subtypes to completely redefine new and create objects however they like.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExposedNew {}
