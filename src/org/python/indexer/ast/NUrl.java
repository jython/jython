/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Indexer;
import org.python.indexer.Scope;
import org.python.indexer.types.NType;

/**
 * Non-AST node used to represent virtual source locations for builtins
 * as external urls.
 */
public class NUrl extends NNode {

    static final long serialVersionUID = -3488021036061979551L;

    private String url;

    private static int count = 0;

    public NUrl(String url) {
        this.url = url == null ? "" : url;
        setStart(count);
        setEnd(count++);
    }

    public NUrl(String url, int start, int end) {
        super(start, end);
        this.url = url == null ? "" : url;
    }

    public String getURL() {
        return url;
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        return setType(Indexer.idx.builtins.BaseStr);
    }

    @Override
    public String toString() {
        return "<Url:\"" + url + "\">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        v.visit(this);
    }
}
