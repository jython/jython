/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import java.util.ArrayList;
import java.util.List;

public abstract class NSequence extends NNode {

    static final long serialVersionUID = 7996591535766850065L;

    public List<NNode> elts;

    public NSequence(List<NNode> elts) {
        this(elts, 0, 1);
    }

    public NSequence(List<NNode> elts, int start, int end) {
        super(start, end);
        this.elts = (elts != null) ? elts : new ArrayList<NNode>();
        addChildren(elts);
    }

    public List<NNode> getElements() {
        return elts;
    }
}
