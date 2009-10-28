/* Copyright (c) Jython Developers */
package org.python.modules.posix;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.python.core.PyObject;

/**
 * Utility class for hiding PosixModule methods depending on the platform, or whether the
 * underlying posix implementation is native.
 */
class Hider {

    /**
     * Hide module level functions defined in the PosixModule dict not applicable to this
     * OS, identified by the PosixModule.Hide annotation.
     *
     * @param cls the PosixModule class
     * @param dict the PosixModule module dict
     * @param os the underlying OS
     * @param native whether the underlying posix is native
     */
    public static void hideFunctions(Class<?> cls, PyObject dict, OS os, boolean isNative) {
        for (Method method: cls.getDeclaredMethods()) {
            if (isHidden(method, os, isNative ? PosixImpl.NATIVE : PosixImpl.JAVA)) {
                dict.__setitem__(method.getName(), null);
            }
        }
    }

    /**
     * Determine if method should be hidden for this OS/PosixImpl.
     */
    private static boolean isHidden(Method method, OS os, PosixImpl posixImpl) {
        if (method.isAnnotationPresent(Hide.class)) {
            Hide hide = method.getAnnotation(Hide.class);
            if (hide.posixImpl() != PosixImpl.NOT_APPLICABLE && hide.posixImpl() == posixImpl) {
                return true;
            }
            for (OS hideOS : hide.value()) {
                if (os == hideOS) {
                    return true;
                }
            }
        }
        return false;
    }
}

/**
 * Tags PosixModule methods as hidden on the specified OS or PosixImpl.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface Hide {

    /** Hide method on these OSes. */
    OS[] value() default {};

    /**
     * @Hide(posixImpl = PosixImpl.JAVA) hides the method from Python when the POSIX
     * library isn't native. The default NOT_APPLICABLE means the POSIX implementation
     * doesn't matter.
     */
    PosixImpl posixImpl() default PosixImpl.NOT_APPLICABLE;
}

/**
 * The type of underlying POSIX library implementation (native or not).
 */
enum PosixImpl {NOT_APPLICABLE, NATIVE, JAVA};
