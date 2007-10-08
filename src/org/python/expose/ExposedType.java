package org.python.expose;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Indicates a given class should be made visible to Python code as a builtin
 * type.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ExposedType {

    /**
     * @return the name to expose this item as. Defaults to the actual name of
     *         the class.
     */
    String name() default "";
}
