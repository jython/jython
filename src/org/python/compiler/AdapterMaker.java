// Copyright (c) Corporation for National Research Initiatives
package org.python.compiler;

import java.lang.reflect.Method;
import java.util.HashSet;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.python.util.Generic;

public class AdapterMaker extends ProxyMaker {

    public AdapterMaker(String adapterName, Class<?> interfac) {
        super(adapterName, Object.class, new Class<?>[] {interfac});
    }

    @Override
    public void build() throws Exception {
        names = Generic.set();
        int access = Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNCHRONIZED;
        classfile = new ClassFile(myClass, "java/lang/Object", access);
        classfile.addInterface(mapClass(interfaces[0]));
        addMethods(interfaces[0], new HashSet<String>());
        addConstructors(Object.class);
        doConstants();
    }

    @Override
    public void doConstants() throws Exception {
        for (String name : names) {
            classfile.addField(name, $pyObj, Opcodes.ACC_PUBLIC);
        }
    }

    @Override
    public void addMethod(Method method, int access) throws Exception {
        Class<?>[] parameters = method.getParameterTypes();
        Class<?> ret = method.getReturnType();
        String name = method.getName();
        names.add(name);
        Code code = classfile.addMethod(name, makeSig(ret, parameters), Opcodes.ACC_PUBLIC);
        code.aload(0);
        code.getfield(classfile.name, name, $pyObj);
        code.dup();
        Label returnNull = new Label();
        code.ifnull(returnNull);
        callMethod(code, name, parameters, ret, method.getExceptionTypes());
        code.label(returnNull);
        doNullReturn(code, ret);
    }
}
