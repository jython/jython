package org.python.expose;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Indicates a given method or class should be made visible to Python code.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Exposed {

    /**
     * @return the name to expose this item as.  Defaults to the actual name of the item.
     */
    String name() default "";

    /**
     * @return default arguments for a method.  Starts at the number of arguments - defaults.length
     */
    String[] defaults() default {};
}
