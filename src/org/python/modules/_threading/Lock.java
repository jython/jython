package org.python.modules._threading;

import java.util.concurrent.locks.ReentrantLock;
import org.python.core.ContextManager;
import org.python.core.Py;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.core.ThreadState;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "_threading.Lock")
public class Lock extends PyObject implements ContextManager {

    public static final PyType TYPE = PyType.fromClass(Lock.class);
    final ReentrantLock _lock;

    public Lock() {
        _lock = new ReentrantLock();
    }

    @ExposedNew
    final static PyObject Lock___new__ (PyNewWrapper new_, boolean init,
            PyType subtype, PyObject[] args, String[] keywords) {
        final int nargs = args.length;
        return new Lock();
    }
    

    @ExposedMethod(defaults = "true")
    final boolean Lock_acquire(boolean blocking) {
        if (blocking) {
            _lock.lock();
            return true;
        } else {
            return _lock.tryLock();
        }
    }

    public boolean acquire() {
        return Lock_acquire(true);
    }

    public boolean acquire(boolean blocking) {
        return Lock_acquire(blocking);
    }

    @ExposedMethod
    final PyObject Lock___enter__() {
        _lock.lock();
        return this;
    }

    public PyObject __enter__(ThreadState ts) {
        _lock.lock();
        return this;
    }

    @ExposedMethod
    final void Lock_release() {
        if (!_lock.isHeldByCurrentThread()) {
            throw Py.AssertionError("release() of un-acquire()d lock");
        }
        _lock.unlock();
    }

    public void release() {
        Lock_release();
    }

    @ExposedMethod
    final boolean Lock___exit__(PyObject type, PyObject value, PyObject traceback) {
        _lock.unlock();
        return false;
    }

    public boolean __exit__(ThreadState ts, PyObject type, PyObject value, PyObject traceback) {
        _lock.unlock();
        return false;
    }

    @ExposedMethod
    final boolean Lock_locked() {
        return _lock.isLocked();
    }

    public boolean locked() {
        return Lock_locked();
    }

    @ExposedMethod
    final boolean Lock__is_owned() {
        return _lock.isHeldByCurrentThread();
    }

    public boolean _is_owned() {
        return Lock__is_owned();
    }
}
