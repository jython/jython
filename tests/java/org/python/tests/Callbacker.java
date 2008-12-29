package org.python.tests;

public class Callbacker {

    public interface Callback {

        public void call();

        public void call(long oneArg);
    }

    public static void callNoArg(Callback c) {
        c.call();
    }

    public static void callOneArg(Callback c, long arg) {
        c.call(arg);
    }
}
