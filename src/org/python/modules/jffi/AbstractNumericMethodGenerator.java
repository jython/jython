package org.python.modules.jffi;

import com.kenai.jffi.Platform;
import org.objectweb.asm.Label;
import org.python.core.PyObject;

import static org.python.modules.jffi.CodegenUtils.*;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

/**
 *
 */
abstract class AbstractNumericMethodGenerator implements JITMethodGenerator {

    public void generate(AsmClassBuilder builder, String functionName, JITSignature signature) {
        SkinnyMethodAdapter mv = new SkinnyMethodAdapter(builder.getClassVisitor(),
                ACC_PUBLIC | ACC_FINAL, functionName,
                sig(PyObject.class, params(PyObject.class, signature.getParameterCount())),
                null, null);

        mv.start();
        generate(builder, mv, signature);
        mv.visitMaxs(10, 10);
        mv.visitEnd();
    }

    public void generate(AsmClassBuilder builder, SkinnyMethodAdapter mv, JITSignature signature) {
        final Class nativeIntType = getInvokerIntType();
        int maxPointerIndex = -1;
        Label[] fallback = new Label[signature.getParameterCount()];
        for (int i = 0; i < signature.getParameterCount(); i++) {
            fallback[i] = new Label();
        }

        mv.getstatic(p(JITInvoker.class), "jffiInvoker", ci(com.kenai.jffi.Invoker.class));
        // [ stack now contains: Invoker ]
        mv.aload(0);
        mv.getfield(p(JITInvoker.class), "jffiFunction", ci(com.kenai.jffi.Function.class));
        // [ stack now contains: Invoker, Function ]
        final int firstParam = 1;
        
        // Perform any generic data conversions on the parameters
        for (int i = 0; i < signature.getParameterCount(); ++i) {
            if (signature.hasParameterConverter(i)) {
                mv.aload(0); // this
                mv.getfield(builder.getClassName(), builder.getParameterConverterFieldName(i), ci(NativeDataConverter.class));
                mv.aload(firstParam + i); // PyObject parameter
                mv.invokevirtual(p(NativeDataConverter.class), "toNative", sig(PyObject.class, PyObject.class));
                mv.astore(firstParam + i);
            }
        }

        // Load and un-box parameters
        for (int i = 0; i < signature.getParameterCount(); ++i) {
            final NativeType parameterType = signature.getParameterType(i);
            final int paramVar = i + firstParam;
            mv.aload(paramVar);
            switch (parameterType) {
                case BOOL:
                    unbox(mv, "boolValue");
                    break;

                case BYTE:
                    unbox(mv, "s8Value");
                    break;
                
                case UBYTE:
                    unbox(mv, "u8Value");
                    break;
                
                case SHORT:
                    unbox(mv, "s16Value");
                    break;
                
                case USHORT:
                    unbox(mv, "u16Value");
                    break;
                
                case INT:
                    unbox(mv, "s32Value");
                    break;
                
                case UINT:
                    unbox(mv, "u32Value");
                    break;
                    
                case LONG:
                    if (Platform.getPlatform().longSize() == 32) {
                        unbox(mv, "s32Value");
                    } else {
                        unbox(mv, "s64Value");
                    }
                    break;
                
                case ULONG:
                    if (Platform.getPlatform().longSize() == 32) {
                        unbox(mv, "u32Value");
                    } else {
                        unbox(mv, "u64Value");
                    }
                    break;
                
                case LONGLONG:
                    unbox(mv, "s64Value");
                    break;
                
                case ULONGLONG:
                    unbox(mv, "u64Value");
                    break;
                
                case POINTER:
                    maxPointerIndex = i;
                    Label direct = new Label();
                    Label done = new Label();
                    Label converted = new Label();
                    
                    // If a direct pointer is passed in, jump straight to conversion
                    mv.instance_of(p(Pointer.class));
                    mv.iftrue(direct);

                    mv.aload(paramVar);
                    mv.invokestatic(p(JITRuntime.class), "other2ptr", sig(PyObject.class, PyObject.class));
                    mv.label(converted);
                    mv.dup();
                    mv.astore(paramVar);
                    mv.instance_of(p(Pointer.class));
                    mv.iffalse(fallback[i]);
                    
                    mv.label(direct);
                    // The parameter is guaranteed to be a direct pointer now
                    mv.aload(paramVar);
                    unbox(mv, "pointerValue");
                    mv.label(done);
                    break;

                case FLOAT:
                    unbox(mv, "f32Value");
                    break;

                case DOUBLE:
                    unbox(mv, "f64Value");
                    break;

                default:
                    throw new UnsupportedOperationException("unsupported parameter type " + parameterType);
            }
        }

        // stack now contains [ Invoker, Function, int/long args ]
        mv.invokevirtual(p(com.kenai.jffi.Invoker.class),
                getInvokerMethodName(signature),
                getInvokerSignature(signature.getParameterCount()));


        // box up the raw int/long result
        boxResult(mv, signature.getResultType());
        emitResultConversion(mv, builder, signature);;
        mv.areturn();
        
        // Generate code to pop all the converted arguments off the stack 
        // when falling back to buffer-invocation
        if (maxPointerIndex >= 0) {
            for (int i = maxPointerIndex; i > 0; i--) {
                mv.label(fallback[i]);
                if (int.class == nativeIntType) {
                    mv.pop();
                } else {
                    mv.pop2();
                }
            }

            mv.label(fallback[0]);
            // Pop ThreadContext, Invoker and Function
            mv.pop(); mv.pop();
            
            // Call the fallback invoker
            mv.aload(0);
            mv.getfield(p(JITInvoker.class), "fallbackInvoker", ci(Invoker.class));

            for (int i = 0; i < signature.getParameterCount(); i++) {
                mv.aload(firstParam + i);
            }
            
            mv.invokevirtual(p(Invoker.class), "invoke",
                    sig(PyObject.class, params(PyObject.class, signature.getParameterCount())));
            emitResultConversion(mv, builder, signature);
            mv.areturn();
        }
    }

    private void emitResultConversion(SkinnyMethodAdapter mv, AsmClassBuilder builder, JITSignature signature) {
        if (signature.hasResultConverter()) {
            mv.aload(0); // [ result, this ]
            mv.getfield(builder.getClassName(), builder.getResultConverterFieldName(), ci(NativeDataConverter.class));
            mv.swap();   // [ converter, result ]
            mv.invokevirtual(p(NativeDataConverter.class), "fromNative", sig(PyObject.class, PyObject.class));
        }
    }

    private void boxResult(SkinnyMethodAdapter mv, String boxMethodName) {
        mv.invokestatic(p(JITRuntime.class), boxMethodName,
                sig(PyObject.class, getInvokerIntType()));
    }

    private void boxResult(SkinnyMethodAdapter mv, NativeType type) {
        switch (type) {
            case BOOL:
                boxResult(mv, "newBoolean");
                break;

            case BYTE:
                boxResult(mv, "newSigned8");
                break;

            case UBYTE:
                boxResult(mv, "newUnsigned8");
                break;

            case SHORT:
                boxResult(mv, "newSigned16");
                break;

            case USHORT:
                boxResult(mv, "newUnsigned16");
                break;

            case INT:
                boxResult(mv, "newSigned32");
                break;

            case UINT:
                boxResult(mv, "newUnsigned32");
                break;

            case LONG:
                if (Platform.getPlatform().longSize() == 32) {
                    boxResult(mv, "newSigned32");
                } else {
                    boxResult(mv, "newSigned64");
                }
                break;

            case ULONG:
                if (Platform.getPlatform().longSize() == 32) {
                    boxResult(mv, "newUnsigned32");
                } else {
                    boxResult(mv, "newUnsigned64");
                }
                break;

            case LONGLONG:
                boxResult(mv, "newSigned64");
                break;

            case ULONGLONG:
                boxResult(mv, "newUnsigned64");
                break;
                
            case FLOAT:
                boxResult(mv, "newFloat32");
                break;
                
            case DOUBLE:
                boxResult(mv, "newFloat64");
                break;

            case VOID:
                boxResult(mv, "newNone");
                break;

            case POINTER:
                boxResult(mv, "newPointer" + Platform.getPlatform().addressSize());
                break;

            case STRING:
                boxResult(mv, "newString");
                break;


            default:
                throw new UnsupportedOperationException("native return type not supported: " + type);

        }
    }

    private void unbox(SkinnyMethodAdapter mv, String method) {
        mv.invokestatic(p(JITRuntime.class), getRuntimeMethod(method), sig(getInvokerIntType(), PyObject.class));
    }

    private String getRuntimeMethod(String method) {
        return method + (int.class == getInvokerIntType() ? "32" : "64");
    }

    abstract String getInvokerMethodName(JITSignature signature);

    abstract String getInvokerSignature(int parameterCount);

    abstract Class getInvokerIntType();


    public static boolean isPrimitiveInt(Class c) {
        return byte.class == c || char.class == c || short.class == c || int.class == c || boolean.class == c;
    }

    public static final void widen(SkinnyMethodAdapter mv, Class from, Class to) {
        if (long.class == to && long.class != from && isPrimitiveInt(from)) {
            mv.i2l();
        }
    }

    public static final void narrow(SkinnyMethodAdapter mv, Class from, Class to) {
        if (!from.equals(to) && isPrimitiveInt(to)) {
            if (long.class == from) {
                mv.l2i();
            }

            if (byte.class == to) {
                mv.i2b();

            } else if (short.class == to) {
                mv.i2s();

            } else if (char.class == to) {
                mv.i2c();

            } else if (boolean.class == to) {
                // Ensure only 0x0 and 0x1 values are used for boolean
                mv.iconst_1();
                mv.iand();
            }
        }
    }
    
    protected static String[] buildSignatures(Class nativeIntClass, int maxParameters) {
        char sigChar = int.class == nativeIntClass ? 'I' : 'J';
        
        String[] signatures = new String[maxParameters + 1];
        for (int i = 0; i < signatures.length; i++) {
            
            StringBuilder sb = new StringBuilder();
            
            sb.append('(').append(ci(com.kenai.jffi.Function.class));
            
            for (int n = 0; n < i; n++) {
                sb.append(sigChar);
            }
            
            signatures[i] = sb.append(")").append(sigChar).toString();
        }
        
        return signatures;
    }
}
