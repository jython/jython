// Copyright (c) Corporation for National Research Initiatives

package org.python.compiler;

import org.python.parser.*;
import java.io.IOException;
import java.util.Vector;
import java.util.Enumeration;

public class ArgListCompiler extends org.python.parser.Visitor
{
    public boolean arglist, keywordlist;
    public Vector defaults;
    public Vector names;
    public SimpleNode init_code;

    public ArgListCompiler() {
        arglist = keywordlist = false;
        defaults = new Vector();
        names = new Vector();
        init_code = new SimpleNode(PythonGrammarTreeConstants.JJTSUITE);
    }

    public void reset() {
        arglist = keywordlist = false;
        defaults.removeAllElements();
        names.removeAllElements();
        //init_code.removeAllElements();
    }

    public SimpleNode[] getDefaults() {
        SimpleNode[] children = new SimpleNode[defaults.size()];
        for (int i=0; i<children.length; i++) {
            children[i] = (SimpleNode)defaults.elementAt(i);
        }
        return children;
    }

    public Object varargslist(SimpleNode node) throws Exception {
        int n = node.getNumChildren();
        for (int i=0; i<n; i++) {
            node.getChild(i).visit(this);
        }
        return null;
    }

    public Object ExtraArgList(SimpleNode node) throws Exception {
        arglist = true;
        names.addElement(node.getChild(0).visit(this));
        return null;
    }

    public Object ExtraKeywordList(SimpleNode node) throws Exception {
        keywordlist = true;
        names.addElement(node.getChild(0).visit(this));
        return null;
    }

    public Object defaultarg(SimpleNode node) throws Exception {
        Object name = node.getChild(0).visit(this);
        // make sure the named argument isn't already in the list of arguments
        for (Enumeration e=names.elements(); e.hasMoreElements();) {
            String objname = (String)e.nextElement();
            if (objname.equals(name))
                throw new ParseException("duplicate argument name found: " +
                                         name, node);
        }
        names.addElement(name);
        
        //Handle tuple arguments properly
        if (node.getChild(0).id == PythonGrammarTreeConstants.JJTFPLIST) {
            SimpleNode expr = new SimpleNode(
                PythonGrammarTreeConstants.JJTEXPR_STMT);
            // Set the right line number for this expr
            expr.beginLine = node.beginLine;
            expr.jjtAddChild(node.getChild(0), 0);
            SimpleNode nm = new SimpleNode(PythonGrammarTreeConstants.JJTNAME);
            nm.setInfo(name);
            expr.jjtAddChild(nm, 1);
            init_code.jjtAddChild(expr, init_code.getNumChildren());
        }

        // Handle default args if specified
        if (node.getNumChildren() > 1) {
            defaults.addElement(node.getChild(1));
        } else {
            if (defaults.size() > 0)
                throw new ParseException(
                    "non-default argument follows default argument");
        }
        return null;
    }

    public Object fplist(SimpleNode node) throws Exception {
        String name = "(";
        int n = node.getNumChildren();
        for (int i=0; i<n-1; i++) {
            name = name+node.getChild(i).visit(this)+", ";
        }
        name = name+node.getChild(n-1).visit(this)+")";
        return name;
    }

    public Object Name(SimpleNode node) throws ParseException, IOException {
        return node.getInfo();
    }
}
