/* Copyright (c) 2009 Jython project */
package org.python.core.util;

import org.jruby.ext.posix.util.Platform;

/**
 * Methods for testing the platform/operating system that we are on.
 */
public class PlatformUtil {

    /**
     * @return True if the operating system we are on is case insensitive,
     *         where case insensitive means that a file that is stored as FOO
     *         can be accessed as (for example) FoO.
     */
    public static boolean isCaseInsensitive() {
        // Currently we just check to see if we are on windows or macs, which are commonly
        // (though not always!) case insensitive.  There are certainly cases where this is
        // not sufficient, like the case of mounted filesystems.
        return Platform.IS_MAC || Platform.IS_WINDOWS;
    }
}
