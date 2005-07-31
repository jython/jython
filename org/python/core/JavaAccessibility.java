package org.python.core;
import java.lang.reflect.*;

/**
 * Provides a means of using the Java 2
 * {Field|Method|Constructor}.setAccessibility() methods.
 *
 * This class was formerly necessary for Java 1 compattibility.
 * In the future, this class may be removed.
 */

class JavaAccessibility
{
    private static JavaAccessibility access = null;

    static void initialize() {
        // If registry option
        // python.security.respectJavaAccessibility is set, then we set the
        // access object to an instance of the subclass Java2Accessibility
        if (Options.respectJavaAccessibility)
            return;
        access = new Java2Accessibility();
    }

    static boolean accessIsMutable() {
        return access != null;
    }

    /**
     * These methods get overridden in the Java2Accessibility subclass
     */
    void setAccess(Field field, boolean flag) throws SecurityException {
    }

    void setAccess(Method method, boolean flag) throws SecurityException {
    }

    void setAccess(Constructor constructor, boolean flag)
        throws SecurityException
    {}

    public static void setAccessible(Field field, boolean flag)
        throws SecurityException
    {
        if (access != null)
            access.setAccess(field, flag);
    }

    public static void setAccessible(Method method, boolean flag)
        throws SecurityException
    {
        if (access != null)
            access.setAccess(method, flag);
    }

    public static void setAccessible(Constructor constructor, boolean flag)
        throws SecurityException
    {
        if (access != null)
            access.setAccess(constructor, flag);
    }
}
