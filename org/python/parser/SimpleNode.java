// Copyright © Corporation for National Research Initiatives
package org.python.parser;

public class SimpleNode implements Node
{
    protected Node parent;
    public SimpleNode[] children;
    public int id;
    protected PythonGrammar parser;
    public int endLine, endColumn, beginLine, beginColumn;
    Object info;

    public SimpleNode(int i) {
	id = i;
    }

    public SimpleNode(PythonGrammar p, int i) {
	this(i);
	parser = p;
    }

    public static Node jjtCreate(int id) {
	return new SimpleNode(id);
    }

    public static Node jjtCreate(PythonGrammar p, int id) {
	return new SimpleNode(p, id);
    }

    public void jjtOpen() {
    }

    public void jjtClose() {
    }

    public void jjtSetParent(Node n) { parent = n; }
    public Node jjtGetParent() { return parent; }

    public void jjtAddChild(Node n, int i) {
	if (children == null) {
	    children = new SimpleNode[i + 1];
	} else if (i >= children.length) {
	    SimpleNode c[] = new SimpleNode[i + 1];
	    System.arraycopy(children, 0, c, 0, children.length);
	    children = c;
	}
	children[i] = (SimpleNode)n;
    }

    public Node jjtGetChild(int i) {
	return children[i];
    }

    public int jjtGetNumChildren() {
	return (children == null) ? 0 : children.length;
    }

    public SimpleNode getChild(int i) {
	return children[i];
    }

    public int getNumChildren() {
	return (children == null) ? 0 : children.length;
    }

    public Object getInfo() { return info; }

    public void setInfo(Object o) {
	info = o;
	//System.out.println("name: "+info);
    }

    public void setString(String s, int quotes) {
	//System.out.println("string: "+s);
	char quoteChar = s.charAt(0);
	if (quoteChar == 'r' || quoteChar == 'R') {
	    info = s.substring(quotes+1, s.length()-quotes);
	} else {
	    StringBuffer sb = new StringBuffer(s.length());
	    char[] ca = s.toCharArray();
	    int n = ca.length-quotes;
	    int i=quotes;
	    int last_i=i;

	    while (i<n) {
		if (ca[i] == '\r') {
		    sb.append(ca, last_i, i-last_i);
		    sb.append('\n');
		    i++;
		    if (ca[i] == '\n') i++;
		    last_i = i;
		    continue;
		}
		if (ca[i++] != '\\' || i >= n) continue;
		sb.append(ca, last_i, i-last_i-1);
		switch(ca[i++]) {
		case '\r':
		    if (ca[i] == '\n') i++;
		case '\n': break;
		case 'b': sb.append('\b'); break;
		case 't': sb.append('\t'); break;
		case 'n': sb.append('\n'); break;
		case 'f': sb.append('\f'); break;
		case 'r': sb.append('\r'); break;
		case '\"':
		case '\'':
		    sb.append(ca[i-1]);
		    break;
		case '\\': sb.append('\\'); break;
		    //Special Python escapes
		case 'a': sb.append('\007'); break;
		case 'v': sb.append('\013'); break;

		case '0':
		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7':
		    int c = ca[i-1]-'0';
		    if (i<n && '0' <= ca[i] && ca[i] <= '7') {
			c = (c<<3) + (ca[i++] -'0');
			if (i<n && '0' <= ca[i] && ca[i] <= '7') {
			    c = (c<<3) + (ca[i++] -'0');
			}
		    }
		    sb.append((char)c);
		    break;
		case 'x':
		    if (Character.digit(ca[i], 16) != -1) {
			int digit;
			char x=0;
			while (i<n &&
			       (digit = Character.digit(ca[i++], 16)) != -1)
			{
			    x = (char)(x*16 + digit);
			}
			if (i<n) i-=1;
			sb.append(x);
			break;
		    }
		    // If illegal hex digit, just fall through
		default:
		    sb.append('\\');
		    sb.append(ca[i-1]);
		}
		last_i = i;
	    }
	    sb.append(ca, last_i, i-last_i);
	    info = sb.toString();
	}
    }


    public void setInteger(String s, int radix) {
	if (s.endsWith("j") || s.endsWith("J")) {
	    setFloat(s);
	} else {
	    if (s.endsWith("L") || s.endsWith("l")) {
		s = s.substring(0, s.length()-1);
		if (radix == 10) setInfo(s);
		else setInfo(new java.math.BigInteger(s, radix).toString());
	    } else {
		int ndigits = s.length();
		int i=0;
		while (i < ndigits && s.charAt(i) == '0') i++;
		if ((ndigits - i) > 11) {
		    throw new TokenMgrError("integer literal too large",
					    beginLine, beginColumn);
		}

		long l = Long.valueOf(s, radix).longValue();
		if (l > 0xffffffffl || (radix == 10 && l > Integer.MAX_VALUE))
		{
		    throw new TokenMgrError("integer literal too large",
					    beginLine, beginColumn);
		}
		setInfo(new Integer((int)l));
	    }
    	}
    }

    public void setFloat(String s) {
        if (s.endsWith("j") || s.endsWith("J")) {
            setInfo(Double.valueOf(s.substring(0, s.length()-1)));
            id = PythonGrammarTreeConstants.JJTCOMPLEX;
        } else {
            setInfo(Double.valueOf(s));
        }
    }

    /* You can override these two methods in subclasses of SimpleNode to
       customize the way the node appears when the tree is dumped.  If
       your output uses more than one line you should override
       toString(String), otherwise overriding toString() is probably all
       you need to do. */

    public String toString() {
	return PythonGrammarTreeConstants.jjtNodeName[id]+":"+info+
	    " at line "+beginLine;
    }
    public String toString(String prefix) { return prefix + toString(); }

    /* Override this method if you want to customize how the node dumps
       out its children. */

    public void dump(String prefix) {
	System.out.println(toString(prefix));
	if (children != null) {
	    for (int i = 0; i < children.length; ++i) {
		SimpleNode n = (SimpleNode)children[i];
		if (n != null) {
		    n.dump(prefix + " ");
		}
	    }
	}
    }

    public Object visit(Visitor visitor) throws Exception {
	switch(id) {
	case 0:
	    return visitor.single_input(this);
	case 1:
	    return visitor.file_input(this);
	case 2:
	    return visitor.eval_input(this);
	case 3:
	    return visitor.funcdef(this);
	case 5:
	    return visitor.varargslist(this);
	case 6:
	    return visitor.ExtraArgList(this);
	case 7:
	    return visitor.ExtraKeywordList(this);
	case 8:
	    return visitor.defaultarg(this);
	case 9:
	    return visitor.fplist(this);
	case 10:
	    return visitor.expr_stmt(this);
	case 11:
	    return visitor.print_stmt(this);
	case 12:
	    return visitor.del_stmt(this);
	case 13:
	    return visitor.pass_stmt(this);
	case 14:
	    return visitor.break_stmt(this);
	case 15:
	    return visitor.continue_stmt(this);
	case 16:
	    return visitor.return_stmt(this);
	case 17:
	    return visitor.raise_stmt(this);
	case 18:
	    return visitor.Import(this);
	case 19:
	    return visitor.ImportFrom(this);
	case 20:
	    return visitor.dotted_name(this);
	case 21:
	    return visitor.global_stmt(this);
	case 22:
	    return visitor.exec_stmt(this);
	case 23:
	    return visitor.assert_stmt(this);
	case 24:
	    return visitor.if_stmt(this);
	case 25:
	    return visitor.while_stmt(this);
	case 26:
	    return visitor.for_stmt(this);
	case 27:
	    return visitor.try_stmt(this);
	case 28:
	    return visitor.except_clause(this);
	case 29:
	    return visitor.suite(this);
	case 30:
	    return visitor.or_boolean(this);
	case 31:
	    return visitor.and_boolean(this);
	case 32:
	    return visitor.not_1op(this);
	case 33:
	    return visitor.comparision(this);
	case 34:
	    return visitor.less_cmp(this);
	case 35:
	    return visitor.greater_cmp(this);
	case 36:
	    return visitor.equal_cmp(this);
	case 37:
	    return visitor.greater_equal_cmp(this);
	case 38:
	    return visitor.less_equal_cmp(this);
	case 39:
	    return visitor.notequal_cmp(this);
	case 40:
	    return visitor.in_cmp(this);
	case 41:
	    return visitor.not_in_cmp(this);
	case 42:
	    return visitor.is_not_cmp(this);
	case 43:
	    return visitor.is_cmp(this);
	case 44:
	    return visitor.or_2op(this);
	case 45:
	    return visitor.xor_2op(this);
	case 46:
	    return visitor.and_2op(this);
	case 47:
	    return visitor.lshift_2op(this);
	case 48:
	    return visitor.rshift_2op(this);
	case 49:
	    return visitor.add_2op(this);
	case 50:
	    return visitor.sub_2op(this);
	case 51:
	    return visitor.mul_2op(this);
	case 52:
	    return visitor.div_2op(this);
	case 53:
	    return visitor.mod_2op(this);
	case 54:
	    return visitor.pos_1op(this);
	case 55:
	    return visitor.neg_1op(this);
	case 56:
	    return visitor.invert_1op(this);
	case 57:
	    return visitor.pow_2op(this);
	case 58:
	    return visitor.Call_Op(this);
	case 59:
	    return visitor.Index_Op(this);
	case 60:
	    return visitor.Dot_Op(this);
	case 61:
	    return visitor.tuple(this);
	case 62:
	    return visitor.list(this);
	case 63:
	    return visitor.dictionary(this);
	case 64:
	    return visitor.str_1op(this);
	case 65:
	    return visitor.strjoin(this);
	case 66:
	    return visitor.lambdef(this);
	case 67:
	    return visitor.Ellipses(this);
	case 68:
	    return visitor.Slice(this);
	case 69:
	    return visitor.Colon(this);
	case 70:
	    return visitor.Comma(this);
	case 71:
	    return visitor.classdef(this);
	case 72:
	    return visitor.arglist(this);
	case 73:
	    return visitor.Keyword(this);
	case 74:
	    return visitor.Int(this);
	case 75:
	    return visitor.Float(this);
	case 76:
	    return visitor.Complex(this);
	case 77:
	    return visitor.Name(this);
	case 78:
	    return visitor.String(this);
	default:
	    return null;
	}

    }

}

