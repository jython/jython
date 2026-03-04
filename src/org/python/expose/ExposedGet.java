package org.python.expose;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Documented;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target( {ElementType.METHOD, ElementType.FIELD})
public @interface ExposedGet {

    String name() default "";

    /**
     * @return the {@code __doc__} string for this descriptor.
     */
    String doc() default "";
}
