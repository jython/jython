package org.python.core;

public class ThreadState {
    public InterpreterState interp;
    public PyFrame frame;
    //public PyObject curexc_type, curexc_value, curexc_traceback;
    public PyObject exc_type, exc_value, exc_traceback;
    Thread thread;
    public boolean tracing;

    public ThreadState(Thread t) {
        thread = t;
        // Fake multiple interpreter states for now
        if (Py.interp == null) {
            Py.interp = InterpreterState.getInterpreterState();
        }
        interp = Py.interp;
        tracing = false;
        //System.out.println("new thread state");
    }
}
