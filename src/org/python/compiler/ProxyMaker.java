// Copyright (c) Corporation for National Research Initiatives
package org.python.compiler;

import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.util.Generic;

import static org.python.util.CodegenUtils.p;
import static org.python.util.CodegenUtils.sig;


public class ProxyMaker extends ProxyCodeHelpers implements ClassConstants, Opcodes
{
    protected final Class<?> superclass;
    protected final Class<?>[] interfaces;
    Set<String> names;
    Set<String> supernames = Generic.set();
    Set<String> namesAndSigs; // name+signature pairs
    public ClassFile classfile;
    /** The name of the class to build. */
    public String myClass;

    /**
     * Creates a proxy class maker that produces classes named
     * <code>org.python.proxies.(superclassName)</code> with <code>superclass</code> as an
     * implemented interface or extended class, depending on the its type.
     *
     * @deprecated - Use {@link ProxyMaker#ProxyMaker(String, Class, Class[])

     */
    @Deprecated
    public ProxyMaker(String superclassName, Class<?> superclass) {
        this("org.python.proxies." + superclassName,
            superclass.isInterface() ? Object.class : superclass,
            superclass.isInterface() ? new Class<?>[] { superclass} : new Class<?>[0]);

    }

    /**
     * Creates a proxy class maker that produces classes named <code>proxyClassName</code> that
     * extends <code>superclass</code> and implements the interfaces in <code>interfaces</code>.
     */
    public ProxyMaker(String proxyClassName, Class<?> superclass, Class<?>... interfaces) {
        this.myClass = proxyClassName;
        if (superclass == null) {
            superclass = Object.class;
        }
        if (superclass.isInterface()) {
            throw new IllegalArgumentException("Given an interface,  " + superclass.getName()
                    + ", for a proxy superclass");
        }
        this.superclass = superclass;
        if (interfaces == null) {
            interfaces = new Class[0];
        }
        for (Class<?> interfac : interfaces) {
            if (!interfac.isInterface()) {
                throw new IllegalArgumentException(
                    "All classes in the interfaces array must be interfaces, unlike "
                            + interfac.getName());
            }
        }
        this.interfaces = interfaces;
    }

    public void doConstants() throws Exception {
        Code code = classfile.addMethod("<clinit>", makeSig("V"), Modifier.STATIC);
        code.return_();
    }

    public void callSuper(Code code,
                          String name,
                          String superclass,
                          Class<?>[] parameters,
                          Class<?> ret,
                          boolean doReturn) throws Exception {

        code.aload(0);
        int local_index;
        int i;
        for (i=0, local_index=1; i<parameters.length; i++) {
            switch(getType(parameters[i])) {
            case tCharacter:
            case tBoolean:
            case tByte:
            case tShort:
            case tInteger:
                code.iload(local_index);
                local_index += 1;
                break;
            case tLong:
                code.lload(local_index);
                local_index += 2;
                break;
            case tFloat:
                code.fload(local_index);
                local_index += 1;
                break;
            case tDouble:
                code.dload(local_index);
                local_index += 2;
                break;
            default:
                code.aload(local_index);
                local_index += 1;
                break;
            }
        }
        code.invokespecial(superclass, name, makeSig(ret, parameters));

        if (doReturn) {
            doReturn(code, ret);
        }
    }

    public void doJavaCall(Code code, String name, String type,
                          String jcallName)
        throws Exception
    {
        code.invokevirtual("org/python/core/PyObject", jcallName, makeSig($pyObj, $objArr));
        code.invokestatic("org/python/core/Py", "py2"+name, makeSig(type, $pyObj));
    }


    public void getArgs(Code code, Class<?>[] parameters) throws Exception {
        if (parameters.length == 0) {
            code.getstatic("org/python/core/Py", "EmptyObjects", $pyObjArr);
        } else {
            code.iconst(parameters.length);
            code.anewarray("java/lang/Object");

            int array = code.getLocal("[org/python/core/PyObject");
            code.astore(array);

            int local_index;
            int i;
            for (i=0, local_index=1; i<parameters.length; i++) {
                code.aload(array);
                code.iconst(i);

                switch (getType(parameters[i])) {
                case tBoolean:
                case tByte:
                case tShort:
                case tInteger:
                    code.iload(local_index);
                    local_index += 1;
                    code.invokestatic("org/python/core/Py", "newInteger", "(I)" + $pyInteger);
                    break;
                case tLong:
                    code.lload(local_index);
                    local_index += 2;
                    code.invokestatic("org/python/core/Py", "newInteger", "(J)" + $pyObj);
                    break;
                case tFloat:
                    code.fload(local_index);
                    local_index += 1;
                    code.invokestatic("org/python/core/Py", "newFloat", "(F)" + $pyFloat);
                    break;
                case tDouble:
                    code.dload(local_index);
                    local_index += 2;
                    code.invokestatic("org/python/core/Py", "newFloat", "(D)" + $pyFloat);
                    break;
                case tCharacter:
                    code.iload(local_index);
                    local_index += 1;
                    code.invokestatic("org/python/core/Py", "newString", "(C)" + $pyStr);
                    break;
                default:
                    code.aload(local_index);
                    local_index += 1;
                    break;
                }
                code.aastore();
            }
            code.aload(array);
        }
    }

    public void callMethod(Code code,
                           String name,
                           Class<?>[] parameters,
                           Class<?> ret,
                           Class<?>[] exceptions) throws Exception {
        Label start = null;
        Label end = null;

        String jcallName = "_jcall";
        int instLocal = 0;

        if (exceptions.length > 0) {
            start = new Label();
            end = new Label();
            jcallName = "_jcallexc";
            instLocal = code.getLocal("org/python/core/PyObject");
            code.astore(instLocal);
            code.label(start);
            code.aload(instLocal);
        }

        getArgs(code, parameters);

        switch (getType(ret)) {
        case tCharacter:
            doJavaCall(code, "char", "C", jcallName);
            break;
        case tBoolean:
            doJavaCall(code, "boolean", "Z", jcallName);
            break;
        case tByte:
        case tShort:
        case tInteger:
            doJavaCall(code, "int", "I", jcallName);
            break;
        case tLong:
            doJavaCall(code, "long", "J", jcallName);
            break;
        case tFloat:
            doJavaCall(code, "float", "F", jcallName);
            break;
        case tDouble:
            doJavaCall(code, "double", "D", jcallName);
            break;
        case tVoid:
            doJavaCall(code, "void", "V", jcallName);
            break;
        default:
            code.invokevirtual("org/python/core/PyObject", jcallName, makeSig($pyObj, $objArr));
            code.ldc(ret.getName());
            code.invokestatic("java/lang/Class","forName", makeSig($clss, $str));
            code.invokestatic("org/python/core/Py", "tojava", makeSig($obj, $pyObj, $clss));
            // I guess I need this checkcast to keep the verifier happy
            code.checkcast(mapClass(ret));
            break;
        }
        if (end != null) {
            code.label(end);
        }

        doReturn(code, ret);

        if (exceptions.length > 0) {
            boolean throwableFound = false;

            Label handlerStart = null;
            for (Class<?> exception : exceptions) {
                handlerStart = new Label();
                code.label(handlerStart);
                int excLocal = code.getLocal("java/lang/Throwable");
                code.astore(excLocal);

                code.aload(excLocal);
                code.athrow();

                code.visitTryCatchBlock(start, end, handlerStart, mapClass(exception));
                doNullReturn(code, ret);

                code.freeLocal(excLocal);
                if (exception == Throwable.class)
                    throwableFound = true;
            }

            if (!throwableFound) {
                // The final catch (Throwable)
                handlerStart = new Label();
                code.label(handlerStart);
                int excLocal = code.getLocal("java/lang/Throwable");
                code.astore(excLocal);
                code.aload(instLocal);
                code.aload(excLocal);

                code.invokevirtual("org/python/core/PyObject", "_jthrow", makeSig("V", $throwable));
                code.visitTryCatchBlock(start, end, handlerStart, "java/lang/Throwable");

                code.freeLocal(excLocal);
                doNullReturn(code, ret);
            }
            code.freeLocal(instLocal);
        }
    }


    public void addMethod(Method method, int access) throws Exception {
        addMethod(method.getName(), method.getReturnType(), method.getParameterTypes(),
                method.getExceptionTypes(), access, method.getDeclaringClass());
    }
    
    /**
     * Adds a method of the given name to the class being implemented. If
     * <code>declaringClass</code> is null, the generated method will expect to find an object of
     * the method's name in the Python object and call it. If it isn't null, if an object is found
     * in the Python object, it'll be called. Otherwise the superclass will be called. No checking
     * is done to guarantee that the superclass has a method with the same signature.
     */
    public void addMethod(String name,
            Class<?> ret,
            Class<?>[] parameters,
            Class<?>[] exceptions,
            int access,
            Class<?> declaringClass) throws Exception {
        addMethod(name, name, ret, parameters, exceptions, access, declaringClass, null, null);
    }

    
    /**
     * Generates and adds a proxy method to the proxy class
     * 
     * @param name: name of the java method
     * @param pyName: name of the python method to which the java method 
     * proxies (useful for clamped objects)
     * 
     * @param ret: return type
     * @param parameters: parameter types
     * @param exceptions: throwable exception types
     * @param access
     * @param declaringClass
     * @param methodAnnotations: method annotations
     * @param parameterAnnotations: parameter annotations
     * @throws Exception
     */
    public void addMethod(String name,
            String pyName,
            Class<?> ret,
            Class<?>[] parameters,
            Class<?>[] exceptions,
            int access,
            Class<?> declaringClass,
            AnnotationDescr[] methodAnnotations,
            AnnotationDescr[][]parameterAnnotations) throws Exception {
        boolean isAbstract = false;
        
        if (Modifier.isAbstract(access)) {
            access = access & ~Modifier.ABSTRACT;
            isAbstract = true;
        }

        String sig = makeSig(ret, parameters);
        String[] exceptionTypes = mapExceptions(exceptions);

        names.add(name);

        Code code = null;
        if (methodAnnotations != null && parameterAnnotations != null) {
            code = classfile.addMethod(name, sig, access, exceptionTypes, methodAnnotations, parameterAnnotations);
        } else {
            code = classfile.addMethod(name, sig, access, exceptionTypes);
        }

        code.aload(0);
        code.ldc(pyName);

        if (!isAbstract) {
            int tmp = code.getLocal("org/python/core/PyObject");
            code.invokestatic("org/python/compiler/ProxyMaker", "findPython",
                makeSig($pyObj, $pyProxy, $str));
            code.astore(tmp);
            code.aload(tmp);

            Label callPython = new Label();
            code.ifnonnull(callPython);

            String superClass = mapClass(declaringClass);

            callSuper(code, name, superClass, parameters, ret, true);
            code.label(callPython);
            code.aload(tmp);
            callMethod(code, name, parameters, ret, exceptions);

            addSuperMethod("super__"+name, name, superClass, parameters,
                           ret, sig, access);
        } else {
            code.invokestatic("org/python/compiler/ProxyMaker", "findPython",
                makeSig($pyObj, $pyProxy, $str));
            code.dup();
            Label returnNull = new Label();
            code.ifnull(returnNull);
            callMethod(code, name, parameters, ret, exceptions);
            code.label(returnNull);
            code.pop();

            // throw an exception if we cannot load a Python method for this abstract method
            // note that the unreachable return is simply present to simplify bytecode gen
            code.aload(0);
            code.ldc(pyName);
            code.ldc(declaringClass.getName());
            code.invokestatic("org/python/compiler/ProxyCodeHelpers", "notImplementedAbstractMethod",
                    makeSig($pyExc, $pyProxy, $str, $str));
            code.checkcast(p(Throwable.class));
            code.athrow();

            doNullReturn(code, ret);
        }
    }
    
    /**
     * A constructor that is also a method (!)
     */
    public void addConstructorMethodCode(String pyName,
            Class<?>[] parameters,
            Class<?>[] exceptions,
            int access,
            Class<?> declaringClass,
            Code code) throws Exception {
        code.aload(0);
        code.ldc(pyName);
        
        int tmp = code.getLocal("org/python/core/PyObject");
        code.invokestatic("org/python/compiler/ProxyMaker", "findPython",
            makeSig($pyObj, $pyProxy, $str));
        code.astore(tmp);
        code.aload(tmp);

        callMethod(code, "<init>", parameters, Void.TYPE, exceptions);
    }
    
    private String methodString(Method m) {
        StringBuffer buf = new StringBuffer(m.getName());
        buf.append(":");
        Class<?>[] params = m.getParameterTypes();
        for (Class<?> param : params) {
            buf.append(param.getName());
            buf.append(",");
        }
        return buf.toString();
    }

    protected void addMethods(Class<?> c, Set<String> t) throws Exception {
        Method[] methods = c.getDeclaredMethods();
        for (Method method : methods) {
            if (!t.add(methodString(method))) {
                continue;
            }

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
                    addSuperMethod(method, access);
                    continue;
                }
            } else if (Modifier.isFinal(access)) {
                continue;
            } else if (!Modifier.isPublic(access)) {
                continue; // package protected by process of elimination; we can't override
            }
            addMethod(method, access);
        }

        Class<?> sc = c.getSuperclass();
        if (sc != null) {
            addMethods(sc, t);
        }

        for (Class<?> iface : c.getInterfaces()) {
            addMethods(iface, t);
        }
    }

    public void addConstructor(String name,
                               Class<?>[] parameters,
                               Class<?> ret,
                               String sig,
                               int access) throws Exception {
        Code code = classfile.addMethod("<init>", sig, access);
        callSuper(code, "<init>", name, parameters, Void.TYPE, true);
    }

    public void addConstructors(Class<?> c) throws Exception {
        Constructor<?>[] constructors = c.getDeclaredConstructors();
        String name = mapClass(c);
        for (Constructor<?> constructor : constructors) {
            int access = constructor.getModifiers();
            if (Modifier.isPrivate(access)) {
                continue;
            }
            if (Modifier.isNative(access)) {
                access = access & ~Modifier.NATIVE;
            }
            if (Modifier.isProtected(access)) {
                access = access & ~Modifier.PROTECTED | Modifier.PUBLIC;
            }
            Class<?>[] parameters = constructor.getParameterTypes();
            addConstructor(name, parameters, Void.TYPE, makeSig(Void.TYPE, parameters), access);
        }
    }
    
    protected void addClassAnnotation(AnnotationDescr annotation) {
        classfile.addClassAnnotation(annotation);
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
    //   3) For any other method that is overridden, add a method with the
    //   super__ prefix.  This gives access to super. version or the
    //   method.
    //
    public void addSuperMethod(Method method, int access) throws Exception {
        Class<?>[] parameters = method.getParameterTypes();
        Class<?> ret = method.getReturnType();
        String superClass = mapClass(method.getDeclaringClass());
        String superName = method.getName();
        String methodName = superName;
        if (Modifier.isFinal(access)) {
            methodName = "super__" + superName;
            access &= ~Modifier.FINAL;
        }
        addSuperMethod(methodName, superName, superClass, parameters,
                       ret, makeSig(ret, parameters), access);
    }

    public void addSuperMethod(String methodName,
                               String superName,
                               String declClass,
                               Class<?>[] parameters,
                               Class<?> ret,
                               String sig,
                               int access) throws Exception {
        if (methodName.startsWith("super__")) {
            /* rationale: JC java-class, P proxy-class subclassing JC
               in order to avoid infinite recursion P should define super__foo
               only if no class between P and JC in the hierarchy defines
               it yet; this means that the python class needing P is the
               first that redefines the JC method foo.
            */
            try {
                superclass.getMethod(methodName, parameters);
                return;
            } catch (NoSuchMethodException e) {
                // OK, no one else defines it, so we need to
            } catch (SecurityException e) {
                return;
            }
        }
        supernames.add(methodName);
        Code code = classfile.addMethod(methodName, sig, access);
        callSuper(code, superName, declClass, parameters, ret, true);
    }

    public void addProxy() throws Exception {
        // implement PyProxy interface
        classfile.addField("__proxy", $pyObj, Modifier.PROTECTED);
        // setProxy methods
        Code code = classfile.addMethod("_setPyInstance", makeSig("V", $pyObj), Modifier.PUBLIC);
        code.aload(0);
        code.aload(1);
        code.putfield(classfile.name, "__proxy", $pyObj);
        code.return_();

        // getProxy method
        code = classfile.addMethod("_getPyInstance", makeSig($pyObj), Modifier.PUBLIC);
        code.aload(0);
        code.getfield(classfile.name, "__proxy", $pyObj);
        code.areturn();

        String pySys =  "Lorg/python/core/PySystemState;";
        // implement PyProxy interface
        classfile.addField("__systemState", pySys, Modifier.PROTECTED | Modifier.TRANSIENT);

        // setProxy method
        code = classfile.addMethod("_setPySystemState",
                                   makeSig("V", pySys),
                                   Modifier.PUBLIC);

        code.aload(0);
        code.aload(1);
        code.putfield(classfile.name, "__systemState", pySys);
        code.return_();

        // getProxy method
        code = classfile.addMethod("_getPySystemState", makeSig(pySys), Modifier.PUBLIC);
        code.aload(0);
        code.getfield(classfile.name, "__systemState", pySys);
        code.areturn();
    }

    public void addClassDictInit() throws Exception {
        // classDictInit method
        classfile.addInterface(mapClass(org.python.core.ClassDictInit.class));
        Code code = classfile.addMethod("classDictInit", makeSig("V", $pyObj),
            Modifier.PUBLIC | Modifier.STATIC);
        code.aload(0);
        code.ldc("__supernames__");

        int strArray = CodeCompiler.makeStrings(code, supernames);
        code.aload(strArray);
        code.freeLocal(strArray);
        code.invokestatic("org/python/core/Py", "java2py", makeSig($pyObj, $obj));
        code.invokevirtual("org/python/core/PyObject", "__setitem__", makeSig("V", $str, $pyObj));
        code.return_();
    }

    /**
     * Builds this proxy and writes its bytecode to <code>out</code>.
     */
    public void build(OutputStream out) throws Exception {
        build();
        classfile.write(out);
    }

    public void build() throws Exception {
        names = Generic.set();
        namesAndSigs = Generic.set();
        int access = superclass.getModifiers();
        if ((access & Modifier.FINAL) != 0) {
            throw new InstantiationException("can't subclass final class");
        }
        access = Modifier.PUBLIC | Modifier.SYNCHRONIZED;

        classfile = new ClassFile(myClass, mapClass(superclass), access);
        addProxy();
        visitConstructors();
        classfile.addInterface("org/python/core/PyProxy");
        
        visitClassAnnotations();
        visitMethods();
        doConstants();
        addClassDictInit();
    }
    
    /**
     * Visits all methods declared on the given class and classes in its inheritance hierarchy.
     * Methods visible to subclasses are added to <code>seen</code>.
     */
    protected void visitMethods(Class<?> klass) throws Exception {
        for (Method method : klass.getDeclaredMethods()) {
        	
            
            // make sure we have only one name + signature pair available per method
            if (!namesAndSigs.add(methodString(method))) {
            	continue;
            }

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
            		addSuperMethod(method, access);
            		continue;
            	}
            } else if (Modifier.isFinal(access)) {
            	continue;
            } else if (!Modifier.isPublic(access)) {
            	continue; // package protected by process of elimination; we can't override
            }
            addMethod(method, access);
        }

        Class<?> superClass = klass.getSuperclass();
        if (superClass != null) {
            visitMethods(superClass);
        }

        for (Class<?> iface : klass.getInterfaces()) {
            visitMethods(iface);
        }
    }

    /**
     * Called for every method on the proxy's superclass and interfaces that can be overriden by the
     * proxy class. If the proxy wants to perform Python lookup and calling for the method,
     * {@link #addMethod(Method)} should be called. For abstract methods, addMethod must be called.
     */
    protected void visitMethod(Method method) throws Exception {
        addMethod(method, method.getModifiers());
    }

    protected void visitMethods() throws Exception {
        visitMethods(superclass);
        for (Class<?> iface : interfaces) {
            if (iface.isAssignableFrom(superclass)) {
                Py.writeWarning("compiler", "discarding redundant interface: " + iface.getName());
                continue;
            }
            classfile.addInterface(mapClass(iface));
            visitMethods(iface);
        }
    }
     
    /** Adds a constructor that calls through to superclass. */
    protected void addConstructor(Class<?>[] parameters, int access) throws Exception {
        String sig = makeSig(Void.TYPE, parameters);
        Code code = classfile.addMethod("<init>", sig, access);
        callSuper(code, "<init>", mapClass(superclass), parameters, Void.TYPE, true);
    }
    
    /**
     * Called for every constructor on the proxy's superclass that can be overridden by
     * the proxy class.
     */
    protected void visitConstructor(Constructor<?> constructor) throws Exception {
        /* Need a fancy constructor for the Java side of things */
        callInitProxy(constructor.getParameterTypes(), addOpenConstructor(constructor));
    }

    /**
     * Adds a constructor that calls through to the superclass constructor with the same signature
     * and leaves the returned Code open for more operations. The caller of this method must add a
     * return to the Code.
     */
    protected Code addOpenConstructor(Constructor<?> constructor) throws Exception {
        String sig = makeSig(Void.TYPE, constructor.getParameterTypes());
        Code code = classfile.addMethod("<init>", sig, constructor.getModifiers());
        callSuper(code, "<init>", mapClass(superclass), constructor.getParameterTypes(), Void.TYPE, true);
        return code;
    }

    /**
     * Calls __initProxy__ on this class with the given types of parameters, which must be
     * available as arguments to the currently called method in the order of the parameters.
     */
    protected void callInitProxy(Class<?>[] parameters, Code code) throws Exception {
        code.visitVarInsn(ALOAD, 0);
        getArgs(code, parameters);
        code.visitMethodInsn(INVOKEVIRTUAL, classfile.name, "__initProxy__", makeSig("V", $objArr), false);
        code.visitInsn(RETURN);
    }
    
    /**
     * Visits constructors from this proxy's superclass.
     */
    protected void visitConstructors() throws Exception {
        addConstructors(superclass);
    }
    
    protected void visitClassAnnotations() throws Exception {
        // ProxyMaker itself does nothing with class annotations for now
    }
    
}
