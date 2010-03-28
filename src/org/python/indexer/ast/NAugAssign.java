/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NType;

public class NAugAssign extends NNode {

    static final long serialVersionUID = -6479618862099506199L;

    public NNode target;
    public NNode value;
    public String op;

    public NAugAssign(NNode target, NNode value, String op) {
        this(target, value, op, 0, 1);
    }

    public NAugAssign(NNode target, NNode value, String op, int start, int end) {
        super(start, end);
        this.target = target;
        this.value = value;
        this.op = op;
        addChildren(target, value);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        resolveExpr(target, s);
        return setType(resolveExpr(value, s));
    }

    @Override
    public String toString() {
        return "<AugAssign:" + target + " " + op + "= " + value + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(target, v);
            visitNode(value, v);
        }
    }
}
