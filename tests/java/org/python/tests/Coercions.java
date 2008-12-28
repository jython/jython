package org.python.tests;

import org.python.core.PyObject;

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
}
