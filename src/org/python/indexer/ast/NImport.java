/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Builtins;
import org.python.indexer.Indexer;
import org.python.indexer.NBinding;
import org.python.indexer.Scope;
import org.python.indexer.types.NModuleType;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnknownType;

import java.util.List;

public class NImport extends NNode {

    static final long serialVersionUID = -2180402676651342012L;

    public List<NAlias> aliases;  // import foo.bar as bar, ..x.y as y

    public NImport(List<NAlias> aliases) {
        this(aliases, 0, 1);
    }

    public NImport(List<NAlias> aliases, int start, int end) {
        super(start, end);
        this.aliases = aliases;
        addChildren(aliases);
    }

    @Override
    public boolean bindsName() {
        return true;
    }

    @Override
    protected void bindNames(Scope s) throws Exception {
        bindAliases(s, aliases);
    }

    static void bindAliases(Scope s, List<NAlias> aliases) throws Exception {
        NameBinder binder = NameBinder.make();
        for (NAlias a : aliases) {
            if (a.aname != null) {
                binder.bind(s, a.aname, new NUnknownType());
            }
        }
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        Scope scope = s.getScopeSymtab();
        for (NAlias a : aliases) {
            NType modtype = resolveExpr(a, s);
            if (modtype.isModuleType()) {
                importName(scope, a, modtype.asModuleType());
            }
        }
        return getType();
    }

    /**
     * Import a module's alias (if present) or top-level name into the current
     * scope.  Creates references to the other names in the alias.
     *
     * @param mt the module that is actually bound to the imported name.
     * for {@code import x.y.z as foo}, it is {@code z}, a sub-module
     * of {@code x.y}.  For {@code import x.y.z} it is {@code x}.
     */
    private void importName(Scope s, NAlias a, NModuleType mt) throws Exception {
        if (a.aname != null) {
            if (mt.getFile() != null) {
                NameBinder.make().bind(s, a.aname, mt);
            } else {
                // XXX:  seems like the url should be set in loadModule, not here.
                // Can't the moduleTable store url-keyed modules too?
                s.update(a.aname.id,
                         new NUrl(Builtins.LIBRARY_URL + mt.getTable().getPath() + ".html"),
                         mt, NBinding.Kind.SCOPE);
            }
        }

        addReferences(s, a.qname, true/*put top name in scope*/);
    }

    static void addReferences(Scope s, NQname qname, boolean putTopInScope) {
        if (qname == null) {
            return;
        }
        if (!qname.getType().isModuleType()) {
            return;
        }
        NModuleType mt = qname.getType().asModuleType();

        String modQname = mt.getTable().getPath();
        NBinding mb = Indexer.idx.lookupQname(modQname);
        if (mb == null) {
            mb = Indexer.idx.moduleTable.lookup(modQname);
        }

        if (mb == null) {
            Indexer.idx.putProblem(qname.getName(), "module not found");
            return;
        }

        Indexer.idx.putLocation(qname.getName(), mb);

        // All other references in the file should also point to this binding.
        if (putTopInScope && qname.isTop()) {
            s.put(qname.getName().id, mb);
        }

        addReferences(s, qname.getNext(), false);
    }

    @Override
    public String toString() {
        return "<Import:" + aliases + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNodeList(aliases, v);
        }
    }
}
