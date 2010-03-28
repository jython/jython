/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NClassType;
import org.python.indexer.types.NFuncType;
import org.python.indexer.types.NInstanceType;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnionType;
import org.python.indexer.types.NUnknownType;

import java.util.ArrayList;
import java.util.List;

public class NCall extends NNode {

    static final long serialVersionUID = 5212954751978100639L;

    public NNode func;
    public List<NNode> args;
    public List<NKeyword> keywords;
    public NNode kwargs;
    public NNode starargs;

    public NCall(NNode func, List<NNode> args, List<NKeyword> keywords,
                 NNode kwargs, NNode starargs) {
        this(func, args, keywords, kwargs, starargs, 0, 1);
    }

    public NCall(NNode func, List<NNode> args, List<NKeyword> keywords,
                 NNode kwargs, NNode starargs, int start, int end) {
        super(start, end);
        this.func = func;
        this.args = args;
        this.keywords = keywords;
        this.kwargs = kwargs;
        this.starargs = starargs;
        addChildren(func, kwargs, starargs);
        addChildren(args);
        addChildren(keywords);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        NType ft = resolveExpr(func, s);
        List<NType> argTypes = new ArrayList<NType>();
        for (NNode a : args) {
            argTypes.add(resolveExpr(a, s));
        }
        resolveList(keywords, s);
        resolveExpr(starargs, s);
        resolveExpr(kwargs, s);

        if (ft.isClassType()) {
            return setType(ft);  // XXX:  was new NInstanceType(ft)
        }

        if (ft.isFuncType()) {
            return setType(ft.asFuncType().getReturnType().follow());
        }

        if (ft.isUnknownType()) {
            NUnknownType to = new NUnknownType();
            NFuncType at = new NFuncType(to);
            NUnionType.union(ft, at);
            return setType(to);
        }

        addWarning("calling non-function " + ft);
        return setType(new NUnknownType());
    }

    @Override
    public String toString() {
        return "<Call:" + func + ":" + args + ":" + start() + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(func, v);
            visitNodeList(args, v);
            visitNodeList(keywords, v);
            visitNode(kwargs, v);
            visitNode(starargs, v);
        }
    }
}
