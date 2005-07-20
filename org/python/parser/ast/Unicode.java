package org.python.parser.ast;
import org.python.parser.SimpleNode;
import java.io.DataOutputStream;
import java.io.IOException;

public class Unicode extends Str {

    public Unicode(String s) {
        super(s);
    }

    public Unicode(String s, SimpleNode parent) {
        super(s, parent);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("Unicode[");
        sb.append("s=");
        sb.append(dumpThis(this.s));
        sb.append("]");
        return sb.toString();
    }

    public void pickle(DataOutputStream ostream) throws IOException {
        pickleThis(38, ostream);
        pickleThis(this.s, ostream);
    }

    public Object accept(VisitorIF visitor) throws Exception {
        return visitor.visitUnicode(this);
    }

    public void traverse(VisitorIF visitor) throws Exception {
    }

}
