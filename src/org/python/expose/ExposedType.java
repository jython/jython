package org.python.expose;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Documented;

import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;

/**
 * Indicates a given class should be made visible to Python code as a builtin type.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ExposedType {

    /**
     * @return the name to expose this item as. Defaults to the actual name of the class.
     */
    String name() default "";

    /**
     * @return the base type of this type. Must be another class annotated with {@code ExposedType}.
     *         If unspecified, the base is set to {@code object}, or {@code PyObject.class}.
     */
    Class base() default Object.class;

    /**
     * @return whether this type allows subclassing.
     */
    boolean isBaseType() default true;

    /**
     * @return the {@code __doc__} string for this type.
     */
    String doc() default "";
}
