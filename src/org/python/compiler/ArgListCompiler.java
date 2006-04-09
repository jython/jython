// Copyright (c) Corporation for National Research Initiatives

package org.python.compiler;

import java.util.Vector;

import org.python.parser.ParseException;
import org.python.parser.PythonGrammarTreeConstants;
import org.python.parser.Visitor;
import org.python.parser.ast.Assign;
import org.python.parser.ast.Name;
import org.python.parser.ast.Suite;
import org.python.parser.ast.Tuple;
import org.python.parser.ast.argumentsType;
import org.python.parser.ast.exprType;
import org.python.parser.ast.stmtType;

public class ArgListCompiler extends Visitor
    implements PythonGrammarTreeConstants
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
                Assign ass = new Assign(
                    new exprType[] { args.args[i] },
                    new Name(name, Name.Load, args.args[i]), args.args[i]);
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

    public Object visitName(Name node) throws Exception {
        if (node.ctx != Name.Store) 
            return null;
        
        if (fpnames.contains(node.id)) {
            throw new ParseException("duplicate argument name found: " +
                                     node.id, node);
        }
        fpnames.addElement(node.id);
        return node.id;
    }

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
