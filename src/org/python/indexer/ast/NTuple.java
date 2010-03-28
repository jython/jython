/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NTupleType;
import org.python.indexer.types.NType;

import java.util.List;

public class NTuple extends NSequence {

    static final long serialVersionUID = -7647425038559142921L;

    public NTuple(List<NNode> elts) {
        this(elts, 0, 1);
    }

    public NTuple(List<NNode> elts, int start, int end) {
        super(elts, start, end);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        NTupleType thisType = new NTupleType();
        for (NNode e : elts) {
            thisType.add(resolveExpr(e, s));
        }
        return setType(thisType);
    }

    @Override
    public String toString() {
        return "<Tuple:" + start() + ":" + elts + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNodeList(elts, v);
        }
    }
}
