/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.types;

import org.python.indexer.Indexer;
import org.python.indexer.NBinding;
import org.python.indexer.ast.NUrl;

import java.util.ArrayList;
import java.util.List;

public class NTupleType extends NType {

    private List<NType> eltTypes;

    public NTupleType() {
        this.eltTypes = new ArrayList<NType>();
        getTable().addSuper(Indexer.idx.builtins.BaseTuple.getTable());
        getTable().setPath(Indexer.idx.builtins.BaseTuple.getTable().getPath());
    }

    public NTupleType(List<NType> eltTypes) {
        this();
        this.eltTypes = eltTypes;
    }

    public NTupleType(NType elt0) {
        this();
        this.eltTypes.add(elt0);
    }

    public NTupleType(NType elt0, NType elt1) {
        this();
        this.eltTypes.add(elt0);
        this.eltTypes.add(elt1);
    }

    public NTupleType(NType... types) {
        this();
        for (NType type : types) {
            this.eltTypes.add(type);
        }
    }

    public void setElementTypes(List<NType> eltTypes) {
      this.eltTypes = eltTypes;
    }

    public List<NType> getElementTypes() {
      return eltTypes;
    }

    public void add(NType elt) {
        eltTypes.add(elt);
    }

    public NType get(int i) {
        return eltTypes.get(i);
    }

    public NListType toListType() {
        NListType t = new NListType();
        for (NType e : eltTypes) {
            t.add(e);
        }
        return t;
    }

    @Override
    public void printKids(CyclicTypeRecorder ctr, StringBuilder sb) {
        sb.append("[");
        for (NType elt : eltTypes) {
            elt.print(ctr, sb);
            sb.append(",");
        }
        sb.setLength(sb.length() - 1);  // pop last comma
        sb.append("]");
    }
}
