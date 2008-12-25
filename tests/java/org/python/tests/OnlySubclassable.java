package org.python.tests;

public class OnlySubclassable {

    public int filledInByConstructor;

    protected OnlySubclassable() {
        filledInByConstructor = 1;
    }
}
