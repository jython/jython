/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.types;

import org.python.indexer.Indexer;
import org.python.indexer.NBinding;
import org.python.indexer.ast.NUrl;

public class NListType extends NType {

    private NType eltType;

    public NListType() {
        this(new NUnknownType());
    }

    public NListType(NType elt0) {
        eltType = elt0;
        getTable().addSuper(Indexer.idx.builtins.BaseList.getTable());
        getTable().setPath(Indexer.idx.builtins.BaseList.getTable().getPath());
    }

    public void setElementType(NType eltType) {
      this.eltType = eltType;
    }

    /**
     * Returns the type of the elements.  You should wrap the result
     * with {@link NUnknownType#follow} to get to the actual type.
     */
    public NType getElementType() {
      return eltType;
    }

    public void add(NType another) {
        eltType = NUnionType.union(eltType, another);
    }

    public NTupleType toTupleType(int n) {
        NTupleType ret = new NTupleType();
        for (int i = 0; i < n; i++) {
            ret.add(eltType);
        }
        return ret;
    }

    @Override
    public void printKids(CyclicTypeRecorder ctr, StringBuilder sb) {
        eltType.print(ctr, sb);
    }
}
