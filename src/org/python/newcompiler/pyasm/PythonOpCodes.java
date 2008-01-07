// (C) Copyright 2007 Tobias Ivarsson
package org.python.newcompiler.pyasm;

/**
 * These are the opcodes for Python byte code.
 * 
 * @author Tobias Ivarsson
 */
public interface PythonOpCodes {

    public static final char STOP_CODE = 0;
    public static final char POP_TOP = 1;
    public static final char ROT_TWO = 2;
    public static final char ROT_THREE = 3;
    public static final char DUP_TOP = 4;
    public static final char ROT_FOUR = 5;

    public static final char NOP = 9;
    public static final char UNARY_POSITIVE = 10;
    public static final char UNARY_NEGATIVE = 11;
    public static final char UNARY_NOT = 12;
    public static final char UNARY_CONVERT = 13;

    public static final char UNARY_INVERT = 15;

    public static final char LIST_APPEND = 18;
    public static final char BINARY_POWER = 19;
    public static final char BINARY_MULTIPLY = 20;
    public static final char BINARY_DIVIDE = 21;
    public static final char BINARY_MODULO = 22;
    public static final char BINARY_ADD = 23;
    public static final char BINARY_SUBTRACT = 24;
    public static final char BINARY_SUBSCR = 25;
    public static final char BINARY_FLOOR_DIVIDE = 26;
    public static final char BINARY_TRUE_DIVIDE = 27;
    public static final char INPLACE_FLOOR_DIVIDE = 28;
    public static final char INPLACE_TRUE_DIVIDE = 29;
    public static final char SLICE__0 = 30;
    public static final char SLICE__1 = 31;
    public static final char SLICE__2 = 32;
    public static final char SLICE__3 = 33;

    public static final char STORE_SLICE__0 = 40;
    public static final char STORE_SLICE__1 = 41;
    public static final char STORE_SLICE__2 = 42;
    public static final char STORE_SLICE__3 = 43;

    public static final char DELETE_SLICE__0 = 50;
    public static final char DELETE_SLICE__1 = 51;
    public static final char DELETE_SLICE__2 = 52;
    public static final char DELETE_SLICE__3 = 53;

    public static final char INPLACE_ADD = 55;
    public static final char INPLACE_SUBTRACT = 56;
    public static final char INPLACE_MULTIPLY = 57;
    public static final char INPLACE_DIVIDE = 58;
    public static final char INPLACE_MODULO = 59;
    public static final char STORE_SUBSCR = 60;
    public static final char DELETE_SUBSCR = 61;
    public static final char BINARY_LSHIFT = 62;
    public static final char BINARY_RSHIFT = 63;
    public static final char BINARY_AND = 64;
    public static final char BINARY_XOR = 65;
    public static final char BINARY_OR = 66;
    public static final char INPLACE_POWER = 67;
    public static final char GET_ITER = 68;

    public static final char PRINT_EXPR = 70;
    public static final char PRINT_ITEM = 71;
    public static final char PRINT_NEWLINE = 72;
    public static final char PRINT_ITEM_TO = 73;
    public static final char PRINT_NEWLINE_TO = 74;
    public static final char INPLACE_LSHIFT = 75;
    public static final char INPLACE_RSHIFT = 76;
    public static final char INPLACE_AND = 77;
    public static final char INPLACE_XOR = 78;
    public static final char INPLACE_OR = 79;
    public static final char BREAK_LOOP = 80;
    public static final char WITH_CLEANUP = 81;
    public static final char LOAD_LOCALS = 82;
    public static final char RETURN_VALUE = 83;
    public static final char IMPORT_STAR = 84;
    public static final char EXEC_STMT = 85;
    public static final char YIELD_VALUE = 86;
    public static final char POP_BLOCK = 87;
    public static final char END_FINALLY = 88;
    public static final char BUILD_CLASS = 89;

    /**
     * This is not an actual opcode, it only separate the opcodes that doesn't
     * have arguments from those that do.
     * 
     * opcode >= {@link #__HAVE_ARGUMENT} meens that the opcode has an argument.
     */
    public static final char __HAVE_ARGUMENT = 90;

    public static final char STORE_NAME = 90;
    public static final char DELETE_NAME = 91;
    public static final char UNPACK_SEQUENCE = 92;
    public static final char FOR_ITER = 93;

    public static final char STORE_ATTR = 95;
    public static final char DELETE_ATTR = 96;
    public static final char STORE_GLOBAL = 97;
    public static final char DELETE_GLOBAL = 98;
    public static final char DUP_TOPX = 99;
    public static final char LOAD_CONST = 100;
    public static final char LOAD_NAME = 101;
    public static final char BUILD_TUPLE = 102;
    public static final char BUILD_LIST = 103;
    public static final char BUILD_MAP = 104;
    public static final char LOAD_ATTR = 105;
    public static final char COMPARE_OP = 106;
    public static final char IMPORT_NAME = 107;
    public static final char IMPORT_FROM = 108;

    public static final char JUMP_FORWARD = 110;
    public static final char JUMP_IF_FALSE = 111;
    public static final char JUMP_IF_TRUE = 112;
    public static final char JUMP_ABSOLUTE = 113;

    public static final char LOAD_GLOBAL = 116;

    public static final char CONTINUE_LOOP = 119;
    public static final char SETUP_LOOP = 120;
    public static final char SETUP_EXCEPT = 121;
    public static final char SETUP_FINALLY = 122;

    public static final char LOAD_FAST = 124;
    public static final char STORE_FAST = 125;
    public static final char DELETE_FAST = 126;

    public static final char RAISE_VARARGS = 130;
    public static final char CALL_FUNCTION = 131;
    public static final char MAKE_FUNCTION = 132;
    public static final char BUILD_SLICE = 133;
    public static final char MAKE_CLOSURE = 134;
    public static final char LOAD_CLOSURE = 135;
    public static final char LOAD_DEREF = 136;
    public static final char STORE_DEREF = 137;

    public static final char CALL_FUNCTION_VAR = 140;
    public static final char CALL_FUNCTION_KW = 141;
    public static final char CALL_FUNCTION_VAR_KW = 142;
    public static final char EXTENDED_ARG = 143;

}
