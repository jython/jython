/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Indexer;
import org.python.indexer.Scope;

/**
 * A visitor that looks for name-binding constructs in a given scope.
 */
class BindingFinder extends GenericNodeVisitor {

    private Scope scope;  // starting scope

    public BindingFinder(Scope scope) {
        this.scope = scope;
    }

    @Override
    public boolean dispatch(NNode n) {
        if (n.bindsName()) {
            try {
                n.bindNames(scope);
            } catch (Exception x) {
                Indexer.idx.handleException("error binding names for " + n, x);
            }
        }
        // Do not descend into new scopes.
        if (n.isFunctionDef() || n.isClassDef()) {
            return false;
        }
        return super.dispatch(n);
    }
}
