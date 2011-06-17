
package org.python.modules.jffi;


public enum NativeType {
    VOID,
    BOOL,
    BYTE,
    UBYTE,
    SHORT,
    USHORT,
    INT,
    UINT,
    LONGLONG,
    ULONGLONG,
    LONG,
    ULONG,
    FLOAT,
    DOUBLE,
    POINTER,
    BUFFER_IN,
    BUFFER_OUT,
    BUFFER_INOUT,
    STRING,
    ARRAY,
    STRUCT;

    static final com.kenai.jffi.Type jffiType(NativeType type) {
        switch (type) {
            case VOID:
                return com.kenai.jffi.Type.VOID;
            case BYTE:
                return com.kenai.jffi.Type.SINT8;
            case UBYTE:
                return com.kenai.jffi.Type.UINT8;
            case SHORT:
                return com.kenai.jffi.Type.SINT16;
            case USHORT:
                return com.kenai.jffi.Type.UINT16;
            case INT:
            case BOOL:
                return com.kenai.jffi.Type.SINT32;
            case UINT:
                return com.kenai.jffi.Type.UINT32;
            case LONGLONG:
                return com.kenai.jffi.Type.SINT64;
            case ULONGLONG:
                return com.kenai.jffi.Type.UINT64;
            case LONG:
                return com.kenai.jffi.Type.SLONG;
            case ULONG:
                return com.kenai.jffi.Type.ULONG;
            case FLOAT:
                return com.kenai.jffi.Type.FLOAT;
            case DOUBLE:
                return com.kenai.jffi.Type.DOUBLE;
            case POINTER:
            case STRING:
                return com.kenai.jffi.Type.POINTER;

            default:
                throw new RuntimeException("Unknown type " + type);
        }
    }
}
