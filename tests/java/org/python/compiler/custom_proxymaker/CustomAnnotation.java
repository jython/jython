package org.python.compiler.custom_proxymaker;

/**
 * This Annotation contains most of the possible annotation fields,
 * used to test the annotation part of custom proxymaker
 */

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
 
 
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE,ElementType.METHOD,
 ElementType.CONSTRUCTOR,ElementType.ANNOTATION_TYPE,
 ElementType.PACKAGE,ElementType.FIELD,ElementType.LOCAL_VARIABLE})
@Inherited
public @interface CustomAnnotation {
        public enum Priority { LOW, MEDIUM, HIGH }
        String value();
        String[] changedBy() default "";
        Priority[] priorities(); 
        Priority priority() default Priority.MEDIUM;
        String createdBy() default "Darjus Loktevic";
        String lastChanged() default "08/06/2012";
}
