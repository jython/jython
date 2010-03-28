/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NType;

public class NAssert extends NNode {

    static final long serialVersionUID = 7574732756076428388L;

    public NNode test;
    public NNode msg;

    public NAssert(NNode test, NNode msg) {
        this(test, msg, 0, 1);
    }

    public NAssert(NNode test, NNode msg, int start, int end) {
        super(start, end);
        this.test = test;
        this.msg = msg;
        addChildren(test, msg);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        resolveExpr(test, s);
        resolveExpr(msg, s);
        return getType();
    }

    @Override
    public String toString() {
        return "<Assert:" + test + ":" + msg + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(test, v);
            visitNode(msg, v);
        }
    }
}
