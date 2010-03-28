/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

public class NEllipsis extends NNode {

    static final long serialVersionUID = 4148534089952252511L;

    public NEllipsis() {
    }

    public NEllipsis(int start, int end) {
        super(start, end);
    }

    @Override
    public String toString() {
        return "<Ellipsis>";
    }

    @Override
    public void visit(NNodeVisitor v) {
        v.visit(this);
    }
}
