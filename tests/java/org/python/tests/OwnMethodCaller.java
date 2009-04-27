package org.python.tests;

public class OwnMethodCaller {

    public int getValue() {
        return 7;
    }

    public int callGetValue() {
        return getValue();
    }
}
