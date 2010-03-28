/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NListType;
import org.python.indexer.types.NType;

import java.util.List;

public class NGeneratorExp extends NNode {

    static final long serialVersionUID = -8614142736962193509L;

    public NNode elt;
    public List<NComprehension> generators;

    public NGeneratorExp(NNode elt, List<NComprehension> generators) {
        this(elt, generators, 0, 1);
    }

    public NGeneratorExp(NNode elt, List<NComprehension> generators, int start, int end) {
        super(start, end);
        this.elt = elt;
        this.generators = generators;
        addChildren(elt);
        addChildren(generators);
    }

    /**
     * Python's list comprehension will erase any variable used in generators.
     * This is wrong, but we "respect" this bug here.
     */
    @Override
    public NType resolve(Scope s) throws Exception {
        resolveList(generators, s);
        return setType(new NListType(resolveExpr(elt, s)));
    }

    @Override
    public String toString() {
        return "<GeneratorExp:" + start() + ":" + elt + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(elt, v);
            visitNodeList(generators, v);
        }
    }
}
