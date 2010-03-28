/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Indexer;
import org.python.indexer.Scope;
import org.python.indexer.types.NType;

import java.util.ArrayList;
import java.util.List;

public class NBlock extends NNode {

    static final long serialVersionUID = -9096405259154069107L;

    public List<NNode> seq;

    public NBlock(List<NNode> seq) {
        this(seq, 0, 1);
    }

    public NBlock(List<NNode> seq, int start, int end) {
        super(start, end);
        if (seq == null) {
            seq = new ArrayList<NNode>();
        }
        this.seq = seq;
        addChildren(seq);
    }

    @Override
    public NType resolve(Scope scope) throws Exception {
        for (NNode n : seq) {
            // XXX:  This works for inferring lambda return types, but needs
            // to be fixed for functions (should be union of return stmt types).
            NType returnType = resolveExpr(n, scope);
            if (returnType != Indexer.idx.builtins.None) {
                setType(returnType);
            }
        }
        return getType();
    }

    @Override
    public String toString() {
        return "<Block:" + seq + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNodeList(seq, v);
        }
    }
}
