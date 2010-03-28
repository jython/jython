/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NType;

public class NUnaryOp extends NNode {

    static final long serialVersionUID = 4877088513200468108L;

    public NNode op;
    public NNode operand;

    public NUnaryOp(NNode op, NNode n) {
        this(op, n, 0, 1);
    }

    public NUnaryOp(NNode op, NNode n, int start, int end) {
        super(start, end);
        this.op = op;
        this.operand = n;
        addChildren(op, n);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        return setType(resolveExpr(operand, s));
    }

    @Override
    public String toString() {
        return "<UOp:" + op + ":" + operand + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(op, v);
            visitNode(operand, v);
        }
    }
}
