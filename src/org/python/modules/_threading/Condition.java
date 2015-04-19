package org.python.modules._threading;

import org.python.core.ContextManager;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.ThreadState;
import org.python.expose.ExposedType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.core.Traverseproc;
import org.python.core.Visitproc;

@ExposedType(name = "_threading.Condition")
public class Condition extends PyObject implements ContextManager, Traverseproc {

    public static final PyType TYPE = PyType.fromClass(Condition.class);
    private final ConditionSupportingLock _lock;
    private final java.util.concurrent.locks.Condition _condition;

    public Condition() {
        this(new RLock());
    }

    public Condition(ConditionSupportingLock lock) {
        _lock = lock;
        _condition = lock.getLock().newCondition();
    }

    @ExposedNew
    final static PyObject Condition___new__ (PyNewWrapper new_, boolean init,
            PyType subtype, PyObject[] args, String[] keywords) {
        final int nargs = args.length;
        if (nargs == 1) {
            return new Condition((ConditionSupportingLock)args[0]);
        }
        return new Condition();
    }

    public boolean acquire() {
        return Condition_acquire(true);
    }

    public boolean acquire(boolean blocking) {
        return Condition_acquire(blocking);
    }

    @ExposedMethod(defaults = "true")
    final boolean Condition_acquire(boolean blocking) {
        return _lock.acquire(blocking);
    }

    public PyObject __enter__(ThreadState ts) {
        _lock.acquire();
        return this;
    }

    @ExposedMethod
    final PyObject Condition___enter__() {
        Condition_acquire(true);
        return this;
    }

    public void release() {
        Condition_release();
    }

    @ExposedMethod
    final void Condition_release() {
        _lock.release();
    }

    public boolean __exit__(ThreadState ts, PyException exception) {
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
        try {
            if (timeout == Py.None) {
                _condition.await();
            } else {
                long nanos = (long) (timeout.asDouble() * 1e9);
                _condition.awaitNanos(nanos);
            }
        } catch (IllegalMonitorStateException ex) {
            throw Py.RuntimeError("cannot wait on un-acquired lock");
        }
    }

    public void notify$() {
        Condition_notify(1);
    }

    @ExposedMethod(defaults = "1")
    final void Condition_notify(int count) {
        try {
            for( int i = 0; i < count; i++) {
                _condition.signal();
            }
        } catch (IllegalMonitorStateException ex) {
            throw Py.RuntimeError("cannot notify on un-acquired lock");
        }
    }

    public void notifyAll$() {
        Condition_notifyAll();
    }

    @ExposedMethod
    final void Condition_notifyAll() {
        try {
            _condition.signalAll();
        } catch (IllegalMonitorStateException ex) {
            throw Py.RuntimeError("cannot notify on un-acquired lock");
        }
    }

    @ExposedMethod
    final void Condition_notify_all() {
        _condition.signalAll();
    }

    public boolean _is_owned() {
        return Condition__is_owned();
    }

    @ExposedMethod
    final boolean Condition__is_owned() {
        return _lock._is_owned();
    }

    @Override
    public String toString() {
        int count = 0;
        try {
            count = _lock.getWaitQueueLength(_condition);
        } catch (IllegalMonitorStateException ex) {
            // ignore if lock is not held
        }
        return (Py.newString("<_threading.Condition(%s, %d)>").__mod__(
                new PyTuple(
                        Py.newString(_lock.toString()),
                        Py.newInteger(count)))).toString();
    }

    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        return _lock != null ? visit.visit((PyObject)_lock, arg) : 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && _lock == ob;
    }
}
