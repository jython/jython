package org.python.core;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name="generator", base=PyObject.class)
public class PyGenerator extends PyIterator {

    @ExposedGet
    protected PyFrame gi_frame;

    @ExposedGet
    protected boolean gi_running;

    private PyException generatorExit;

    private PyObject closure;

    public PyGenerator(PyFrame frame, PyObject closure) {
        gi_frame = frame;
        this.closure = closure;

        // Create an exception instance while we have a frame to create it from. When the GC runs it
        // doesn't have any associated thread state. this is necessary for finalize calling close on
        // the generator
        generatorExit = Py.makeException(Py.GeneratorExit);
    }

    @ExposedMethod
    public PyObject send(PyObject value) {
        if (gi_frame == null) {
            throw Py.StopIteration("");
        }
        if (gi_frame.f_lasti == 0 && value != Py.None) {
            throw Py.TypeError("can't send non-None value to a just-started generator");
        }
        gi_frame.setGeneratorInput(value);
        return next();
    }

    @ExposedMethod(names="throw", defaults={"null", "null"})
    public PyObject throw$(PyObject type, PyObject value, PyTraceback tb) {
        return raiseException(Py.makeException(type, value, tb));
    }

    @ExposedMethod
    public PyObject close() {
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
    @ExposedMethod(doc="x.next() -> the next value, or raise StopIteration")
    public PyObject next() {
        return super.next();
    }

    @Override
    @ExposedMethod
    public PyObject __iter__() {
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
        if (gi_frame == null || gi_frame.f_lasti == -1)
            return;
        try {
            close();
        } catch (PyException e) {
            // PEP 342 specifies that if an exception is raised by close,
            // we output to stderr and then forget about it;
            String className =  PyException.exceptionClassName(e.type);
            int lastDot = className.lastIndexOf('.');
            if (lastDot != -1) {
                className = className.substring(lastDot + 1);
            }
            String msg = String.format("Exception %s: %s in %s", className, e.value.__repr__()
                    .toString(), __repr__().toString());
            Py.println(Py.getSystemState().stderr, Py.newString(msg));
        } catch (Throwable e) {
            // but we currently ignore any Java exception completely. perhaps we
            // can also output something meaningful too?
        } finally {
            super.finalize();
        }
    }

    public PyObject __iternext__() {
        if (gi_running)
            throw Py.ValueError("generator already executing");
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
            result = gi_frame.f_code.call(gi_frame, closure);
        } catch(PyException e) {
            if (!(e.type == Py.StopIteration || e.type == Py.GeneratorExit)) {
                gi_frame = null;
                throw e;
            } else {
                stopException = e;
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
