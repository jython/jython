// Copyright (c) Corporation for National Research Initiatives
package org.python.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.Hashtable;

import org.python.objectweb.asm.Label;
import org.python.objectweb.asm.MethodVisitor;
import org.python.objectweb.asm.Opcodes;
import org.python.core.Py;

public class ProxyMaker implements ClassConstants, Opcodes
{
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

    public static Hashtable types=fillTypes();

    public static Hashtable fillTypes() {
        Hashtable types = new Hashtable();
        types.put(Boolean.TYPE, new Integer(tBoolean));
        types.put(Byte.TYPE, new Integer(tByte));
        types.put(Short.TYPE, new Integer(tShort));
        types.put(Integer.TYPE, new Integer(tInteger));
        types.put(Long.TYPE, new Integer(tLong));
        types.put(Float.TYPE, new Integer(tFloat));
        types.put(Double.TYPE, new Integer(tDouble));
        types.put(Character.TYPE, new Integer(tCharacter));
        types.put(Void.TYPE, new Integer(tVoid));
        return types;
    }

    public static int getType(Class c) {
        if (c == null) return tNone;
        Object i = types.get(c);
        if (i == null) return tOther;
        else return ((Integer)i).intValue();
    }

    Class superclass;
    Class[] interfaces;
    Hashtable names;
    Hashtable supernames = new Hashtable();
    public ClassFile classfile;
    public String myClass;
    public boolean isAdapter=false;

    // Ctor used by makeProxy and AdapterMaker.
    public ProxyMaker(String classname, Class superclass) {
        this.myClass = "org.python.proxies."+classname;
        if (superclass.isInterface()) {
            this.superclass = Object.class;
            this.interfaces = new Class[] {superclass};
        } else {
            this.superclass = superclass;
            this.interfaces = new Class[0];
        }
    }

    // Ctor used by javamaker.
    public ProxyMaker(String myClass, Class superclass, Class[] interfaces) {
        this.myClass = myClass;
        if (superclass == null)
            superclass = Object.class;
        this.superclass = superclass;
        if (interfaces == null)
            interfaces = new Class[0];
        this.interfaces = interfaces;
    }

    public static String mapClass(Class c) {
        String name = c.getName();
        int index = name.indexOf(".");
        if (index == -1)
            return name;

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

    public static String mapType(Class type) {
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

    public static String makeSignature(Class[] sig, Class ret) {
        StringBuffer buf=new StringBuffer();
        buf.append("(");
        for (int i=0; i<sig.length; i++) {
            buf.append(mapType(sig[i]));
        }
        buf.append(")");
        buf.append(mapType(ret));
        return buf.toString();
    }


    public void doConstants() throws Exception {
        MethodVisitor mv = classfile.addMethod("<clinit>", "()V", Modifier.STATIC);
        mv.visitInsn(RETURN);
    }

    public static void doReturn(MethodVisitor mv, Class type) throws Exception {
        switch (getType(type)) {
        case tNone:
            break;
        case tCharacter:
        case tBoolean:
        case tByte:
        case tShort:
        case tInteger:
            mv.visitInsn(IRETURN);
            break;
        case tLong:
            mv.visitInsn(LRETURN);
            break;
        case tFloat:
            mv.visitInsn(FRETURN);
            break;
        case tDouble:
            mv.visitInsn(DRETURN);
            break;
        case tVoid:
            mv.visitInsn(RETURN);
            break;
        default:
            mv.visitInsn(ARETURN);
            break;
        }
    }

    public static void doNullReturn(MethodVisitor mv, Class type) throws Exception {
        switch (getType(type)) {
        case tNone:
            break;
        case tCharacter:
        case tBoolean:
        case tByte:
        case tShort:
        case tInteger:
            mv.visitInsn(ICONST_0);
            mv.visitInsn(IRETURN);
            break;
        case tLong:
            mv.visitInsn(LCONST_0);
            mv.visitInsn(LRETURN);
            break;
        case tFloat:
            mv.visitInsn(FCONST_0);
            mv.visitInsn(FRETURN);
            break;
        case tDouble:
            mv.visitInsn(DCONST_0);
            mv.visitInsn(DRETURN);
            break;
        case tVoid:
            mv.visitInsn(RETURN);
            break;
        default:
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
            break;
        }
    }

    public void callSuper(MethodVisitor mv, String name, String superclass,
                          Class[] parameters, Class ret,
                          String sig)
        throws Exception
    {
        mv.visitVarInsn(ALOAD, 0);
        int local_index;
        int i;
        for (i=0, local_index=1; i<parameters.length; i++) {
            switch(getType(parameters[i])) {
            case tCharacter:
            case tBoolean:
            case tByte:
            case tShort:
            case tInteger:
                mv.visitVarInsn(ILOAD, local_index);
                local_index += 1;
                break;
            case tLong:
                mv.visitVarInsn(LLOAD, local_index);
                local_index += 2;
                break;
            case tFloat:
                mv.visitVarInsn(FLOAD, local_index);
                local_index += 1;
                break;
            case tDouble:
                mv.visitVarInsn(DLOAD, local_index);
                local_index += 2;
                break;
            default:
                mv.visitVarInsn(ALOAD, local_index);
                local_index += 1;
                break;
            }
        }
        mv.visitMethodInsn(INVOKESPECIAL, superclass, name, sig);

        doReturn(mv, ret);
    }

    public void doJavaCall(MethodVisitor mv, String name, String type,
                          String jcallName)
        throws Exception
    {
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", jcallName, "(" + $objArr + ")" + $pyObj);
        mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "py2"+name, "(" + $pyObj + ")"+type);
    }


    public void getArgs(Code mv, Class[] parameters) throws Exception {
        if (parameters.length == 0) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, "org/python/core/Py", "EmptyObjects", $pyObjArr);
        }
        else {
            mv.iconst(parameters.length);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");

            int array = mv.getLocal("[org/python/core/PyObject");
            mv.visitVarInsn(ASTORE, array);

            int local_index;
            int i;
            for (i=0, local_index=1; i<parameters.length; i++) {
                mv.visitVarInsn(ALOAD, array);
                mv.iconst(i);

                switch (getType(parameters[i])) {
                case tBoolean:
                case tByte:
                case tShort:
                case tInteger:
                    mv.visitVarInsn(ILOAD, local_index);
                    local_index += 1;
                    mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "newInteger", "(I)" + $pyInteger);
                    break;
                case tLong:
                    mv.visitVarInsn(LLOAD, local_index);
                    local_index += 2;
                    mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "newInteger", "(J)" + $pyObj);
                    break;
                case tFloat:
                    mv.visitVarInsn(FLOAD, local_index);
                    local_index += 1;
                    mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "newFloat", "(F)" + $pyFloat);
                    break;
                case tDouble:
                    mv.visitVarInsn(DLOAD, local_index);
                    local_index += 2;
                    mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "newFloat", "(D)" + $pyFloat);
                    break;
                case tCharacter:
                    mv.visitVarInsn(ILOAD, local_index);
                    local_index += 1;
                    mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "newString", "(C)" + $pyStr);
                    break;
                default:
                    mv.visitVarInsn(ALOAD, local_index);
                    local_index += 1;
                    break;
                }
                mv.visitInsn(AASTORE);
            }
            mv.visitVarInsn(ALOAD, array);
        }
    }

    public void callMethod(Code mv, String name, Class[] parameters,
                           Class ret, Class[] exceptions)
        throws Exception
    {
        Label start = null;
        Label end = null;
        
        String jcallName = "_jcall";
        int instLocal = 0;

        if (exceptions.length > 0) {
            start = new Label();
            end = new Label();
            jcallName = "_jcallexc";
            instLocal = mv.getLocal("org/python/core/PyObject");
            mv.visitVarInsn(ASTORE, instLocal);
            mv.visitLabel(start);
            mv.visitVarInsn(ALOAD, instLocal);
        }

        getArgs(mv, parameters);

        switch (getType(ret)) {
        case tCharacter:
            doJavaCall(mv, "char", "C", jcallName);
            break;
        case tBoolean:
            doJavaCall(mv, "boolean", "Z", jcallName);
            break;
        case tByte:
        case tShort:
        case tInteger:
            doJavaCall(mv, "int", "I", jcallName);
            break;
        case tLong:
            doJavaCall(mv, "long", "J", jcallName);
            break;
        case tFloat:
            doJavaCall(mv, "float", "F", jcallName);
            break;
        case tDouble:
            doJavaCall(mv, "double", "D", jcallName);
            break;
        case tVoid:
            doJavaCall(mv, "void", "V", jcallName);
            break;
        default:
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", jcallName, "(" + $objArr + ")" + $pyObj);
            mv.visitLdcInsn(ret.getName());
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class","forName", "(" + $str + ")" + $clss);
            mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "tojava", "(" + $pyObj + $clss + ")" + $obj);
            // I guess I need this checkcast to keep the verifier happy
            mv.visitTypeInsn(CHECKCAST,mapClass(ret));
            break;
        }
        if (exceptions.length > 0)
            mv.visitLabel(end);

        doReturn(mv, ret);

        if (exceptions.length > 0) {
            boolean throwableFound = false;

            Label handlerStart = null;
            for (int i = 0; i < exceptions.length; i++) {
                handlerStart = new Label();
                mv.visitLabel(handlerStart);
                int excLocal = mv.getLocal("java/lang/Throwable");
                mv.visitVarInsn(ASTORE, excLocal);

                mv.visitVarInsn(ALOAD, excLocal);
                mv.visitInsn(ATHROW);

                mv.visitTryCatchBlock(start, end, handlerStart, mapClass(exceptions[i]));
                doNullReturn(mv, ret);

                mv.freeLocal(excLocal);
                if (exceptions[i] == Throwable.class)
                    throwableFound = true;
            }

            if (!throwableFound) {
                // The final catch (Throwable)
                handlerStart = new Label();
                mv.visitLabel(handlerStart);
                int excLocal = mv.getLocal("java/lang/Throwable");
                mv.visitVarInsn(ASTORE, excLocal);
                mv.visitVarInsn(ALOAD, instLocal);
                mv.visitVarInsn(ALOAD, excLocal);

                mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "_jthrow", "(" + $throwable + ")V");
                mv.visitTryCatchBlock(start, end, handlerStart, "java/lang/Throwable");

                mv.freeLocal(excLocal);
                doNullReturn(mv, ret);
            }
            mv.freeLocal(instLocal);
        }
    }


    public void addMethod(Method method, int access) throws Exception {
        boolean isAbstract = false;

        if (Modifier.isAbstract(access)) {
            access = access & ~Modifier.ABSTRACT;
            isAbstract = true;
        }

        Class[] parameters = method.getParameterTypes();
        Class ret = method.getReturnType();
        String sig = makeSignature(parameters, ret);

        String name = method.getName();
        names.put(name, name);

        Code mv = classfile.addMethod(name, sig, access);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(name);

        if (!isAbstract) {
            int tmp = mv.getLocal("org/python/core/PyObject");
            mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "jfindattr", "(" + $pyProxy + $str + ")" + $pyObj);
            mv.visitVarInsn(ASTORE, tmp);
            mv.visitVarInsn(ALOAD, tmp);

            Label callPython = new Label();
            mv.visitJumpInsn(IFNONNULL, callPython);

            String superclass = mapClass(method.getDeclaringClass());

            callSuper(mv, name, superclass, parameters, ret, sig);
            mv.visitLabel(callPython);
            mv.visitVarInsn(ALOAD, tmp);
            callMethod(mv, name, parameters, ret,
                       method.getExceptionTypes());

            addSuperMethod("super__"+name, name, superclass, parameters,
                           ret, sig, access);
        }
        else {
            if (!isAdapter) {
                mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "jgetattr", "(" + $pyProxy + $str + ")" + $pyObj);
                callMethod(mv, name, parameters, ret,
                           method.getExceptionTypes());
            }
            else {
                mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "jfindattr", "(" + $pyProxy + $str + ")" + $pyObj);
                mv.visitInsn(DUP);
                Label returnNull = new Label();
                mv.visitJumpInsn(IFNULL, returnNull);

                callMethod(mv, name, parameters, ret,
                           method.getExceptionTypes());
                mv.visitLabel(returnNull);
	        mv.visitInsn(POP);
                doNullReturn(mv, ret);
            }
        }
    }

    private String methodString(Method m) {
        StringBuffer buf = new StringBuffer(m.getName());
        buf.append(":");
        Class[] params = m.getParameterTypes();
        for (int i=0; i<params.length; i++) {
            buf.append(params[i].getName());
            buf.append(",");
        }
        return buf.toString();
    }

    protected void addMethods(Class c, Hashtable t) throws Exception {
        Method[] methods = c.getDeclaredMethods();
        for (int i=0; i<methods.length; i++) {
            Method method = methods[i];
            String s = methodString(method);
            if (t.containsKey(s))
                continue;
            t.put(s, s);

            int access = method.getModifiers();
            if (Modifier.isStatic(access) || Modifier.isPrivate(access)) {
                continue;
            }

            if (Modifier.isNative(access)) {
                access = access & ~Modifier.NATIVE;
            }

            if (Modifier.isProtected(access)) {
                access = (access & ~Modifier.PROTECTED) | Modifier.PUBLIC;
                if (Modifier.isFinal(access)) {
                    addSuperMethod(methods[i], access);
                    continue;
                }
            }
            else if (Modifier.isFinal(access)) {
                continue;
            }
            addMethod(methods[i], access);
        }

        Class sc = c.getSuperclass();
        if (sc != null)
            addMethods(sc, t);

        Class[] interfaces = c.getInterfaces();
        for (int j=0; j<interfaces.length; j++) {
            addMethods(interfaces[j], t);
        }
    }

    public void addConstructor(String name, Class[] parameters, Class ret,
                               String sig, int access)
        throws Exception
    {
        MethodVisitor mv = classfile.addMethod("<init>", sig, access);
        callSuper(mv, "<init>", name, parameters, Void.TYPE, sig);
    }

    public void addConstructors(Class c) throws Exception {
        Constructor[] constructors = c.getDeclaredConstructors();
        String name = mapClass(c);
        for (int i=0; i<constructors.length; i++) {
            int access = constructors[i].getModifiers();
            if (Modifier.isPrivate(access))
                continue;
            if (Modifier.isNative(access))
                access = access & ~Modifier.NATIVE;
            if (Modifier.isProtected(access))
                access = access & ~Modifier.PROTECTED | Modifier.PUBLIC;
            Class[] parameters = constructors[i].getParameterTypes();
            String sig = makeSignature(parameters, Void.TYPE);
            addConstructor(name, parameters, Void.TYPE, sig, access);
        }
    }

    // Super methods are added for the following three reasons:
    //
    //   1) for a protected non-final method add a public method with no
    //   super__ prefix.  This gives needed access to this method for
    //   subclasses
    //
    //   2) for protected final methods, add a public method with the
    //   super__ prefix.  This avoids the danger of trying to override a
    //   final method
    //
    //   3) For any other method that is overriden, add a method with the
    //   super__ prefix.  This gives access to super. version or the
    //   method.
    //
    public void addSuperMethod(Method method, int access) throws Exception {
        Class[] parameters = method.getParameterTypes();
        Class ret = method.getReturnType();
        String sig = makeSignature(parameters, ret);
        String superclass = mapClass(method.getDeclaringClass());
        String superName = method.getName();
        String methodName = superName;
        if (Modifier.isFinal(access)) {
            methodName = "super__"+superName;
            access &= ~Modifier.FINAL;
        }
        addSuperMethod(methodName, superName, superclass, parameters,
                       ret, sig, access);
    }

    public void addSuperMethod(String methodName, String superName,
                               String declClass, Class[] parameters,
                               Class ret, String sig, int access)
        throws Exception
    {
        if (methodName.startsWith("super__")) {
            /* rationale: JC java-class, P proxy-class subclassing JC
               in order to avoid infinite recursion P should define super__foo
               only if no class between P and JC in the hierarchy defines
               it yet; this means that the python class needing P is the
               first that redefines the JC method foo.
            */
            try {
                superclass.getMethod(methodName,parameters);
                return;
            } catch(NoSuchMethodException e) {
            } catch(SecurityException e) {
                return;
            }
        }
        supernames.put(methodName, methodName);
        MethodVisitor mv = classfile.addMethod(methodName, sig, access);
        callSuper(mv, superName, declClass, parameters, ret, sig);
    }

    public void addProxy() throws Exception {
        // implement PyProxy interface
        classfile.addField("__proxy", "Lorg/python/core/PyInstance;",
                           Modifier.PROTECTED);
        // setProxy method
        MethodVisitor mv = classfile.addMethod("_setPyInstance",
                                        "(Lorg/python/core/PyInstance;)V",
                                        Modifier.PUBLIC);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, classfile.name, "__proxy", "Lorg/python/core/PyInstance;");
        mv.visitInsn(RETURN);

        // getProxy method
        mv = classfile.addMethod("_getPyInstance",
                                   "()Lorg/python/core/PyInstance;",
                                   Modifier.PUBLIC);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, classfile.name, "__proxy", "Lorg/python/core/PyInstance;");
        mv.visitInsn(ARETURN);

        // implement PyProxy interface
        classfile.addField("__systemState",
                           "Lorg/python/core/PySystemState;",
                           Modifier.PROTECTED | Modifier.TRANSIENT);

        // setProxy method
        mv = classfile.addMethod("_setPySystemState",
                                   "(Lorg/python/core/PySystemState;)V",
                                   Modifier.PUBLIC);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, classfile.name, "__systemState", "Lorg/python/core/PySystemState;");
        mv.visitInsn(RETURN);

        // getProxy method
        mv = classfile.addMethod("_getPySystemState",
                                   "()Lorg/python/core/PySystemState;",
                                   Modifier.PUBLIC);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, classfile.name, "__systemState", "Lorg/python/core/PySystemState;");
        mv.visitInsn(ARETURN);
    }

    public void addClassDictInit() throws Exception {
        int n = supernames.size();

        // classDictInit method
        classfile.addInterface(mapClass(org.python.core.ClassDictInit.class));
        Code mv = classfile.addMethod("classDictInit",
                                   "(" + $pyObj + ")V",
                                   Modifier.PUBLIC | Modifier.STATIC);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn("__supernames__");

        String[] names = new String[n];
        Enumeration e = supernames.keys();
        for (int i = 0; e.hasMoreElements(); )
           names[i++] = (String) e.nextElement();
        CodeCompiler.makeStrings(mv, names, n);
        mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "java2py", "(" + $obj + ")" + $pyObj);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/python/core/PyObject", "__setitem__", "(" + $str + $pyObj + ")V");
        mv.visitInsn(RETURN);
    }

    public void build() throws Exception {
        names = new Hashtable();
        int access = superclass.getModifiers();
        if ((access & Modifier.FINAL) != 0) {
            throw new InstantiationException("can't subclass final class");
        }
        access = Modifier.PUBLIC | Modifier.SYNCHRONIZED;

        classfile = new ClassFile(myClass, mapClass(superclass), access);
        addProxy();
        addConstructors(superclass);
        classfile.addInterface("org/python/core/PyProxy");

        Hashtable seenmethods = new Hashtable();
        addMethods(superclass, seenmethods);
        for (int i=0; i<interfaces.length; i++) {
            if (interfaces[i].isAssignableFrom(superclass)) {
                Py.writeWarning("compiler",
                                "discarding redundant interface: "+
                                interfaces[i].getName());
                continue;
            }
            classfile.addInterface(mapClass(interfaces[i]));
            addMethods(interfaces[i], seenmethods);
        }
        doConstants();
        addClassDictInit();
    }

    public static String makeProxy(Class superclass, OutputStream ostream)
        throws Exception
    {
        ProxyMaker pm = new ProxyMaker(superclass.getName(), superclass);
        pm.build();
        pm.classfile.write(ostream);
        return pm.myClass;
    }

    public static File makeFilename(String name, File dir) {
        int index = name.indexOf(".");
        if (index == -1)
            return new File(dir, name+".class");

        return makeFilename(name.substring(index+1, name.length()),
                            new File(dir, name.substring(0, index)));
    }

    // This is not general enough
    public static OutputStream getFile(String d, String name)
        throws IOException
    {
        File dir = new File(d);
        File file = makeFilename(name, dir);
        new File(file.getParent()).mkdirs();
        //System.out.println("proxy file: "+file);
        return new FileOutputStream(file);
    }
}
