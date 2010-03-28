/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.NBinding;
import org.python.indexer.Scope;
import org.python.indexer.types.NFuncType;
import org.python.indexer.types.NListType;
import org.python.indexer.types.NTupleType;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnionType;
import org.python.indexer.types.NUnknownType;

public class NFor extends NNode {

    static final long serialVersionUID = 3228529969554646406L;

    public NNode target;
    public NNode iter;
    public NBlock body;
    public NBlock orelse;

    public NFor(NNode target, NNode iter, NBlock body, NBlock orelse) {
        this(target, iter, body, orelse, 0, 1);
    }

    public NFor(NNode target, NNode iter, NBlock body, NBlock orelse,
                int start, int end) {
        super(start, end);
        this.target = target;
        this.iter = iter;
        this.body = body;
        this.orelse = orelse;
        addChildren(target, iter, body, orelse);
    }

    @Override
    public boolean bindsName() {
        return true;
    }

    @Override
    protected void bindNames(Scope s) throws Exception {
        bindNames(s, target, NameBinder.make());
    }

    private void bindNames(Scope s, NNode target, NameBinder binder) throws Exception {
        if (target instanceof NName) {
            binder.bind(s, (NName)target, new NUnknownType());
            return;
        }
        if (target instanceof NSequence) {
            for (NNode n : ((NSequence)target).getElements()) {
                bindNames(s, n, binder);
            }
        }
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        NameBinder.make().bindIter(s, target, iter);

        if (body == null) {
            setType(new NUnknownType());
        } else {
            setType(resolveExpr(body, s));
        }
        if (orelse != null) {
            addType(resolveExpr(orelse, s));
        }
        return getType();
    }

    @Override
    public String toString() {
        return "<For:" + target + ":" + iter + ":" + body + ":" + orelse + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(target, v);
            visitNode(iter, v);
            visitNode(body, v);
            visitNode(orelse, v);
        }
    }
}
