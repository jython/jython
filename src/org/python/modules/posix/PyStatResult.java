/* Copyright (c) Jython Developers */
package org.python.modules.posix;

import jnr.posix.FileStat;

import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyList;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.PyType;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

@ExposedType(name = "stat_result", isBaseType = false)
public class PyStatResult extends PyTuple {

    public static final PyType TYPE = PyType.fromClass(PyStatResult.class);

    static {
        // Can only determine the module name during runtime
        TYPE.setName(PosixModule.getOSName() + "." + TYPE.fastGetName());
    }

    @ExposedGet
    public PyObject st_mode, st_ino, st_dev, st_nlink, st_uid, st_gid, st_size, st_atime, st_mtime,
        st_ctime;

    @ExposedGet
    public static final int n_sequence_fields = 10, n_fields = 10, n_unnamed_fields = 10;

    PyStatResult(PyObject... vals) {
        super(TYPE, vals);
        st_mode = vals[0];
        st_ino = vals[1];
        st_dev = vals[2];
        st_nlink = vals[3];
        st_uid = vals[4];
        st_gid = vals[5];
        st_size = vals[6];
        st_atime = vals[7];
        st_mtime = vals[8];
        st_ctime = vals[9];
    }

    @ExposedNew
    static PyObject stat_result_new(PyNewWrapper wrapper, boolean init, PyType subtype,
                                    PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("stat_result", args, keywords, new String[] {"tuple"}, 1);
        PyObject obj = ap.getPyObject(0);
        if (obj instanceof PyTuple) {
            if (obj.__len__() != n_fields) {
                String msg = String.format("stat_result() takes a %s-sequence (%s-sequence given)",
                                           n_fields, obj.__len__());
                throw Py.TypeError(msg);
            }
            // tuples are immutable, so we can just use its underlying array
            return new PyStatResult(((PyTuple)obj).getArray());
        }
        else {
            PyList seq = new PyList(obj);
            if (seq.__len__() != n_fields) {
                String msg = String.format("stat_result() takes a %s-sequence (%s-sequence given)",
                                           n_fields, obj.__len__());
                throw Py.TypeError(msg);
            }
            return new PyStatResult((PyObject[])seq.__tojava__(PyObject[].class));
        }
    }

    /**
     * Return a Python stat result from a posix layer FileStat object.
     *
     * Ideally times would be returned as floats, like CPython, however, the underlying FileStat
     * object from JNR Posix only has integer resolution.
     *
     * We should be able to resolve by using java.nio.file.attribute.PosixFileAttributeView
     */
    public static PyStatResult fromFileStat(FileStat stat) {
        return new PyStatResult(Py.newInteger(stat.mode()), Py.newInteger(stat.ino()),
                                Py.newLong(stat.dev()), Py.newInteger(stat.nlink()),
                                Py.newInteger(stat.uid()), Py.newInteger(stat.gid()),
                                Py.newInteger(stat.st_size()), Py.newInteger(stat.atime()),
                                Py.newInteger(stat.mtime()), Py.newInteger(stat.ctime()));
    }

    @Override
    public synchronized PyObject __eq__(PyObject o) {
        return stat_result___eq__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final synchronized PyObject stat_result___eq__(PyObject o) {
        if (getType() != o.getType() && !getType().isSubType(o.getType())) {
            return null;
        }
        int tl = __len__();
        int ol = o.__len__();
        if (tl != ol) {
            return Py.False;
        }
        int i = cmp(this, tl, o, ol);
        return (i < 0) ? Py.True : Py.False;
    }

    @Override
    public synchronized PyObject __ne__(PyObject o) {
        return stat_result___ne__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final synchronized PyObject stat_result___ne__(PyObject o) {
        PyObject eq = stat_result___eq__(o);
        if (eq == null) {
            return null;
        }
        return eq.__not__();
    }

    /**
     * Used for pickling.
     *
     * @return a tuple of (class, tuple)
     */
    @Override
    public PyObject __reduce__() {
        return stat_result___reduce__();
    }

    @ExposedMethod
    final PyObject stat_result___reduce__() {
        PyTuple newargs = __getnewargs__();
        return new PyTuple(getType(), newargs);
    }

    @Override
    public PyTuple __getnewargs__() {
        return new PyTuple(new PyList(getArray()));
    }

    @Override
    public PyString __repr__() {
        return (PyString) Py.newString(
                "posix.stat_result(" +
                "st_mode=%r, st_ino=%r, st_dev=%r, st_nlink=%r, st_uid=%r, "+
                "st_gid=%r, st_size=%r, st_atime=%r, st_mtime=%r, st_ctime=%r)").__mod__(this);
    }
}
