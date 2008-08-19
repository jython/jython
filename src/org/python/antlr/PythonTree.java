package org.python.antlr;

import org.antlr.runtime.tree.BaseTree;
import org.antlr.runtime.tree.Tree;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;

import java.io.DataOutputStream;
import java.io.IOException;

import org.python.antlr.ast.VisitorIF;

public class PythonTree extends BaseTree implements AST {

    public boolean from_future_checked = false;
    private int charStartIndex = -1;
    private int charStopIndex = -1;

	/** A single token is the payload */
	public Token token;

	/** What token indexes bracket all tokens associated with this node
	 *  and below?
	 */
	protected int startIndex=-1, stopIndex=-1;

	/** Who is the parent node of this node; if null, implies node is root */
	public PythonTree parent;

	/** What index is this node in the child list? Range: 0..n-1 */
	public int childIndex = -1;

    /**
     * The empty constructor is intended only for use by PythonErrorNode.
     */
    public PythonTree() {
    }

    public PythonTree(int ttype, Token t) {
        CommonToken c = new CommonToken(ttype, t.getText());
        c.setLine(t.getLine());
        c.setTokenIndex(t.getTokenIndex());
        c.setCharPositionInLine(t.getCharPositionInLine());
        c.setChannel(t.getChannel());
        c.setStartIndex(((CommonToken)t).getStartIndex());
        c.setStopIndex(((CommonToken)t).getStopIndex());
        token = c;
    }

    public PythonTree(Token t) {
        this.token = t;
    }

    public PythonTree(PythonTree node) {
		super(node);
		token = node.token;
		startIndex = node.startIndex;
		stopIndex = node.stopIndex;
        charStartIndex = node.getCharStartIndex();
        charStopIndex = node.getCharStopIndex();
    }
	
	public Token getToken() {
		return token;
	}

	public Tree dupNode() {
		return new PythonTree(this);
	}

	public boolean isNil() {
		return token==null;
	}

	public int getType() {
		if (token==null) {
			return Token.INVALID_TOKEN_TYPE;
		}
		return token.getType();
	}

	public String getText() {
		if (token==null) {
			return null;
		}
		return token.getText();
	}

	public int getLine() {
		if (token==null || token.getLine()==0) {
			if ( getChildCount()>0 ) {
				return getChild(0).getLine();
			}
			return 0;
		}
		return token.getLine();
	}

	public int getCharPositionInLine() {
		if (token==null || token.getCharPositionInLine()==-1) {
			if (getChildCount()>0) {
				return getChild(0).getCharPositionInLine();
			}
			return 0;
		} else if (token != null && token.getCharPositionInLine() == -2) {
            //XXX: yucky fix because CPython's ast uses -1 as a real value
            //     for char pos in certain circumstances (for example, the
            //     char pos of multi-line strings.  I would just use -1,
            //     but ANTLR is using -1 in special ways also.
            return -1;
        }
		return token.getCharPositionInLine();
	}

	public int getTokenStartIndex() {
		if ( startIndex==-1 && token!=null ) {
			return token.getTokenIndex();
		}
		return startIndex;
	}

	public void setTokenStartIndex(int index) {
		startIndex = index;
	}

	public int getTokenStopIndex() {
		if ( stopIndex==-1 && token!=null ) {
			return token.getTokenIndex();
		}
		return stopIndex;
	}

	public void setTokenStopIndex(int index) {
		stopIndex = index;
	}

	public int getChildIndex() {
		return childIndex;
	}

	public Tree getParent() {
		return parent;
	}

	public void setParent(Tree t) {
		this.parent = (PythonTree)t;
	}

	public void setChildIndex(int index) {
		this.childIndex = index;
	}

    public int getCharStartIndex() {
        if (charStartIndex == -1 && token != null) {
            return ((CommonToken)token).getStartIndex();
        }
        return charStartIndex ;
    }

    public void setCharStartIndex(int index) {
        charStartIndex  = index;
    }

    /*
     * Adding one to stopIndex from Tokens.  ANTLR defines the char position as
     * being the array index of the actual characters. Most tools these days
     * define document offsets as the positions between the characters.  If you
     * imagine drawing little boxes around each character and think of the
     * numbers as pointing to either the left or right side of a character's
     * box, then 0 is before the first character - and in a Document of 10
     * characters, position 10 is after the last character.
     */
    public int getCharStopIndex() {

        if (charStopIndex == -1 && token != null) {
            return ((CommonToken)token).getStopIndex() + 1;
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
		if ( getType()==Token.INVALID_TOKEN_TYPE ) {
			return "<errornode>";
		}
		if ( token==null ) {
			return null;
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
