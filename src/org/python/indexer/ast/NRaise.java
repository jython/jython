/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NType;

public class NRaise extends NNode {

    static final long serialVersionUID = 5384576775167988640L;

    public NNode exceptionType;
    public NNode inst;
    public NNode traceback;

    public NRaise(NNode exceptionType, NNode inst, NNode traceback) {
        this(exceptionType, inst, traceback, 0, 1);
    }

    public NRaise(NNode exceptionType, NNode inst, NNode traceback, int start, int end) {
        super(start, end);
        this.exceptionType = exceptionType;
        this.inst = inst;
        this.traceback = traceback;
        addChildren(exceptionType, inst, traceback);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        resolveExpr(exceptionType, s);
        resolveExpr(inst, s);
        resolveExpr(traceback, s);
        return getType();
    }

    @Override
    public String toString() {
        return "<Raise:" + traceback + ":" + exceptionType + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(exceptionType, v);
            visitNode(inst, v);
            visitNode(traceback, v);
        }
    }
}
