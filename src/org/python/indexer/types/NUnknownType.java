/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.types;

public class NUnknownType extends NType {

    private NType pointer;

    public NUnknownType() {
        pointer = null;
    }

    public static void point(NType u, NType v) {
        if (u.isUnknownType()) {
            ((NUnknownType)u).pointer = v;
        }
    }

    public static NType follow(NType t) {
        if (t instanceof NUnknownType) {
            NUnknownType tv = (NUnknownType)t;
            if (tv.pointer != null) {
                return follow(tv.pointer);
            } else {
                return tv;
            }
        } else {
            return t;
        }
    }

    @Override
    public void printKids(CyclicTypeRecorder ctr, StringBuilder sb) {
        if (pointer == null) {
            sb.append("null");
        } else {
            pointer.print(ctr, sb);
        }
    }
}
