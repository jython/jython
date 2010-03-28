/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NType;

public class NIndex extends NNode {

    static final long serialVersionUID = -8920941673115420849L;

    public NNode value;

    public NIndex(NNode n) {
        this(n, 0, 1);
    }

    public NIndex(NNode n, int start, int end) {
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
        return "<Index:" + value + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(value, v);
        }
    }
}
