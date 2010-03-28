/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NDictType;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnionType;
import org.python.indexer.types.NUnknownType;

import java.util.List;

public class NDict extends NNode {

    static final long serialVersionUID = 318144953740238374L;

    public List<NNode> keys;
    public List<NNode> values;

    public NDict(List<NNode> keys, List<NNode> values) {
        this(keys, values, 0, 1);
    }

    public NDict(List<NNode> keys, List<NNode> values, int start, int end) {
        super(start, end);
        this.keys = keys;
        this.values = values;
        addChildren(keys);
        addChildren(values);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        NType keyType = resolveListAsUnion(keys, s);
        NType valType = resolveListAsUnion(values, s);
        return setType(new NDictType(keyType, valType));
    }

    @Override
    public String toString() {
        return "<Dict>";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            // XXX:  should visit in alternating order
            visitNodeList(keys, v);
            visitNodeList(values, v);
        }
    }
}
