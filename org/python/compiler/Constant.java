// Copyright © Corporation for National Research Initiatives

package org.python.compiler;

import java.io.*;

abstract class Constant {
    public Module module;
    public static int access = ClassFile.STATIC | ClassFile.FINAL;
    public String name;

    public abstract void get(Code c) throws IOException;
    public abstract void put(Code c) throws IOException;
}
