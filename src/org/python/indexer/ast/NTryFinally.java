/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnionType;

public class NTryFinally extends NNode {

    static final long serialVersionUID = 136428581711609107L;

    public NBlock body;
    public NBlock finalbody;

    public NTryFinally(NBlock body, NBlock orelse) {
        this(body, orelse, 0, 1);
    }

    public NTryFinally(NBlock body, NBlock orelse, int start, int end) {
        super(start, end);
        this.body = body;
        this.finalbody = orelse;
        addChildren(body, orelse);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        if (body != null) {
            setType(resolveExpr(body, s));
        }
        if (finalbody != null) {
            addType(resolveExpr(finalbody, s));
        }
        return getType();
    }

    @Override
    public String toString() {
        return "<TryFinally:" + body + ":" + finalbody + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(body, v);
            visitNode(finalbody, v);
        }
    }
}
