package org.python.compiler;


import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.Type;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyMethod;
import org.python.core.PyObject;
import org.python.core.PyProxy;
import org.python.core.PyReflectedFunction;
import org.python.util.Generic;

/*
 * Various constants and methods for generating Proxy code
 */
public class ProxyCodeHelpers {
    public static final int tBoolean=0;
    public static final int tByte=1;
    public static final int tShort=2;
    public static final int tInteger=3;
    public static final int tLong=4;
    public static final int tFloat=5;
    public static final int tDouble=6;
    public static final int tCharacter=7;
    public static final int tVoid=8;
    public static final int tOther=9;
    public static final int tNone=10;

    public static Map<Class<?>, Integer> types = fillTypes();

    public static Map<Class<?>, Integer> fillTypes() {
        Map<Class<?>, Integer> typeMap = Generic.map();
        typeMap.put(Boolean.TYPE, tBoolean);
        typeMap.put(Byte.TYPE, tByte);
        typeMap.put(Short.TYPE, tShort);
        typeMap.put(Integer.TYPE, tInteger);
        typeMap.put(Long.TYPE, tLong);
        typeMap.put(Float.TYPE, tFloat);
        typeMap.put(Double.TYPE, tDouble);
        typeMap.put(Character.TYPE, tCharacter);
        typeMap.put(Void.TYPE, tVoid);
        return typeMap;
    }

    public static int getType(Class<?> c) {
        if (c == null) {
            return tNone;
        }
        Object i = types.get(c);
        if (i == null) {
            return tOther;
        } else {
            return ((Integer)i);
        }
    }

    /**
     * Retrieves <code>name</code> from the PyObject in <code>proxy</code> if it's defined in
     * Python.  This is a specialized helper function for internal PyProxy use.
     */
    public static PyObject findPython(PyProxy proxy, String name) {
        PyObject o = proxy._getPyInstance();
        if (o == null) {
            proxy.__initProxy__(new Object[0]);
            o = proxy._getPyInstance();
        }
        PyObject ret = o.__findattr__(name);
        if (ret instanceof PyMethod) {
            PyMethod meth = ((PyMethod)ret);
            if (meth.__func__ instanceof PyReflectedFunction) {
                PyReflectedFunction func = (PyReflectedFunction)meth.__func__;
                if (func.nargs > 0 && proxy.getClass() == func.argslist[0].declaringClass) {
                    // This function is the default return for the proxy type if the Python instance
                    // hasn't returned something of its own from __findattr__, so do the standard
                    // Java call on this
                    return null;
                }
            }
        }
        Py.setSystemState(proxy._getPySystemState());
        return ret;
    }

    public static PyException notImplementedAbstractMethod(
            PyProxy proxy, String name, String superClass) {
        PyObject o = proxy._getPyInstance();
        String msg = String.format(
                "'%.200s' object does not implement abstract method '%.200s' from '%.200s'",
                o.getType().fastGetName(),
                name,
                superClass);
        return Py.NotImplementedError(msg);
    }

    public static String mapClass(Class<?> c) {
        String name = c.getName();
        int index = name.indexOf(".");
        if (index == -1) {
            return name;
        }
        StringBuffer buf = new StringBuffer(name.length());
        int last_index = 0;
        while (index != -1) {
            buf.append(name.substring(last_index, index));
            buf.append("/");
            last_index = index+1;
            index = name.indexOf(".", last_index);
        }
        buf.append(name.substring(last_index, name.length()));
        return buf.toString();
    }

    public static String mapType(Class<?> type) {
        if (type.isArray())
            return "["+mapType(type.getComponentType());

        switch (getType(type)) {
        case tByte: return "B";
        case tCharacter:  return "C";
        case tDouble:  return "D";
        case tFloat:  return "F";
        case tInteger:  return "I";
        case tLong:  return "J";
        case tShort:  return "S";
        case tBoolean:  return "Z";
        case tVoid:  return "V";
        default:
            return "L"+mapClass(type)+";";
        }
    }

    public static String makeSig(Class<?> ret, Class<?>... sig) {
        String[] mapped = new String[sig.length];
        for (int i = 0; i < mapped.length; i++) {
            mapped[i] = mapType(sig[i]);
        }
        return makeSig(mapType(ret), mapped);
    }

    public static String makeSig(String returnType, String... parameterTypes) {
        StringBuilder buf = new StringBuilder("(");
        for (String param : parameterTypes) {
            buf.append(param);
        }
        return buf.append(')').append(returnType).toString();
    }

    public static void doReturn(Code code, Class<?> type) throws Exception {
        switch (getType(type)) {
        case tNone:
            break;
        case tCharacter:
        case tBoolean:
        case tByte:
        case tShort:
        case tInteger:
            code.ireturn();
            break;
        case tLong:
            code.lreturn();
            break;
        case tFloat:
            code.freturn();
            break;
        case tDouble:
            code.dreturn();
            break;
        case tVoid:
            code.return_();
            break;
        default:
            code.areturn();
            break;
        }
    }

    public static void doNullReturn(Code code, Class<?> type) throws Exception {
        switch (getType(type)) {
        case tNone:
            break;
        case tCharacter:
        case tBoolean:
        case tByte:
        case tShort:
        case tInteger:
            code.iconst_0();
            code.ireturn();
            break;
        case tLong:
            code.lconst_0();
            code.lreturn();
            break;
        case tFloat:
            code.fconst_0();
            code.freturn();
            break;
        case tDouble:
            code.dconst_0();
            code.dreturn();
            break;
        case tVoid:
            code.return_();
            break;
        default:
            code.aconst_null();
            code.areturn();
            break;
        }
    }

    public static String[] mapClasses(Class<?>[] classes) {
        String[] mapped = new String[classes.length];
        for (int i = 0; i < mapped.length; i++) {
            mapped[i] = mapClass(classes[i]);
        }
        return mapped;
    } 
 
    public static String[] mapExceptions(Class<?>[] classes) {
        String[] exceptionTypes = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
            // Exceptions are represented by their internal names
            exceptionTypes[i] = Type.getType(classes[i]).getInternalName();
        }
        return exceptionTypes;
    }
    
    public static class MethodDescr {

        public final Class<?> returnType;

        public final String name;

        public final Class<?>[] parameters;
        public final Class<?>[] exceptions;
        public final Map<String, Object> methodAnnotations;
        public final Map<String, Object>[] parameterAnnotations;

        public MethodDescr(Method m) {
            this(m.getName(), m.getReturnType(), m.getParameterTypes(), m.getExceptionTypes(), null, null);
        }

        public MethodDescr(String name,
                Class<?> returnType,
                Class<?>[] parameters,
                Class<?>[] exceptions) {
            this.name = name;
            this.returnType = returnType;
            this.parameters = parameters;
            this.exceptions = exceptions;
            this.methodAnnotations = null;
            this.parameterAnnotations = null;
        }
        
        public MethodDescr(String name,
                           Class<?> returnType,
                           Class<?>[] parameters,
                           Class<?>[] exceptions,
                           Map<String, Object> methodAnnotations,
                           Map<String, Object>[] parameterAnnotations) {
            this.name = name;
            this.returnType = returnType;
            this.parameters = parameters;
            this.exceptions = exceptions;
            this.methodAnnotations = methodAnnotations;
            this.parameterAnnotations = parameterAnnotations;
        }

        @Override
        public int hashCode() {
            return name.hashCode() + parameters.length;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MethodDescr)) {
                return false;
            }
            MethodDescr oDescr = (MethodDescr)obj;
            if (!name.equals(oDescr.name) || parameters.length != oDescr.parameters.length) {
                return false;
            }
            for (int i = 0; i < parameters.length; i++) {
                if (!parameters[i].equals(oDescr.parameters[i])) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class ConstructorDescr extends MethodDescr {

        public ConstructorDescr(Constructor<?> cons) {
            this(cons.getParameterTypes(), cons.getExceptionTypes());
        }

        public ConstructorDescr(Class<?>[] parameters, Class<?>[] exceptions) {
            super("<init>", Void.TYPE, parameters, exceptions);
        }
    }

    public static class AnnotationDescr {
        public final Class<?> annotation;
        public final Map<String, Object> fields;
        
        public AnnotationDescr(Class<?>annotation) {
            this.annotation = annotation;
            this.fields = null;
        }
        
        public AnnotationDescr(Class<?>annotation, Map<String, Object> fields) {
            this.annotation = annotation;
            this.fields = fields;
        }
        
        public boolean hasFields() {
            if (fields == null) {
                return false;
            }
            return true;
        }
        
        public String getName() {
            return mapType(annotation);
        }
        
        public Map<String, Object> getFields() {
            return fields;
        }
        
        @Override
        public int hashCode() {
            if (hasFields()) {
                int hash = annotation.hashCode();
                for (Entry<String, Object> field: fields.entrySet()) {
                    hash += field.getKey().hashCode() + field.getValue().hashCode();
                }
                return hash;
            } else {
                return annotation.hashCode();
            }
        }
    }
}
