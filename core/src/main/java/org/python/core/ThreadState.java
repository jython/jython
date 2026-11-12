// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

/**
 * Represents a platform thread (that is, a Java {@code Thread})
 * internally to the runtime.
 */
// Compare CPython struct _ts in cpython/pystate.h
// and CPython PyThreadState in pystate.h
class ThreadState {

    /**
     * Current ThreadState mapped from the current platform thread (that is,
     * the Java {@code Thread} this represents to Python).
     */
    // Compare CPython struct _ts in cpython/pystate.h
    static final ThreadLocal<ThreadState> current = new ThreadLocal<ThreadState>() {

        @Override
        protected ThreadState initialValue() { return new ThreadState(Thread.currentThread()); }
    };

    // Intentionally missing: Interpreter interp;

    /** The top frame of the call stack. */
    PyFrame<? extends PyCode> frame = null;

    // Missing: recursion control recursion state and config
    // Missing: tracing/profiling (do we need that?)
    // Missing: exception support (main. generators and co-routines).
    // Missing: thread-local dict
    // Missing: hooks for _threadmodule (join, resources, etc.).
    // Intentionally missing: anything to do with a GIL
    // Missing: async exception support. (How does that work?)
    // Missing: async support generally

    // Missing: etc..

    /**
     * Java {@code Thread} represented by this {@code ThreadState}: the Java
     * thread in which this {@code ThreadState} was created.
     */
    // Should it be impossible to have two with the same value?
    // Compare CPython _ts.id in cpython/pystate.h
    final Thread thread;

    /**
     * Make the given stack frame the new top of the stack. This sets
     * {@link #frame this.frame}, the top of stack, to the provided frame
     * and {@link PyFrame#back frame.back} to the previous {@link #frame}.
     *
     * @param frame new stack top.
     */
    void push(PyFrame<? extends PyCode> frame) {
        // XXX Increment & check interpreter stack depth?
        assert frame.back == null;
        frame.back = this.frame;
        this.frame = frame;
    }

    /**
     * Remove and return the frame at the top of the stack. The new stack
     * top is taken from the back reference of the current stack top.
     *
     * @return stack top prior to call.
     */
    PyFrame<? extends PyCode> pop() {
        // XXX Decrement interpreter stack depth?
        PyFrame<? extends PyCode> prevFrame = this.frame;
        this.frame = prevFrame.back;
        prevFrame.back = null;
        return prevFrame;
    }

    /**
     * Constructor exclusively used by the ThreadLocal {@link #current}.
     *
     * @param javaThread to encapsulate
     */
    private ThreadState(Thread javaThread) { this.thread = javaThread; }

    /**
     * Report whether the stack is empty.
     *
     * @return {@code true} iff the stack is empty
     */
    // Compare CPython PyEval_GetFrame in ceval.c
    boolean stackEmpty() { return frame == null; }

    /**
     * Get the "current frame", which is the frame at the top of the stack
     * (if there is one).
     *
     * @return the current frame (top of stack)
     * @throws PyBaseException (SystemError) if the stack is empty
     */
    // Compare CPython PyEval_GetFrame in ceval.c
    // Compare CPython 3.11 PyThreadState_GetFrame in pystate.c
    PyFrame<? extends PyCode> getFrame() {
        if (frame != null) {
            return frame;
        } else {
            throw noCurrentFrame("frame");
        }
    }

    /**
     * Get the {@code builtins} mapping, used for example to augment the
     * global dictionary in {@code exec()}, from the current frame at the
     * top of the stack. The {@code builtins} of a frame often the
     * {@code dict} of the {@code builtins} module of the
     * {@link Interpreter} that created it, or another {@code dict}, but may
     * be any object. Not {@code null}.
     *
     * @return the builtins
     * @throws PyBaseException (SystemError) if the stack is empty
     */
    // Compare CPython PyEval_GetBuiltins in ceval.c
    Object getBuiltins() {
        if (frame != null) {
            return frame.getBuiltins();
        } else {
            throw noCurrentFrame("builtins");
        }
    }

    /**
     * Get the {@code locals} mapping from the current frame at the top of
     * the stack. The {@code locals} of a frame are often a {@code dict} but
     * may be any object. It will be accessed using the Python mapping
     * protocol when interpreting byte code. Not {@code null}.
     *
     * @return the locals object of the current top frame.
     * @throws PyBaseException (SystemError) if the stack is empty
     */
    // Compare CPython PyEval_GetLocals in ceval.c
    Object getLocals() throws PyBaseException {
        if (frame != null) {
            return frame.getLocals();
        } else {
            throw noCurrentFrame("locals");
        }
    }

    /**
     * Get the global dictionary, used for example as a default in
     * {@code exec()}, from the current frame at the top of the stack. Not
     * {@code null}.
     *
     * @return the global dictionary.
     * @throws PyBaseException (SystemError) if the stack is empty
     */
    // Compare CPython PyEval_GetGlobals in ceval.c
    PyDict getGlobals() throws PyBaseException {
        if (frame != null) {
            return frame.getGlobals();
        } else {
            throw noCurrentFrame("globals");
        }
    }

    /**
     * Get the current {@link Interpreter}, the one associated with the
     * current frame on the top of the stack through its {@code function}
     * object. This is used when a new function is defined or an import
     * statement executed. A {@code ThreadState} where the stack is empty
     * has no current interpreter.
     *
     * @return the interpreter. Not {@code null}.
     * @throws PyBaseException (SystemError) if the stack is empty
     */
    // Compare CPython PyEval_GetLocals in ceval.c
    // Compare CPython 3.11 PyThreadState_GetInterpreter in pystate.c
    Interpreter getInterpreter() throws PyBaseException {
        if (frame != null) {
            return frame.getInterpreter();
        } else {
            throw noCurrentFrame("interpreter");
        }
    }

    /**
     * Find or create a {@code ThreadState} representing the current Java
     * {@code Thread}. This is never {@code null}.
     *
     * @return current thread state
     */
    static final ThreadState get() { return current.get(); }

    /**
     * Return a {@link SystemError} with a message along the lines "Cannot
     * get THING because no current frame exists"
     *
     * @param thing we couldn't get
     * @return exception to throw
     */
    static private PyBaseException noCurrentFrame(String thing) {
        return PyErr.format(PyExc.SystemError, "Cannot get %s because no current frame exists",
                thing);
    }
}
