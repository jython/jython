/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NType;

public class NExec extends NNode {

    static final long serialVersionUID = -1840017898177850339L;

    public NNode body;
    public NNode globals;
    public NNode locals;

    public NExec(NNode body, NNode globals, NNode locals) {
        this(body, globals, locals, 0, 1);
    }

    public NExec(NNode body, NNode globals, NNode locals, int start, int end) {
        super(start, end);
        this.body = body;
        this.globals = globals;
        this.locals = locals;
        addChildren(body, globals, locals);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        resolveExpr(body, s);
        resolveExpr(globals, s);
        resolveExpr(locals, s);
        return getType();
    }

    @Override
    public String toString() {
        return "<Exec:" + start() + ":" + end() + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(body, v);
            visitNode(globals, v);
            visitNode(locals, v);
        }
    }
}
