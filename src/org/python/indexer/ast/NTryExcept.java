/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnionType;

import java.util.List;

public class NTryExcept extends NNode {

    static final long serialVersionUID = 7210847998428480831L;

    public List<NExceptHandler> handlers;
    public NBlock body;
    public NBlock orelse;

    public NTryExcept(List<NExceptHandler> handlers, NBlock body, NBlock orelse) {
        this(handlers, body, orelse, 0, 1);
    }

    public NTryExcept(List<NExceptHandler> handlers, NBlock body, NBlock orelse,
                      int start, int end) {
        super(start, end);
        this.handlers = handlers;
        this.body = body;
        this.orelse = orelse;
        addChildren(handlers);
        addChildren(body, orelse);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        resolveList(handlers, s);
        if (body != null) {
            setType(resolveExpr(body, s));
        }
        if (orelse != null) {
            addType(resolveExpr(orelse, s));
        }
        return getType();
    }

    @Override
    public String toString() {
        return "<TryExcept:" + handlers + ":" + body + ":" + orelse + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNodeList(handlers, v);
            visitNode(body, v);
            visitNode(orelse, v);
        }
    }
}
