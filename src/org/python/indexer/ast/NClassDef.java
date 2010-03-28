/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Builtins;
import org.python.indexer.Indexer;
import org.python.indexer.NBinding;
import org.python.indexer.Scope;
import org.python.indexer.types.NClassType;
import org.python.indexer.types.NDictType;
import org.python.indexer.types.NTupleType;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnknownType;

import java.util.ArrayList;
import java.util.List;

public class NClassDef extends NNode {

    static final long serialVersionUID = 7513873538009667540L;

    public NName name;
    public List<NNode> bases;
    public NBody body;

    public NClassDef(NName name, List<NNode> bases, NBlock body) {
        this(name, bases, body, 0, 1);
    }

    public NClassDef(NName name, List<NNode> bases, NBlock body, int start, int end) {
        super(start, end);
        this.name = name;
        this.bases = bases;
        this.body = new NBody(body);
        addChildren(name, this.body);
        addChildren(bases);
    }

    @Override
    public boolean isClassDef() {
        return true;
    }

    @Override
    public boolean bindsName() {
        return true;
    }

    @Override
    protected void bindNames(Scope s) throws Exception {
        Scope container = s.getScopeSymtab();
        setType(new NClassType(name.id, container));

        // If we already defined this class in this scope, don't redefine it.
        NType existing = container.lookupType(name.id);
        if (existing != null && existing.isClassType()) {
            return;
        }

        NameBinder.make(NBinding.Kind.CLASS).bind(container, name, getType());
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        NClassType thisType = getType().asClassType();
        List<NType> baseTypes = new ArrayList<NType>();
        for (NNode base : bases) {
            NType baseType = resolveExpr(base, s);
            if (baseType.isClassType()) {
                thisType.addSuper(baseType);
            }
            baseTypes.add(baseType);
        }

        Builtins builtins = Indexer.idx.builtins;
        addSpecialAttribute("__bases__", new NTupleType(baseTypes));
        addSpecialAttribute("__name__", builtins.BaseStr);
        addSpecialAttribute("__module__", builtins.BaseStr);
        addSpecialAttribute("__doc__", builtins.BaseStr);
        addSpecialAttribute("__dict__", new NDictType(builtins.BaseStr, new NUnknownType()));

        resolveExpr(body, getTable());
        return getType();
    }

    private void addSpecialAttribute(String name, NType proptype) {
        NBinding b = getTable().update(name, Builtins.newTutUrl("classes.html"),
                                       proptype, NBinding.Kind.ATTRIBUTE);
        b.markSynthetic();
        b.markStatic();
        b.markReadOnly();
    }

    @Override
    public String toString() {
        return "<ClassDef:" + name.id + ":" + start() + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(name, v);
            visitNodeList(bases, v);
            visitNode(body, v);
        }
    }
}
