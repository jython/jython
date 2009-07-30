// Copyright 2001 Finn Bock

package org.python.compiler;

import java.util.Vector;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @Deprecated Not used.
 */
public class LineNumberTable {
    int attName;
    Vector<Short> lines;

    public LineNumberTable() throws IOException {
        lines = new Vector<Short>();
    }

    public void write(DataOutputStream stream) throws IOException {
        stream.writeShort(attName);
        int n = lines.size();
        stream.writeInt(n * 2 + 2);
        stream.writeShort(n / 2);
        for (int i = 0; i < n; i += 2) {
            Short startpc = lines.elementAt(i);
            Short lineno =  lines.elementAt(i+1);
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

