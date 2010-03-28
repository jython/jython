/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Indexer;
import org.python.indexer.Scope;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnknownType;
import org.python.indexer.types.NUnionType;

public class NBinOp extends NNode {

    static final long serialVersionUID = -8961832251782335108L;

    public NNode left;
    public NNode right;
    public String op;

    public NBinOp(NNode target, NNode value, String op) {
        this(target, value, op, 0, 1);
    }

    public NBinOp(NNode target, NNode value, String op, int start, int end) {
        super(start, end);
        this.left = target;
        this.right = value;
        this.op = op;
        addChildren(target, value);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        NType ltype = null, rtype = null;
        if (left != null) {
            ltype = resolveExpr(left, s).follow();
        }
        if (right != null) {
            rtype = resolveExpr(right, s).follow();
        }

        // If either non-null operand is a string, assume the result is a string.
        if (ltype == Indexer.idx.builtins.BaseStr || rtype == Indexer.idx.builtins.BaseStr) {
            return setType(Indexer.idx.builtins.BaseStr);
        }
        // If either non-null operand is a number, assume the result is a number.
        if (ltype == Indexer.idx.builtins.BaseNum || rtype == Indexer.idx.builtins.BaseNum) {
            return setType(Indexer.idx.builtins.BaseNum);
        }

        if (ltype == null) {
            return setType(rtype == null ? new NUnknownType() : rtype);
        }

        if (rtype == null) {
            return setType(ltype == null ? new NUnknownType() : ltype);
        }

        return setType(NUnionType.union(ltype, rtype));
    }

    @Override
    public String toString() {
        return "<BinOp:" + left + " " + op + " " + right + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(left, v);
            visitNode(right, v);
        }
    }
}
