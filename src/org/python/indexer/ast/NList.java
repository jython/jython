/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.ast.NNode;
import org.python.indexer.types.NListType;
import org.python.indexer.types.NType;

import java.util.List;

public class NList extends NSequence {

    static final long serialVersionUID = 6623743056841822992L;

    public NList(List<NNode> elts) {
        this(elts, 0, 1);
    }

    public NList(List<NNode> elts, int start, int end) {
        super(elts, start, end);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        if (elts.size() == 0) {
            return setType(new NListType());  // list<unknown>
        }

        NListType listType = null;
        for (NNode elt : elts) {
            if (listType == null) {
                listType = new NListType(resolveExpr(elt, s));
            } else {
                listType.add(resolveExpr(elt, s));
            }
        }
        if (listType != null) {
            setType(listType);
        }
        return getType();
    }

    @Override
    public String toString() {
        return "<List:" + start() + ":" + elts + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNodeList(elts, v);
        }
    }
}
