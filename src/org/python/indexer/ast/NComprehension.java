/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnknownType;

import java.util.List;

public class NComprehension extends NNode {

    static final long serialVersionUID = -598250664243757218L;

    public NNode target;
    public NNode iter;
    public List<NNode> ifs;

    public NComprehension(NNode target, NNode iter, List<NNode> ifs) {
        this(target, iter, ifs, 0, 1);
    }

    public NComprehension(NNode target, NNode iter, List<NNode> ifs, int start, int end) {
        super(start, end);
        this.target = target;
        this.iter = iter;
        this.ifs = ifs;
        addChildren(target, iter);
        addChildren(ifs);
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
        resolveList(ifs, s);
        return setType(target.getType());
    }

    @Override
    public String toString() {
        return "<Comprehension:" + start() + ":" + target + ":" + iter + ":" + ifs + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(target, v);
            visitNode(iter, v);
            visitNodeList(ifs, v);
        }
    }
}
