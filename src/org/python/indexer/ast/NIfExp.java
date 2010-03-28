/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NType;

public class NIfExp extends NNode {

    static final long serialVersionUID = 8516153579808365723L;

    public NNode test;
    public NNode body;
    public NNode orelse;

    public NIfExp(NNode test, NNode body, NNode orelse) {
        this(test, body, orelse, 0, 1);
    }

    public NIfExp(NNode test, NNode body, NNode orelse, int start, int end) {
        super(start, end);
        this.test = test;
        this.body = body;
        this.orelse = orelse;
        addChildren(test, body, orelse);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        resolveExpr(test, s);
        if (body != null) {
            setType(resolveExpr(body, s));
        }
        if (orelse != null) {
            addType(resolveExpr(orelse, s));
        }
        return getType();
    }

    @Override
    public String toString() {
        return "<IfExp:" + start() + ":" + test + ":" + body + ":" + orelse + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(test, v);
            visitNode(body, v);
            visitNode(orelse, v);
        }
    }
}
