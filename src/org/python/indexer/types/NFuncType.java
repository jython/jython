/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.types;

import org.python.indexer.Indexer;

public class NFuncType extends NType {

    private NType returnType;

    public NFuncType() {
        this(new NUnknownType());
    }

    public NFuncType(NType from, NType to) {
        this(to);
    }

    public NFuncType(NType from1, NType from2, NType to) {
        this(to);
    }

    public NFuncType(NType to) {
        setReturnType(to);
        getTable().addSuper(Indexer.idx.builtins.BaseFunction.getTable());
        getTable().setPath(Indexer.idx.builtins.BaseFunction.getTable().getPath());
    }

    public void setReturnType(NType to) {
        if (to == null) {
            to = new NUnknownType();
        }
        this.returnType = to;
    }

    public NType getReturnType() {
        return returnType;
    }

    @Override
    public void printKids(CyclicTypeRecorder ctr, StringBuilder sb) {
        sb.append("_:");  // XXX:  placeholder for fromType
        returnType.print(ctr, sb);
    }
}
