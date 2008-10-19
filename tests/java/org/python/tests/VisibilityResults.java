package org.python.tests;

public interface VisibilityResults {
    // Value not used by default by any of the visible classes
    public static final int UNUSED = -1;

    // Returns for Invisible.java
    public static final int PROTECTED_STATIC_FIELD = 1;
    public static final int PROTECTED_FIELD = 2;
    public static final int PACKAGE_STATIC_FIELD = 3;
    public static final int PACKAGE_FIELD = 4;
    public static final int PROTECTED_STATIC_METHOD = 5;
    public static final int OVERLOADED_STATIC_METHOD = 6;
    public static final int PROTECTED_METHOD = 7;
    public static final int OVERLOADED_PROTECTED_METHOD = 8;
    public static final int PACKAGE_STATIC_METHOD = 9;
    public static final int PACKAGE_METHOD = 10;

    // Returns for Visible.java
    public static final int PUBLIC_FIELD = 101;
    public static final int PUBLIC_STATIC_FIELD = 102;
    public static final int PUBLIC_METHOD = 103;
    public static final int OVERLOADED_PUBLIC_METHOD = 104;
    public static final int EXTRA_ARG_PUBLIC_METHOD = 105;
    public static final int OVERLOADED_EXTRA_ARG_PUBLIC_METHOD = 106;
    public static final int PUBLIC_STATIC_METHOD = 107;
    public static final int OVERLOADED_PUBLIC_STATIC_METHOD = 108;
    public static final int EXTRA_ARG_PUBLIC_STATIC_METHOD = 109;
    public static final int PUBLIC_METHOD_FIELD = 110;
    public static final int PUBLIC_STATIC_METHOD_FIELD = 111;

    // Returns for SubVisible.java
    public static final int SUBCLASS_OVERRIDE = 201;
    public static final int SUBCLASS_OVERLOAD = 202;
    public static final int SUBCLASS_STATIC_OVERRIDE = 203;
    public static final int SUBCLASS_STATIC_OVERLOAD = 204;
}
