package org.python.tests;


public class SomePyMethods {

    public int b = 3;

    public int __getitem__(int idx) {
        return idx * 2;
    }

    public int __getattr__(String name) {
        return 2;
    }

}
