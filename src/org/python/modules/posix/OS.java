/* Copyright (c) Jython Developers */
package org.python.modules.posix;

import java.lang.reflect.Method;

import org.python.core.PyObject;
import org.python.core.PySystemState;

/**
 * A Marker tagging what OS we're running on, with some accompanying information about
 * that platform.
 */
enum OS {
    NT("Windows", new String[] {"cmd.exe", "/c"}, new String[] {"command.com", "/c"}),
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
        return name().toLowerCase();
    }

    String[][] getShellCommands() {
        return shellCommands;
    }

    /**
     * Hide module level functions defined in the PosixModule dict not applicable to this
     * OS, identified by the PosixModule.Hide annotation.
     *
     * @param dict The PosixModule module dict
     */
    void hideFunctions(PyObject dict) {
        for (Method method: PosixModule.class.getDeclaredMethods()) {
            if (isHidden(method)) {
                dict.__setitem__(method.getName(), null);
            }
        }
    }

    /**
     * Determine if method should be hidden for this OS.
     *
     * @param method a PosixModule Method
     * @return true if should be hidden
     */
    private boolean isHidden(Method method) {
        if (method.isAnnotationPresent(PosixModule.Hide.class)) {
            for (OS os : method.getAnnotation(PosixModule.Hide.class).value()) {
                if (os == this) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Return the OS we're running on.
     */
    static OS getOS() {
        String osName = PySystemState.registry.getProperty("python.os");
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
}
