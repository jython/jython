/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer;

import org.python.indexer.ast.NNode;
import org.python.indexer.ast.NUrl;
import org.python.indexer.types.NModuleType;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnknownType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * An {@code NBinding} collects information about a fully qualified name (qname)
 * in the code graph.<p>
 *
 * Each qname has an associated {@link NType}.  When a particular qname is
 * assigned values of different types at different locations, its type is
 * represented as a {@link NUnionType}. <p>
 *
 * Each qname has a set of one or more definitions, and a set of zero or
 * more references.  Definitions and references correspond to code locations. <p>
 */
public class NBinding implements Comparable<Object> {

    /**
     * In addition to its type, each binding has a {@link Kind} enumeration that
     * attempts to represent the structural role the name plays in the code.
     * This is a rough categorization that takes into account type information,
     * structural (AST) information, and possibly other semantics.  It can help
     * IDEs with presentation decisions, and can be useful to expose to users as
     * a parameter for filtering queries on the graph.
     */
    public enum Kind {
        ATTRIBUTE,  // attr accessed with "." on some other object
        CLASS,  // class definition
        CONSTRUCTOR,  // __init__ functions in classes
        FUNCTION,  // plain function
        METHOD,  // static or instance method
        MODULE,  // file
        PARAMETER,  // function param
        SCOPE,  // top-level variable ("scope" means we assume it can have attrs)
        VARIABLE  // local variable
    }

    private static final int PROVISIONAL = 1 << 0; // (for internal use only)
    private static final int STATIC = 1 << 1;     // static fields/methods
    private static final int SYNTHETIC = 1 << 2;  // auto-generated bindings
    private static final int READONLY = 1 << 3;   // non-writable attributes
    private static final int DEPRECATED = 1 << 4; // documented as deprecated
    private static final int BUILTIN = 1 << 5;  // not from a source file

    // The indexer is heavily memory-constrained, so these sets are initially
    // small.  The vast majority of bindings have only one definition.
    private static final int DEF_SET_INITIAL_CAPACITY = 1;
    private static final int REF_SET_INITIAL_CAPACITY = 8;

    private String name;   // unqualified name
    private String qname;  // qualified name
    private NType type;    // inferred type
    Kind kind;     // name usage context
    int modifiers; // metadata flags

    private List<Def> defs;
    private Set<Ref> refs;

    public NBinding(String id, NNode node, NType type, Kind kind) {
        this(id, node != null ? new Def(node) : null, type, kind);
    }

    public NBinding(String id, Def def, NType type, Kind kind) {
        if (id == null) {
            throw new IllegalArgumentException("'id' param cannot be null");
        }
        qname = name = id;
        defs = new ArrayList<Def>(DEF_SET_INITIAL_CAPACITY);
        addDef(def);
        this.type = type == null ? new NUnknownType() : type;
        this.kind = kind == null ? Kind.SCOPE : kind;
    }

    /**
     * Sets the binding's simple (unqualified) name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the unqualified name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the binding's qualified name.  This should in general be the
     * same as {@code binding.getType().getTable().getPath()}.
     */
    public void setQname(String qname) {
        this.qname = qname;
    }

    /**
     * Returns the qualified name.
     */
    public String getQname() {
        return qname;
    }

    /**
     * Adds {@code node} as a definition for this binding.  This is called
     * automatically (when appropriate) by adding the binding to a
     * {@link Scope}.
     */
    public void addDef(NNode node) {
        if (node != null) {
            addDef(new Def(node));
        }
    }

    public void addDefs(Collection<NNode> nodes) {
        for (NNode n : nodes) {
            addDef(n);
        }
    }

    /**
     * Adds {@code def} as a definition for this binding.  This is called
     * automatically (when appropriate) by adding the binding to a
     * {@link Scope}.  If {@code node} is an {@link NUrl}, and this is the
     * binding's only definition, it will be marked as a builtin.
     */
    public void addDef(Def def) {
        if (def == null) {
            return;
        }
        List<Def> defs = getDefs();
        // A def may be added during the name-binding phase, and re-added
        // when the type is updated during the resolve phase.
        if (defs.contains(def)) {
            return;
        }

        defs.add(def);
        def.setBinding(this);

        if (def.isURL()) {
            markBuiltin();
        }
    }

    public void addRef(NNode node) {
        addRef(new Ref(node));
    }

    public void addRef(Ref ref) {
        getRefs().add(ref);
    }

    public void removeRef(Ref node) {
        getRefs().remove(node);
    }

    /**
     * Returns the first definition, which by convention is treated as
     * the one that introduced the binding.
     */
    public Def getSignatureNode() {
        return getDefs().isEmpty() ? null : getDefs().get(0);
    }

    public void setType(NType type) {
        this.type = type;
    }

    public NType getType() {
        return type;
    }

    /**
     * Returns the type, first calling {@link NUnknownType#follow} on it.
     * For external consumers of the index, this is usually preferable
     * to calling {@link #getType}.
     */
    public NType followType() {
        return NUnknownType.follow(type);
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

    public void markStatic() {
        modifiers |= STATIC;
    }

    public boolean isStatic() {
        return (modifiers & STATIC) != 0;
    }

    public void markSynthetic() {
        modifiers |= SYNTHETIC;
    }

    public boolean isSynthetic() {
        return (modifiers & SYNTHETIC) != 0;
    }

    public void markReadOnly() {
        modifiers |= READONLY;
    }

    public boolean isReadOnly() {
        return (modifiers & READONLY) != 0;
    }

    public boolean isDeprecated() {
        return (modifiers & DEPRECATED) != 0;
    }

    public void markDeprecated() {
        modifiers |= DEPRECATED;
    }

    public boolean isBuiltin() {
        return (modifiers & BUILTIN) != 0;
    }

    public void markBuiltin() {
        modifiers |= BUILTIN;
    }

    public void setProvisional(boolean isProvisional) {
        if (isProvisional) {
            modifiers |= PROVISIONAL;
        } else {
            modifiers &= ~PROVISIONAL;
        }
    }

    public boolean isProvisional() {
        return (modifiers & PROVISIONAL) != 0;
    }

    /**
     * Bindings can be sorted by their location for outlining purposes.
     */
    public int compareTo(Object o) {
        return getSignatureNode().start() - ((NBinding)o).getSignatureNode().start();
    }

    /**
     * Return the (possibly empty) set of definitions for this binding.
     * @return the defs
     */
    public List<Def> getDefs() {
        if (defs == null) {
            defs = new ArrayList<Def>(DEF_SET_INITIAL_CAPACITY);
        }
        return defs;
    }

    /**
     * Returns the number of definitions found for this binding.
     */
    public int getNumDefs() {
        return defs == null ? 0 : defs.size();
    }

    public boolean hasRefs() {
        return refs == null ? false : !refs.isEmpty();
    }

    public int getNumRefs() {
        return refs == null ? 0 : refs.size();
    }

    /**
     * Returns the set of references to this binding.
     */
    public Set<Ref> getRefs() {
        if (refs == null) {
            refs = new LinkedHashSet<Ref>(REF_SET_INITIAL_CAPACITY);
        }
        return refs;
    }

    /**
     * Returns a filename associated with this binding, for debug
     * messages.
     * @return the filename associated with the type (if present)
     *     or the first definition (if present), otherwise a string
     *     describing what is known about the binding's source.
     */
    public String getFirstFile() {
        NType bt = getType();
        if (bt instanceof NModuleType) {
            String file = bt.asModuleType().getFile();
            return file != null ? file : "<built-in module>";
        }
        if (defs != null) {
            for (Def def : defs) {
                String file = def.getFile();
                if (file != null) {
                    return file;
                }
            }
            return "<built-in binding>";
        }
        return "<unknown source>";
    }

    @Override
    public String toString() {
        StringBuilder sb =  new StringBuilder();
        sb.append("<Binding:").append(qname);
        sb.append(":type=").append(type);
        sb.append(":kind=").append(kind);
        sb.append(":defs=").append(defs);
        sb.append(":refs=");
        if (getRefs().size() > 10) {
            sb.append("[");
            sb.append(refs.iterator().next());
            sb.append(", ...(");
            sb.append(refs.size() - 1);
            sb.append(" more)]");
        } else {
            sb.append(refs);
        }
        sb.append(">");
        return sb.toString();
    }
}
