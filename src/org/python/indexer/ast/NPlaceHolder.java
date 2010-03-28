/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

public class NPlaceHolder extends NNode {

    static final long serialVersionUID = -8732894605739403419L;

    public NPlaceHolder() {
    }

    public NPlaceHolder(int start, int end) {
        super(start, end);
    }

    @Override
    public String toString() {
        return "<PlaceHolder:" + start() + ":" + end() + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        v.visit(this);
    }
}
