// Copyright 2002 Finn Bock

package org.python.core;

public class PyGenerator extends PyIterator {
    public PyFrame gi_frame;
    PyObject closure;
    public boolean gi_running;

    public PyGenerator(PyFrame frame, PyObject closure) {
        this.gi_frame = frame;
        this.closure = closure;
        this.gi_running = false;
    }

    private static final String[] __members__ = { "gi_frame", "gi_running",
            "next",
            // begin newcompiler
            "close", "send", "throw",
    // FIXME: it should be throw, not _throw, but throw is a java keyword
    // end newcompiler
    };

    public PyObject __dir__() {
        PyString members[] = new PyString[__members__.length];
        for (int i = 0; i < __members__.length; i++)
            members[i] = new PyString(__members__[i]);
        PyList ret = new PyList(members);
        PyDictionary accum = new PyDictionary();
        addKeys(accum, "__dict__");
        ret.extend(accum.keys());
        ret.sort();
        return ret;
    }

    // begin newcompiler

    public PyObject next() {
        gi_frame.setGeneratorInput(Py.None);
        return super.next();
    }

    public PyObject send(PyObject value) {
        gi_frame.setGeneratorInput(value);
        return super.next();
    }

    private PyObject _throw(PyException ex) {
        gi_frame.setGeneratorException(ex);
        return super.next();
    }

    public PyObject throw$(PyObject type) {
        return _throw(new PyException(type));
    }

    public PyObject throw$(PyObject type, PyObject value) {
        return _throw(new PyException(type, value));
    }

    public PyObject throw$(PyObject type, PyObject value, PyTraceback tb) {
        return _throw(new PyException(type, value, tb));
    }

    public PyObject close() {
        try {
            throw$(Py.GeneratorExit);
            throw Py.RuntimeError("generator ignored GeneratorExit");
        } catch (PyException e) {
            if (!(e.type == Py.StopIteration || e.type == Py.GeneratorExit)) {
                throw e;
            }
        }
        return Py.None;
    }

    protected void finalize() throws Throwable {
        if (gi_frame.f_lasti == -1) /* Generator already closed. */
            return;
        try {
            close();
        } catch (Throwable e) {
            Py.println(Py.getSystemState().stderr, new PyString("Exception "
                    + e + " in " + this + " ignored."));
        } finally {
            super.finalize();
        }
    }

    // end newcompiler

    public PyObject __iternext__() {
        if (gi_running)
            throw Py.ValueError("generator already executing");
        if (gi_frame.f_lasti == -1)
            return null;
        gi_running = true;
        PyObject result = null;
        try {
            result = gi_frame.f_code.call(gi_frame, closure);
        } catch (PyException e) {
            if (!e.type.equals(Py.StopIteration)) {
                throw e;
            } else {
                stopException = e;
                return null;
            }
        } finally {
            gi_running = false;
        }
        // System.out.println("lasti:" + gi_frame.f_lasti);
        // if (result == Py.None)
        // new Exception().printStackTrace();
        if (result == Py.None && gi_frame.f_lasti == -1)
            return null;
        return result;
    }
}
