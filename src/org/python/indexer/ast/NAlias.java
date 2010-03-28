/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Scope;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnknownType;

import java.util.List;

/**
 * A name alias.  Used for the components of import and import-from statements.
 */
public class NAlias extends NNode {

    static final long serialVersionUID = 4127878954298987559L;

    /**
     * Represents one of three possibilities:
     * <ol>
     *   <li>import stmt: qname of one of the imported modules</li>
     *   <li>import-from stmt: qname of the referenced module</li>
     *   <li>import-from stmt: simple name imported from referenced module</li>
     * </ol>
     */
    public NQname qname; // ["." ["." ["." ["a" ["b" ["c"]]]]]]

    /**
     * The un-parsed qname or simple name corresponding to {@link #qname}.
     */
    public String name;  // "...a.b.c"

    /**
     * The alias name, if an "as" clause was specified.
     */
    public NName aname;  // "bar" in "... foo as bar"

    public NAlias(String name, NQname qname, NName aname) {
        this(name, qname, aname, 0, 1);
    }

    public NAlias(String name, NQname qname, NName aname, int start, int end) {
        super(start, end);
        this.qname = qname;
        this.name = name;
        this.aname = aname;
        addChildren(qname, aname);
    }

    /**
     * Returns the alias, if non-{@code null}, else the simple name.
     */
    public String getBoundName() {
        return aname != null ? aname.id : name;
    }

    /**
     * Resolves and returns the referenced
     * {@link org.python.indexer.types.NModuleType} in an import or
     * or import-from statement.  NImportFrom statements manually
     * resolve their child NAliases.
     */
    @Override
    public NType resolve(Scope s) throws Exception {
        setType(resolveExpr(qname, s));

        // "import a.b.c" defines 'a' (the top module) in the scope, whereas
        // "import a.b.c as x" defines 'x', which refers to the bottom module.
        if (aname != null && qname != null) {
            setType(qname.getBottom().getType());
            aname.setType(getType());
        }

        return getType();
    }

    @Override
    public String toString() {
        return "<Alias:" + name + ":" + aname + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(qname, v);
            visitNode(aname, v);
        }
    }
}
