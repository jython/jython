/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Indexer;
import org.python.indexer.Scope;
import org.python.indexer.types.NType;

import java.util.List;

/**
 * An {@link NBlock} variant used for the bodies of functions, lambdas,
 * classes and modules.
 */
public class NBody extends NBlock {

    static final long serialVersionUID = 1518962862898927516L;

    public NBody(NBlock block) {
        this(block == null ? null : block.seq);
    }

    public NBody(List<NNode> seq) {
        super(seq);
    }

    public NBody(List<NNode> seq, int start, int end) {
        super(seq, start, end);
    }

    private class GlobalFinder extends DefaultNodeVisitor {
        private Scope scope;  // starting scope

        public GlobalFinder(Scope scope) {
            this.scope = scope;
        }

        @Override
        public boolean visit(NGlobal n) {
            resolveExpr(n, scope);
            return false;
        }

        // Do not descend into new scopes.
        @Override
        public boolean visit(NFunctionDef n) {
            return false;
        }
        @Override
        public boolean visit(NLambda n) {
            return false;
        }
        @Override
        public boolean visit(NClassDef n) {
            return false;
        }
    }

    @Override
    public NType resolve(Scope scope) throws Exception {
        try {
            scope.setNameBindingPhase(true);
            visit(new GlobalFinder(scope));
            visit(new BindingFinder(scope));
        } finally {
            scope.setNameBindingPhase(false);
        }
        return super.resolve(scope);
    }
}
