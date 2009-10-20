/* Copyright (c) Jython Developers */
package org.python.modules.posix;

import com.kenai.constantine.Constant;
import com.kenai.constantine.platform.Errno;

import org.jruby.ext.posix.JavaPOSIX;
import org.jruby.ext.posix.POSIX;
import org.jruby.ext.posix.POSIXFactory;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;

/**
 * The underlying _posix or _nt module, named depending on the platform.
 *
 * This currently contains only some of the basics of the posix/nt modules (which are
 * implemented in Python). In particular things like the PythonPOSIXHandler that are
 * slower to instantiate and thus would affect startup time.
 *
 * Eventually more if not all of the pure Python module should end up here.
 */
public class PosixModule implements ClassDictInit {

    public static final PyString __doc__ = new PyString(
        "This module provides access to operating system functionality that is\n" +
        "standardized by the C Standard and the POSIX standard (a thinly\n" +
        "disguised Unix interface).  Refer to the library manual and\n" +
        "corresponding Unix manual entries for more information on calls.");

    /** Current OS information. */
    private static OS os = OS.getOS();

    /** Platform specific POSIX services. */
    private static POSIX posix = POSIXFactory.getPOSIX(new PythonPOSIXHandler(), true);

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__name__", new PyString("_" + os.getModuleName()));
        dict.__setitem__("__doc__", __doc__);

        // os.open flags, only expose what we support
        dict.__setitem__("O_RDONLY", Py.newInteger(0x0));
        dict.__setitem__("O_WRONLY", Py.newInteger(0x1));
        dict.__setitem__("O_RDWR", Py.newInteger(0x2));
        dict.__setitem__("O_APPEND", Py.newInteger(0x8));
        dict.__setitem__("O_SYNC", Py.newInteger(0x80));
        dict.__setitem__("O_CREAT", Py.newInteger(0x200));
        dict.__setitem__("O_TRUNC", Py.newInteger(0x400));
        dict.__setitem__("O_EXCL", Py.newInteger(0x800));

        // os.access constants
        dict.__setitem__("F_OK", Py.Zero);
        dict.__setitem__("X_OK", Py.newInteger(1 << 0));
        dict.__setitem__("W_OK", Py.newInteger(1 << 1));
        dict.__setitem__("R_OK", Py.newInteger(1 << 2));
        // Successful termination
        dict.__setitem__("EX_OK", Py.Zero);

        dict.__setitem__("error", Py.OSError);
        dict.__setitem__("stat_result", PyStatResult.TYPE);
        dict.__setitem__("_posix_impl", Py.java2py(posix));
        dict.__setitem__("_native_posix", Py.newBoolean(!(posix instanceof JavaPOSIX)));

        // Hide from Python
        dict.__setitem__("classDictInit", null);
        dict.__setitem__("getPOSIX", null);
        dict.__setitem__("getOSName", null);

        dict.__setitem__("__all__", dict.invoke("keys"));
    }

    public static PyString __doc__lstat = new PyString(
        "lstat(path) -> stat result\n\n" +
        "Like stat(path), but do not follow symbolic links.");
    public static PyObject lstat(String path) {
        return PyStatResult.fromFileStat(posix.lstat(Py.getSystemState().getPath(path)));
    }

    public static PyString __doc__stat = new PyString(
        "stat(path) -> stat result\n\n" +
        "Perform a stat system call on the given path.\n\n" +
        "Note that some platforms may return only a small subset of the\n" +
        "standard fields");
    public static PyObject stat(String path) {
        return PyStatResult.fromFileStat(posix.stat(Py.getSystemState().getPath(path)));
    }

    public static PyString __doc__strerror = new PyString(
        "strerror(code) -> string\n\n" + 
        "Translate an error code to a message string.");
    public static PyObject strerror(int code) {
        Constant errno = Errno.valueOf(code);
        if (errno == Errno.__UNKNOWN_CONSTANT__) {
            return new PyString("Unknown error: " + code);
        }
        if (errno.name() == errno.toString()) {
            // Fake constant or just lacks a description, fallback to Linux's
            // XXX: have constantine handle this fallback
            errno = Enum.valueOf(com.kenai.constantine.platform.linux.Errno.class,
                                 errno.name());
        }
        return new PyString(errno.toString());
    }

    /**
     * Helper function for the subprocess module, returns the potential shell commands for
     * this OS.
     *
     * @return a tuple of lists of command line arguments. E.g. (['/bin/sh', '-c'])
     */
    public static PyObject _get_shell_commands() {
        String[][] commands = os.getShellCommands();
        PyObject[] commandsTup = new PyObject[commands.length];
        int i = 0;
        for (String[] command : commands) {
            PyList args = new PyList();
            for (String arg : command) {
                args.append(new PyString(arg));
            }
            commandsTup[i++] = args;
        }
        return new PyTuple(commandsTup);
    }

    public static POSIX getPOSIX() {
        return posix;
    }

    public static String getOSName() {
        return os.getModuleName();
    }
}
