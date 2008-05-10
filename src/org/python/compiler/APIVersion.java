// Copyright (c) Corporation for National Research Initiatives

package org.python.compiler;
import java.io.DataOutputStream;
import java.io.IOException;

public class APIVersion {
    int attName;
    int version;

    public APIVersion(int version) throws IOException {
        //FJW attName = pool.UTF8("org.python.APIVersion");
        //FJW this.version = version;
    }

    public void write(DataOutputStream stream) throws IOException {
        //FJW stream.writeShort(attName);
        //FJW stream.writeInt(4);
        //FJW stream.writeInt(version);
    }
}
