// (C) Copyright 2007 Tobias Ivarsson
package org.python.newcompiler.pyasm;

/**
 * These are constants for operators used by the {@link CodeVisitor}.
 * 
 * @author Tobias Ivarsson
 */
public interface Operator {
    
    // Inplace and Binary operators
    public static final int ADD = 0;
    public static final int SUBTRACT = 1;
    public static final int MULTIPLY = 2;
    public static final int DIVIDE = 3;
    public static final int FLOOR_DIVIDE = 4;
    public static final int TRUE_DIVIDE = 5;
    public static final int MODULO = 6;
    public static final int POWER = 7;
    public static final int LSHIFT = 8;
    public static final int RSHIFT = 9;
    public static final int AND = 10;
    public static final int OR = 11;
    public static final int XOR = 12;
    public static final int SUBSCRIPT = 13;

    // Unary operators
    public static final int INVERT = 0;
    public static final int POSITIVE = 1;
    public static final int NEGATIVE = 2;
    public static final int NOT = 3;
    public static final int CONVERT = 4;

    // Comparators
    public static final int LESS_THAN = 0;
    public static final int LESS_THAN_OR_EQUAL = 1;
    public static final int EQUAL = 2;
    public static final int NOT_EQUAL = 3;
    public static final int GREATER_THAN = 4;
    public static final int GREATER_THAN_OR_EQUAL = 5;
    public static final int IN = 6;
    public static final int NOT_IN = 7;
    public static final int IS = 8;
    public static final int IS_NOT = 9;
    public static final int EXCEPTION_MATCH = 10;
    
}
