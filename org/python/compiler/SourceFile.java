// Copyright © Corporation for National Research Initiatives

package org.python.compiler;
import java.io.*;

public class SourceFile extends Attribute {
    int attName;
    int filename;

    public SourceFile(String name, ConstantPool pool) throws IOException {
        attName = pool.UTF8("SourceFile");
        filename = pool.UTF8(name);
    }

    public void write(DataOutputStream stream) throws IOException {
        stream.writeShort(attName);
        stream.writeInt(2);
        stream.writeShort(filename);
    }
}
