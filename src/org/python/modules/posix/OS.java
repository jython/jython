/* Copyright (c) Jython Developers */
package org.python.modules.posix;

import java.util.Locale;

import org.python.core.Options;
import org.python.core.PySystemState;
import org.python.core.RegistryKey;

/**
 * A Marker tagging what OS we're running on, with some accompanying information about
 * that platform.
 */
enum OS {

    // Need for "command.com" questionable (https://en.wikipedia.org/wiki/COMMAND.COM)
    NT("Windows", new String[] {getenv("ComSpec", "C:\\WINDOWS\\System32\\cmd.exe"), "/c"},
            new String[] {"command.com", "/c"}), //
    // http://bugs.jython.org/issue1842
    IBMi("OS/400", new String[] {"/QOpenSys/usr/bin/sh", "-c"}), //
    POSIX(new String[] {"/bin/sh", "-c"});

    /** An array of potential shell commands this platform may use. */
    private final String[][] shellCommands;

    /**
     * Name to match against os.name System property for identification
     * (os.name.startswith(pattern)). Defaults to name().
     */
    private final String pattern;

    OS(String pattern, String[]... shellCommands) {
        this.shellCommands = shellCommands;
        this.pattern = pattern != null ? pattern : name();
    }

    OS(String[]... shellCommands) {
        this(null, shellCommands);
    }

    String getModuleName() {
        return name().toLowerCase(Locale.ROOT);
    }

    String[][] getShellCommands() {
        return shellCommands;
    }

    /**
     * Return the OS we're running on.
     */
    static OS getOS() {
        String osName = PySystemState.registry.getProperty(
                                                RegistryKey.PYTHON_OS);
        if (osName == null) {
            osName = System.getProperty("os.name");
        }

        for (OS os : OS.values()) {
            if (osName.startsWith(os.pattern)) {
                return os;
            }
        }
        return OS.POSIX;
    }

    /**
     * Get the value of an environment variable, or return the given default if the variable is
     * undefined or the security environment prevents access. An empty string value from the
     * environment is treated as undefined.
     * <p>
     * This accesses the read-only Java copy of the system environment directly, not
     * {@code os.environ} so that it is safe to use before the {@code os} module is available.
     *
     * @param name to access in the environment.
     * @param defaultValue to return if {@code name} is not defined or "" or access is forbidden.
     * @return the corresponding value or {@code defaultValue}.
     */
    private static String getenv(String name, String defaultValue) {
        try {
            String value = System.getenv(name);
            return (value != null && value.length() > 0) ? value : defaultValue;
        } catch (SecurityException e) {
            return defaultValue;
        }
    }
}
