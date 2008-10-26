/* Copyright (c) 2007 Jython Developers */
package org.python.core.io;

import org.python.core.Py;
import org.python.core.PyFile;
import org.python.core.PyObject;

/**
 * Jython file descriptor management.
 *
 * File descriptor objects in Jython are instances of RawIOBase.
 *
 * @author Philip Jenvey
 */
public class FileDescriptors {

    public static PyFile wrap(RawIOBase raw, String mode, int bufsize) {
        return new PyFile(raw, "<fdopen>", mode, bufsize);
    }

    /**
     * Return the RawIOBase associated with the specified file descriptor.
     *
     * Raises a Python exception is the file descriptor is invalid
     *
     * @param fd
     *            a Jython file descriptor object
     * @return the RawIOBase associated with the file descriptor
     */
    public static RawIOBase get(PyObject fd) {
        return Py.tojava(fd, RawIOBase.class);
    }
}
