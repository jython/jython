
package org.python.modules.jffi;

import org.python.core.Py;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposeAsSuperclass;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "jffi.Type", base = PyObject.class)
public class Type extends PyObject {
    public static final PyType TYPE = PyType.fromClass(Type.class);
    static {
        TYPE.fastGetDict().__setitem__("Array", Array.TYPE);
    }
    public static final Type VOID = primitive(NativeType.VOID);
    public static final Type SINT8 = primitive(NativeType.BYTE);
    public static final Type UINT8 = primitive(NativeType.UBYTE);
    public static final Type SINT16 = primitive(NativeType.SHORT);
    public static final Type UINT16 = primitive(NativeType.USHORT);
    public static final Type SINT32 = primitive(NativeType.INT);
    public static final Type UINT32 = primitive(NativeType.UINT);
    public static final Type SINT64 = primitive(NativeType.LONGLONG);
    public static final Type UINT64 = primitive(NativeType.ULONGLONG);
    public static final Type SLONG = primitive(NativeType.LONG);
    public static final Type ULONG = primitive(NativeType.ULONG);
    public static final Type FLOAT = primitive(NativeType.FLOAT);
    public static final Type DOUBLE = primitive(NativeType.DOUBLE);
    public static final Type POINTER = primitive(NativeType.POINTER);
    public static final Type STRING = primitive(NativeType.STRING);

    final com.kenai.jffi.Type jffiType;
    
    final NativeType nativeType;

    /** Size of this type in bytes */
    @ExposedGet
    public final int size;

    /** Minimum alignment of this type in bytes */
    @ExposedGet
    public final int alignment;

    /** The <tt>MemoryOp</tt> used to read/write items of this type */
    private final MemoryOp memoryOp;

    Type(NativeType type, com.kenai.jffi.Type jffiType, MemoryOp memoryOp) {
        this.nativeType = type;
        this.jffiType = jffiType;
        this.size = jffiType.size();
        this.alignment = jffiType.alignment();
        this.memoryOp = memoryOp;
    }


    public NativeType getNativeType() {
        return nativeType;
    }

    MemoryOp getMemoryOp() {
        return memoryOp;
    }

    public int alignment() {
        return alignment;
    }

    public int size() {
        return size;
    }    
    
    static final Type primitive(NativeType type) {
        Type t = new Builtin(type, NativeType.jffiType(type));
        Type.TYPE.fastGetDict().__setitem__(type.name(), t);
        return t;
    }

    static final class Builtin extends Type implements ExposeAsSuperclass {
        public Builtin(NativeType type, com.kenai.jffi.Type jffiType) {
            super(type, jffiType, MemoryOp.getMemoryOp(type));
        }

        @Override
        public final String toString() {
            return "<jffi.Type." + nativeType.name() + ">";
        }
    }

    @ExposedType(name = "jffi.Type.Array", base = Type.class)
    static final class Array extends Type {
        public static final PyType TYPE = PyType.fromClass(Array.class);
        final Type componentType;
        
        @ExposedGet
        public final int length;

        public Array(Type componentType, int length) {
            super(NativeType.ARRAY, new com.kenai.jffi.Array(componentType.jffiType, length), null);
            this.componentType = componentType;
            this.length = length;
        }

        @ExposedNew
        public static PyObject Array_new(PyNewWrapper new_, boolean init, PyType subtype,
                PyObject[] args, String[] keywords) {

            if (args.length != 2) {
                throw Py.TypeError(String.format("__init__() takes exactly 2 arguments (%d given)", args.length));
            }

            if (!(args[0] instanceof Type)) {
                throw Py.TypeError("expected jffi.Type");
            }

            return new Array((Type) args[0], args[1].asInt());
        }

        @Override
        public int __len__() {
            return length;
        }


    }
}
