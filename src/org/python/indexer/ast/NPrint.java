/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NType;

import java.util.List;

public class NPrint extends NNode {

    static final long serialVersionUID = 689872588518071148L;

    public NNode dest;
    public List<NNode> values;

    public NPrint(NNode dest, List<NNode> elts) {
        this(dest, elts, 0, 1);
    }

    public NPrint(NNode dest, List<NNode> elts, int start, int end) {
        super(start, end);
        this.dest = dest;
        this.values = elts;
        addChildren(dest);
        addChildren(elts);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        resolveExpr(dest, s);
        resolveList(values, s);
        return getType();
    }

    @Override
    public String toString() {
        return "<Print:" + values + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(dest, v);
            visitNodeList(values, v);
        }
    }
}
