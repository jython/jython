package org.python.tests;

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
}
