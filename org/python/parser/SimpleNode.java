// Copyright © Corporation for National Research Initiatives
package org.python.parser;

import org.python.core.Py;

public class SimpleNode implements Node
{
    protected Node parent;
    public SimpleNode[] children;
    public int id;
    protected PythonGrammar parser;
    public int endLine, endColumn, beginLine, beginColumn;
    Object info;
    public int aug_tmp1, aug_tmp2, aug_tmp3, aug_tmp4;

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
        info = parseString(s, quotes, beginLine, beginColumn);
    }

    public static String parseString(String s, int quotes, 
                                     int beginLine, int beginColumn) {
        //System.out.println("string: "+s);
        char quoteChar = s.charAt(0);
        int start=0;
        boolean ustring = false;
        if (quoteChar == 'u' || quoteChar == 'U') {
            ustring = true;
            start++;
        }  
        quoteChar = s.charAt(start);
        if (quoteChar == 'r' || quoteChar == 'R') {
            return s.substring(quotes+start+1, s.length()-quotes);
        } else {
            StringBuffer sb = new StringBuffer(s.length());
            char[] ca = s.toCharArray();
            int n = ca.length-quotes;
            int i=quotes+start;
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
                case 'u':
                    if (!ustring) {
                        sb.append('u');
                        break;
                    }
                    if (i+4 > n)
                        throw new TokenMgrError(
                               "Unicode-Escape decoding error: "+
                               "truncated \\uXXXX", beginLine, beginColumn);
                    char u=0;
                    for (int j = 0; j < 4; j++) {
                        int digit = Character.digit(ca[i++], 16);
                        if (digit == -1)
                            throw new TokenMgrError(
                                 "Unicode-Escape decoding error: "+
                                 "truncated \\uXXXX", beginLine, beginColumn);
                        u = (char)(u*16 + digit);
                    }
                    sb.append(u);
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
            return sb.toString();
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
                    throw Py.OverflowError("integer literal too large");
                }

                long l = Long.valueOf(s, radix).longValue();
                if (l > 0xffffffffl || (radix == 10 && l > Integer.MAX_VALUE))
                {
                    throw Py.OverflowError("integer literal too large");
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
        case PythonGrammarTreeConstants.JJTSINGLE_INPUT:
            return visitor.single_input(this);
        case PythonGrammarTreeConstants.JJTFILE_INPUT:
            return visitor.file_input(this);
        case PythonGrammarTreeConstants.JJTEVAL_INPUT:
            return visitor.eval_input(this);
        case PythonGrammarTreeConstants.JJTFUNCDEF:
            return visitor.funcdef(this);
        case PythonGrammarTreeConstants.JJTVARARGSLIST:
            return visitor.varargslist(this);
        case PythonGrammarTreeConstants.JJTEXTRAARGLIST:
            return visitor.ExtraArgList(this);
        case PythonGrammarTreeConstants.JJTEXTRAKEYWORDLIST:
            return visitor.ExtraKeywordList(this);
        case PythonGrammarTreeConstants.JJTDEFAULTARG:
            return visitor.defaultarg(this);
        case PythonGrammarTreeConstants.JJTFPLIST:
            return visitor.fplist(this);
        case PythonGrammarTreeConstants.JJTEXPR_STMT:
            return visitor.expr_stmt(this);
        case PythonGrammarTreeConstants.JJTPRINT_STMT:
            return visitor.print_stmt(this);
        case PythonGrammarTreeConstants.JJTPRINT_EXT:
            return visitor.print_ext(this);
        case PythonGrammarTreeConstants.JJTDEL_STMT:
            return visitor.del_stmt(this);
        case PythonGrammarTreeConstants.JJTPASS_STMT:
            return visitor.pass_stmt(this);
        case PythonGrammarTreeConstants.JJTBREAK_STMT:
            return visitor.break_stmt(this);
        case PythonGrammarTreeConstants.JJTCONTINUE_STMT:
            return visitor.continue_stmt(this);
        case PythonGrammarTreeConstants.JJTRETURN_STMT:
            return visitor.return_stmt(this);
        case PythonGrammarTreeConstants.JJTRAISE_STMT:
            return visitor.raise_stmt(this);
        case PythonGrammarTreeConstants.JJTIMPORT:
            return visitor.Import(this);
        case PythonGrammarTreeConstants.JJTIMPORTFROM:
            return visitor.ImportFrom(this);
        case PythonGrammarTreeConstants.JJTDOTTED_NAME:
            return visitor.dotted_name(this);
        case PythonGrammarTreeConstants.JJTGLOBAL_STMT:
            return visitor.global_stmt(this);
        case PythonGrammarTreeConstants.JJTEXEC_STMT:
            return visitor.exec_stmt(this);
        case PythonGrammarTreeConstants.JJTASSERT_STMT:
            return visitor.assert_stmt(this);
        case PythonGrammarTreeConstants.JJTIF_STMT:
            return visitor.if_stmt(this);
        case PythonGrammarTreeConstants.JJTWHILE_STMT:
            return visitor.while_stmt(this);
        case PythonGrammarTreeConstants.JJTFOR_STMT:
            return visitor.for_stmt(this);
        case PythonGrammarTreeConstants.JJTTRY_STMT:
            return visitor.try_stmt(this);
        case PythonGrammarTreeConstants.JJTEXCEPT_CLAUSE:
            return visitor.except_clause(this);
        case PythonGrammarTreeConstants.JJTSUITE:
            return visitor.suite(this);
        case PythonGrammarTreeConstants.JJTOR_BOOLEAN:
            return visitor.or_boolean(this);
        case PythonGrammarTreeConstants.JJTAND_BOOLEAN:
            return visitor.and_boolean(this);
        case PythonGrammarTreeConstants.JJTNOT_1OP:
            return visitor.not_1op(this);
        case PythonGrammarTreeConstants.JJTCOMPARISION:
            return visitor.comparision(this);
        case PythonGrammarTreeConstants.JJTLESS_CMP:
            return visitor.less_cmp(this);
        case PythonGrammarTreeConstants.JJTGREATER_CMP:
            return visitor.greater_cmp(this);
        case PythonGrammarTreeConstants.JJTEQUAL_CMP:
            return visitor.equal_cmp(this);
        case PythonGrammarTreeConstants.JJTGREATER_EQUAL_CMP:
            return visitor.greater_equal_cmp(this);
        case PythonGrammarTreeConstants.JJTLESS_EQUAL_CMP:
            return visitor.less_equal_cmp(this);
        case PythonGrammarTreeConstants.JJTNOTEQUAL_CMP:
            return visitor.notequal_cmp(this);
        case PythonGrammarTreeConstants.JJTIN_CMP:
            return visitor.in_cmp(this);
        case PythonGrammarTreeConstants.JJTNOT_IN_CMP:
            return visitor.not_in_cmp(this);
        case PythonGrammarTreeConstants.JJTIS_NOT_CMP:
            return visitor.is_not_cmp(this);
        case PythonGrammarTreeConstants.JJTIS_CMP:
            return visitor.is_cmp(this);
        case PythonGrammarTreeConstants.JJTOR_2OP:
            return visitor.or_2op(this);
        case PythonGrammarTreeConstants.JJTXOR_2OP:
            return visitor.xor_2op(this);
        case PythonGrammarTreeConstants.JJTAND_2OP:
            return visitor.and_2op(this);
        case PythonGrammarTreeConstants.JJTLSHIFT_2OP:
            return visitor.lshift_2op(this);
        case PythonGrammarTreeConstants.JJTRSHIFT_2OP:
            return visitor.rshift_2op(this);
        case PythonGrammarTreeConstants.JJTADD_2OP:
            return visitor.add_2op(this);
        case PythonGrammarTreeConstants.JJTSUB_2OP:
            return visitor.sub_2op(this);
        case PythonGrammarTreeConstants.JJTMUL_2OP:
            return visitor.mul_2op(this);
        case PythonGrammarTreeConstants.JJTDIV_2OP:
            return visitor.div_2op(this);
        case PythonGrammarTreeConstants.JJTMOD_2OP:
            return visitor.mod_2op(this);
        case PythonGrammarTreeConstants.JJTPOS_1OP:
            return visitor.pos_1op(this);
        case PythonGrammarTreeConstants.JJTNEG_1OP:
            return visitor.neg_1op(this);
        case PythonGrammarTreeConstants.JJTINVERT_1OP:
            return visitor.invert_1op(this);
        case PythonGrammarTreeConstants.JJTPOW_2OP:
            return visitor.pow_2op(this);
        case PythonGrammarTreeConstants.JJTCALL_OP:
            return visitor.Call_Op(this);
        case PythonGrammarTreeConstants.JJTINDEX_OP:
            return visitor.Index_Op(this);
        case PythonGrammarTreeConstants.JJTDOT_OP:
            return visitor.Dot_Op(this);
        case PythonGrammarTreeConstants.JJTTUPLE:
            return visitor.tuple(this);
        case PythonGrammarTreeConstants.JJTLIST:
            return visitor.list(this);
        case PythonGrammarTreeConstants.JJTDICTIONARY:
            return visitor.dictionary(this);
        case PythonGrammarTreeConstants.JJTSTR_1OP:
            return visitor.str_1op(this);
        case PythonGrammarTreeConstants.JJTSTRJOIN:
            return visitor.strjoin(this);
        case PythonGrammarTreeConstants.JJTLAMBDEF:
            return visitor.lambdef(this);
        case PythonGrammarTreeConstants.JJTELLIPSES:
            return visitor.Ellipses(this);
        case PythonGrammarTreeConstants.JJTSLICE:
            return visitor.Slice(this);
        case PythonGrammarTreeConstants.JJTCOLON:
            return visitor.Colon(this);
        case PythonGrammarTreeConstants.JJTCOMMA:
            return visitor.Comma(this);
        case PythonGrammarTreeConstants.JJTCLASSDEF:
            return visitor.classdef(this);
        case PythonGrammarTreeConstants.JJTARGLIST:
            return visitor.arglist(this);
        case PythonGrammarTreeConstants.JJTKEYWORD:
            return visitor.Keyword(this);
        case PythonGrammarTreeConstants.JJTINT:
            return visitor.Int(this);
        case PythonGrammarTreeConstants.JJTFLOAT:
            return visitor.Float(this);
        case PythonGrammarTreeConstants.JJTCOMPLEX:
            return visitor.Complex(this);
        case PythonGrammarTreeConstants.JJTNAME:
            return visitor.Name(this);
        case PythonGrammarTreeConstants.JJTSTRING:
            return visitor.String(this);
        case PythonGrammarTreeConstants.JJTAUG_PLUS:
            return visitor.aug_plus(this);
        case PythonGrammarTreeConstants.JJTAUG_MINUS:
            return visitor.aug_minus(this);
        case PythonGrammarTreeConstants.JJTAUG_MULTIPLY:
            return visitor.aug_multiply(this);
        case PythonGrammarTreeConstants.JJTAUG_DIVIDE:
            return visitor.aug_divide(this);
        case PythonGrammarTreeConstants.JJTAUG_MODULO:
            return visitor.aug_modulo(this);
        case PythonGrammarTreeConstants.JJTAUG_AND:
            return visitor.aug_and(this);
        case PythonGrammarTreeConstants.JJTAUG_OR:
            return visitor.aug_or(this);
        case PythonGrammarTreeConstants.JJTAUG_XOR:
            return visitor.aug_xor(this);
        case PythonGrammarTreeConstants.JJTAUG_LSHIFT:
            return visitor.aug_lshift(this);
        case PythonGrammarTreeConstants.JJTAUG_RSHIFT:
            return visitor.aug_rshift(this);
        case PythonGrammarTreeConstants.JJTAUG_POWER:
            return visitor.aug_power(this);
        case PythonGrammarTreeConstants.JJTLIST_ITER:
            return visitor.list_iter(this);
        default:
            throw new ParseException("Unexpected node: "+this);
        }

    }

}

