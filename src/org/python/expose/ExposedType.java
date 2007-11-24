package org.python.expose;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;

/**
 * Indicates a given class should be made visible to Python code as a builtin
 * type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ExposedType {

    /**
     * @return the name to expose this item as. Defaults to the actual name of
     *         the class.
     */
    String name() default "";
}
