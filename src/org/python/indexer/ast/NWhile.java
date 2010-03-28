/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnionType;

public class NWhile extends NNode {

    static final long serialVersionUID = -2419753875936526587L;

    public NNode test;
    public NBlock body;
    public NBlock orelse;

    public NWhile(NNode test, NBlock body, NBlock orelse) {
        this(test, body, orelse, 0, 1);
    }

    public NWhile(NNode test, NBlock body, NBlock orelse, int start, int end) {
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
        return "<While:" + test + ":" + body + ":" + orelse + ":" + start() + ">";
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
