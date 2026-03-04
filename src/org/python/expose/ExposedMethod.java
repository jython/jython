package org.python.expose;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Documented;

/**
 * Indicates a method should be exposed to Python code.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExposedMethod {

    /**
     * @return the names to expose this method as. Defaults to the name of the method.
     */
    String[] names() default {};

    /**
     * @return default arguments. Starts at the number of arguments - defaults.length.
     */
    String[] defaults() default {};

    /**
     * @return how to expose this method. See {@link MethodType} for the options.
     */
    MethodType type() default MethodType.DEFAULT;

    /**
     * @return the {@code __doc__} string for this method.
     */
    String doc() default "";
}
