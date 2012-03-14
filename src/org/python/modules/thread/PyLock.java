// Copyright (c) Corporation for National Research Initiatives
package org.python.modules.thread;

import org.python.core.*;

public class PyLock extends PyObject implements ContextManager {

    private boolean locked = false;

    public boolean acquire() {
        return acquire(true);
    }

    public synchronized boolean acquire(boolean waitflag) {
        if (waitflag) {
            while (locked) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    System.err.println("Interrupted thread");
                }
            }
            locked = true;
            return true;
        } else {
            if (locked) {
                return false;
            } else {
                locked = true;
                return true;
            }
        }
    }

    public synchronized void release() {
        if (locked) {
            locked = false;
            notifyAll();
        } else {
            throw Py.ValueError("lock not acquired");
        }
    }

    public boolean locked() {
        return locked;
    }

    @Override
    public PyObject __enter__(ThreadState ts) {
        acquire();
        return this;
    }

    @Override
    public boolean __exit__(ThreadState ts, PyException exception) {
        release();
        return false;
    }
}
