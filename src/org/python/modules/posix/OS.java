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
