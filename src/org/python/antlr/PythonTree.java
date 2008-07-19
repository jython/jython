package org.python.antlr;

import org.antlr.runtime.tree.BaseTree;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;

import java.io.DataOutputStream;
import java.io.IOException;

import org.python.antlr.ast.VisitorIF;

public class PythonTree extends CommonTree implements AST {

    public boolean from_future_checked = false;
    private int charStartIndex = -1;
    private int charStopIndex = -1;

    public PythonTree(int ttype, Token t) {
        super();
        CommonToken c = new CommonToken(ttype, t.getText());
        c.setLine(t.getLine());
        c.setTokenIndex(t.getTokenIndex());
        c.setCharPositionInLine(t.getCharPositionInLine());
        c.setChannel(t.getChannel());
        c.setStartIndex(((CommonToken)t).getStartIndex());
        c.setStopIndex(((CommonToken)t).getStopIndex());
        token = c;
    }

    public PythonTree(Token token) {
        super(token);
    }

    public PythonTree(PythonTree node) {
        super(node);
        charStartIndex = node.getCharStartIndex();
        charStopIndex = node.getCharStopIndex();
    }

    public int getCharStartIndex() {
        if (charStartIndex == -1 && token != null) {
            if (token instanceof CommonToken) {
                return ((CommonToken)token).getStartIndex();
            }
            if (token instanceof ImaginaryToken) {
                return ((ImaginaryToken)token).getStartIndex();
            }
        }
        return charStartIndex ;
    }

    public void setCharStartIndex(int index) {
        charStartIndex  = index;
    }

    public int getCharStopIndex() {
        if (charStopIndex == -1 && token != null) {
            if (token instanceof CommonToken) {
                return ((CommonToken)token).getStopIndex();
            }
            if (token instanceof ImaginaryToken) {
                return ((ImaginaryToken)token).getStopIndex();
            }
        }
        return charStopIndex;
    }

    public void setCharStopIndex(int index) {
        charStopIndex = index;
    }

    public String toString() {
        if (isNil()) {
            return "None";
        }
        return token.getText() + "(" + this.getLine() + "," + this.getCharPositionInLine() + ")";
    }

    public String info() {
        return this.getCharStartIndex() + ":" + this.getCharStopIndex();
    }

    public String toStringTree() {
        if (children == null || children.size() == 0) {
            return this.toString();// + "[" + this.info() + "]";
        }
        StringBuffer buf = new StringBuffer();
        if (!isNil()) {
            buf.append("(");
            buf.append(this.toString());// + "[" + this.info() + "]");
            buf.append(' ');
        }
        for (int i = 0; children != null && i < children.size(); i++) {
            BaseTree t = (BaseTree)children.get(i);
            if (i > 0) {
                buf.append(' ');
            }
            buf.append(t.toStringTree());
        }
        if (!isNil()) {
            buf.append(")");
        }
        return buf.toString();
    }

    protected String dumpThis(String s) {
        return s;
    }

    protected String dumpThis(Object o) {
        if (o instanceof PythonTree) {
            return ((PythonTree)o).toStringTree();
        }
        return String.valueOf(o);
    }

    protected String dumpThis(Object[] s) {
        StringBuffer sb = new StringBuffer();
        if (s == null) {
            sb.append("null");
        } else {
            sb.append("(");
            for (int i = 0; i < s.length; i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append(dumpThis(s[i]));
            }
            sb.append(")");
        }
        
        return sb.toString();
    }

    public <R> R accept(VisitorIF<R> visitor) throws Exception {
        throw new RuntimeException("Unexpected node: " + this);
    }
    
    public void traverse(VisitorIF visitor) throws Exception {
        throw new RuntimeException("Cannot traverse node: " + this);
    }
}
