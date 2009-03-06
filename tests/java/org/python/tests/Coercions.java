package org.python.tests;

import java.io.Serializable;

import org.python.core.PyObject;

/**
 * Called in various forms by test_java_visibility.CoercionsTest to see if Python wrapped Java types
 * are unwrapped to the correct call.
 */
public class Coercions {

    public String takeInt(int i) {
        return "" + i;
    }

    public String takeInteger(Integer i) {
        return "" + i;
    }

    public String takeNumber(Number n) {
        return "" + n;
    }

    public String takePyObjInst(PyObject[] args) {
        return "" + args.length;
    }

    public static String takeArray(float[] f) {
        return "float";
    }

    public static String takeArray(double[] d) {
        return "double";
    }

    public static String takePyObj(PyObject[] args) {
        return "" + args.length;
    }

    public static String takeArray(Object[] obj) {
        return "Object[]";
    }

    // Not really an array, but we want to make sure Python code doesn't pick up on it
    // inappropriately.
    public static String takeArray(Object obj) {
        return "Object";
    }

    public static String takeArray(SubVisible[] vis) {
        return "SubVisible[]";
    }

    public static String takeArray(OtherSubVisible[] vis) {
        return "OtherSubVisible[]";
    }

    public static String takeArray(Visible[] vis) {
        return "Visible[]";
    }

    public String tellClassNameObject(Object o) {
        return o.getClass().toString();
    }

    public String tellClassNameSerializable(Serializable o) {
        return o.getClass().toString();
    }

    public static String take(int i) {
        return "take with int arg: " + i;
    }

    public static String take(char c) {
        return "take with char arg: " + c;
    }

    public static String take(boolean b) {
        return "take with boolean arg: " + b;
    }

    public static String take(byte bt) {
        return "take with byte arg: " + bt;
    }

    public static int takeIterable(Iterable<Integer> it) {
        int sum = 0;
        for (Integer integer : it) {
            sum += integer;
        }
        return sum;
    }

    public static boolean takeBoolIterable(Iterable<Boolean> it) {
        for (Boolean integer : it) {
            if (!integer) {
                return false;
            }
        }
        return true;
    }

    public static void runRunnable(Runnable r){
        r.run();
    }
}
