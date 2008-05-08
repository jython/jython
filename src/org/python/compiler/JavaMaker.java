// Copyright (c) Corporation for National Research Initiatives

package org.python.compiler;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.python.objectweb.asm.MethodVisitor;
import org.python.objectweb.asm.Opcodes;
import org.python.core.PyObject;

public class JavaMaker extends ProxyMaker implements ClassConstants
{
    public String pythonClass, pythonModule;
    public String[] properties;
    public String[] packages;
    //Hashtable methods;
    PyObject methods;
    public boolean frozen, main;

    public JavaMaker(Class superclass, Class[] interfaces,
                     String pythonClass, String pythonModule, String myClass,
                     PyObject methods)
    {
        this(superclass, interfaces, pythonClass, pythonModule, myClass,
             null, null, methods, false, false);
    }

    public JavaMaker(Class superclass, Class[] interfaces,
                     String pythonClass, String pythonModule, String myClass,
                     String[] packages, String[] properties,
                     PyObject methods,
                     boolean frozen, boolean main)
    {
        super(myClass, superclass, interfaces);
//         System.out.println("props: "+properties+", "+properties.length);
        this.pythonClass = pythonClass;
        this.pythonModule = pythonModule;
        this.packages = packages;
        this.properties = properties;
        this.frozen = frozen;
        this.main = main;
        this.methods = methods;
    }

    private void makeStrings(Code mv, String[] list) throws Exception {
        if (list != null) {
            int n = list.length;
            mv.iconst(n);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/String");

            int strings = mv.getLocal("[java/lang/String");
            mv.visitVarInsn(ASTORE, strings);
            for(int i=0; i<n; i++) {
                mv.visitVarInsn(ALOAD, strings);
                mv.iconst(i);
                mv.visitLdcInsn(list[i]);
                mv.visitInsn(AASTORE);
            }
            mv.visitVarInsn(ALOAD, strings);
            mv.freeLocal(strings);
        } else {
            mv.visitInsn(ACONST_NULL);
        }
    }

    public void addConstructor(String name, Class[] parameters, Class ret,
                               String sig, int access)
        throws Exception
    {
        /* Need a fancy constructor for the Java side of things */
        Code mv = classfile.addMethod("<init>", sig, access);
        callSuper(mv, "<init>", name, parameters, null, sig);
        mv.visitVarInsn(ALOAD, 0);
        getArgs(mv, parameters);

        mv.visitMethodInsn(INVOKEVIRTUAL, classfile.name, "__initProxy__", "([Ljava/lang/Object;)V");
        mv.visitInsn(RETURN);
    }

    public void addProxy() throws Exception {
        if (methods != null)
            super.addProxy();

        // _initProxy method
       Code mv = classfile.addMethod("__initProxy__",
                        "([Ljava/lang/Object;)V", Modifier.PUBLIC);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(pythonModule);
        mv.visitLdcInsn(pythonClass);
        
        mv.visitVarInsn(ALOAD, 1);

        makeStrings(mv, packages);
        makeStrings(mv, properties);

        mv.iconst(frozen ? 1 : 0);

        mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "initProxy", "(" + $pyProxy + $str + $str + $objArr + $strArr + $strArr + "Z)V");
        mv.visitInsn(RETURN);

        if (main)
            addMain();
    }

//     public void addMethods(Class c) throws Exception {
//         if (methods != null) {
//             super.addMethods(c);
//         }
//     }

    public void addMethod(Method method, int access) throws Exception {
        //System.out.println("add: "+method.getName()+", "+
        //                   methods.containsKey(method.getName()));
        // Check to see if it's an abstract method
        if (Modifier.isAbstract(access)) {
            // Maybe throw an exception here???
            super.addMethod(method, access);
        } else if (methods.__finditem__(method.getName().intern()) != null) {
            super.addMethod(method, access);
        } else if (Modifier.isProtected(method.getModifiers())) {
            addSuperMethod(method, access);
        }
    }

/*
    public void addSuperMethod(String methodName, String superName,
                               String superclass, Class[] parameters,
                               Class ret, String sig, int access)
        throws Exception
    {
        if (!PyProxy.class.isAssignableFrom(this.superclass)) {
            super.addSuperMethod(methodName,superName,superclass,parameters,
                                 ret,sig,access);
        }
    }

*/

    public void addMain() throws Exception {
        Code mv = classfile.addMethod("main", "(" + $str + ")V",
                                        Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);

        // Load the class of the Python module to run
        mv.visitLdcInsn(pythonModule);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class","forName", "(" + $str + ")" + $clss);

        // Load in any command line arguments
        mv.visitVarInsn(ALOAD, 0);
        makeStrings(mv, packages);
        makeStrings(mv, properties);
        mv.iconst(frozen ? 1 : 0);

        mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "runMain", "(" + $clss + $strArr + $strArr + $strArr + "Z)V");
        mv.visitInsn(RETURN);
    }
}
