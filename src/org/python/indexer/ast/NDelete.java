/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NType;

import java.util.List;

public class NDelete extends NNode {

    static final long serialVersionUID = -2223255555054110766L;

    public List<NNode> targets;

    public NDelete(List<NNode> elts) {
        this(elts, 0, 1);
    }

    public NDelete(List<NNode> elts, int start, int end) {
        super(start, end);
        this.targets = elts;
        addChildren(elts);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        for (NNode n : targets) {
            resolveExpr(n, s);
            if (n instanceof NName) {
                s.remove(((NName)n).id);
            }
        }
        return getType();
    }

    @Override
    public String toString() {
        return "<Delete:" + targets + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNodeList(targets, v);
        }
    }
}
