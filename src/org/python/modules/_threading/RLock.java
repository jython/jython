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
import org.python.core.Untraversable;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@Untraversable
@ExposedType(name = "_threading.RLock")
public class RLock extends PyObject implements ContextManager, ConditionSupportingLock {

    public static final PyType TYPE = PyType.fromClass(RLock.class);
    private final RLockImplementation _lock = new RLockImplementation();

    public RLock() {
    }

    public java.util.concurrent.locks.Lock getLock() {
        return _lock;
    }

    @ExposedNew
    final static PyObject RLock___new__(PyNewWrapper new_, boolean init,
                                        PyType subtype, PyObject[] args, String[] keywords) {
        final int nargs = args.length;
        return new RLock();
    }

    @ExposedMethod(defaults = "true")
    final boolean RLock_acquire(boolean blocking) {
        if (blocking) {
            _lock.lock();
            return true;
        } else {
            return _lock.tryLock();
        }
    }

    public boolean acquire() {
        return RLock_acquire(true);
    }

    public boolean acquire(boolean blocking) {
        return RLock_acquire(blocking);
    }

    @ExposedMethod
    final PyObject RLock___enter__() {
        _lock.lock();
        return this;
    }

    public PyObject __enter__(ThreadState ts) {
        _lock.lock();
        return this;
    }

    @ExposedMethod
    final void RLock_release() {
        if (!_lock.isHeldByCurrentThread() || _lock.getHoldCount() <= 0) {
            throw Py.RuntimeError("cannot release un-acquired lock");
        }
        _lock.unlock();
    }

    public void release() {
        RLock_release();
    }

    @ExposedMethod
    final boolean RLock___exit__(PyObject type, PyObject value, PyObject traceback) {
        _lock.unlock();
        return false;
    }

    public boolean __exit__(ThreadState ts, PyException exception) {
        _lock.unlock();
        return false;
    }

    @ExposedMethod
    final boolean RLock_locked() {
        return _lock.isLocked();
    }

    public boolean locked() {
        return RLock_locked();
    }

    @ExposedMethod
    final boolean RLock__is_owned() {
        return _lock.isHeldByCurrentThread();
    }

    public boolean _is_owned() {
        return RLock__is_owned();
    }

    public int getWaitQueueLength(java.util.concurrent.locks.Condition condition) {
        return _lock.getWaitQueueLength(condition);
    }

    @ExposedMethod
    public String toString() {
        String owner = _lock.getOwnerName();
        return Py.newString("<_threading.RLock owner=%r count=%d>").
                __mod__(new PyTuple(
                        owner != null ? Py.newStringOrUnicode(owner) : Py.None,
                        Py.newInteger(_lock.getHoldCount()))).toString();
    }
}


final class RLockImplementation extends java.util.concurrent.locks.ReentrantLock {
    String getOwnerName() {
        Thread owner = getOwner();
        return owner != null ? owner.getName() : null;
    }
}


