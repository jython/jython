package org.python.expose;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a method should be exposed to Python code.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExposedMethod {

    /**
     * Returns the names to expose this method as. Defaults to the name of the method.
     */
    String[] names() default {};

    /**
     * Returns default arguments. Starts at the number of arguments - defaults.length.
     */
    String[] defaults() default {};

    /**
     * Returns how to expose this method. See {@link MethodType} for the options.
     */
    MethodType type() default MethodType.DEFAULT;

    /**
     * Returns the __doc__ String for this method.
     */
    String doc() default "";
}