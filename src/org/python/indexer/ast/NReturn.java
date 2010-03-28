/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NType;

public class NReturn extends NNode {

    static final long serialVersionUID = 5795610129307339141L;

    public NNode value;

    public NReturn(NNode n) {
        this(n, 0, 1);
    }

    public NReturn(NNode n, int start, int end) {
        super(start, end);
        this.value = n;
        addChildren(n);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        return setType(resolveExpr(value, s));
    }

    @Override
    public String toString() {
        return "<Return:" + value + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(value, v);
        }
    }
}
