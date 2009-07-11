package org.python.modules._threading;

import org.python.core.ContextManager;
import org.python.core.Py;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.core.ThreadState;
import org.python.expose.ExposedType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;

@ExposedType(name = "_threading.Condition")
public class Condition extends PyObject implements ContextManager {

    public static final PyType TYPE = PyType.fromClass(Condition.class);
    private final Lock _lock;
    private final java.util.concurrent.locks.Condition _condition;

    public Condition() {
        this(new Lock());
    }

    public Condition(Lock lock) {
        _lock = lock;
        _condition = lock._lock.newCondition();
    }

    @ExposedNew
    final static PyObject Condition___new__ (PyNewWrapper new_, boolean init,
            PyType subtype, PyObject[] args, String[] keywords) {
        final int nargs = args.length;
        if (nargs == 1) {
            return new Condition((Lock)args[0]);
        }
        return new Condition();
    }

    public boolean acquire() {
        return Condition_acquire();
    }

    @ExposedMethod
    final boolean Condition_acquire() {
        return _lock.acquire();
    }

    public PyObject __enter__(ThreadState ts) {
        _lock.acquire();
        return this;
    }

    @ExposedMethod
    final PyObject Condition___enter__() {
        Condition_acquire();
        return this;
    }

    public void release() {
        Condition_release();
    }

    @ExposedMethod
    final void Condition_release() {
        _lock.release();
    }

    public boolean __exit__(ThreadState ts, PyObject type, PyObject value, PyObject traceback) {
        _lock.release();
        return false;
    }

    @ExposedMethod
    final boolean Condition___exit__(PyObject type, PyObject value, PyObject traceback) {
        Condition_release();
        return false;
    }

    public void wait$(PyObject timeout) throws InterruptedException {
        Condition_wait(timeout);
    }

    @ExposedMethod(defaults = "Py.None")
    final void Condition_wait(PyObject timeout) throws InterruptedException {
        if (timeout == Py.None) {
            _condition.await();
        } else {
            long nanos = (long) (timeout.__float__().asDouble() * 1e9);
            _condition.awaitNanos(nanos);
        }
    }

    public void notify$() {
        Condition_notify();
    }

    @ExposedMethod
    final void Condition_notify() {
        _condition.signal();
    }

    public void notifyAll$() {
        Condition_notifyAll();
    }

    @ExposedMethod
    final void Condition_notifyAll() {
        _condition.signalAll();
    }

    public boolean _is_owned() {
        return Condition__is_owned();
    }

    @ExposedMethod
    final boolean Condition__is_owned() {
        return _lock._lock.isHeldByCurrentThread();
    }
}

