package org.python.expose;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target( {ElementType.METHOD, ElementType.FIELD})
public @interface ExposedGet {

    String name() default "";

    /**
     * Returns the __doc__ String for this descriptor.
     */
    String doc() default "";
}
