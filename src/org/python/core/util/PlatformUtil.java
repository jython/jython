// Copyright (c) 2009 Jython project
package org.python.core.util;

// Note: Sun does not sponsor the jna project, even though the package name
// might imply otherwise.
import com.sun.jna.Platform;

/**
 * Methods for testing the platform/operating system that we are on.
 */
public class PlatformUtil {

    /**
     * @return True if the operating system we are on is case insensitive,
     *         where case insensitive means that a file that is stored as FOO
     *         can be accessed as (for example) FoO.
     */
    //Currently we just check to see if we are on windows or macs, which are
    //commonly (though not always!) case insensitive.  There are certainly cases
    //where this is not sufficient, like the case of mounted filesystems.
    public static boolean isCaseInsensitive() {
        return Platform.isMac() || Platform.isWindows();
    }
}
