// Copyright (c) Corporation for National Research Initiatives

package org.python.compiler;

import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Hashtable;

import org.python.objectweb.asm.Label;
import org.python.objectweb.asm.MethodVisitor;
import org.python.objectweb.asm.Opcodes;


public class AdapterMaker extends ProxyMaker
{
    public AdapterMaker(Class interfac) {
        super(interfac.getName()+"$Adapter", interfac);
    }

    public void build() throws Exception {
        names = new Hashtable();

        //Class superclass = org.python.core.PyAdapter.class;
        int access = Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNCHRONIZED;
        classfile = new ClassFile(myClass, "java/lang/Object", access);

        classfile.addInterface(mapClass(interfaces[0]));

        addMethods(interfaces[0], new Hashtable());
        addConstructors(Object.class);
        doConstants();
    }


    public static String makeAdapter(Class interfac, OutputStream ostream)
        throws Exception
    {
        AdapterMaker pm = new AdapterMaker(interfac);
        pm.build();
        pm.classfile.write(ostream);
        return pm.myClass;
    }

    public void doConstants() throws Exception {
        for (Enumeration e=names.keys(); e.hasMoreElements();)  {
            String name = (String)e.nextElement();
            classfile.addField(name, "Lorg/python/core/PyObject;",
                               Opcodes.ACC_PUBLIC);
        }
    }

    public void addMethod(Method method, int access) throws Exception {
        Class[] parameters = method.getParameterTypes();
        Class ret = method.getReturnType();
        String sig = makeSignature(parameters, ret);

        String name = method.getName();
        //System.out.println(name+": "+sig);
        names.put(name, name);

        Code mv = classfile.addMethod(name, sig, Opcodes.ACC_PUBLIC);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, classfile.name, name, "Lorg/python/core/PyObject;");
        mv.visitInsn(DUP);
        Label returnNull = new Label();
        mv.visitJumpInsn(IFNULL, returnNull);
        callMethod(mv, name, parameters, ret, method.getExceptionTypes());
        mv.visitLabel(returnNull);
        doNullReturn(mv, ret);
    }
}
