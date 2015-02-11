package org.python.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a given class is not traversable and does
 * intentionally not implement {@link org.python.core.Traverseproc}.
 * This annotation is ignored if the class implements {@code Traverseproc},
 * i.e. it cannot be used to remove {@code Traverseproc} support of a
 * superclass. Thus it is well defined what happens if both
 * {@code Traverseproc} and {@code {@literal @}Untraversable}
 * are present: {@code Traverseproc} wins.<br>
 * If a class does not implement {@code Traverseproc} and is not
 * annotated with {@code {@literal @}Untraversable}, gc assumes
 * that the programmers were not aware of Jython's traverse
 * mechanism and attempts to traverse the target object by using
 * Java-reflection (which is assumably very inefficient).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Untraversable {
    //this is a pure marker interface
}
