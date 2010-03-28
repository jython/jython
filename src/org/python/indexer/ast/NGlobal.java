/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Indexer;
import org.python.indexer.NBinding;
import org.python.indexer.Scope;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnknownType;

import java.util.List;

public class NGlobal extends NNode {

    static final long serialVersionUID = 5978320165592263568L;

    public List<NName> names;

    public NGlobal(List<NName> names) {
        this(names, 0, 1);
    }

    public NGlobal(List<NName> names, int start, int end) {
        super(start, end);
        this.names = names;
        addChildren(names);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        Scope moduleTable = s.getGlobalTable();
        for (NName name : names) {
            if (s.isGlobalName(name.id)) {
                continue;  // already bound by this (or another) global stmt
            }
            s.addGlobalName(name.id);
            NBinding b = moduleTable.lookup(name);
            if (b == null) {
                b = moduleTable.put(name.id, null, new NUnknownType(), NBinding.Kind.SCOPE);
            }
            Indexer.idx.putLocation(name, b);
        }
        return getType();
    }

    @Override
    public String toString() {
        return "<Global:" + names + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNodeList(names, v);
        }
    }
}
