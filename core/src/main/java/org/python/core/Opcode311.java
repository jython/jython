// Copyright (c)2023 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

/** Constants for opcodes taken from CPython {@code opcode.h} */
class Opcode311 {

    static final int POP_TOP = 1;
    static final int ROT_TWO = 2;
    static final int ROT_THREE = 3;
    static final int DUP_TOP = 4;
    static final int DUP_TOP_TWO = 5;
    static final int ROT_FOUR = 6;
    static final int NOP = 9;
    static final int UNARY_POSITIVE = 10;
    static final int UNARY_NEGATIVE = 11;
    static final int UNARY_NOT = 12;
    static final int UNARY_INVERT = 15;
    static final int BINARY_MATRIX_MULTIPLY = 16;
    static final int INPLACE_MATRIX_MULTIPLY = 17;
    static final int BINARY_POWER = 19;
    static final int BINARY_MULTIPLY = 20;
    static final int BINARY_MODULO = 22;
    static final int BINARY_ADD = 23;
    static final int BINARY_SUBTRACT = 24;
    static final int BINARY_SUBSCR = 25;
    static final int BINARY_FLOOR_DIVIDE = 26;
    static final int BINARY_TRUE_DIVIDE = 27;
    static final int INPLACE_FLOOR_DIVIDE = 28;
    static final int INPLACE_TRUE_DIVIDE = 29;
    static final int GET_AITER = 50;
    static final int GET_ANEXT = 51;
    static final int BEFORE_ASYNC_WITH = 52;
    static final int BEGIN_FINALLY = 53;
    static final int END_ASYNC_FOR = 54;
    static final int INPLACE_ADD = 55;
    static final int INPLACE_SUBTRACT = 56;
    static final int INPLACE_MULTIPLY = 57;
    static final int INPLACE_MODULO = 59;
    static final int STORE_SUBSCR = 60;
    static final int DELETE_SUBSCR = 61;
    static final int BINARY_LSHIFT = 62;
    static final int BINARY_RSHIFT = 63;
    static final int BINARY_AND = 64;
    static final int BINARY_XOR = 65;
    static final int BINARY_OR = 66;
    static final int INPLACE_POWER = 67;
    static final int GET_ITER = 68;
    static final int GET_YIELD_FROM_ITER = 69;
    static final int PRINT_EXPR = 70;
    static final int LOAD_BUILD_CLASS = 71;
    static final int YIELD_FROM = 72;
    static final int GET_AWAITABLE = 73;
    static final int INPLACE_LSHIFT = 75;
    static final int INPLACE_RSHIFT = 76;
    static final int INPLACE_AND = 77;
    static final int INPLACE_XOR = 78;
    static final int INPLACE_OR = 79;
    static final int WITH_CLEANUP_START = 81;
    static final int WITH_CLEANUP_FINISH = 82;
    static final int RETURN_VALUE = 83;
    static final int IMPORT_STAR = 84;
    static final int SETUP_ANNOTATIONS = 85;
    static final int YIELD_VALUE = 86;
    static final int POP_BLOCK = 87;
    static final int END_FINALLY = 88;
    static final int POP_EXCEPT = 89;
    /**
     * Opcodes with this value or greater are followed by a one-byte
     * argument, and those less than or equal to it, by a zero byte that
     * is ignored.
     */
    static final int HAVE_ARGUMENT = 90; // not an opcode
    static final int STORE_NAME = 90;
    static final int DELETE_NAME = 91;
    static final int UNPACK_SEQUENCE = 92;
    static final int FOR_ITER = 93;
    static final int UNPACK_EX = 94;
    static final int STORE_ATTR = 95;
    static final int DELETE_ATTR = 96;
    static final int STORE_GLOBAL = 97;
    static final int DELETE_GLOBAL = 98;
    static final int LOAD_CONST = 100;
    static final int LOAD_NAME = 101;
    static final int BUILD_TUPLE = 102;
    static final int BUILD_LIST = 103;
    static final int BUILD_SET = 104;
    static final int BUILD_MAP = 105;
    static final int LOAD_ATTR = 106;
    static final int COMPARE_OP = 107;
    static final int IMPORT_NAME = 108;
    static final int IMPORT_FROM = 109;
    static final int JUMP_FORWARD = 110;
    static final int JUMP_IF_FALSE_OR_POP = 111;
    static final int JUMP_IF_TRUE_OR_POP = 112;
    static final int JUMP_ABSOLUTE = 113;
    static final int POP_JUMP_IF_FALSE = 114;
    static final int POP_JUMP_IF_TRUE = 115;
    static final int LOAD_GLOBAL = 116;
    static final int SETUP_FINALLY = 122;
    static final int LOAD_FAST = 124;
    static final int STORE_FAST = 125;
    static final int DELETE_FAST = 126;
    static final int RAISE_VARARGS = 130;
    static final int CALL_FUNCTION = 131;
    static final int MAKE_FUNCTION = 132;
    static final int BUILD_SLICE = 133;
    static final int LOAD_CLOSURE = 135;
    static final int LOAD_DEREF = 136;
    static final int STORE_DEREF = 137;
    static final int DELETE_DEREF = 138;
    static final int CALL_FUNCTION_KW = 141;
    static final int CALL_FUNCTION_EX = 142;
    static final int SETUP_WITH = 143;
    static final int EXTENDED_ARG = 144;
    static final int LIST_APPEND = 145;
    static final int SET_ADD = 146;
    static final int MAP_ADD = 147;
    static final int LOAD_CLASSDEREF = 148;
    static final int BUILD_LIST_UNPACK = 149;
    static final int BUILD_MAP_UNPACK = 150;
    static final int BUILD_MAP_UNPACK_WITH_CALL = 151;
    static final int BUILD_TUPLE_UNPACK = 152;
    static final int BUILD_SET_UNPACK = 153;
    static final int SETUP_ASYNC_WITH = 154;
    static final int FORMAT_VALUE = 155;
    static final int BUILD_CONST_KEY_MAP = 156;
    static final int BUILD_STRING = 157;
    static final int BUILD_TUPLE_UNPACK_WITH_CALL = 158;
    static final int LOAD_METHOD = 160;
    static final int CALL_METHOD = 161;
    static final int CALL_FINALLY = 162;
    static final int POP_FINALLY = 163;

    /**
     * EXCEPT_HANDLER is a special, implicit block type that is created
     * when entering an except handler. It is not an opcode.
     */
    static final int EXCEPT_HANDLER = 257;
}
