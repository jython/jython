// Copyright © Corporation for National Research Initiatives

package org.python.compiler;
import java.io.*;

public class APIVersion extends Attribute {
    int attName;
    int version;

    public APIVersion(int version, ConstantPool pool) throws IOException {
        attName = pool.UTF8("org.python.APIVersion");
        this.version = version;
    }

    public void write(DataOutputStream stream) throws IOException {
        stream.writeShort(attName);
        stream.writeInt(4);
        stream.writeInt(version);
    }
}
