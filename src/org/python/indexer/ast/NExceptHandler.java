/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnknownType;

public class NExceptHandler extends NNode {

    static final long serialVersionUID = 6215262228266158119L;

    public NNode name;
    public NNode exceptionType;
    public NBlock body;

    public NExceptHandler(NNode name, NNode exceptionType, NBlock body) {
        this(name, exceptionType, body, 0, 1);
    }

    public NExceptHandler(NNode name, NNode exceptionType, NBlock body, int start, int end) {
        super(start, end);
        this.name = name;
        this.exceptionType = exceptionType;
        this.body = body;
        addChildren(name, exceptionType, body);
    }

    @Override
    public boolean bindsName() {
        return true;
    }

    @Override
    protected void bindNames(Scope s) throws Exception {
        if (name != null) {
            NameBinder.make().bind(s, name, new NUnknownType());
        }
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        NType typeval = new NUnknownType();
        if (exceptionType != null) {
            typeval = resolveExpr(exceptionType, s);
        }
        if (name != null) {
            NameBinder.make().bind(s, name, typeval);
        }
        if (body != null) {
            return setType(resolveExpr(body, s));
        } else {
            return setType(new NUnknownType());
        }
    }

    @Override
    public String toString() {
        return "<ExceptHandler:" + start() + ":" + name + ":" + exceptionType + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(name, v);
            visitNode(exceptionType, v);
            visitNode(body, v);
        }
    }
}
