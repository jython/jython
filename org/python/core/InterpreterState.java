package org.python.core;

public class InterpreterState {
    public PyObject sysdict;
    //public PyDictionary modules;
    public PyObject builtins;
    public TraceFunction tracefunc;
    public TraceFunction profilefunc;

    private static InterpreterState interp=null;

    public static InterpreterState getInterpreterState() {
        //System.out.println("want interp state: "+Thread.currentThread());
        if (interp == null) {
            synchronized (InterpreterState.class) {
                //System.out.println("creating interp state");

                interp = new InterpreterState();
            }
            //System.out.println("creating __builtin__");

            interp.builtins = PyJavaClass.lookup(__builtin__.class).__dict__;
            //System.out.println("creating sys");

            interp.sysdict = PyJavaClass.lookup(sys.class).__dict__;
            //System.out.println("created interp state");
        }
        return interp;
    }

    public InterpreterState() {
        ;
    }
}
