package org.python.core;
import java.lang.reflect.*;

/**
 * Provides the Java 2 {Field|Method|Constructor}.setAccessibility()
 * methods when compiled with, and running under Java 2.
 *
 * This class should not be compilied (and it won't compile) under Java 1.
 */

class Java2Accessibility extends JavaAccessibility
{
    void setAccess(Field field, boolean flag) throws SecurityException {
        field.setAccessible(flag);
    }

    void setAccess(Method method, boolean flag) throws SecurityException {
        method.setAccessible(flag);
    }

    void setAccess(Constructor constructor, boolean flag)
        throws SecurityException
    {
        constructor.setAccessible(flag);
    }
}
