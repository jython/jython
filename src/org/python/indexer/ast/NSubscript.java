/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Indexer;
import org.python.indexer.Scope;
import org.python.indexer.types.NFuncType;
import org.python.indexer.types.NListType;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnknownType;

public class NSubscript extends NNode {

    static final long serialVersionUID = -493854491438387425L;

    public NNode value;
    public NNode slice;  // an NIndex or NSlice

    public NSubscript(NNode value, NNode slice) {
        this(value, slice, 0, 1);
    }

    public NSubscript(NNode value, NNode slice, int start, int end) {
        super(start, end);
        this.value = value;
        this.slice = slice;
        addChildren(value, slice);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        NType vt = resolveExpr(value, s);
        NType st = resolveExpr(slice, s);

        // slicing
        if (vt.isUnknownType()) {
            if (st.isListType()) {
                return setType(vt);
            }
            return setType(new NUnknownType());
        }

        if (st.isListType()) {
            NType getslice_type = vt.getTable().lookupTypeAttr("__getslice__");
            if (getslice_type == null) {
                addError("The type can't be sliced: " + vt);
                return setType(new NUnknownType());
            }
            if (!getslice_type.isFuncType()) {
                addError("The type's __getslice__ method is not a function: "
                                       + getslice_type);
                return setType(new NUnknownType());
            }
            return setType(getslice_type.asFuncType().getReturnType().follow());
        }

        // subscription
        if (slice instanceof NIndex) {
            if (vt.isListType()) {
                warnUnlessNumIndex(st);
                return setType(vt.asListType().getElementType());
            }
            if (vt.isTupleType()) {
                warnUnlessNumIndex(st);
                return setType(vt.asTupleType().toListType().getElementType());
            }
            if (vt.isStrType()) {
                warnUnlessNumIndex(st);
                return setType(Indexer.idx.builtins.BaseStr);
            }
            // XXX:  unicode, buffer, xrange

            if (vt.isDictType()) {
                if (!st.follow().equals(vt.asDictType().getKeyType())) {
                    addWarning("Possible KeyError (wrong type for subscript)");
                }
                return setType(vt.asDictType().getValueType());  // infer it regardless
            }
            // else fall through
        }

        // subscription via delegation
        if (vt.isUnionType()) {
            for (NType u : vt.asUnionType().getTypes()) {
                NType gt = vt.getTable().lookupTypeAttr("__getitem__");
                if (gt != null) {
                    return setType(get__getitem__type(gt, gt));
                }
            }
        }
        NType gt = vt.getTable().lookupTypeAttr("__getitem__");
        return setType(get__getitem__type(gt, vt));
    }

    private void warnUnlessNumIndex(NType subscriptType) {
        NType follow = subscriptType.follow();
        if (!follow.isNumType() && !follow.isUnknownType()) {
            addWarning("Possible KeyError: subscript should be a number; found " + follow);
        }
    }

    private NType get__getitem__type(NType gt, NType vt) {
        if (gt == null) {
            addError("indexing type without __getitem__ method: " + vt);
            return new NUnknownType();
        }
        if (!gt.isFuncType()) {
            addError("The type's __getitem__ method is not a function: " + gt);
            return new NUnknownType();
        }
        return gt.asFuncType().getReturnType().follow();
    }

    @Override
    public String toString() {
        return "<Subscript:" + value + ":" + slice + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(value, v);
            visitNode(slice, v);
        }
    }
}
