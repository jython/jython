/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NType;

/**
 * Represents a keyword argument (name=value) in a function call.
 */
public class NKeyword extends NNode {

    static final long serialVersionUID = 9031782645918578266L;

    public String arg;
    public NNode value;

    public NKeyword(String arg, NNode value) {
        this(arg, value, 0, 1);
    }

    public NKeyword(String arg, NNode value, int start, int end) {
        super(start, end);
        this.arg = arg;
        this.value = value;
        addChildren(value);
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        return setType(resolveExpr(value, s));
    }

    @Override
    public String toString() {
        return "<Keyword:" + arg + ":" + value + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(value, v);
        }
    }
}
