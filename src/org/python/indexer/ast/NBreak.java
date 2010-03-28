/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

public class NBreak extends NNode {

    static final long serialVersionUID = 2114759731430768793L;

    public NBreak() {
    }

    public NBreak(int start, int end) {
        super(start, end);
    }

    @Override
    public String toString() {
        return "<Break>";
    }

    @Override
    public void visit(NNodeVisitor v) {
        v.visit(this);
    }
}
