// Copyright © Corporation for National Research Initiatives

package org.python.compiler;

import java.util.Hashtable;
import java.util.Enumeration;
import java.lang.reflect.Method;
import java.io.*;


public class AdapterMaker extends ProxyMaker
{
    public AdapterMaker(String classname) {
        super(classname+"$Adapter");
        this.classname = classname;
    }

    public void build(Class listener) throws Exception {
        names = new Hashtable();

        //Class superclass = org.python.core.PyAdapter.class;
        int access = ClassFile.PUBLIC | ClassFile.SYNCHRONIZED;
        classfile = new ClassFile(myClass, "java/lang/Object", access);

        classfile.addInterface(mapClass(listener));

        addMethods(listener, new Hashtable());
        addConstructors(Object.class);
        doConstants();
    }

    public void build() throws Exception {
        build(Class.forName(classname));
    }

    public static String makeAdapter(String classname, OutputStream ostream)
        throws Exception
    {
        AdapterMaker pm = new AdapterMaker(classname);
        pm.build();
        pm.classfile.write(ostream);
        return pm.myClass;
    }

    public void doConstants() throws Exception {
        for (Enumeration e=names.keys(); e.hasMoreElements();)  {
            String name = (String)e.nextElement();
            classfile.addField(name, "Lorg/python/core/PyObject;",
                               ClassFile.PUBLIC);
        }
    }
        
    public void addMethod(Method method, int access) throws Exception {
        Class[] parameters = method.getParameterTypes();
        Class ret = method.getReturnType();
        String sig = makeSignature(parameters, ret);

        String name = method.getName();
        //System.out.println(name+": "+sig);
        names.put(name, name);

        Code code = classfile.addMethod(name, sig, ClassFile.PUBLIC);

        code.aload(0);
        int pyfunc = code.pool.Fieldref(classfile.name, name,
                                        "Lorg/python/core/PyObject;");
        code.getfield(pyfunc);
        code.dup();
        Label returnNull = code.getLabel();
        code.ifnull(returnNull);
        callMethod(code, name, parameters, ret);
        returnNull.setPosition();
        doNullReturn(code, ret);
    }
}
