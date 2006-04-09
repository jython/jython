// Copyright 2001 Finn Bock

package org.python.compiler;

import java.io.*;
import java.util.*;

public class LineNumberTable extends Attribute {
    int attName;
    ConstantPool pool;
    Vector lines;

    public LineNumberTable(ConstantPool pool) throws IOException {
        this.pool = pool;
        attName = pool.UTF8("LineNumberTable");
        lines = new Vector();
    }

    public void write(DataOutputStream stream) throws IOException {
        stream.writeShort(attName);
        int n = lines.size();
        stream.writeInt(n * 2 + 2);
        stream.writeShort(n / 2);
        for (int i = 0; i < n; i += 2) {
            Short startpc = (Short) lines.elementAt(i);
            Short lineno =  (Short) lines.elementAt(i+1);
            stream.writeShort(startpc.shortValue());
            stream.writeShort(lineno.shortValue());
        }
    }

    public void addLine(int startpc, int lineno) {
        lines.addElement(new Short((short) startpc));
        lines.addElement(new Short((short) lineno));
    }

    public int length() {
        return lines.size() * 2 + 8;
    }
}

