// Copyright (c) Corporation for National Research Initiatives

package org.python.compiler;

import java.util.ArrayList;
import java.util.List;

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
    public List<exprType> defaults;
    public List<String> names;
    public List<String> fpnames;
    public List<stmtType> init_code;

    public ArgListCompiler() {
        arglist = keywordlist = false;
        defaults = null;
        names = new ArrayList<String>();
        fpnames = new ArrayList<String>();
        init_code = new ArrayList<stmtType>();
    }

    public void reset() {
        arglist = keywordlist = false;
        defaults = null;
        names.clear();
        init_code.clear();
    }

    public void appendInitCode(Suite node) {
        int n = node.body.size();
        List<stmtType> newtree = new ArrayList<stmtType>();
        newtree.addAll(init_code);
        newtree.addAll(node.body);
        node.body = newtree;
    }

    public List<exprType> getDefaults() {
        return defaults;
    }

    public void visitArgs(argumentsType args) throws Exception {
        for (int i = 0; i < args.args.size(); i++) {
            String name = (String) visit(args.args.get(i));
            names.add(name);
            if (args.args.get(i) instanceof Tuple) {
                List<exprType> targets = new ArrayList<exprType>();
                targets.add(args.args.get(i));
                Assign ass = new Assign(args.args.get(i),
                    targets,
                    new Name(args.args.get(i), name, expr_contextType.Load));
                init_code.add(ass);
            }
        }
        if (args.vararg != null) {
            arglist = true;
            names.add(args.vararg);
        }
        if (args.kwarg != null) {
            keywordlist = true;
            names.add(args.kwarg);
        }
        
        defaults = args.defaults;
        for (int i = 0; i < defaults.size(); i++) {
            if (defaults.get(i) == null)
                throw new ParseException(
                    "non-default argument follows default argument",
                    args.args.get(args.args.size() - defaults.size() + i));
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
        fpnames.add(node.id);
        return node.id;
    }

    @Override
    public Object visitTuple(Tuple node) throws Exception {
        StringBuffer name = new StringBuffer("(");
        int n = node.elts.size();
        for (int i = 0; i < n-1; i++) {
            name.append(visit(node.elts.get(i)));
            name.append(", ");
        }
        name.append(visit(node.elts.get(n - 1)));
        name.append(")");
        return name.toString();
    }
}
