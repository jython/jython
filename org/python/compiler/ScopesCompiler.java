// (C) Copyright 2001 Samuele Pedroni

package org.python.compiler;

import org.python.parser.*;
import java.util.*;

public class ScopesCompiler extends Visitor implements ScopeConstants {

    private CompilationContext code_compiler;

    private boolean nested_scopes = false;

    private Stack scopes;
    private ScopeInfo cur = null;

    private int mode;
    private static final int GET=0;
    private static final int SET=1;
    private static final int DEL=2;
    private static final int AUGSET=4;

    private int level      = 0;
    private int func_level = 0;

    public ScopesCompiler(CompilationContext code_compiler) {
        this.code_compiler = code_compiler;
        scopes = new Stack();
        mode = GET;
        nested_scopes = code_compiler.getFutures().areNestedScopesOn();
        // System.err.println("nested-scopes: "+nested_scopes);
    }

    public Object set(SimpleNode node) throws Exception {
        return modal(node,SET);
    }

    public Object del(SimpleNode node) throws Exception {
        return modal(node,DEL);
    }


    public Object augset(SimpleNode node) throws Exception {
        return modal(node,AUGSET);
    }

    public Object modal(SimpleNode node, int newmode)throws Exception {
        mode = newmode;
        node.visit(this);
        mode = GET;
        return null;
    }

    public void beginScope(String name, int kind, SimpleNode node,
                           ArgListCompiler ac)
    {
        if (cur != null) {
            scopes.push(cur);
        }
        if (kind == FUNCSCOPE) func_level++;
        node.scope = cur = new ScopeInfo(name, node, level++, kind,
                                         func_level, ac, nested_scopes);
    }

    public void endScope() throws Exception {
        if (cur.kind == FUNCSCOPE) func_level--;
        level--;
        ScopeInfo up = (!scopes.empty())?(ScopeInfo)scopes.pop():null;
        cur.cook(up,code_compiler);
        cur.dump(); // dbg
        cur = up;
    }

    public void parse(SimpleNode node) throws Exception {
        try {
            node.visit(this);
        } catch(Throwable t) {
            throw org.python.core.parser.fixParseError(null, t,
                    code_compiler.getFilename());
        }
    }

    public Object single_input(SimpleNode node) throws Exception {
        beginScope("<single-top>",TOPSCOPE,node,null);
        suite(node);
        endScope();
        return null;
    }

    public Object file_input(SimpleNode node) throws Exception {
        beginScope("<file-top>",TOPSCOPE,node,null);
        suite(node);
        endScope();
        return null;
    }

    public Object eval_input(SimpleNode node) throws Exception {
        beginScope("<eval-top>",TOPSCOPE,node,null);
        return_stmt(node);
        endScope();
        return null;
    }

    private String def(SimpleNode node) {
        String name = (String)node.getChild(0).getInfo();
        cur.addBound(name);
        return name;
    }

    public Object funcdef(SimpleNode node) throws Exception {
        String my_name = def(node);
        ArgListCompiler ac = new ArgListCompiler();
        SimpleNode suite;
        if (node.getNumChildren() == 3) {
            suite = node.getChild(2);
            //Parse arguments
            node.getChild(1).visit(ac);
        } else {
            suite = node.getChild(1);
        }
        SimpleNode[] defaults = ac.getDefaults();
        int defc = defaults.length;
        for(int i=0; i<defc; i++) {
            defaults[i].visit(this);
        }
        beginScope(my_name,FUNCSCOPE,node,ac);
        int n = ac.names.size();
        for (int i=0; i<n; i++) {
            cur.addParam((String)ac.names.elementAt(i));
        }
        ac.init_code.visit(this);
        cur.markFromParam();
        suite.visit(this);
        endScope();
        return null;
    }

    public Object expr_stmt(SimpleNode node) throws Exception {
        int n = node.getNumChildren();
        node.getChild(n-1).visit(this);
        for (int i=0; i<n-1; i++) {
            set(node.getChild(i));
        }
        return null;
    }

    public Object print_ext(SimpleNode node) throws Exception {
        node.getChild(0).visit(this);
        return null;
    }

    public Object print_stmt(SimpleNode node) throws Exception {
        int n = node.getNumChildren();
        if ( n > 0 ) {
            for (int i=0; i<n-1; i++) {
                node.getChild(i).visit(this);
            }
            if(node.getChild(n-1).id != PythonGrammarTreeConstants.JJTCOMMA)
                node.getChild(n-1).visit(this);
        }
        return null;
    }

    public Object del_stmt(SimpleNode node) throws Exception {
        int n = node.getNumChildren();
        for (int i=0; i<n; i++) {
            del(node.getChild(i));
        }
        return null;
    }

    public Object pass_stmt(SimpleNode n) throws Exception {
        return null;
    }

    public Object break_stmt(SimpleNode n) throws Exception {
        return null;
    }

    public Object continue_stmt(SimpleNode n) throws Exception {
        return null;
    }

    public Object return_stmt(SimpleNode node) throws Exception {
        if (node.getNumChildren() == 1) node.getChild(0).visit(this);
        return null;
    }

    public void stmt(SimpleNode node) throws Exception {
        int n = node.getNumChildren();
        for (int i=0; i<n; i++) node.getChild(i).visit(this);
    }

    public Object raise_stmt(SimpleNode node) throws Exception {
        stmt(node);
        return null;
    }

    public Object Import(SimpleNode node) throws Exception {
        int n = node.getNumChildren();
        for(int i = 0; i < n; i++) {
            SimpleNode imp = node.getChild(i);
            switch(imp.id) {
            case PythonGrammarTreeConstants.JJTDOTTED_NAME:
                cur.addBound((String)imp.getChild(0).getInfo());
                break;
            case PythonGrammarTreeConstants.JJTDOTTED_AS_NAME:
                cur.addBound((String)imp.getChild(1).getInfo());
                break;
            }
        }
        return null;
    }

    public Object ImportFrom(SimpleNode node) throws Exception {
        Future.checkFromFuture(node); // future stmt support
        int n = node.getNumChildren();
        if (n == 1) {
            cur.from_import_star = true;
            return null;
        }
        for (int i = 1; i < n; i++) {
            SimpleNode imp = node.getChild(i);
            switch(imp.id) {
            case PythonGrammarTreeConstants.JJTNAME:
                cur.addBound((String)imp.getInfo());
                break;
            case PythonGrammarTreeConstants.JJTIMPORT_AS_NAME:
                cur.addBound((String)imp.getChild(1).getInfo());
                break;
            }
        }

        return null;
    }

    public Object global_stmt(SimpleNode node) throws Exception {
        int n = node.getNumChildren();
        for(int i = 0; i < n; i++) {
            String name = (String)node.getChild(i).getInfo();
            int prev = cur.addGlobal(name);
            if (prev >= 0) {
                if ((prev&FROM_PARAM) != 0)
                    code_compiler.error("name '"+name+"' is local and global",
                                        true,node);
                if ((prev&GLOBAL) != 0) continue;
                String what;
                if ((prev&BOUND) != 0) what = "assignment"; else what = "use";
                code_compiler.error("name '"+name+"' declared global after "+
                                    what,false,node);
            }
        }
        return null;
    }

    public Object exec_stmt(SimpleNode node) throws Exception {
        cur.exec = true;
        int n = node.getNumChildren();
        if (n == 1) cur.unqual_exec = true;
        for (int i = 0; i < n; i++) node.getChild(i).visit(this);
        return null;
    }

    public Object assert_stmt(SimpleNode node) throws Exception {
        stmt(node);
        return null;
    }

    public Object if_stmt(SimpleNode node) throws Exception {
        stmt(node);
        return null;
    }

    public Object while_stmt(SimpleNode node) throws Exception {
        stmt(node);
        return null;
    }

    public Object for_stmt(SimpleNode node) throws Exception {
        if (mode != GET) illassign(node);
        set(node.getChild(0));
        node.getChild(1).visit(this);
        node.getChild(2).visit(this);
        if (node.getNumChildren()>3) node.getChild(3).visit(this);
        return null;
    }

    public Object try_stmt(SimpleNode node) throws Exception {
        int n = node.getNumChildren();
        for (int i=0; i<n; i++) {
            if (i%2 == 1 && i != n-1) {
                switch(node.getChild(i).getNumChildren()) {
                case 2:
                    set(node.getChild(i).getChild(1));
                case 1:
                    node.getChild(i).getChild(0).visit(this);
                }
                continue;
            }
            node.getChild(i).visit(this);
        }
        return null;
    }

    public Object suite(SimpleNode node) throws Exception {
        stmt(node);
        return null;
    }

    private static void illassign(SimpleNode node) throws Exception {
        String target = "operator";
        if (node.id == PythonGrammarTreeConstants.JJTCALL_OP) {
            target = "function call";
        } else if ((node.id == PythonGrammarTreeConstants.JJTFOR_STMT)) {
            target = "list comprehension";
        }
        throw new ParseException("can't assign to "+target,node);
    }

    public void binaryop(SimpleNode node) throws Exception {
        if (mode != GET) illassign(node);
        node.getChild(0).visit(this);
        node.getChild(1).visit(this);
    }

    public void unaryop(SimpleNode node) throws Exception {
        if (mode != GET) illassign(node);
        node.getChild(0).visit(this);
    }


    public Object or_boolean(SimpleNode node) throws Exception {
        binaryop(node);
        return null;
    }

    public Object and_boolean(SimpleNode node) throws Exception {
        binaryop(node);
        return null;
    }

    public Object not_1op(SimpleNode node) throws Exception {
        unaryop(node);
        return null;
    }

    public Object comparision(SimpleNode node) throws Exception {
        if (mode != GET) illassign(node);
        int n = node.getNumChildren();
        for (int i=0; i<n; i++) {
            if (i%2 == 0) node.getChild(i).visit(this);
        }
        return null;
    }

    public Object or_2op(SimpleNode node) throws Exception {
        binaryop(node);
        return null;
    }

    public Object xor_2op(SimpleNode node) throws Exception {
        binaryop(node);
        return null;
    }

    public Object and_2op(SimpleNode node) throws Exception {
        binaryop(node);
        return null;
    }

    public Object lshift_2op(SimpleNode node) throws Exception {
        binaryop(node);
        return null;
    }

    public Object rshift_2op(SimpleNode node) throws Exception {
        binaryop(node);
        return null;
    }

    public Object add_2op(SimpleNode node) throws Exception {
        binaryop(node);
        return null;
    }

    public Object sub_2op(SimpleNode node) throws Exception {
        binaryop(node);
        return null;
    }

    public Object mul_2op(SimpleNode node) throws Exception {
        binaryop(node);
        return null;
    }

    public Object div_2op(SimpleNode node) throws Exception {
        binaryop(node);
        return null;
    }

    public Object floordiv_2op(SimpleNode node) throws Exception {
        binaryop(node);
        return null;
    }

    public Object mod_2op(SimpleNode node) throws Exception {
        binaryop(node);
        return null;
    }

    public Object pos_1op(SimpleNode node) throws Exception {
        unaryop(node);
        return null;
    }

    public Object neg_1op(SimpleNode node) throws Exception {
        unaryop(node);
        return null;
    }

    public Object invert_1op(SimpleNode node) throws Exception {
        unaryop(node);
        return null;
    }

    public Object pow_2op(SimpleNode node) throws Exception {
        binaryop(node);
        return null;
    }

    public Object str_1op(SimpleNode node) throws Exception {
        unaryop(node);
        return null;
    }

    public Object strjoin(SimpleNode node) throws Exception {
        binaryop(node);
        return null;
    }

    public Object Call_Op(SimpleNode node) throws Exception {
        if (mode != GET) illassign(node);
        node.getChild(0).visit(this);
        if (node.getNumChildren()>1) {
            SimpleNode args=node.getChild(1);
            int n = args.getNumChildren();
            for (int i=0; i<n; i++) {
                SimpleNode arg = args.getChild(i);
                switch(arg.id) {
                case PythonGrammarTreeConstants.JJTKEYWORD:
                    arg.getChild(1).visit(this);
                    break;
                case PythonGrammarTreeConstants.JJTEXTRAARGVALUELIST:
                case PythonGrammarTreeConstants.JJTEXTRAKEYWORDVALUELIST:
                    arg.getChild(0).visit(this);
                    break;
                default:
                    arg.visit(this);
                }
            }
        }
        return null;
    }

    public Object Index_Op(SimpleNode node) throws Exception {
        int prevmode= mode;
        mode = GET;
        node.getChild(0).visit(this);
        node.getChild(1).visit(this);
        mode = prevmode;
        return null;
    }

    public Object Dot_Op(SimpleNode node) throws Exception {
        int prevmode = mode;
        mode = GET;
        node.getChild(0).visit(this);
        mode = prevmode;
        return null;
    }

    public Object tuple(SimpleNode node) throws Exception {
        if (mode ==AUGSET) {
            throw new ParseException(
            "augmented assign to tuple not possible", node);
        }
        int n = node.getNumChildren();
        if (n > 0) {
            for (int i=0; i<n-1; i++) node.getChild(i).visit(this);
            if (node.getChild(n-1).id != PythonGrammarTreeConstants.JJTCOMMA)
                node.getChild(n-1).visit(this);
        }
        return null;
    }

    public Object fplist(SimpleNode node) throws Exception {
        return list(node);
    }

    public Object list(SimpleNode node) throws Exception {
        if (mode ==AUGSET) {
            throw new ParseException(
            "augmented assign to list not possible", node);
        }
        int n = node.getNumChildren();
        if (n > 0) {
            for (int i=0; i<n-1; i++) node.getChild(i).visit(this);
            if (node.getChild(n-1).id != PythonGrammarTreeConstants.JJTCOMMA)
                node.getChild(n-1).visit(this);
        }
        return null;
    }

    public Object list_iter(SimpleNode node) throws Exception {
        if (node.getNumChildren() == 1) node.getChild(0).visit(this);
        return null;
    }

    public Object dictionary(SimpleNode node) throws Exception {
        if (mode != GET) illassign(node);
        stmt(node);
        return null;
    }

    public Object lambdef(SimpleNode node) throws Exception {
        if (mode != GET) illassign(node);
        ArgListCompiler ac = new ArgListCompiler();
        SimpleNode expr;
        if (node.getNumChildren() == 2) {
            expr = node.getChild(1);
            //Parse arguments
            node.getChild(0).visit(ac);
        } else {
            expr = node.getChild(0);
        }
        SimpleNode[] defaults = ac.getDefaults();
        int defc = defaults.length;
        for(int i=0; i<defc; i++) {
            defaults[i].visit(this);
        }
        beginScope("<lambda>",FUNCSCOPE,node,ac);
        int n = ac.names.size();
        for (int i=0; i<n; i++) {
            cur.addParam((String)ac.names.elementAt(i));
        }
        ac.init_code.visit(this);
        cur.markFromParam();
        expr.visit(this);
        endScope();
        return null;
    }

    public Object Ellipses(SimpleNode n) throws Exception {
        return null;
    }

    public Object Slice(SimpleNode node) throws Exception {
        int n = node.getNumChildren();
        for (int i=0; i<n; i++) {
            SimpleNode snode = node.getChild(i);
            if (snode.id != PythonGrammarTreeConstants.JJTCOLON) {
                snode.visit(this);
            }
        }
        return null;
    }

    public Object classdef(SimpleNode node) throws Exception {
        String cl_name = def(node);
        int n = node.getNumChildren();
        SimpleNode suite = node.getChild(n-1);
        for (int i=1; i<n-1; i++) node.getChild(i).visit(this);
        beginScope(cl_name,CLASSSCOPE,node,null);
        suite.visit(this);
        endScope();
        return null;
    }

    public Object Int(SimpleNode node) throws Exception {
        if (mode != GET) illassign(node);
        return null;
    }

    public Object Float(SimpleNode node) throws Exception {
        if (mode != GET) illassign(node);
        return null;
    }

    public Object Complex(SimpleNode node) throws Exception {
        if (mode != GET) illassign(node);
        return null;
    }

    public Object Name(SimpleNode node) throws Exception {
        String name = (String)node.getInfo();
        if ( mode != GET) {
            if (name.equals("__debug__"))
                code_compiler.error("can not assign to __debug__",false,node);
            cur.addBound(name);
        }
        else cur.addUsed(name);
        return null;
    }

    public Object String(SimpleNode node) throws Exception {
        if (mode != GET) illassign(node);
        return null;
    }

    public void aug_assign(SimpleNode node) throws Exception {
        augset(node.getChild(0));
        node.getChild(1).visit(this);
    }

    public Object aug_plus(SimpleNode node) throws Exception {
        aug_assign(node);
        return null;
    }

    public Object aug_minus(SimpleNode node) throws Exception {
        aug_assign(node);
        return null;
    }

    public Object aug_multiply(SimpleNode node) throws Exception {
        aug_assign(node);
        return null;
    }

    public Object aug_divide(SimpleNode node) throws Exception {
        aug_assign(node);
        return null;
    }

    public Object aug_floordivide(SimpleNode node) throws Exception {
        aug_assign(node);
        return null;
    }

    public Object aug_modulo(SimpleNode node) throws Exception {
        aug_assign(node);
        return null;
    }

    public Object aug_and(SimpleNode node) throws Exception {
        aug_assign(node);
        return null;
    }

    public Object aug_or(SimpleNode node) throws Exception {
        aug_assign(node);
        return null;
    }

    public Object aug_xor(SimpleNode node) throws Exception {
        aug_assign(node);
        return null;
    }

    public Object aug_lshift(SimpleNode node) throws Exception {
        aug_assign(node);
        return null;
    }

    public Object aug_rshift(SimpleNode node) throws Exception {
        aug_assign(node);
        return null;
    }

    public Object aug_power(SimpleNode node) throws Exception {
        aug_assign(node);
        return null;
    }

}
