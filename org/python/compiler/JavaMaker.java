// Copyright (c) Corporation for National Research Initiatives

package org.python.compiler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Hashtable;
import org.python.core.PyObject;
import org.python.core.PyProxy;

public class JavaMaker extends ProxyMaker
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

    private void makeStrings(Code code, String[] list) throws Exception {
        if (list != null) {
            int n = list.length;
            code.iconst(n);
            code.anewarray(code.pool.Class("java/lang/String"));
            int strings = code.getLocal();
            code.astore(strings);
            for(int i=0; i<n; i++) {
                code.aload(strings);
                code.iconst(i);
                code.ldc(list[i]);
                code.aastore();
            }
            code.aload(strings);
            code.freeLocal(strings);
        } else {
            code.aconst_null();
        }
    }

    public void addConstructor(String name, Class[] parameters, Class ret,
                               String sig, int access)
        throws Exception
    {
        /* Need a fancy constructor for the Java side of things */
        Code code = classfile.addMethod("<init>", sig, access);
        callSuper(code, "<init>", name, parameters, null, sig);
        code.aload(0);
        getArgs(code, parameters);

        int initProxy = code.pool.Methodref(
                 classfile.name, "__initProxy__",
                 "([Ljava/lang/Object;)V");
        code.invokevirtual(initProxy);
        code.return_();
    }

    public void addProxy() throws Exception {
        if (methods != null)
            super.addProxy();

        // _initProxy method
        Code code = classfile.addMethod("__initProxy__", "([Ljava/lang/Object;)V", Modifier.PUBLIC);

        code.aload(0);
        code.ldc(pythonModule);
        code.ldc(pythonClass);

        code.aload(1);

        makeStrings(code, packages);
        makeStrings(code, properties);

        code.iconst(frozen ? 1 : 0);

        int initProxy = code.pool.Methodref(
            "org/python/core/Py", "initProxy",
            "(Lorg/python/core/PyProxy;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;[Ljava/lang/String;[Ljava/lang/String;Z)V");
        code.invokestatic(initProxy);
        code.return_();

        if (main)
            addMain();
    }

//     public void addMethods(Class c) throws Exception {
//         if (methods != null) {
//             super.addMethods(c);
//         }
//     }

    public void addMethod(Method method, int access) throws Exception {
//         System.out.println("add: "+method.getName()+", "+methods.containsKey(method.getName()));
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
            super.addSuperMethod(methodName,superName,superclass,parameters,ret,sig,access);
        }
    }

*/
  
    public void addMain() throws Exception {
        Code code = classfile.addMethod("main", "([Ljava/lang/String;)V",
                                        ClassFile.PUBLIC | ClassFile.STATIC);

        // Load the class of the Python module to run
        int forname = code.pool.Methodref(
               "java/lang/Class","forName",
               "(Ljava/lang/String;)Ljava/lang/Class;");
        code.ldc(pythonModule);
        code.invokestatic(forname);

        // Load in any command line arguments
        code.aload(0);
        makeStrings(code, packages);
        makeStrings(code, properties);
        code.iconst(frozen ? 1 : 0);

        int runMain = code.pool.Methodref(
            "org/python/core/Py", "runMain",
            "(Ljava/lang/Class;[Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;Z)V");
        code.invokestatic(runMain);
        code.return_();
    }
}
