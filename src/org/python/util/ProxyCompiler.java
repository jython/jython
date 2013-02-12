package org.python.util;

import java.util.Properties;

import org.python.core.PySystemState;

public class ProxyCompiler {

    /**
     * Compiles the python file by loading it
     *
     * FIXME: this is quite hackish right now. It basically starts a whole interpreter and sets
     * proxyDebugDirectory as the destination for the compiled proxy class
     *
     * @param filename python filename to exec
     * @param destDir destination directory for the proxy classes
     */
    public static void compile(String filename, String destDir) {
        Properties props = new Properties(System.getProperties());
        props.setProperty(PySystemState.PYTHON_CACHEDIR_SKIP, "true");
        PySystemState.initialize(props, null);
        PythonInterpreter interp = new PythonInterpreter();

        String origProxyDir = org.python.core.Options.proxyDebugDirectory;
        try {
            org.python.core.Options.proxyDebugDirectory = destDir;
            interp.execfile(filename);
        } finally {
            org.python.core.Options.proxyDebugDirectory = origProxyDir;
        }
    }

}
