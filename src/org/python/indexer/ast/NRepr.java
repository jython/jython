/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Indexer;
import org.python.indexer.Scope;
import org.python.indexer.types.NType;

public class NRepr extends NNode {

    static final long serialVersionUID = -7920982714296311413L;

    public NNode value;

    public NRepr(NNode n) {
        this(n, 0, 1);
    }

    public NRepr(NNode n, int start, int end) {
        super(start, end);
        this.value = n;
        addChildren(n);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        resolveExpr(value, s);
        return setType(Indexer.idx.builtins.BaseStr);
    }

    @Override
    public String toString() {
        return "<Repr:" + value +  ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(value, v);
        }
    }
}
