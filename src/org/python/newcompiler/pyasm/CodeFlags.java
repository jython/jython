// (C) Copyright 2007 Tobias Ivarsson
package org.python.newcompiler.pyasm;

public class CodeFlags {

    public static final int CO_OPTIMIZED = 0x0001;
    public static final int CO_NEWLOCALS = 0x0002;
    public static final int CO_VARARGS = 0x0004;
    public static final int CO_VARKEYWORDS = 0x0008;
    public static final int CO_NESTED = 0x0010;
    public static final int CO_GENERATOR = 0x0020;
    /*
     * The CO_NOFREE flag is set if there are no free or cell variables. This
     * information is redundant, but it allows a single flag test to determine
     * whether there is any extra work to be done when the call frame it setup.
     */
    public static final int CO_NOFREE = 0x0040;

    /*
     * CO_GENERATOR_ALLOWED is no longer used. 
     * 
     * Stopped defining in 2.5, do not re-use.
     */
    public static final int CO_GENERATOR_ALLOWED = 0x1000;
    public static final int CO_FUTURE_DIVISION = 0x2000;
    /* do absolute imports by default */
    public static final int CO_FUTURE_ABSOLUTE_IMPORT = 0x4000;
    public static final int CO_FUTURE_WITH_STATEMENT = 0x8000;

    /*final public static int CO_ALL_FEATURES = CO_NESTED | CO_GENERATOR_ALLOWED
            | CO_FUTURE_DIVISION | CO_FUTURE_WITH_STATEMENT
            | CO_FUTURE_ABSOLUTE_IMPORT;//*/

}
