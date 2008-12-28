package org.python.tests;

/**
 * Exposes several methods that should be visible statically and on instances in Python.
 */
public class Visible extends Invisible {
    public int visibleField;

    public static final int sharedNameField = VISIBLE_SHARED_NAME_FIELD;

    public static int visibleStaticField = PUBLIC_STATIC_FIELD;

    public int visibleInstance = PUBLIC_METHOD_FIELD;

    public static int visibleStatic = PUBLIC_STATIC_METHOD_FIELD;

    public static class StaticInner {

        public static int visibleStaticField = PUBLIC_STATIC_FIELD;
    }

    public Visible() {
        this(PUBLIC_FIELD);
    }

    public Visible(int visibileFieldValue) {
        visibleField = visibileFieldValue;
    }

    public int visibleInstance(int input) {
        return PUBLIC_METHOD;
    }

    public int visibleInstance(String input) {
        return OVERLOADED_PUBLIC_METHOD;
    }

    public int visibleInstance(int iinput, String input) {
        return EXTRA_ARG_PUBLIC_METHOD;
    }

    public int getSharedNameField() {
        return sharedNameField * 10;
    }

    public static int visibleInstance(String sinput, String input) {
        return OVERLOADED_EXTRA_ARG_PUBLIC_METHOD;
    }

    public static int visibleStatic(int input) {
        return PUBLIC_STATIC_METHOD;
    }

    public static int visibleStatic(String input) {
        return OVERLOADED_PUBLIC_STATIC_METHOD;
    }

    public static int visibleStatic(int iinput, String input) {
        return EXTRA_ARG_PUBLIC_STATIC_METHOD;
    }
}
