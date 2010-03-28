/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NListType;
import org.python.indexer.types.NType;

public class NYield extends NNode {

    static final long serialVersionUID = 2639481204205358048L;

    public NNode value;

    public NYield(NNode n) {
        this(n, 0, 1);
    }

    public NYield(NNode n, int start, int end) {
        super(start, end);
        this.value = n;
        addChildren(n);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        return setType(new NListType(resolveExpr(value, s)));
    }

    @Override
    public String toString() {
        return "<Yield:" + start() + ":" + value + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(value, v);
        }
    }
}
