/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

public class NPass extends NNode {

    static final long serialVersionUID = 3668786487029793620L;

    public NPass() {
    }

    public NPass(int start, int end) {
        super(start, end);
    }

    @Override
    public String toString() {
        return "<Pass>";
    }

    @Override
    public void visit(NNodeVisitor v) {
        v.visit(this);
    }
}
