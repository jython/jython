// Copyright (c) Corporation for National Research Initiatives

package org.python.compiler;

import java.io.IOException;

import org.objectweb.asm.Opcodes;

abstract class Constant implements Opcodes{
    public Module module;
    public static int access = ACC_STATIC | ACC_FINAL;
    public String name;

    public abstract void get(Code mv) throws IOException;

    public abstract void put(Code mv) throws IOException;
}
