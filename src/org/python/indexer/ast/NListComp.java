/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NListType;
import org.python.indexer.types.NType;

import java.util.List;

public class NListComp extends NNode {

    static final long serialVersionUID = -150205687457446323L;

    public NNode elt;
    public List<NComprehension> generators;

    public NListComp(NNode elt, List<NComprehension> generators) {
        this(elt, generators, 0, 1);
    }

    public NListComp(NNode elt, List<NComprehension> generators, int start, int end) {
        super(start, end);
        this.elt = elt;
        this.generators = generators;
        addChildren(elt);
        addChildren(generators);
    }

    /**
     * Python's list comprehension will bind the variables used in generators.
     * This will erase the original values of the variables even after the
     * comprehension.
     */
    @Override
    public NType resolve(Scope s) throws Exception {
        NameBinder binder = NameBinder.make();
        resolveList(generators, s);
        return setType(new NListType(resolveExpr(elt, s)));
    }

    @Override
    public String toString() {
        return "<NListComp:" + start() + ":" + elt + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(elt, v);
            visitNodeList(generators, v);
        }
    }
}
