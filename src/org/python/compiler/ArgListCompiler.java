// Copyright (c) Corporation for National Research Initiatives

package org.python.compiler;

import java.util.Vector;

import org.python.antlr.ParseException;
import org.python.antlr.Visitor;
import org.python.antlr.ast.Assign;
import org.python.antlr.ast.Name;
import org.python.antlr.ast.Suite;
import org.python.antlr.ast.Tuple;
import org.python.antlr.ast.argumentsType;
import org.python.antlr.ast.expr_contextType;
import org.python.antlr.ast.exprType;
import org.python.antlr.ast.stmtType;

public class ArgListCompiler extends Visitor
{
    public boolean arglist, keywordlist;
    public exprType[] defaults;
    public Vector names;
    public Vector fpnames;
    public Vector init_code;

    public ArgListCompiler() {
        arglist = keywordlist = false;
        defaults = null;
        names = new Vector();
        fpnames = new Vector();
        init_code = new Vector();
    }

    public void reset() {
        arglist = keywordlist = false;
        defaults = null;
        names.removeAllElements();
        init_code.removeAllElements();
    }

    public void appendInitCode(Suite node) {
        int n = node.body.length;
        stmtType[] newtree = new stmtType[init_code.size() + n];
        init_code.copyInto(newtree);
        System.arraycopy(node.body, 0, newtree, init_code.size(), n);
        node.body = newtree;
    }

    public exprType[] getDefaults() {
        return defaults;
    }

    public void visitArgs(argumentsType args) throws Exception {
        for (int i = 0; i < args.args.length; i++) {
            String name = (String) visit(args.args[i]);
            names.addElement(name);
            if (args.args[i] instanceof Tuple) {
                Assign ass = new Assign(args.args[i],
                    new exprType[] { args.args[i] },
                    new Name(args.args[i], name, expr_contextType.Load));
                init_code.addElement(ass);
            }
        }
        if (args.vararg != null) {
            arglist = true;
            names.addElement(args.vararg);
        }
        if (args.kwarg != null) {
            keywordlist = true;
            names.addElement(args.kwarg);
        }
        
        defaults = args.defaults;
        for (int i = 0; i < defaults.length; i++) {
            if (defaults[i] == null)
                throw new ParseException(
                    "non-default argument follows default argument",
                    args.args[args.args.length - defaults.length + i]);
        }
    }

    @Override
    public Object visitName(Name node) throws Exception {
        //FIXME: do we need Store and Param, or just Param?
        if (node.ctx != expr_contextType.Store && node.ctx != expr_contextType.Param) {
            return null;
        } 

        if (fpnames.contains(node.id)) {
            throw new ParseException("duplicate argument name found: " +
                                     node.id, node);
        }
        fpnames.addElement(node.id);
        return node.id;
    }

    @Override
    public Object visitTuple(Tuple node) throws Exception {
        StringBuffer name = new StringBuffer("(");
        int n = node.elts.length;
        for (int i = 0; i < n-1; i++) {
            name.append(visit(node.elts[i]));
            name.append(", ");
        }
        name.append(visit(node.elts[n - 1]));
        name.append(")");
        return name.toString();
    }
}
