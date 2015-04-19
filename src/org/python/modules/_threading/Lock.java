package org.python.modules._threading;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.TimeUnit;
import org.python.core.ContextManager;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.ThreadState;
import org.python.core.Untraversable;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@Untraversable
@ExposedType(name = "_threading.Lock")
public class Lock extends PyObject implements ContextManager, ConditionSupportingLock {

    public static final PyType TYPE = PyType.fromClass(Lock.class);
    // see http://bugs.jython.org/issue2328 - need to support another thread
    // releasing this lock, per CPython semantics, so support semantics with
    // custom non-reentrant lock, not a ReentrantLock
    private final Mutex _lock = new Mutex();

    public Lock() {
    }

    public java.util.concurrent.locks.Lock getLock() {
        return _lock;
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

    public boolean __exit__(ThreadState ts, PyException exception) {
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
        return _lock.isLocked();
    }

    public boolean _is_owned() {
        return Lock__is_owned();
    }

    public int getWaitQueueLength(java.util.concurrent.locks.Condition condition) {
        return _lock.getWaitQueueLength(condition);
    }

    @ExposedMethod
    public String toString() {
        String owner = _lock.getOwnerName();
        return Py.newString("<_threading.Lock owner=%r locked=%s>").
                __mod__(new PyTuple(
                        owner != null ? Py.newStringOrUnicode(owner) : Py.None,
                        Py.newBoolean(_lock.isLocked()))).toString();
    }

}


final class Mutex implements java.util.concurrent.locks.Lock {

	// Our internal   helper class
	private static class Sync extends AbstractQueuedSynchronizer {
		// Reports whether in locked state
		protected boolean isHeldExclusively() {
			return getState() == 1;
		}

		// Acquires the lock if state is zero
		public boolean tryAcquire(int acquires) {
			assert acquires == 1; // Otherwise unused
			if (compareAndSetState(0, 1)) {
				setExclusiveOwnerThread(Thread.currentThread());
				return true;
			}
			return false;
		}

		// Releases the lock by setting state to zero
        protected boolean tryRelease(int releases) {
            assert releases == 1; // Otherwise unused
            if (getState() == 0) {
                throw new IllegalMonitorStateException();
            }
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

		// Provides a Condition
		ConditionObject newCondition() { return new ConditionObject(); }

        Thread getOwner() {
            return getExclusiveOwnerThread();
        }
	}

	// The sync object does all the hard work. We just forward to it.
	private final Sync sync = new Sync();

	public void lock()                { sync.acquire(1); }
	public boolean tryLock()          { return sync.tryAcquire(1); }
	public void unlock()              { sync.release(1); }
	public java.util.concurrent.locks.Condition newCondition()   { return sync.newCondition(); }
	public boolean isLocked()         { return sync.isHeldExclusively(); }
	public boolean hasQueuedThreads() { return sync.hasQueuedThreads(); }
	public void lockInterruptibly() throws InterruptedException {
		sync.acquireInterruptibly(1);
	}
	public boolean tryLock(long timeout, TimeUnit unit)
		throws InterruptedException {
		return sync.tryAcquireNanos(1, unit.toNanos(timeout));
	}
    public int getWaitQueueLength(java.util.concurrent.locks.Condition condition) {
        if (condition instanceof AbstractQueuedSynchronizer.ConditionObject) {
            return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject) condition);
        } else {
            // punt, no estimate available, but this should never occur using
            // standard locks and conditions from this module
            return 0;
        }
    }
    String getOwnerName() {
        Thread owner = sync.getOwner();
        return owner != null ? owner.getName() : null;
    }
}
