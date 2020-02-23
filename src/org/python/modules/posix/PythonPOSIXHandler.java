/* Copyright (c) Jython Developers */
package org.python.modules.posix;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.logging.Level;

import jnr.constants.platform.Errno;
import jnr.posix.POSIXHandler;

import org.python.core.imp;
import org.python.core.Options;
import org.python.core.PrePy;
import org.python.core.Py;
import org.python.core.PyObject;


/**
 * Jython specific hooks for our underlying POSIX library.
 */
public class PythonPOSIXHandler implements POSIXHandler {

    @Override
    public void error(Errno error, String extraData) {
        throw Py.OSError(error, Py.newStringOrUnicode(extraData));
    }

    @Override
    public void error(Errno error, String methodName, String extraData) {
        throw Py.OSError(error, Py.newStringOrUnicode(extraData));
    }

    @Override
    public void unimplementedError(String methodName) {
        if (methodName.startsWith("stat.")) {
            // Ignore unimplemented FileStat methods
            return;
        }
        throw Py.NotImplementedError(methodName);
    }

    @Override
    public void warn(WARNING_ID id, String message, Object... data) {
    }

    @Override
    public boolean isVerbose() {
        // Verbose if the general threshold for logging is FINE or lower.
        return PrePy.getLoggingLevel().intValue() <= Level.FINE.intValue();
    }

    @Override
    public File getCurrentWorkingDirectory() {
        return new File(Py.getSystemState().getCurrentWorkingDir());
    }

    @Override
    public String[] getEnv() {
        PyObject items = imp.load("os").__getattr__("environ").invoke("items");
        String[] env = new String[items.__len__()];
        int i = 0;
        for (PyObject item : items.asIterable()) {
            env[i++] = String.format("%s=%s", item.__getitem__(0), item.__getitem__(1));
        }
        return env;
    }

    @Override
    public InputStream getInputStream() {
        return System.in;
    }

    @Override
    public PrintStream getOutputStream() {
        return System.out;
    }

    @Override
    public int getPID() {
        return 0;
    }

    @Override
    public PrintStream getErrorStream() {
        return System.err;
    }
}
