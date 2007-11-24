package org.python.expose;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Indicates a method should be made visible to Python code as the new method on
 * a type, __new__ in Python terms. Only one per type and the method must be
 * static and take the new arguments <code>PyNewWrapper new_, boolean init, PyType
 subtype, PyObject[] args, String[] keywords</code>.
 */ 
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExposedNew {}
