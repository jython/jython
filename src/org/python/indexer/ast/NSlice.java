/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NListType;
import org.python.indexer.types.NType;

public class NSlice extends NNode {

    static final long serialVersionUID = 8685364390631331543L;

    public NNode lower;
    public NNode step;
    public NNode upper;

    public NSlice(NNode lower, NNode step, NNode upper) {
        this(lower, step, upper, 0, 1);
    }

    public NSlice(NNode lower, NNode step, NNode upper, int start, int end) {
        super(start, end);
        this.lower = lower;
        this.step = step;
        this.upper = upper;
        addChildren(lower, step, upper);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        resolveExpr(lower, s);
        resolveExpr(step, s);
        resolveExpr(upper, s);
        return setType(new NListType());
    }

    @Override
    public String toString() {
        return "<Slice:" + lower + ":" + step + ":" + upper + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(lower, v);
            visitNode(step, v);
            visitNode(upper, v);
        }
    }
}
