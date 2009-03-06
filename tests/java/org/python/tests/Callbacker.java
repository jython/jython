package org.python.tests;

import java.util.List;

import org.python.util.Generic;

public class Callbacker {

    public interface Callback {

        public void call();

        public void call(long oneArg);
    }

    public static class CollectingCallback implements Callback {

        public List<String> calls = Generic.list();

        public void call() {
            calls.add("call()");
        }

        public void call(long oneArg) {
            calls.add("call(" + oneArg + ")");
        }
    }

    public static void callNoArg(Callback c) {
        c.call();
    }

    public static void callOneArg(Callback c, long arg) {
        c.call(arg);
    }
}
