package org.python.core;
import java.lang.reflect.*;

/**
 * Provides a means of using the Java 2
 * {Field|Method|Constructor}.setAccessibility() methods that can be
 * compiled with Java 1 or Java 2.
 *
 * When compiling with Java 2, Java2Accessibility.java must also be
 * compiled.  If compiled Java 1, or if the Java2Accessibility class cannot
 * be found, then the methods here have no effect.
 */

public class JavaAccessibility
{
    private static JavaAccessibility access = null;

    static void initialize() {
        // If we can find it, and the registry option
        // python.security.respectJavaAccessibility is set, then we set the
        // access object to an instance of the subclass Java2Accessibility
        if (Options.respectJavaAccessibility)
            return;
        try {
            Class c = Class.forName("org.python.core.Java2Accessibility");
            Class.forName("java.lang.reflect.AccessibleObject");
            access = (JavaAccessibility)c.newInstance();
        }
        catch (InstantiationException e) {}
        catch (IllegalAccessException e) {}
        catch (ClassNotFoundException e) {}
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
