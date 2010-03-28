/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Indexer;
import org.python.indexer.Scope;
import org.python.indexer.types.NType;

import java.util.List;

public class NCompare extends NNode {

    static final long serialVersionUID = 1013460919393985064L;

    public NNode left;
    public List<NNode> ops;
    public List<NNode> comparators;

    public NCompare(NNode left, List<NNode> ops, List<NNode> comparators) {
        this(left, ops, comparators, 0, 1);
    }

    public NCompare(NNode left, List<NNode> ops, List<NNode> comparators, int start, int end) {
        super(start, end);
        this.left = left;
        this.ops = ops;
        this.comparators = comparators;
        addChildren(left);
        addChildren(ops);
        addChildren(comparators);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        setType(Indexer.idx.builtins.BaseNum);
        resolveExpr(left, s);
        resolveList(comparators, s);
        return getType();
    }

    @Override
    public String toString() {
        return "<Compare:" + left + ":" + ops + ":" + comparators + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(left, v);
            visitNodeList(ops, v);
            visitNodeList(comparators, v);
        }
    }
}
