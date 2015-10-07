package org.python.core;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "sys.version_info", isBaseType = false)
public class PyVersionInfo extends PyTuple {
    public static final PyType TYPE = PyType.fromClass(PyVersionInfo.class);

    @ExposedGet
    public PyObject major, minor, micro, releaselevel, serial;

    @ExposedGet
    public static final int n_sequence_fields = 5, n_fields = 5, n_unnamed_fields = 5;

    PyVersionInfo(PyObject... vals) {
        super(TYPE, vals);
        major = vals[0];
        minor = vals[1];
        micro = vals[2];
        releaselevel = vals[3];
        serial = vals[4];
    }

    @ExposedNew
    final static PyObject version_info_new(PyNewWrapper new_, boolean init, PyType subtype,
                                    PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("version_info", args, keywords, new String[] {"tuple"}, 1);
        PyObject obj = ap.getPyObject(0);
        if (obj instanceof PyTuple) {
            if (obj.__len__() != n_fields) {
                String msg = String.format("version_info() takes a %s-sequence (%s-sequence given)",
                        n_fields, obj.__len__());
                throw Py.TypeError(msg);
            }
            // tuples are immutable, so we can just use its underlying array
            return new PyVersionInfo(((PyTuple)obj).getArray());
        }
        else {
            PyList seq = new PyList(obj);
            if (seq.__len__() != n_fields) {
                String msg = String.format("version_info() takes a %s-sequence (%s-sequence given)",
                        n_fields, obj.__len__());
                throw Py.TypeError(msg);
            }
            return new PyVersionInfo((PyObject[])seq.__tojava__(PyObject[].class));
        }
    }

    /**
     * Used for pickling.
     *
     * @return a tuple of (class, tuple)
     */
    @Override
    public PyObject __reduce__() {
        return version_info___reduce__();
    }

    @ExposedMethod
    final PyObject version_info___reduce__() {
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
                TYPE.fastGetName() + "(" +
                        "major=%r, minor=%r, micro=%r, releaselevel=%r, serial=%r)").__mod__(this);
    }

}
