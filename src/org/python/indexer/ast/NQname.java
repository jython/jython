/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Indexer;
import org.python.indexer.NBinding;
import org.python.indexer.Scope;
import org.python.indexer.Util;
import org.python.indexer.types.NType;
import org.python.indexer.types.NModuleType;
import org.python.indexer.types.NUnknownType;

import java.io.File;

/**
 * Recursive doubly-linked list representation of a qualified module name,
 * either absolute or relative.  Similar to {@link NAttribute}, but handles
 * leading dots and other import-specific special cases.<p>
 *
 * Qualified names with leading dots are given {@code NQname} elements for each
 * leading dot.  Dots separating simple names are not given {@code NQname}
 * elements. <p>
 */
public class NQname extends NNode {

    static final long serialVersionUID = -5892553606852895686L;

    private NQname next;
    private NName name;

    public NQname(NQname next, NName name) {
        this(next, name, 0, 1);
    }

    public NQname(NQname next, NName name, int start, int end) {
        super(start, end);
        if (name == null)
            throw new IllegalArgumentException("null name");
        this.name = name;
        this.next = next;
        addChildren(name, next);
    }

    /**
     * Returns this component of the qname chain.
     */
    public NName getName() {
        return name;
    }

    /**
     * Returns the previous component of this qname chain, or {@code null} if
     * this is the first component.
     */
    public NQname getPrevious() {
        NNode parent = getParent();
        if (parent instanceof NQname) {
            return (NQname)parent;
        }
        return null;
    }

    /**
     * Returns the next component of the chain, or {@code null} if this is
     * the last component.
     */
    public NQname getNext() {
        return next;
    }

    /**
     * Returns the last/bottom component of the chain.
     */
    public NQname getBottom() {
        return next == null ? this : next.getBottom();
    }

    /**
     * Returns {@code true} if this is the first/top component of the chain,
     * or if the name is unqualified (i.e. has only one component, no dots).
     */
    public boolean isTop() {
        return getPrevious() == null;
    }

    /**
     * Returns {@code true} if this is the last/bottom component of the chain,
     * or if the name is unqualified (i.e. has only one component, no dots).
     */
    public boolean isBottom() {
        return next == null;
    }

    /**
     * Returns {@code true} if this qname represents a simple, non-dotted module
     * name such as "os", "random" or "foo".
     */
    public boolean isUnqualified() {
        return isTop() && isBottom();
    }

    /**
     * Joins all components in this qname chain, beginning with the
     * current component.
     */
    public String toQname() {
        return isBottom() ? name.id : name.id + "." + next.toQname();
    }

    /**
     * Returns the qname down to (and including) this component, ending
     * with this component's name.  For instance, if this {@code NQname}
     * instance represents the {@code foo} in {@code org.foo.bar}, this
     * method will return {@code org.foo}.
     */
    public String thisQname() {
        NQname n = getTop();
        StringBuilder sb = new StringBuilder();
        sb.append(n.name.id);
        while (n != this) {
            sb.append(".");
            n = n.next;
            sb.append(n.name.id);
        }
        return sb.toString();
    }

    /**
     * Returns the top (first) component in the chain.
     */
    public NQname getTop() {
        return isTop() ? this : getPrevious().getTop();
    }

    /**
     * Returns {@code true} if this qname component is a leading dot.
     */
    public boolean isDot() {
        return ".".equals(name.id);
    }

    /**
     * Resolves and loads the module named by this qname.
     * @return the module represented by the qname up to this point.
     */
    @Override
    public NType resolve(Scope s) throws Exception {
        setType(name.setType(new NUnknownType()));

        // Check for top-level native or standard module.
        if (isUnqualified()) {
            NModuleType mt = Indexer.idx.loadModule(name.id);
            if (mt != null) {
                return setType(name.setType(mt));
            }
        } else {
            // Check for second-level builtin such as "os.path".
            NModuleType mt = Indexer.idx.getBuiltinModule(thisQname());
            if (mt != null) {
                setType(name.setType(mt));
                resolveExpr(next, s);
                return mt;
            }
        }

        return resolveInFilesystem(s);
    }

    private NType resolveInFilesystem(Scope s) throws Exception {
        NModuleType start = getStartModule(s);
        if (start == null) {
            reportUnresolvedModule();
            return getType();
        }

        String qname = start.getTable().getPath();
        String relQname;
        if (isDot()) {
            relQname = Util.getQnameParent(qname);
        } else if (!isTop()) {
            relQname = qname + "." + name.id;
        } else {
            // top name:  first look in current dir, then sys.path
            String dirQname = isInitPy() ? qname : Util.getQnameParent(qname);
            relQname = dirQname + "." + name.id;
            if (Indexer.idx.loadModule(relQname) == null) {
                relQname = name.id;
            }
        }

        NModuleType mod = Indexer.idx.loadModule(relQname);
        if (mod == null) {
            reportUnresolvedModule();
            return getType();
        }
        setType(name.setType(mod));

        if (!isTop() && mod.getFile() != null) {
            Scope parentPkg = getPrevious().getTable();
            NBinding mb = Indexer.idx.moduleTable.lookup(mod.getFile());
            parentPkg.put(name.id, mb);
        }

        resolveExpr(next, s);
        return getType();
    }

    private boolean isInitPy()  {
        String path = getFile();
        if (path == null) {
            return false;
        }
        return new File(path).getName().equals("__init__.py");
    }

    private NModuleType getStartModule(Scope s) throws Exception {
        if (!isTop()) {
            return getPrevious().getType().asModuleType();
        }

        // Start with module for current file (i.e. containing directory).

        NModuleType start = null;
        Scope mtable = s.getSymtabOfType(Scope.Type.MODULE);
        if (mtable != null) {
            start = Indexer.idx.loadModule(mtable.getPath());
            if (start != null) {
                return start;
            }
        }

        String dir = new File(getFile()).getParent();
        if (dir == null) {
            Indexer.idx.warn("Unable to find parent dir for " + getFile());
            return null;
        }

        return Indexer.idx.loadModule(dir);
    }

    private void reportUnresolvedModule() {
        addError("module not found: " + name.id);
        Indexer.idx.recordUnresolvedModule(thisQname(), getFile());
    }

    @Override
    public String toString() {
        return "<QName:" + name + ":" + next + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(next, v);
            visitNode(name, v);
        }
    }
}
