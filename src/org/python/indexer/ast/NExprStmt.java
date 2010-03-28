/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NType;

/**
 * Expression statement.
 */
public class NExprStmt extends NNode {

    static final long serialVersionUID = 7366113211576923188L;

    public NNode value;

    public NExprStmt(NNode n) {
        this(n, 0, 1);
    }

    public NExprStmt(NNode n, int start, int end) {
        super(start, end);
        this.value = n;
        addChildren(n);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        resolveExpr(value, s);
        return getType();
    }

    @Override
    public String toString() {
        return "<ExprStmt:" + value + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(value, v);
        }
    }
}
