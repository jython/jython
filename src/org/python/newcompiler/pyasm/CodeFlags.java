// (C) Copyright 2007 Tobias Ivarsson
package org.python.newcompiler.pyasm;

public class CodeFlags {
    
    final public static int CO_OPTIMIZED      = 0x0001;
    //final public static int CO_NEWLOCALS    = 0x0002
    final public static int CO_VARARGS        = 0x0004;
    final public static int CO_VARKEYWORDS    = 0x0008;
    final public static int CO_GENERATOR      = 0x0020;
    
    final public static int CO_NESTED         = 0x0010;
    final public static int CO_GENERATOR_ALLOWED = 0x1000;
    final public static int CO_FUTUREDIVISION = 0x2000;
    final public static int CO_ALL_FEATURES = CO_NESTED|CO_GENERATOR_ALLOWED|CO_FUTUREDIVISION;
    
}
