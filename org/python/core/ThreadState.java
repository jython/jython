package org.python.core;

public class ThreadState {
    //public InterpreterState interp;
    public PySystemState systemState;
    public PyFrame frame;
    //public PyObject curexc_type, curexc_value, curexc_traceback;
    //public PyObject exc_type, exc_value, exc_traceback;
    public PyException exception;
    public Thread thread;
    public boolean tracing;

    public ThreadState(Thread t, PySystemState systemState) {
        thread = t;
        // Fake multiple interpreter states for now
        /*if (Py.interp == null) {
            Py.interp = InterpreterState.getInterpreterState();
        }*/
        this.systemState = systemState;
        //interp = Py.interp;
        tracing = false;
        //System.out.println("new thread state");
    }
}
