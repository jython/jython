/*
 *
 */
package org.python.modules.jffi;

import com.kenai.jffi.CallingConvention;
import org.python.core.PyObject;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 *
 */
class JITCompiler {
    
    private final Map<JITSignature, HandleRef> 
            handles = new HashMap<JITSignature, HandleRef>();

    private final Map<Class, JITHandle> classes = Collections.synchronizedMap(new WeakHashMap<Class, JITHandle>());

    private final ReferenceQueue referenceQueue = new ReferenceQueue();
    
    private final JITHandle failedHandle = new JITHandle(this,
            new JITSignature(NativeType.VOID, new NativeType[0], false, new boolean[0], CallingConvention.DEFAULT, false),
            true);

    private static class SingletonHolder {
        private static final JITCompiler INSTANCE = new JITCompiler();
    }
    
    public static JITCompiler getInstance() {
        return SingletonHolder.INSTANCE;
    }
    
    private static final class HandleRef extends WeakReference<JITHandle> {
        JITSignature signature;

        public HandleRef(JITHandle handle, JITSignature signature, ReferenceQueue refqueue) {
            super(handle, refqueue);
            this.signature = signature;
        }
    }

    private void cleanup() {
        HandleRef ref;
        while ((ref = (HandleRef) referenceQueue.poll()) != null) {
            handles.remove(ref.signature);
        }
    }
    
    
    JITHandle getHandle(PyObject resultType, PyObject[] parameterTypes, CallingConvention convention, boolean ignoreErrno) {
        
        boolean hasResultConverter = !(resultType instanceof CType.Builtin);
        NativeType nativeResultType;

        if (resultType instanceof CType.Builtin) {
            nativeResultType = ((CType) resultType).getNativeType();
        /*
        } else if (resultType instanceof MappedType) {
            nativeResultType = ((MappedType) resultType).getRealType().getNativeType();
        */
        } else {
            return failedHandle;
        }

        NativeType[] nativeParameterTypes = new NativeType[parameterTypes.length];
        boolean[] hasParameterConverter = new boolean[parameterTypes.length];
        
        for (int i = 0; i < hasParameterConverter.length; i++) {
            CType parameterType = CType.typeOf(parameterTypes[i]);
            if (parameterType instanceof CType.Builtin) {
                nativeParameterTypes[i] = parameterType.getNativeType();
        /*
            } else if (parameterType instanceof MappedType) {
                nativeParameterTypes[i] = ((MappedType) parameterType).getRealType().getNativeType();
        */
            } else {
                return failedHandle;
            }

            hasParameterConverter[i] = !(parameterType instanceof CType.Builtin);
        }
        
        JITSignature jitSignature = new JITSignature(nativeResultType, nativeParameterTypes, 
                hasResultConverter, hasParameterConverter, convention, ignoreErrno);
        
        synchronized (this) {
            cleanup();
            HandleRef ref = handles.get(jitSignature);
            JITHandle handle = ref != null ? ref.get() : null;
            if (handle == null) {
                handle = new JITHandle(this, jitSignature, false);
                handles.put(jitSignature, new HandleRef(handle, jitSignature, referenceQueue));
            }
            
            return handle;
        }
    }

    void registerClass(JITHandle handle, Class<? extends Invoker> klass) {
        classes.put(klass, handle);
    }
}
