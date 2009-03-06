package org.python.tests;

public class SubVisible extends Visible implements VisibleOverride {

    public static final int sharedNameField = SUBVISIBLE_SHARED_NAME_FIELD;
    /**
     * Overrides {@link Visible#visibleStatic(int)}
     */
    public static int visibleStatic(int input) {
        return SUBCLASS_STATIC_OVERRIDE;
    }

    /**
     * Overloads {@link Visible#visibleStatic(int, String)}
     */
    public static int visibleStatic(double input, String sinput) {
        return SUBCLASS_STATIC_OVERLOAD;
    }

    /**
     * Ensure that overridden methods on subclasses are picked up properly. Overrides
     * {@link Visible#visibleInstance(int)}
     */
    @Override
    public int visibleInstance(int input) {
        return SUBCLASS_OVERRIDE;
    }

    /**
     * Ensure that overloading {@link Visible#visibleInstance(int, String)} gets mapped correctly
     */
    public int visibleInstance(double input, String sinput) {
        return SUBCLASS_OVERLOAD;
    }

    public int getSharedNameField() {
        return sharedNameField * 10;
    }

    /**
     * Increase the visibility of {@link Invisible#packageMethod()}.
     */
    public int packageMethod() {
        return super.packageMethod();
    }
}
