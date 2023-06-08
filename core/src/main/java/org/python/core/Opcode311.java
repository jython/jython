// Copyright (c)2023 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

/** Constants for opcodes taken from CPython {@code opcode.h} */
class Opcode311 {

    /**
     * EXCEPT_HANDLER is a special, implicit block type that is created
     * when entering an except handler. It is not an opcode.
     */
    static final int EXCEPT_HANDLER = 257;

    /* Instruction opcodes for compiled code */

    /** CPython opcode POP_TOP */
    static final int POP_TOP = 1;
    /** CPython opcode PUSH_NULL */
    static final int PUSH_NULL = 2;
    /** CPython opcode NOP */
    static final int NOP = 9;
    /** CPython opcode UNARY_POSITIVE */
    static final int UNARY_POSITIVE = 10;
    /** CPython opcode UNARY_NEGATIVE */
    static final int UNARY_NEGATIVE = 11;
    /** CPython opcode UNARY_NOT */
    static final int UNARY_NOT = 12;
    /** CPython opcode UNARY_INVERT */
    static final int UNARY_INVERT = 15;
    /** CPython opcode BINARY_SUBSCR */
    static final int BINARY_SUBSCR = 25;
    /** CPython opcode GET_LEN */
    static final int GET_LEN = 30;
    /** CPython opcode MATCH_MAPPING */
    static final int MATCH_MAPPING = 31;
    /** CPython opcode MATCH_SEQUENCE */
    static final int MATCH_SEQUENCE = 32;
    /** CPython opcode MATCH_KEYS */
    static final int MATCH_KEYS = 33;
    /** CPython opcode PUSH_EXC_INFO */
    static final int PUSH_EXC_INFO = 35;
    /** CPython opcode CHECK_EXC_MATCH */
    static final int CHECK_EXC_MATCH = 36;
    /** CPython opcode CHECK_EG_MATCH */
    static final int CHECK_EG_MATCH = 37;
    /** CPython opcode WITH_EXCEPT_START */
    static final int WITH_EXCEPT_START = 49;
    /** CPython opcode GET_AITER */
    static final int GET_AITER = 50;
    /** CPython opcode GET_ANEXT */
    static final int GET_ANEXT = 51;
    /** CPython opcode BEFORE_ASYNC_WITH */
    static final int BEFORE_ASYNC_WITH = 52;
    /** CPython opcode BEFORE_WITH */
    static final int BEFORE_WITH = 53;
    /** CPython opcode END_ASYNC_FOR */
    static final int END_ASYNC_FOR = 54;
    /** CPython opcode STORE_SUBSCR */
    static final int STORE_SUBSCR = 60;
    /** CPython opcode DELETE_SUBSCR */
    static final int DELETE_SUBSCR = 61;
    /** CPython opcode GET_ITER */
    static final int GET_ITER = 68;
    /** CPython opcode GET_YIELD_FROM_ITER */
    static final int GET_YIELD_FROM_ITER = 69;
    /** CPython opcode PRINT_EXPR */
    static final int PRINT_EXPR = 70;
    /** CPython opcode LOAD_BUILD_CLASS */
    static final int LOAD_BUILD_CLASS = 71;
    /** CPython opcode LOAD_ASSERTION_ERROR */
    static final int LOAD_ASSERTION_ERROR = 74;
    /** CPython opcode RETURN_GENERATOR */
    static final int RETURN_GENERATOR = 75;
    /** CPython opcode LIST_TO_TUPLE */
    static final int LIST_TO_TUPLE = 82;
    /** CPython opcode RETURN_VALUE */
    static final int RETURN_VALUE = 83;
    /** CPython opcode IMPORT_STAR */
    static final int IMPORT_STAR = 84;
    /** CPython opcode SETUP_ANNOTATIONS */
    static final int SETUP_ANNOTATIONS = 85;
    /** CPython opcode YIELD_VALUE */
    static final int YIELD_VALUE = 86;
    /** CPython opcode ASYNC_GEN_WRAP */
    static final int ASYNC_GEN_WRAP = 87;
    /** CPython opcode PREP_RERAISE_STAR */
    static final int PREP_RERAISE_STAR = 88;
    /** CPython opcode POP_EXCEPT */
    static final int POP_EXCEPT = 89;

    /**
     * Opcodes with this value or greater are followed by a one-byte
     * argument, and those less than or equal to it, by a zero byte that
     * is ignored.
     */
    static final int HAVE_ARGUMENT = 90;

    /** CPython opcode STORE_NAME */
    static final int STORE_NAME = 90;
    /** CPython opcode DELETE_NAME */
    static final int DELETE_NAME = 91;
    /** CPython opcode UNPACK_SEQUENCE */
    static final int UNPACK_SEQUENCE = 92;
    /** CPython opcode FOR_ITER */
    static final int FOR_ITER = 93;
    /** CPython opcode UNPACK_EX */
    static final int UNPACK_EX = 94;
    /** CPython opcode STORE_ATTR */
    static final int STORE_ATTR = 95;
    /** CPython opcode DELETE_ATTR */
    static final int DELETE_ATTR = 96;
    /** CPython opcode STORE_GLOBAL */
    static final int STORE_GLOBAL = 97;
    /** CPython opcode DELETE_GLOBAL */
    static final int DELETE_GLOBAL = 98;
    /** CPython opcode SWAP */
    static final int SWAP = 99;
    /** CPython opcode LOAD_CONST */
    static final int LOAD_CONST = 100;
    /** CPython opcode LOAD_NAME */
    static final int LOAD_NAME = 101;
    /** CPython opcode BUILD_TUPLE */
    static final int BUILD_TUPLE = 102;
    /** CPython opcode BUILD_LIST */
    static final int BUILD_LIST = 103;
    /** CPython opcode BUILD_SET */
    static final int BUILD_SET = 104;
    /** CPython opcode BUILD_MAP */
    static final int BUILD_MAP = 105;
    /** CPython opcode LOAD_ATTR */
    static final int LOAD_ATTR = 106;
    /** CPython opcode COMPARE_OP */
    static final int COMPARE_OP = 107;
    /** CPython opcode IMPORT_NAME */
    static final int IMPORT_NAME = 108;
    /** CPython opcode IMPORT_FROM */
    static final int IMPORT_FROM = 109;
    /** CPython opcode JUMP_FORWARD */
    static final int JUMP_FORWARD = 110;
    /** CPython opcode JUMP_IF_FALSE_OR_POP */
    static final int JUMP_IF_FALSE_OR_POP = 111;
    /** CPython opcode JUMP_IF_TRUE_OR_POP */
    static final int JUMP_IF_TRUE_OR_POP = 112;
    /** CPython opcode POP_JUMP_FORWARD_IF_FALSE */
    static final int POP_JUMP_FORWARD_IF_FALSE = 114;
    /** CPython opcode POP_JUMP_FORWARD_IF_TRUE */
    static final int POP_JUMP_FORWARD_IF_TRUE = 115;
    /** CPython opcode LOAD_GLOBAL */
    static final int LOAD_GLOBAL = 116;
    /** CPython opcode IS_OP */
    static final int IS_OP = 117;
    /** CPython opcode CONTAINS_OP */
    static final int CONTAINS_OP = 118;
    /** CPython opcode RERAISE */
    static final int RERAISE = 119;
    /** CPython opcode COPY */
    static final int COPY = 120;
    /** CPython opcode BINARY_OP */
    static final int BINARY_OP = 122;
    /** CPython opcode SEND */
    static final int SEND = 123;
    /** CPython opcode LOAD_FAST */
    static final int LOAD_FAST = 124;
    /** CPython opcode STORE_FAST */
    static final int STORE_FAST = 125;
    /** CPython opcode DELETE_FAST */
    static final int DELETE_FAST = 126;
    /** CPython opcode POP_JUMP_FORWARD_IF_NOT_NONE */
    static final int POP_JUMP_FORWARD_IF_NOT_NONE = 128;
    /** CPython opcode POP_JUMP_FORWARD_IF_NONE */
    static final int POP_JUMP_FORWARD_IF_NONE = 129;
    /** CPython opcode RAISE_VARARGS */
    static final int RAISE_VARARGS = 130;
    /** CPython opcode GET_AWAITABLE */
    static final int GET_AWAITABLE = 131;
    /** CPython opcode MAKE_FUNCTION */
    static final int MAKE_FUNCTION = 132;
    /** CPython opcode BUILD_SLICE */
    static final int BUILD_SLICE = 133;
    /** CPython opcode JUMP_BACKWARD_NO_INTERRUPT */
    static final int JUMP_BACKWARD_NO_INTERRUPT = 134;
    /** CPython opcode MAKE_CELL */
    static final int MAKE_CELL = 135;
    /** CPython opcode LOAD_CLOSURE */
    static final int LOAD_CLOSURE = 136;
    /** CPython opcode LOAD_DEREF */
    static final int LOAD_DEREF = 137;
    /** CPython opcode STORE_DEREF */
    static final int STORE_DEREF = 138;
    /** CPython opcode DELETE_DEREF */
    static final int DELETE_DEREF = 139;
    /** CPython opcode JUMP_BACKWARD */
    static final int JUMP_BACKWARD = 140;
    /** CPython opcode CALL_FUNCTION_EX */
    static final int CALL_FUNCTION_EX = 142;
    /** CPython opcode EXTENDED_ARG */
    static final int EXTENDED_ARG = 144;
    /** CPython opcode LIST_APPEND */
    static final int LIST_APPEND = 145;
    /** CPython opcode SET_ADD */
    static final int SET_ADD = 146;
    /** CPython opcode MAP_ADD */
    static final int MAP_ADD = 147;
    /** CPython opcode LOAD_CLASSDEREF */
    static final int LOAD_CLASSDEREF = 148;
    /** CPython opcode COPY_FREE_VARS */
    static final int COPY_FREE_VARS = 149;
    /** CPython opcode RESUME */
    static final int RESUME = 151;
    /** CPython opcode MATCH_CLASS */
    static final int MATCH_CLASS = 152;
    /** CPython opcode FORMAT_VALUE */
    static final int FORMAT_VALUE = 155;
    /** CPython opcode BUILD_CONST_KEY_MAP */
    static final int BUILD_CONST_KEY_MAP = 156;
    /** CPython opcode BUILD_STRING */
    static final int BUILD_STRING = 157;
    /** CPython opcode LOAD_METHOD */
    static final int LOAD_METHOD = 160;
    /** CPython opcode LIST_EXTEND */
    static final int LIST_EXTEND = 162;
    /** CPython opcode SET_UPDATE */
    static final int SET_UPDATE = 163;
    /** CPython opcode DICT_MERGE */
    static final int DICT_MERGE = 164;
    /** CPython opcode DICT_UPDATE */
    static final int DICT_UPDATE = 165;
    /** CPython opcode PRECALL */
    static final int PRECALL = 166;
    /** CPython opcode CALL */
    static final int CALL = 171;
    /** CPython opcode KW_NAMES */
    static final int KW_NAMES = 172;
    /** CPython opcode POP_JUMP_BACKWARD_IF_NOT_NONE */
    static final int POP_JUMP_BACKWARD_IF_NOT_NONE = 173;
    /** CPython opcode POP_JUMP_BACKWARD_IF_NONE */
    static final int POP_JUMP_BACKWARD_IF_NONE = 174;
    /** CPython opcode POP_JUMP_BACKWARD_IF_FALSE */
    static final int POP_JUMP_BACKWARD_IF_FALSE = 175;
    /** CPython opcode POP_JUMP_BACKWARD_IF_TRUE */
    static final int POP_JUMP_BACKWARD_IF_TRUE = 176;

    // Adaptive opcodes (not needed for Jython). ---------------------

    /** CPython opcode BINARY_OP_ADAPTIVE */
    static final int BINARY_OP_ADAPTIVE = 3;
    /** CPython opcode BINARY_OP_ADD_FLOAT */
    static final int BINARY_OP_ADD_FLOAT = 4;
    /** CPython opcode BINARY_OP_ADD_INT */
    static final int BINARY_OP_ADD_INT = 5;
    /** CPython opcode BINARY_OP_ADD_UNICODE */
    static final int BINARY_OP_ADD_UNICODE = 6;
    /** CPython opcode BINARY_OP_INPLACE_ADD_UNICODE */
    static final int BINARY_OP_INPLACE_ADD_UNICODE = 7;
    /** CPython opcode BINARY_OP_MULTIPLY_FLOAT */
    static final int BINARY_OP_MULTIPLY_FLOAT = 8;
    /** CPython opcode BINARY_OP_MULTIPLY_INT */
    static final int BINARY_OP_MULTIPLY_INT = 13;
    /** CPython opcode BINARY_OP_SUBTRACT_FLOAT */
    static final int BINARY_OP_SUBTRACT_FLOAT = 14;
    /** CPython opcode BINARY_OP_SUBTRACT_INT */
    static final int BINARY_OP_SUBTRACT_INT = 16;
    /** CPython opcode BINARY_SUBSCR_ADAPTIVE */
    static final int BINARY_SUBSCR_ADAPTIVE = 17;
    /** CPython opcode BINARY_SUBSCR_DICT */
    static final int BINARY_SUBSCR_DICT = 18;
    /** CPython opcode BINARY_SUBSCR_GETITEM */
    static final int BINARY_SUBSCR_GETITEM = 19;
    /** CPython opcode BINARY_SUBSCR_LIST_INT */
    static final int BINARY_SUBSCR_LIST_INT = 20;
    /** CPython opcode BINARY_SUBSCR_TUPLE_INT */
    static final int BINARY_SUBSCR_TUPLE_INT = 21;
    /** CPython opcode CALL_ADAPTIVE */
    static final int CALL_ADAPTIVE = 22;
    /** CPython opcode CALL_PY_EXACT_ARGS */
    static final int CALL_PY_EXACT_ARGS = 23;
    /** CPython opcode CALL_PY_WITH_DEFAULTS */
    static final int CALL_PY_WITH_DEFAULTS = 24;
    /** CPython opcode COMPARE_OP_ADAPTIVE */
    static final int COMPARE_OP_ADAPTIVE = 26;
    /** CPython opcode COMPARE_OP_FLOAT_JUMP */
    static final int COMPARE_OP_FLOAT_JUMP = 27;
    /** CPython opcode COMPARE_OP_INT_JUMP */
    static final int COMPARE_OP_INT_JUMP = 28;
    /** CPython opcode COMPARE_OP_STR_JUMP */
    static final int COMPARE_OP_STR_JUMP = 29;
    /** CPython opcode EXTENDED_ARG_QUICK */
    static final int EXTENDED_ARG_QUICK = 34;
    /** CPython opcode JUMP_BACKWARD_QUICK */
    static final int JUMP_BACKWARD_QUICK = 38;
    /** CPython opcode LOAD_ATTR_ADAPTIVE */
    static final int LOAD_ATTR_ADAPTIVE = 39;
    /** CPython opcode LOAD_ATTR_INSTANCE_VALUE */
    static final int LOAD_ATTR_INSTANCE_VALUE = 40;
    /** CPython opcode LOAD_ATTR_MODULE */
    static final int LOAD_ATTR_MODULE = 41;
    /** CPython opcode LOAD_ATTR_SLOT */
    static final int LOAD_ATTR_SLOT = 42;
    /** CPython opcode LOAD_ATTR_WITH_HINT */
    static final int LOAD_ATTR_WITH_HINT = 43;
    /** CPython opcode LOAD_CONST__LOAD_FAST */
    static final int LOAD_CONST__LOAD_FAST = 44;
    /** CPython opcode LOAD_FAST__LOAD_CONST */
    static final int LOAD_FAST__LOAD_CONST = 45;
    /** CPython opcode LOAD_FAST__LOAD_FAST */
    static final int LOAD_FAST__LOAD_FAST = 46;
    /** CPython opcode LOAD_GLOBAL_ADAPTIVE */
    static final int LOAD_GLOBAL_ADAPTIVE = 47;
    /** CPython opcode LOAD_GLOBAL_BUILTIN */
    static final int LOAD_GLOBAL_BUILTIN = 48;
    /** CPython opcode LOAD_GLOBAL_MODULE */
    static final int LOAD_GLOBAL_MODULE = 55;
    /** CPython opcode LOAD_METHOD_ADAPTIVE */
    static final int LOAD_METHOD_ADAPTIVE = 56;
    /** CPython opcode LOAD_METHOD_CLASS */
    static final int LOAD_METHOD_CLASS = 57;
    /** CPython opcode LOAD_METHOD_MODULE */
    static final int LOAD_METHOD_MODULE = 58;
    /** CPython opcode LOAD_METHOD_NO_DICT */
    static final int LOAD_METHOD_NO_DICT = 59;
    /** CPython opcode LOAD_METHOD_WITH_DICT */
    static final int LOAD_METHOD_WITH_DICT = 62;
    /** CPython opcode LOAD_METHOD_WITH_VALUES */
    static final int LOAD_METHOD_WITH_VALUES = 63;
    /** CPython opcode PRECALL_ADAPTIVE */
    static final int PRECALL_ADAPTIVE = 64;
    /** CPython opcode PRECALL_BOUND_METHOD */
    static final int PRECALL_BOUND_METHOD = 65;
    /** CPython opcode PRECALL_BUILTIN_CLASS */
    static final int PRECALL_BUILTIN_CLASS = 66;
    /** CPython opcode PRECALL_BUILTIN_FAST_WITH_KEYWORDS */
    static final int PRECALL_BUILTIN_FAST_WITH_KEYWORDS = 67;
    /** CPython opcode PRECALL_METHOD_DESCRIPTOR_FAST_WITH_KEYWORDS */
    static final int PRECALL_METHOD_DESCRIPTOR_FAST_WITH_KEYWORDS = 72;
    /** CPython opcode PRECALL_NO_KW_BUILTIN_FAST */
    static final int PRECALL_NO_KW_BUILTIN_FAST = 73;
    /** CPython opcode PRECALL_NO_KW_BUILTIN_O */
    static final int PRECALL_NO_KW_BUILTIN_O = 76;
    /** CPython opcode PRECALL_NO_KW_ISINSTANCE */
    static final int PRECALL_NO_KW_ISINSTANCE = 77;
    /** CPython opcode PRECALL_NO_KW_LEN */
    static final int PRECALL_NO_KW_LEN = 78;
    /** CPython opcode PRECALL_NO_KW_LIST_APPEND */
    static final int PRECALL_NO_KW_LIST_APPEND = 79;
    /** CPython opcode PRECALL_NO_KW_METHOD_DESCRIPTOR_FAST */
    static final int PRECALL_NO_KW_METHOD_DESCRIPTOR_FAST = 80;
    /** CPython opcode PRECALL_NO_KW_METHOD_DESCRIPTOR_NOARGS */
    static final int PRECALL_NO_KW_METHOD_DESCRIPTOR_NOARGS = 81;
    /** CPython opcode PRECALL_NO_KW_METHOD_DESCRIPTOR_O */
    static final int PRECALL_NO_KW_METHOD_DESCRIPTOR_O = 113;
    /** CPython opcode PRECALL_NO_KW_STR_1 */
    static final int PRECALL_NO_KW_STR_1 = 121;
    /** CPython opcode PRECALL_NO_KW_TUPLE_1 */
    static final int PRECALL_NO_KW_TUPLE_1 = 127;
    /** CPython opcode PRECALL_NO_KW_TYPE_1 */
    static final int PRECALL_NO_KW_TYPE_1 = 141;
    /** CPython opcode PRECALL_PYFUNC */
    static final int PRECALL_PYFUNC = 143;
    /** CPython opcode RESUME_QUICK */
    static final int RESUME_QUICK = 150;
    /** CPython opcode STORE_ATTR_ADAPTIVE */
    static final int STORE_ATTR_ADAPTIVE = 153;
    /** CPython opcode STORE_ATTR_INSTANCE_VALUE */
    static final int STORE_ATTR_INSTANCE_VALUE = 154;
    /** CPython opcode STORE_ATTR_SLOT */
    static final int STORE_ATTR_SLOT = 158;
    /** CPython opcode STORE_ATTR_WITH_HINT */
    static final int STORE_ATTR_WITH_HINT = 159;
    /** CPython opcode STORE_FAST__LOAD_FAST */
    static final int STORE_FAST__LOAD_FAST = 161;
    /** CPython opcode STORE_FAST__STORE_FAST */
    static final int STORE_FAST__STORE_FAST = 167;
    /** CPython opcode STORE_SUBSCR_ADAPTIVE */
    static final int STORE_SUBSCR_ADAPTIVE = 168;
    /** CPython opcode STORE_SUBSCR_DICT */
    static final int STORE_SUBSCR_DICT = 169;
    /** CPython opcode STORE_SUBSCR_LIST_INT */
    static final int STORE_SUBSCR_LIST_INT = 170;
    /** CPython opcode UNPACK_SEQUENCE_ADAPTIVE */
    static final int UNPACK_SEQUENCE_ADAPTIVE = 177;
    /** CPython opcode UNPACK_SEQUENCE_LIST */
    static final int UNPACK_SEQUENCE_LIST = 178;
    /** CPython opcode UNPACK_SEQUENCE_TUPLE */
    static final int UNPACK_SEQUENCE_TUPLE = 179;
    /** CPython opcode UNPACK_SEQUENCE_TWO_TUPLE */
    static final int UNPACK_SEQUENCE_TWO_TUPLE = 180;

    /** Synthetic CPython opcode used to control instruction tracing. */
    static final int DO_TRACING = 255;

    // An encoding of binary operations used only by BINARY_OP

    /** Encoding of ADD used in BINARY_OP opcode. */
    static final int NB_ADD = 0;
    /** Encoding of AND used in BINARY_OP opcode. */
    static final int NB_AND = 1;
    /** Encoding of FLOOR_DIVIDE used in BINARY_OP opcode. */
    static final int NB_FLOOR_DIVIDE = 2;
    /** Encoding of LSHIFT used in BINARY_OP opcode. */
    static final int NB_LSHIFT = 3;
    /** Encoding of MATRIX_MULTIPLY used in BINARY_OP opcode. */
    static final int NB_MATRIX_MULTIPLY = 4;
    /** Encoding of MULTIPLY used in BINARY_OP opcode. */
    static final int NB_MULTIPLY = 5;
    /** Encoding of REMAINDER used in BINARY_OP opcode. */
    static final int NB_REMAINDER = 6;
    /** Encoding of OR used in BINARY_OP opcode. */
    static final int NB_OR = 7;
    /** Encoding of POWER used in BINARY_OP opcode. */
    static final int NB_POWER = 8;
    /** Encoding of RSHIFT used in BINARY_OP opcode. */
    static final int NB_RSHIFT = 9;
    /** Encoding of SUBTRACT used in BINARY_OP opcode. */
    static final int NB_SUBTRACT = 10;
    /** Encoding of TRUE_DIVIDE used in BINARY_OP opcode. */
    static final int NB_TRUE_DIVIDE = 11;
    /** Encoding of XOR used in BINARY_OP opcode. */
    static final int NB_XOR = 12;
    /** Encoding of INPLACE_ADD used in BINARY_OP opcode. */
    static final int NB_INPLACE_ADD = 13;
    /** Encoding of INPLACE_AND used in BINARY_OP opcode. */
    static final int NB_INPLACE_AND = 14;
    /** Encoding of INPLACE_FLOOR_DIVIDE used in BINARY_OP opcode. */
    static final int NB_INPLACE_FLOOR_DIVIDE = 15;
    /** Encoding of INPLACE_LSHIFT used in BINARY_OP opcode. */
    static final int NB_INPLACE_LSHIFT = 16;
    /** Encoding of INPLACE_MATRIX_MULTIPLY used in BINARY_OP opcode. */
    static final int NB_INPLACE_MATRIX_MULTIPLY = 17;
    /** Encoding of INPLACE_MULTIPLY used in BINARY_OP opcode. */
    static final int NB_INPLACE_MULTIPLY = 18;
    /** Encoding of INPLACE_REMAINDER used in BINARY_OP opcode. */
    static final int NB_INPLACE_REMAINDER = 19;
    /** Encoding of INPLACE_OR used in BINARY_OP opcode. */
    static final int NB_INPLACE_OR = 20;
    /** Encoding of INPLACE_POWER used in BINARY_OP opcode. */
    static final int NB_INPLACE_POWER = 21;
    /** Encoding of INPLACE_RSHIFT used in BINARY_OP opcode. */
    static final int NB_INPLACE_RSHIFT = 22;
    /** Encoding of INPLACE_SUBTRACT used in BINARY_OP opcode. */
    static final int NB_INPLACE_SUBTRACT = 23;
    /** Encoding of INPLACE_TRUE_DIVIDE used in BINARY_OP opcode. */
    static final int NB_INPLACE_TRUE_DIVIDE = 24;
    /** Encoding of INPLACE_XOR used in BINARY_OP opcode. */
    static final int NB_INPLACE_XOR = 25;

    /*
     * Various CPython opcodes are followed by an in-line cache, which
     * is zero in the byte code initially. We do not implement this
     * cache, or squeeze them out which would involve recomputing the
     * jumps.
     *
     * It works to treat 0 as a NOP, but it is inefficient, so we end
     * those instructions with a jump (advance of the IP) of the right
     * size. CPython can get these from a sizeof() the appropriate
     * struct but we work it out by hand from the struct quoted here in
     * the comments.
     */

    /**
     * In CPython 3.11 the in-line cache that follows certain
     * instructions is zero in the byte code initially.
     */
    static final int CACHE = 0;

    // @formatter:off
    // #define CACHE_ENTRIES(cache) (sizeof(cache)/sizeof(_Py_CODEUNIT))

    //  typedef struct {
    //      _Py_CODEUNIT counter;
    //      _Py_CODEUNIT index;
    //      _Py_CODEUNIT module_keys_version[2];
    //      _Py_CODEUNIT builtin_keys_version;
    //  } _PyLoadGlobalCache;

    /** Cache following LOAD_GLOBAL opcode (words). */
    static final int INLINE_CACHE_ENTRIES_LOAD_GLOBAL = 5; // _PyLoadGlobalCache

    //  typedef struct {
    //      _Py_CODEUNIT counter;
    //  } _PyBinaryOpCache;

    /** Cache following BINARY_OP opcode (words). */
    static final int INLINE_CACHE_ENTRIES_BINARY_OP = 1; // _PyBinaryOpCache

    //  typedef struct {
    //      _Py_CODEUNIT counter;
    //  } _PyUnpackSequenceCache;

    /** Cache following UNPACK_SEQUENCE opcode (words). */
    static final int INLINE_CACHE_ENTRIES_UNPACK_SEQUENCE = 1; // _PyUnpackSequenceCache

    //  typedef struct {
    //      _Py_CODEUNIT counter;
    //      _Py_CODEUNIT mask;
    //  } _PyCompareOpCache;

    /** Cache following COMPARE_OP opcode (words). */
    static final int INLINE_CACHE_ENTRIES_COMPARE_OP = 2; // _PyCompareOpCache

    //  typedef struct {
    //      _Py_CODEUNIT counter;
    //      _Py_CODEUNIT type_version[2];
    //      _Py_CODEUNIT func_version;
    //  } _PyBinarySubscrCache;

    /** Cache following BINARY_SUBSCR opcode (words). */
    static final int INLINE_CACHE_ENTRIES_BINARY_SUBSCR = 4; // _PyBinarySubscrCache

    //  typedef struct {
    //      _Py_CODEUNIT counter;
    //      _Py_CODEUNIT version[2];
    //      _Py_CODEUNIT index;
    //  } _PyAttrCache;

    /** Cache following LOAD_ATTR opcode (words). */
    static final int INLINE_CACHE_ENTRIES_LOAD_ATTR = 4; // _PyAttrCache
    /** Cache following STORE_ATTR opcode (words). */
    static final int INLINE_CACHE_ENTRIES_STORE_ATTR = 4; // _PyAttrCache

    //  typedef struct {
    //      _Py_CODEUNIT counter;
    //      _Py_CODEUNIT type_version[2];
    //      _Py_CODEUNIT dict_offset;
    //      _Py_CODEUNIT keys_version[2];
    //      _Py_CODEUNIT descr[4];
    //  } _PyLoadMethodCache;

    /** Cache following LOAD_METHOD opcode (words). */
    static final int INLINE_CACHE_ENTRIES_LOAD_METHOD = 10; // _PyLoadMethodCache

    //  typedef struct {
    //      _Py_CODEUNIT counter;
    //      _Py_CODEUNIT func_version[2];
    //      _Py_CODEUNIT min_args;
    //  } _PyCallCache;

    /** Cache following CALL opcode (words). */
    static final int INLINE_CACHE_ENTRIES_CALL = 4; // _PyCallCache

    //  typedef struct {
    //      _Py_CODEUNIT counter;
    //  } _PyPrecallCache;

    /** Cache following PRECALL opcode (words). */
    static final int INLINE_CACHE_ENTRIES_PRECALL = 1; // _PyPrecallCache

    //  typedef struct {
    //      _Py_CODEUNIT counter;
    //  } _PyStoreSubscrCache;

    /** Cache following STORE_SUBSCR opcode (words). */
    static final int INLINE_CACHE_ENTRIES_STORE_SUBSCR = 1; // _PyStoreSubscrCache
    // @formatter:on
}
