/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

public class NContinue extends NNode {

    static final long serialVersionUID = 1646681898280823606L;

    public NContinue() {
    }

    public NContinue(int start, int end) {
        super(start, end);
    }

    @Override
    public String toString() {
        return "<Continue>";
    }

    @Override
    public void visit(NNodeVisitor v) {
        v.visit(this);
    }
}
