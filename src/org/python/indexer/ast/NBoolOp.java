/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnionType;
import org.python.indexer.types.NUnknownType;

import java.util.List;

/**
 * Represents the "and"/"or" operators.
 */
public class NBoolOp extends NNode {

    static final long serialVersionUID = -5261954056600388069L;

    public enum OpType { AND, OR, UNDEFINED }

    OpType op;
    public List<NNode> values;

    public NBoolOp(OpType op, List<NNode> values) {
        this(op, values, 0, 1);
    }

    public NBoolOp(OpType op, List<NNode> values, int start, int end) {
        super(start, end);
        this.op = op;
        this.values = values;
        addChildren(values);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        if (op == OpType.AND) {
            NType last = null;
            for (NNode e : values) {
                last = resolveExpr(e, s);
            }
            return setType(last == null ? new NUnknownType() : last);
        }

        // OR
        return setType(resolveListAsUnion(values, s));
    }

    @Override
    public String toString() {
        return "<BoolOp:" + op + ":" + values + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNodeList(values, v);
        }
    }
}
