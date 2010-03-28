/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Indexer;
import org.python.indexer.NBinding;
import org.python.indexer.Scope;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnknownType;

public class NName extends NNode {

    static final long serialVersionUID = -1160862551327528304L;

    public final String id;  // identifier

    public NName(String id) {
        this(id, 0, 1);
    }

    public NName(String id, int start, int end) {
        super(start, end);
        if (id == null) {
            throw new IllegalArgumentException("'id' param cannot be null");
        }
        this.id = id;
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        NBinding b = s.lookup(id);
        if (b == null) {
            b = makeTempBinding(s);
        }
        Indexer.idx.putLocation(this, b);
        return setType(b.followType());
    }

    /**
     * Returns {@code true} if this name is structurally in a call position.
     */
    public boolean isCall() {
        // foo(...)
        if (parent != null && parent.isCall() && this == ((NCall)parent).func) {
            return true;
        }

        // <expr>.foo(...)
        NNode gramps;
        return parent instanceof NAttribute
                && this == ((NAttribute)parent).attr
                && (gramps = parent.parent) instanceof NCall
                && parent == ((NCall)gramps).func;
    }

    /**
     * Create a temporary binding and definition for this name.
     * If we later encounter a true definition we'll remove this
     * node from the defs and add it to the refs.
     */
    private NBinding makeTempBinding(Scope s) {
        Scope scope = s.getScopeSymtab();

        NBinding b = scope.put(id, this, new NUnknownType(), NBinding.Kind.SCOPE);
        setType(b.getType().follow());

        // Update the qname to this point in case we add attributes later.
        // If we don't add attributes, this path extension is harmless/unused.
        getTable().setPath(scope.extendPath(id));

        return b;
    }

    /**
     * Returns {@code true} if this name node is the {@code attr} child
     * (i.e. the attribute being accessed) of an {@link NAttribute} node.
     */
    public boolean isAttribute() {
        return parent instanceof NAttribute
                && ((NAttribute)parent).getAttr() == this;
    }

    @Override
    public String toString() {
        return "<Name:" + start() + ":" + id +  ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        v.visit(this);
    }
}
