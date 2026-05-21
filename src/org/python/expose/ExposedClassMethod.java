package org.python.expose;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Documented;

/**
 * Indicates a method should be exposed as a classmethod to Python code.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExposedClassMethod {

    /**
     * @return the names to expose this method as. Defaults to just actual name of the method.
     */
    String[] names() default {};

    /**
     * @return default arguments. Starts at the number of arguments - defaults.length.
     */
    String[] defaults() default {};

    /**
     * @return the {@code __doc__} string for this method.
     */
    String doc() default "";
}
