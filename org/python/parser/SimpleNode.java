// Copyright (c) Corporation for National Research Initiatives
package org.python.parser;

import org.python.parser.ast.*;
import java.io.DataOutputStream;
import java.io.IOException;

public class SimpleNode implements Node {
    public int beginLine, beginColumn;

    public boolean from_future_checked = false; // from __future__ support

    public SimpleNode() { }

    public static Node jjtCreate(PythonGrammar p, int id) {
        return p.jjtree.openNode(id);
    }

    public int getId() {
        return -1;
    }

    public Object getImage() {
        return null;
    }

    public void setImage(Object image) {
    }

    /* You can override these two methods in subclasses of SimpleNode to
       customize the way the node appears when the tree is dumped.  If
       your output uses more than one line you should override
       toString(String), otherwise overriding toString() is probably all
       you need to do. */

    public String toString() {
        return super.toString() + " at line "+beginLine;
    }
    public String toString(String prefix) { return prefix + toString(); }

    public Object accept(VisitorIF visitor) throws Exception {
        throw new ParseException("Unexpected node: "+this);
    }

    public void traverse(VisitorIF visitor) throws Exception {
        throw new ParseException("Unexpected node: "+this);
    }

    /* Override this method if you want to customize how the node dumps
       ut its children. */

    protected String dumpThis(String s) {
        return s;
    }

    protected String dumpThis(Object o) {
        return String.valueOf(o);
    }

    protected String dumpThis(Object[] s) {
        StringBuffer sb = new StringBuffer();
        if (s == null) {
            sb.append("null");
        } else {
            sb.append("[");
            for (int i = 0; i < s.length; i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append(String.valueOf(s[i]));
            }
            sb.append("]");
        }
        
        return sb.toString();
    }

    protected String dumpThis(int i) {
        return Integer.toString(i);
    }

    protected String dumpThis(int i, String[] names) {
        // XXX Verify bounds.
        return names[i];
    }

    protected String dumpThis(int[] arr, String[] names) {
        StringBuffer sb = new StringBuffer();
        if (arr == null) {
            sb.append("null");
        } else {
            sb.append("[");
            for (int i = 0; i < arr.length; i++) {
                if (i > 0)
                    sb.append(", ");
                // XXX Verify bounds.
                sb.append(names[arr[i]]);
            }
            sb.append("]");
        }
        return sb.toString();
    }

    protected String dumpThis(boolean b) {
        return String.valueOf(b);
    }


    public void pickle(DataOutputStream ostream) throws IOException {
        throw new IOException("Pickling not implemented");
    }

    protected void pickleThis(String s, DataOutputStream ostream)
        throws IOException
    {
        if (s == null) {
            ostream.writeInt(-1);
        } else {
            ostream.writeInt(s.length());
            ostream.writeBytes(s);
        }
    }

    protected void pickleThis(String[] s, DataOutputStream ostream)
        throws IOException
    {
        if (s == null) {
            ostream.writeInt(-1);
        } else {
            ostream.writeInt(s.length);
            for (int i = 0; i < s.length; i++) {
                pickleThis(s[i], ostream);
            }
        }
    }

    protected void pickleThis(SimpleNode o, DataOutputStream ostream)
        throws IOException
    {
        if (o == null) {
            ostream.writeInt(-1);
        } else {
            o.pickle(ostream);
        }
    }

    protected void pickleThis(SimpleNode[] s, DataOutputStream ostream)
        throws IOException
    {
        if (s == null) {
            ostream.writeInt(-1);
        } else {
            ostream.writeInt(s.length);
            for (int i = 0; i < s.length; i++) {
                pickleThis(s[i], ostream);
            }
        }
    }

    protected void pickleThis(int i, DataOutputStream ostream)
        throws IOException
    {
        ostream.writeInt(i);
    }

    protected void pickleThis(int[] arr, DataOutputStream ostream)
        throws IOException
    {
        if (arr == null) {
            ostream.writeInt(-1);
        } else {
            ostream.writeInt(arr.length);
            for (int i = 0; i < arr.length; i++) {
                ostream.writeInt(arr[i]);
            }
        }
    }

    protected void pickleThis(boolean b, DataOutputStream ostream)
        throws IOException
    {
        ostream.writeBoolean(b);
    }

    protected void pickleThis(Object n, DataOutputStream ostream)
        throws IOException
    {
        String s = n.toString();
        ostream.writeInt(s.length());
        ostream.writeBytes(s);
    }
}
