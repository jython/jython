package org.python.expose;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.python.core.PyInteger;

/**
 * Can appear on two forms of methods to indicate a method should be part of the __new__ object
 * creation. Can only appear once per exposed type.
 * 
 * In the first form, the method must be static and take the arguments
 * <code>PyNewWrapper new_, boolean init, PyType
 subtype, PyObject[] args, String[] keywords</code>.
 * In this case, the method has full responsibility for creating and initting the object and will be
 * invoked for every subtype of this exposed type. Essentially its for object instantation that must
 * be called for every instance of that object. See {@link PyInteger#int_new} for an example of this
 * type of ExposedNew.
 * 
 * In the second form, the method must be an instance method that takes the standard Jython call
 * arguments, <code>PyObject[] args, String[] keywords</code>. In this case, the basic new
 * functionality is handled by PyOverridableNew and the method with ExposedNew is called as __init__
 * as part of that process. This allows subtypes to completely redefine new and create objects
 * however they like.
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExposedNew {}
