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
		if ( charStartIndex == -1 && token != null ) {
			return token.getTokenIndex();
		}
		return charStartIndex ;
	}

	public void setCharStartIndex(int index) {
		charStartIndex  = index;
	}

	public int getCharStopIndex() {
        //XXX: This direct iteration of children is here because PythonTreeAdaptor
        //     is getting last tokens with indexes of (0,0).  Fix there and then 
        //     remove this part.
        if (children != null && children.size() > 0) {
            for (int i = children.size() - 1; i >= 0; i--) {
                PythonTree t = (PythonTree)children.get(i);
                if (t.getCharStopIndex() > 0) {
                    return t.getCharStopIndex();
                }
            }
        }
		if ( charStopIndex == -1 && token != null ) {
			return token.getTokenIndex();
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

    public String toStringTree() {
        if (children == null || children.size() == 0) {
            // System.out.println("Where are my children? -- asks " + token.getText());
            return this.toString();
        }
        StringBuffer buf = new StringBuffer();
        if (!isNil()) {
            buf.append("(");
            buf.append(this.toString());
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

    public <R> R accept(VisitorIF<R> visitor) throws Exception {
        throw new RuntimeException("Unexpected node: " + this);
    }
    
    public void traverse(VisitorIF visitor) throws Exception {
        throw new RuntimeException("Cannot traverse node: " + this);
    }
}
