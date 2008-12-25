package org.python.tests;

/**
 * A class with nothing visible from Python by default, though several fields should be visible when
 * subclassing.
 */
public class Invisible implements VisibilityResults {

    private static int privateStaticField;

    private int privateField;

    protected static int protectedStaticField = PROTECTED_STATIC_FIELD;

    protected int protectedField = PROTECTED_FIELD;

    static int packageStaticField = PACKAGE_STATIC_FIELD;

    int packageField = PACKAGE_FIELD;

    Invisible() {}

    private static int privateStaticMethod() {
        return 7;
    }

    private int privateMethod() {
        return 7;
    }

    protected static int protectedStaticMethod(int input) {
        return PROTECTED_STATIC_METHOD;
    }

    protected static int protectedStaticMethod(String input) {
        return OVERLOADED_STATIC_METHOD;
    }

    protected int protectedMethod(int input) {
        return PROTECTED_METHOD;
    }

    protected int protectedMethod(String input) {
        return OVERLOADED_PROTECTED_METHOD;
    }

    static int packageStaticMethod() {
        return PACKAGE_STATIC_METHOD;
    }

    int packageMethod() {
        return PACKAGE_METHOD;
    }
}
