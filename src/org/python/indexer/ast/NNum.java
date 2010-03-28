/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Indexer;
import org.python.indexer.Scope;
import org.python.indexer.types.NType;

public class NNum extends NNode {

    static final long serialVersionUID = -425866329526788376L;

    public Object n;

    public NNum(int n) {
        this.n = n;
    }

    public NNum(Object n, int start, int end) {
        super(start, end);
        this.n = n;
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        return setType(Indexer.idx.builtins.BaseNum);
    }

    @Override
    public String toString() {
        return "<Num:" + n + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        v.visit(this);
    }
}
