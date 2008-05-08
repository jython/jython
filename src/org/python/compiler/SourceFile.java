// Copyright (c) Corporation for National Research Initiatives

package org.python.compiler;
import java.io.DataOutputStream;
import java.io.IOException;

public class SourceFile extends Attribute {
    String attName;
    String filename;

    public SourceFile(String name) throws IOException {
        //this.attName = pool.UTF8("SourceFile");
        //this.filename = pool.UTF8(name);
        this.attName = "SourceFile";
        this.filename = name;
    }

    public void write(DataOutputStream stream) throws IOException {
	//FIXME: need to LDC the att and name.
        //FJW stream.writeShort(attName);
        //FJW stream.writeInt(2);
        //FJW stream.writeShort(filename);
    }
}
