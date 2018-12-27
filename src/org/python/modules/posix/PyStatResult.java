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
import org.python.core.Visitproc;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
        super(TYPE, new PyObject[] {vals[0], vals[1], vals[2], vals[3], vals[4], vals[5], vals[6],
                vals[7].__int__(), vals[8].__int__(), vals[9].__int__()});
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

    protected PyStatResult(PyObject[] vals, PyObject st_atime, PyObject st_mtime, PyObject st_ctime) {
        super(TYPE, vals);
        st_mode = vals[0];
        st_ino = vals[1];
        st_dev = vals[2];
        st_nlink = vals[3];
        st_uid = vals[4];
        st_gid = vals[5];
        st_size = vals[6];
        this.st_atime = st_atime;
        this.st_mtime = st_mtime;
        this.st_ctime = st_ctime;
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
            if (obj instanceof PyStatResult) {
                return new PyStatResult(((PyTuple) obj).getArray(), ((PyStatResult) obj).st_atime,
                        ((PyStatResult) obj).st_mtime, ((PyStatResult) obj).st_ctime);
            } else {
                return new PyStatResult(((PyTuple) obj).getArray());
            }
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

    // Return a Python stat result from a posix layer FileStat object, a bulk Map retrieval of attributes, or AttributeView.
    public static PyStatResult fromFileStat(FileStat stat) {
        return new PyStatResult(
                Py.newInteger(stat.mode()),
                Py.newInteger(stat.ino()),
                Py.newLong(stat.dev()),
                Py.newInteger(stat.nlink()),
                Py.newInteger(stat.uid()),
                Py.newInteger(stat.gid()),
                Py.newInteger(stat.st_size()),
                Py.newFloat(stat.atime()),
                Py.newFloat(stat.mtime()),
                Py.newFloat(stat.ctime()));
    }

    private static double fromFileTime(FileTime fileTime) {
        return fileTime.to(TimeUnit.NANOSECONDS) / 1e9;
    }

    private static Long zeroOrValue(Long value) {
        if (value == null) {
            return Long.valueOf(0L);
        } else {
            return value;
        }
    }

    private static Integer zeroOrValue(Integer value) {
        if (value == null) {
            return Integer.valueOf(0);
        } else {
            return value;
        }
    }

    public static PyStatResult fromUnixFileAttributes(Map<String, Object> stat) {

        Integer mode = zeroOrValue((Integer)stat.get("mode"));
        Long ino = zeroOrValue((Long)stat.get("ino"));
        Long dev = zeroOrValue((Long)stat.get("dev"));
        Integer nlink = zeroOrValue((Integer)stat.get("nlink"));
        Integer uid = zeroOrValue((Integer)stat.get("uid"));
        Integer gid = zeroOrValue((Integer)stat.get("gid"));
        Long size = zeroOrValue((Long)stat.get("size"));

        return new PyStatResult(
                Py.newInteger(mode),
                Py.newInteger(ino),
                Py.newLong(dev),
                Py.newInteger(nlink),
                Py.newInteger(uid),
                Py.newInteger(gid),
                Py.newInteger(size),
                Py.newFloat(fromFileTime((FileTime)stat.get("lastAccessTime"))),
                Py.newFloat(fromFileTime((FileTime)stat.get("lastModifiedTime"))),
                Py.newFloat(fromFileTime((FileTime) stat.get("ctime"))));
    }

    public static PyStatResult fromDosFileAttributes(int mode, DosFileAttributes stat) {
        return new PyStatResult(
                Py.newInteger(mode),
                Py.newInteger(0),
                Py.newLong(0),
                Py.newInteger(0),
                Py.newInteger(0),
                Py.newInteger(0),
                Py.newInteger(stat.size()),
                Py.newFloat(fromFileTime(stat.lastAccessTime())),
                Py.newFloat(fromFileTime(stat.lastModifiedTime())),
                Py.newFloat(fromFileTime(stat.creationTime())));
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
        PyList lst = new PyList(getArray());
        lst.pyset(7, st_atime);
        lst.pyset(8, st_mtime);
        lst.pyset(9, st_ctime);
        return new PyTuple(lst);
    }

    @Override
    public PyString __repr__() {
        return (PyString) Py.newString(
                TYPE.fastGetName() + "(" +
                "st_mode=%r, st_ino=%r, st_dev=%r, st_nlink=%r, st_uid=%r, "+
                "st_gid=%r, st_size=%r, st_atime=%r, st_mtime=%r, st_ctime=%r)").__mod__(this);
    }


    /* Traverseproc implementation
     * Note that there are more fields to traverse. However traverse in PyTuple handles those.
     * Here we only traverse values that are not exactly referenced in PyTuple entries.
     */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        int retVal = super.traverse(visit, arg);
        if (retVal != 0) {
            return retVal;
        }
        if (st_atime != null) {
            retVal = visit.visit(st_atime, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (st_mtime != null) {
            retVal = visit.visit(st_mtime, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        return st_ctime != null ? visit.visit(st_ctime, arg) : 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && (ob == st_atime || ob == st_mtime
            || ob == st_ctime || super.refersDirectlyTo(ob));
    }
}
