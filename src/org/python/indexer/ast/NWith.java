/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NType;

public class NWith extends NNode {

    static final long serialVersionUID = 560128079414064421L;

    public NNode optional_vars;
    public NNode context_expr;
    public NBlock body;

    public NWith(NNode optional_vars, NNode context_expr, NBlock body) {
        this(optional_vars, context_expr, body, 0, 1);
    }

    public NWith(NNode optional_vars, NNode context_expr, NBlock body, int start, int end) {
        super(start, end);
        this.optional_vars = optional_vars;
        this.context_expr = context_expr;
        this.body = body;
        addChildren(optional_vars, context_expr, body);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        NType val = resolveExpr(context_expr, s);
        NameBinder.make().bind(s, optional_vars, val);
        return setType(resolveExpr(body, s));
    }

    @Override
    public String toString() {
        return "<With:" + context_expr + ":" + optional_vars + ":" + body + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(optional_vars, v);
            visitNode(context_expr, v);
            visitNode(body, v);
        }
    }
}
