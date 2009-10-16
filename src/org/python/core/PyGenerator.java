/* Copyright (c) Jython Developers */
package org.python.core;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name = "generator", base = PyObject.class, isBaseType = false)
public class PyGenerator extends PyIterator {

    public static final PyType TYPE = PyType.fromClass(PyGenerator.class);

    @ExposedGet
    protected PyFrame gi_frame;

    @ExposedGet
    protected boolean gi_running;

    private PyException generatorExit;

    private PyObject closure;

    public PyGenerator(PyFrame frame, PyObject closure) {
        super(TYPE);
        gi_frame = frame;
        this.closure = closure;

        // Create an exception instance while we have a frame to create it from. When the GC runs it
        // doesn't have any associated thread state. this is necessary for finalize calling close on
        // the generator
        generatorExit = Py.makeException(Py.GeneratorExit);
    }

    public PyObject send(PyObject value) {
        return generator_send(value);
    }

    @ExposedMethod
    final PyObject generator_send(PyObject value) {
        if (gi_frame == null) {
            throw Py.StopIteration("");
        }
        if (gi_frame.f_lasti == 0 && value != Py.None) {
            throw Py.TypeError("can't send non-None value to a just-started generator");
        }
        gi_frame.setGeneratorInput(value);
        return next();
    }

    public PyObject throw$(PyObject type, PyObject value, PyObject tb) {
        return generator_throw$(type, value, tb);
    }

    @ExposedMethod(names="throw", defaults={"null", "null"})
    final PyObject generator_throw$(PyObject type, PyObject value, PyObject tb) {
        if (tb == Py.None) {
            tb = null;
        } else if (tb != null && !(tb instanceof PyTraceback)) {
            throw Py.TypeError("throw() third argument must be a traceback object");
        }
        return raiseException(Py.makeException(type, value, tb));
    }

    public PyObject close() {
        return generator_close();
    }

    @ExposedMethod
    final PyObject generator_close() {
        try {
            raiseException(generatorExit);
            throw Py.RuntimeError("generator ignored GeneratorExit");
        } catch (PyException e) {
            if (!(e.type == Py.StopIteration || e.type == Py.GeneratorExit)) {
                throw e;
            }
        }
        return Py.None;
    }

    @Override
    public PyObject next() {
        return generator_next();
    }

    @ExposedMethod(doc="x.next() -> the next value, or raise StopIteration")
    final PyObject generator_next() {
        return super.next();
    }

    @Override
    public PyObject __iter__() {
        return generator___iter__();
    }

    @ExposedMethod
    final PyObject generator___iter__() {
        return this;
    }

    private PyObject raiseException(PyException ex) {
        if (gi_frame == null || gi_frame.f_lasti == 0) {
            throw ex;
        }
        gi_frame.setGeneratorInput(ex);
        return next();
    }

    @Override
    protected void finalize() throws Throwable {
        if (gi_frame == null || gi_frame.f_lasti == -1) {
            return;
        }
        try {
            close();
        } catch (PyException pye) {
            // PEP 342 specifies that if an exception is raised by close,
            // we output to stderr and then forget about it;
            String className =  PyException.exceptionClassName(pye.type);
            int lastDot = className.lastIndexOf('.');
            if (lastDot != -1) {
                className = className.substring(lastDot + 1);
            }
            String msg = String.format("Exception %s: %s in %s", className, pye.value.__repr__(),
                                       __repr__());
            Py.println(Py.getSystemState().stderr, Py.newString(msg));
        } catch (Throwable t) {
            // but we currently ignore any Java exception completely. perhaps we
            // can also output something meaningful too?
        } finally {
            super.finalize();
        }
    }

    @Override
    public PyObject __iternext__() {
        return __iternext__(Py.getThreadState());
    }

    public PyObject __iternext__(ThreadState state) {
        if (gi_running) {
            throw Py.ValueError("generator already executing");
        }
        if (gi_frame == null) {
            return null;
        }

        if (gi_frame.f_lasti == -1) {
            gi_frame = null;
            return null;
        }
        gi_running = true;
        PyObject result = null;
        try {
            result = gi_frame.f_code.call(state, gi_frame, closure);
        } catch (PyException pye) {
            if (!(pye.type == Py.StopIteration || pye.type == Py.GeneratorExit)) {
                gi_frame = null;
                throw pye;
            } else {
                stopException = pye;
                gi_frame = null;
                return null;
            }
        } finally {
            gi_running = false;
        }
        if (result == Py.None && gi_frame.f_lasti == -1) {
            return null;
        }
        return result;
    }
}
