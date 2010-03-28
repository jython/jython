/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.types;

import org.python.indexer.Indexer;
import org.python.indexer.NBinding;
import org.python.indexer.ast.NUrl;

public class NDictType extends NType {

    private NType keyType;
    private NType valueType;

    public NDictType() {
        this(new NUnknownType(), new NUnknownType());
    }

    public NDictType(NType key0, NType val0) {
        keyType = key0;
        valueType = val0;
        getTable().addSuper(Indexer.idx.builtins.BaseDict.getTable());
        getTable().setPath(Indexer.idx.builtins.BaseDict.getTable().getPath());
    }

    public void setKeyType(NType keyType) {
      this.keyType = keyType;
    }

    public NType getKeyType() {
      return keyType;
    }

    public void setValueType(NType valType) {
      this.valueType = valType;
    }

    public NType getValueType() {
      return valueType;
    }

    public void add(NType key, NType val) {
        keyType = NUnionType.union(keyType, key);
        valueType = NUnionType.union(valueType, val);
    }

    public NTupleType toTupleType(int n) {
        NTupleType ret = new NTupleType();
        for (int i = 0; i < n; i++) {
            ret.add(keyType);
        }
        return ret;
    }

    @Override
    public void printKids(CyclicTypeRecorder ctr, StringBuilder sb) {
        keyType.print(ctr, sb);
        sb.append(":");
        valueType.print(ctr, sb);
    }
}
