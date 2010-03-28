/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnknownType;

import java.util.List;

public class NAssign extends NNode {

    static final long serialVersionUID = 928890389856851537L;

    public List<NNode> targets;
    public NNode rvalue;

    public NAssign(List<NNode> targets, NNode rvalue) {
        this(targets, rvalue, 0, 1);
    }

    public NAssign(List<NNode> targets, NNode rvalue, int start, int end) {
        super(start, end);
        this.targets = targets;
        this.rvalue = rvalue;
        addChildren(targets);
        addChildren(rvalue);
    }

    @Override
    public boolean bindsName() {
        return true;
    }

    @Override
    protected void bindNames(Scope s) throws Exception {
        NameBinder binder = NameBinder.make();
        for (NNode target : targets) {
            binder.bind(s, target, new NUnknownType());
        }
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        NType valueType = resolveExpr(rvalue, s);
        switch (targets.size()) {
            case 0:
                break;
            case 1:
                NameBinder.make().bind(s, targets.get(0), valueType);
                break;
            default:
                NameBinder.make().bind(s, targets, valueType);
                break;
        }
        return setType(valueType);
    }

    @Override
    public String toString() {
        return "<Assign:" + targets + "=" + rvalue + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNodeList(targets, v);
            visitNode(rvalue, v);
        }
    }
}
