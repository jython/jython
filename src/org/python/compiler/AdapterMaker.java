// Copyright (c) Corporation for National Research Initiatives

package org.python.compiler;

import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.HashSet;


public class AdapterMaker extends ProxyMaker
{
    public AdapterMaker(Class<?> interfac) {
        super(interfac.getName()+"$Adapter", interfac);
    }

    public void build() throws Exception {
        names = new HashSet<String>();

        int access = ClassFile.PUBLIC | ClassFile.SYNCHRONIZED;
        classfile = new ClassFile(myClass, "java/lang/Object", access);

        classfile.addInterface(mapClass(interfaces[0]));

        addMethods(interfaces[0], new HashSet<String>());
        addConstructors(Object.class);
        doConstants();
    }


    public static String makeAdapter(Class<?> interfac, OutputStream ostream)
        throws Exception
    {
        AdapterMaker pm = new AdapterMaker(interfac);
        pm.build();
        pm.classfile.write(ostream);
        return pm.myClass;
    }

    public void doConstants() throws Exception {
        for (String name : names) {
            classfile.addField(name, "Lorg/python/core/PyObject;", ClassFile.PUBLIC);
        }
    }

    public void addMethod(Method method, int access) throws Exception {
        Class<?>[] parameters = method.getParameterTypes();
        Class<?> ret = method.getReturnType();
        String sig = makeSignature(parameters, ret);

        String name = method.getName();
        names.add(name);

        Code code = classfile.addMethod(name, sig, ClassFile.PUBLIC);

        code.aload(0);
        int pyfunc = code.pool.Fieldref(classfile.name, name, "Lorg/python/core/PyObject;");
        code.getfield(pyfunc);
        code.dup();
        Label returnNull = code.getLabel();
        code.ifnull(returnNull);
        callMethod(code, name, parameters, ret, method.getExceptionTypes());
        returnNull.setPosition();
        doNullReturn(code, ret);
    }
}
