// Copyright © Corporation for National Research Initiatives

package org.python.compiler;

import org.python.parser.*;
import java.util.*;

public class LocalsCompiler extends Visitor
{
    public Hashtable globals, locals;
    public Vector names;
    int mode;
    static final int GET = 0;
    static final int SET = 1;
    static final int DEL = 2;

    public LocalsCompiler() {
        mode = GET;
        globals = new Hashtable();
        locals = new Hashtable();
        names = new Vector();
    }

    public Object set(SimpleNode node) throws Exception {
        return set(node, SET);
    }

    public Object del(SimpleNode node) throws Exception {
        return set(node, DEL);
    }

    public Object set(SimpleNode node, int newmode) throws Exception {
        mode = newmode;
        try {
            node.visit(this);
        } catch (ParseException exc) {
            int line = node.beginLine;
            String xtra = "";
            if (line > 1) {
                xtra = " (line "+line+")";
            }
            String target = "operator";
            if (node.id == PythonGrammarTreeConstants.JJTCALL_OP) {
                target = "function call";
            }
            throw org.python.core.Py.SyntaxError("can't assign to "+
                                                 target+xtra);
        }
        mode = GET;
        return null;
    }

    public Object single_input(SimpleNode node) throws Exception {
        return suite(node);
    }

    public Object file_input(SimpleNode node) throws Exception {
        return suite(node);
    }

    public Object eval_input(SimpleNode node) throws Exception {
        return null;
    }

    public Object funcdef(SimpleNode node) throws Exception {
        return set(node.getChild(0));
    }

    public Object expr_stmt(SimpleNode node) throws Exception {
        int n = node.getNumChildren();
        for (int i=0; i<n-1; i++) {
            set(node.getChild(i));
        }
        return null;
    }

    public Object print_stmt(SimpleNode node) throws Exception {
        return null;
    }

    public Object del_stmt(SimpleNode node) throws Exception {
        int n = node.getNumChildren();
        for (int i=0; i<n; i++) {
            del(node.getChild(i));
        }
        return null;
    }

    public Object pass_stmt(SimpleNode node) throws Exception {
        return null;
    }

    public Object break_stmt(SimpleNode node) throws Exception {
        return null;
    }

    public Object continue_stmt(SimpleNode node) throws Exception {
        return null;
    }

    public Object return_stmt(SimpleNode node) throws Exception {
        return null;
    }

    public Object raise_stmt(SimpleNode node) throws Exception {
        return null;
    }

    public Object Import(SimpleNode node) throws Exception {
        int n = node.getNumChildren();
        for (int i=0; i<n; i++) {
            set(node.getChild(i).getChild(0));
        }
        return null;
    }

    public Object ImportFrom(SimpleNode node) throws Exception {
        int n = node.getNumChildren();
        for (int i=1; i<n; i++) {
            set(node.getChild(i));
        }
        return null;
    }

    public Object global_stmt(SimpleNode node) throws Exception {
        int n = node.getNumChildren();
        for (int i=0; i<n; i++) {
            Object name = node.getChild(i).getInfo();
            globals.put(name, name);
        }
        return null;
    }

    public Object exec_stmt(SimpleNode node) throws Exception {
        //Disable locals somehow here?
        return null;
    }

    public Object assert_stmt(SimpleNode node) throws Exception {
        return null;
    }

    public Object if_stmt(SimpleNode node) throws Exception {
        int n = node.getNumChildren();
        for (int i=0; i<n; i++) {
            if (i%2 == 1 || i == n-1)
                node.getChild(i).visit(this);
        }
        return null;
    }

    public Object while_stmt(SimpleNode node) throws Exception {
        node.getChild(1).visit(this);
        if (node.getNumChildren() == 3) {
            node.getChild(2).visit(this);
        }
        return null;
    }

    public Object for_stmt(SimpleNode node) throws Exception {
        set(node.getChild(0));

        node.getChild(2).visit(this);

        if (node.getNumChildren() > 3) {
            //Do else clause if provided
            node.getChild(3).visit(this);
        }
        return null;
    }

    public Object try_stmt(SimpleNode node) throws Exception {
        int n = node.getNumChildren();
        for (int i=0; i<n; i++) {
            if (i%2 == 1 && i != n-1) {
                if (node.getChild(i).getNumChildren() == 2) {
                    set(node.getChild(i).getChild(1));
                }
                continue;
            }
            node.getChild(i).visit(this);
        }
        return null;
    }

    public Object except_clause(SimpleNode node) throws Exception {
        throw new ParseException("Unhandled Node: "+node);
    }

    public Object suite(SimpleNode node) throws Exception {
        int n = node.getNumChildren();
        for (int i=0; i<n; i++) {
            node.getChild(i).visit(this);
        }
        return null;
    }

    public Object Index_Op(SimpleNode node) throws Exception {
        return null;
    }

    public Object Dot_Op(SimpleNode node) throws Exception {
        return null;
    }

    public Object tuple(SimpleNode node) throws Exception {
        if (mode == SET) {
            int n = node.getNumChildren();
            for (int i=0; i<n; i++) {
                if (node.getChild(i).id != PythonGrammarTreeConstants.JJTCOMMA)
                {
                    set(node.getChild(i));
                }
            }
        } else if (mode == DEL) {
            int n = node.getNumChildren();
            for (int i=0; i<n; i++) {
                if (node.getChild(i).id != PythonGrammarTreeConstants.JJTCOMMA)
                {
                    del(node.getChild(i));
                }
            }
        }
        return null;
    }

    public Object fplist(SimpleNode node) throws Exception {
        return tuple(node);
    }

    public Object list(SimpleNode node) throws Exception {
        return tuple(node);
    }

    public Object classdef(SimpleNode node) throws Exception {
        return funcdef(node);
    }

    public void addLocal(String name) {
        if (locals.get(name) == null) {
            names.addElement(name);
            locals.put(name, new Integer(locals.size()));
        }
    }

    public Object Name(SimpleNode node) throws Exception {
        if (mode == SET) {
            Object name = node.getInfo();
            if (globals.get(name) == null) {
                addLocal((String)name);
            }
        }
        return null;
    }
}
