/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer;

import org.python.indexer.ast.NName;
import org.python.indexer.ast.NNode;
import org.python.indexer.ast.NUrl;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnionType;
import org.python.indexer.types.NUnknownType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;

/**
 * Symbol table.
 */
public class Scope {

    /**
     * For preventing circular inheritance from recursing.
     */
    private static Set<Scope> looked = new HashSet<Scope>();

    public enum Type {
        CLASS,
        INSTANCE,
        FUNCTION,
        MODULE,
        GLOBAL,
        SCOPE
    }

    /**
     * XXX: This table is incorrectly overloaded to contain both object
     * attributes and lexical-ish scope names, when they are in some cases
     * separate namespaces.  (In particular, they're effectively the same
     * namespace for module scope and class scope, and they're different for
     * function scope, which uses the {@code func_dict} namespace for storing
     * attributes.)
     */
    private Map<String, NBinding> table;  // stays null for most scopes (mem opt)

    private Scope parent;
    private List<Scope> supers;
    private Set<String> globalNames;
    private Type scopeType;
    private String path = "";
    private int lambdaCounter = 0;
    private boolean isBindingPhase = false;

    public Scope(Scope parent, Type type) {
        if (type == null) {
            throw new IllegalArgumentException("'type' param cannot be null");
        }
        setParent(parent);
        setScopeType(type);
    }

    public void setTable(Map<String, NBinding> table) {
        this.table = table;
    }

    /**
     * Returns an immutable view of the table.
     */
    public Map<String, NBinding> getTable() {
        if (table != null) {
            return Collections.unmodifiableMap(table);
        }
        Map<String, NBinding> map = Collections.emptyMap();
        return map;
    }

    public void setParent(Scope parent) {
        this.parent = parent;
    }

    public Scope getParent() {
        return parent;
    }

    public void addSuper(Scope sup) {
        if (supers == null) {
            supers = new ArrayList<Scope>();
        }
        supers.add(sup);
    }

    public void setSupers(List<Scope> supers) {
        this.supers = supers;
    }

    public List<Scope> getSupers() {
        if (supers != null) {
            return Collections.unmodifiableList(supers);
        }
        List<Scope> list = Collections.emptyList();
        return list;
    }

    public void setScopeType(Type type) {
        this.scopeType = type;
    }

    public Type getScopeType() {
        return scopeType;
    }

    public boolean isFunctionScope() {
        return scopeType == Type.FUNCTION;
    }

    /**
     * Mark a name as being global (i.e. module scoped) for name-binding and
     * name-lookup operations in this code block and any nested scopes.
     */
    public void addGlobalName(String name) {
        if (name == null) {
            return;
        }
        if (globalNames == null) {
            globalNames = new HashSet<String>();
        }
        globalNames.add(name);
    }

    /**
     * Returns {@code true} if {@code name} appears in a {@code global}
     * statement in this scope or any enclosing scope.
     */
    public boolean isGlobalName(String name) {
        if (globalNames != null) {
            return globalNames.contains(name);
        }
        return parent == null ? false : parent.isGlobalName(name);
    }

    /**
     * Directly assigns a binding to a name in this table.  Does not add a new
     * definition or reference to the binding.  This form of {@code put} is
     * often followed by a call to {@link putLocation} to create a reference to
     * the binding.  When there is no code location associated with {@code id},
     * or it is otherwise undesirable to create a reference, the
     * {@link putLocation} call is omitted.
     */
    public void put(String id, NBinding b) {
        putBinding(id, b);
    }

    /**
     * Adds a definition and/or reference to the table.
     * If there is no binding for {@code id}, creates one and gives it
     * {@code type} and {@code kind}.  <p>
     *
     * If a binding already exists, then add either a definition or a reference
     * at {@code loc} to the binding.  By convention we consider it a definition
     * if the type changes.  If the passed type is different from the binding's
     * current type, set the binding's type to the union of the old and new
     * types, and add a definition.  If the new type is the same, just add a
     * reference.  <p>
     *
     * If the binding already exists, {@code kind} is only updated if a
     * definition was added <em>and</em> the binding's type was previously the
     * unknown type.
     */
    public NBinding put(String id, NNode loc, NType type, NBinding.Kind kind) {
        if (type == null) {
            throw new IllegalArgumentException("Null type: id=" + id + ", loc=" + loc);
        }
        NBinding b = lookupScope(id);
        return insertOrUpdate(b, id, loc, type, kind);
    }

    /**
     * Same as {@link #put}, but adds the name as an attribute of this scope.
     * Looks up the superclass chain to see if the attribute exists, rather
     * than looking in the lexical scope chain.
     *
     * @return the new binding, or {@code null} if the current scope does
     * not have a properly initialized path.
     */
    public NBinding putAttr(String id, NNode loc, NType type, NBinding.Kind kind) {
        if (type == null) {
            throw new IllegalArgumentException("Null type: id=" + id + ", loc=" + loc);
        }

        // Attributes are always part of a qualified name.  If there is no qname
        // on the target type, it's a bug (we forgot to set the path somewhere.)
        if ("".equals(path)) {
            Indexer.idx.reportFailedAssertion(
                "Attempting to set attr '" + id + "' at location " + loc
                + (loc != null ? loc.getFile() : "")
                + " in scope with no path (qname) set: " + this.toShortString());
            return null;
        }

        NBinding b = lookupAttr(id);
        return insertOrUpdate(b, id, loc, type, kind);
    }

    private NBinding insertOrUpdate(NBinding b, String id, NNode loc, NType t, NBinding.Kind k) {
        if (b == null) {
            b = insertBinding(new NBinding(id, loc, t, k));
        } else {
            updateType(b, loc, t, k);
        }
        return b;
    }

    /**
     * Adds a new binding for {@code id}.  If a binding already existed,
     * replaces its previous definitions, if any, with {@code loc}.  Sets the
     * binding's type to {@code type} (not a union with the previous type).
     */
    public NBinding update(String id, NNode loc, NType type, NBinding.Kind kind) {
        if (type == null) {
            throw new IllegalArgumentException("Null type: id=" + id + ", loc=" + loc);
        }
        return update(id, new Def(loc), type, kind);
    }

    /**
     * Adds a new binding for {@code id}.  If a binding already existed,
     * replaces its previous definitions, if any, with {@code loc}.  Sets the
     * binding's type to {@code type} (not a union with the previous type).
     */
    public NBinding update(String id, Def loc, NType type, NBinding.Kind kind) {
        if (type == null) {
            throw new IllegalArgumentException("Null type: id=" + id + ", loc=" + loc);
        }
        NBinding b = lookupScope(id);
        if (b == null) {
            return insertBinding(new NBinding(id, loc, type, kind));
        }

        b.getDefs().clear();  // XXX:  what about updating refs & idx.locations?
        b.addDef(loc);
        b.setType(type);

        // XXX: is this a bug?  I think he meant to do this check before the
        // line above that sets b.type, if it's supposed to be like put().
        if (b.getType().isUnknownType()) {
            b.setKind(kind);
        }
        return b;
    }

    private NBinding insertBinding(NBinding b) {
        switch (b.getKind()) {
            case MODULE:
                b.setQname(b.getType().getTable().path);
                break;
            case PARAMETER:
                b.setQname(extendPathForParam(b.getName()));
                break;
            default:
                b.setQname(extendPath(b.getName()));
                break;
        }

        b = Indexer.idx.putBinding(b);
        putBinding(b.getName(), b);
        return b;
    }

    private void putBinding(String id, NBinding b) {
        ensureTable();
        table.put(id, b);
    }

    private void updateType(NBinding b, NNode loc, NType type, NBinding.Kind kind) {
        NType curType = b.followType();
        if (!isNewType(curType, type)) {
            if (loc != null
                && !(loc instanceof NUrl)
                && !b.getDefs().contains(loc)) {
                Indexer.idx.putLocation(loc, b);
            }
            return;
        }

        if (loc != null && !b.getRefs().contains(loc)) {
            b.addDef(loc);
            b.setProvisional(false);
        }

        // The union ordering matters here.  If they're two different unknown
        // types, union() points the first one to the second one.  We want to
        // keep the binding's existing type iff its table contains provisional
        // attribute bindings that we need to look up later.
        NType btype = b.getType();
        NType t1, t2;
        if (btype.isUnknownType() && !btype.getTable().isEmpty()) {
            t1 = type;
            t2 = btype;
        } else {
            t1 = btype;
            t2 = type;
        }
        NType newType = NUnionType.union(t1, t2);
        b.setType(newType);

        if (curType.isUnknownType()) {
            b.setKind(kind);
        }

        retargetReferences(b, curType);
    }

    /**
     * If the current type had a provisional binding, retarget its refs to the
     * new type.  It probably only works one level deep: need dataflow analysis
     * in the general case.  However, it does pick up some extra references,
     * so it's reasonable for now.
     */
    private void retargetReferences(NBinding b, NType curType) {
        Scope newScope = b.followType().getTable();
        for (Map.Entry<String, NBinding> e : curType.getTable().entrySet()) {
            String attr = e.getKey();
            NBinding oldBinding = e.getValue();
            if (!oldBinding.isProvisional()) {
                continue;
            }
            Indexer.idx.removeBinding(oldBinding);
            NBinding newBinding = newScope.lookupAttr(attr);
            if (newBinding != null) {
                List<Ref> refs = new ArrayList<Ref>();  // avoid ConcurrentModificationException
                refs.addAll(oldBinding.getRefs());
                for (Ref ref : refs) {
                    Indexer.idx.updateLocation(ref, newBinding);
                }
            }
        }
    }

    /**
     * Returns {@code true} if the binding is being assigned a new type.
     */
    private boolean isNewType(NType curType, NType type) {
        // In the bindNames() phase we want all places where a given name
        // is bound in the same scope to share the same binding, because
        // we haven't resolved the types yet.  This takes care of that case.
        if (isBindingPhase) {
            return false;
        }

        if (curType.isUnionType()) {
            return !curType.asUnionType().contains(type);
        }

        return curType != type;
    }

    public void remove(String id) {
        if (table != null) {
            table.remove(id);
        }
    }

    /**
     * Create a copy of the symbol table but without the links to parent, supers
     * and children. Useful for creating instances.
     *
     * @return the symbol table for use by the instance.
     */
    public Scope copy(Type tableType) {
        Scope ret = new Scope(null, tableType);
        if (table != null) {
            ret.ensureTable();
            ret.table.putAll(table);
        }
        return ret;
    }

    public void setPath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("'path' param cannot be null");
        }
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String a, String b) {
        NBinding b1 = lookup(a);
        NBinding b2 = lookup(b);
        if (b1 != null && b2 != null) {
            b1.setQname(b2.getQname());
        }
    }

    /**
     * Look up a name (String) in the current symbol table.  If not found,
     * recurse on the parent table.
     */
    public NBinding lookup(String name) {
        NBinding b = getModuleBindingIfGlobal(name);
        if (b != null) {
            return b;
        }
        if (table != null) {
            NBinding ent = table.get(name);
            if (ent != null) {
                return ent;
            }
        }
        if (getParent() == null) {
            return null;
        }
        return getParent().lookup(name);
    }

    /**
     * Specialized version for the convenience of looking up {@code Name}s.
     * For all other types return {@code null}.
     */
    public NBinding lookup(NNode n) {
        if (n instanceof NName) {
            return lookup(((NName)n).id);
        }
        return null;
    }

    /**
     * Look up a name, but only in the current scope.
     * @return the local binding for {@code name}, or {@code null}.
     */
    public NBinding lookupLocal(String name) {
        NBinding b = getModuleBindingIfGlobal(name);
        if (b != null) {
            return b;
        }
        return table == null ? null : table.get(name);
    }

    /**
     * Look up an attribute in the type hierarchy.  Don't look at parent link,
     * because the enclosing scope may not be a super class. The search is
     * "depth first, left to right" as in Python's (old) multiple inheritance
     * rule. The new MRO can be implemented, but will probably not introduce
     * much difference.
     * @param supersOnly search only in the supers' scopes, not in local table.
     */
    public NBinding lookupAttr(String name, boolean supersOnly) {
        if (looked.contains(this)) {
            return null;
        }
        if (table != null && !supersOnly) {
            NBinding b = table.get(name);
            if (b != null) {
                return b;
            }
        }
        if (supers == null || supers.isEmpty()) {
            return null;
        }
        looked.add(this);
        try {
            for (Scope p : supers) {
                NBinding b = p.lookupAttr(name);
                if (b != null) {
                    return b;
                }
            }
            return null;
        } finally {
            looked.remove(this);
        }
    }

    /**
     * Look up an attribute in the local scope and superclass scopes.
     * @see lookupAttr(String,boolean)
     */
    public NBinding lookupAttr(String name) {
        return lookupAttr(name, false);
    }

    /**
     * Look up the scope chain for a binding named {@code name}
     * and if found, return its type.
     */
    public NType lookupType(String name) {
        return lookupType(name, false);
    }

    /**
     * Look for a binding named {@code name} and if found, return its type.
     * @param localOnly {@code true} to look only in the current scope;
     *        if {@code false}, follows the scope chain.
     */
    public NType lookupType(String name, boolean localOnly) {
        NBinding b = localOnly ? lookupLocal(name) : lookup(name);
        if (b == null) {
            return null;
        }
        NType ret = b.followType();
        // XXX:  really need to make ModuleTable polymorphic...
        if (this == Indexer.idx.moduleTable) {
            if (ret.isModuleType()) {
                return ret;
            }
            if (ret.isUnionType()) {
                for (NType t : ret.asUnionType().getTypes()) {
                    NType realType = t.follow();
                    if (realType.isModuleType()) {
                        return realType;
                    }
                }
            }
            Indexer.idx.warn("Found non-module type in module table: " + b);
            return null;
        }
        return ret;
    }

    public NType lookupTypeAttr(String name) {
        NBinding b = lookupAttr(name);
        if (b != null) {
            return b.followType();
        }
        return null;
    }

    /**
     * Look up a name, but the search is bounded by a type and will not proceed
     * to an outer scope when reaching a certain type of symbol table.
     *
     * @param name the name to be looked up
     * @param typebound the type we wish the search to be bounded at
     * @return a binding, or {@code null} if not found
     */
    public NBinding lookupBounded(String name, Type typebound) {
        if (scopeType == typebound) {
            return table == null ? null : table.get(name);
        }
        if (getParent() == null) {
            return null;
        }
        return getParent().lookupBounded(name, typebound);
    }

    /**
     * Returns {@code true} if this is a scope in which names may be bound.
     */
    public boolean isScope() {
        switch (scopeType) {
            case CLASS:
            case INSTANCE:
            case FUNCTION:
            case MODULE:
            case GLOBAL:
                return true;
            default:
                return false;
        }
    }

    /**
     * Find the enclosing scope-defining symbol table.  <p>
     *
     * More precisely, if a form introduces a new name in the "current scope",
     * resolving the form needs to search up the symbol-table chain until it
     * finds the table representing the scope to which the name should be added.
     * Used by {@link org.python.indexer.ast.NameBinder} to create new name
     * bindings in the appropriate enclosing table with the appropriate binding
     * type.
     */
    public Scope getScopeSymtab() {
        if (this.isScope()) {
            return this;
        }
        if (getParent() == null) {
            Indexer.idx.reportFailedAssertion("No binding scope found for " + this.toShortString());
            return this;
        }
        return getParent().getScopeSymtab();
    }

    /**
     * Look up a name, but bounded by a scope defining construct. Those scopes
     * are of type module, class, instance or function. This is used in
     * determining the locations of a variable's definition.
     */
    public NBinding lookupScope(String name) {
        NBinding b = getModuleBindingIfGlobal(name);
        if (b != null) {
            return b;
        }
        Scope st = getScopeSymtab();
        if (st != null) {
            return st.lookupLocal(name);
        }
        return null;
    }

    /**
     * Find a symbol table of a certain type in the enclosing scopes.
     */
    public Scope getSymtabOfType(Type type) {
        if (scopeType == type) {
            return this;
        }
        if (parent == null) {
            return null;
        }
        return parent.getSymtabOfType(type);
    }

    /**
     * Returns the global scope (i.e. the module scope for the current module).
     */
    public Scope getGlobalTable() {
        Scope result = getSymtabOfType(Type.MODULE);
        if (result == null) {
            Indexer.idx.reportFailedAssertion("No module table found for " + this);
            result = this;
        }
        return result;
    }

    /**
     * Returns the containing lexical scope (which may be this scope)
     * for lexical name lookups.  In particular, it skips class scopes.
     */
    public Scope getEnclosingLexicalScope() {
        if (scopeType == Scope.Type.FUNCTION
            || scopeType == Scope.Type.MODULE) {
            return this;
        }
        if (parent == null) {
            Indexer.idx.reportFailedAssertion("No lexical scope found for " + this);
            return this;
        }
        return parent.getEnclosingLexicalScope();
    }

    /**
     * If {@code name} is declared as a global, return the module binding.
     */
    private NBinding getModuleBindingIfGlobal(String name) {
        if (isGlobalName(name)) {
            Scope module = getGlobalTable();
            if (module != null && module != this) {
                return module.lookupLocal(name);
            }
        }
        return null;
    }

    /**
     * Name binding occurs in a separate pass before the name resolution pass,
     * building out the scope tree and binding names in the correct scopes.
     * In this pass, the name binding and lookup rules are slightly different.
     * This condition is transient:  no scopes will be in the name-binding phase
     * in a completed index (or module).
     */
    public boolean isNameBindingPhase() {
        return isBindingPhase;
    }

    public void setNameBindingPhase(boolean isBindingPhase) {
        this.isBindingPhase = isBindingPhase;
    }

    /**
     * Merge all records from another symbol table. Used by {@code import from *}.
     */
    public void merge(Scope other) {
        ensureTable();
        this.table.putAll(other.table);
    }

    public Set<String> keySet() {
        if (table != null) {
            return table.keySet();
        }
        Set<String> result = Collections.emptySet();
        return result;
    }

    public Collection<NBinding> values() {
        if (table != null) {
            return table.values();
        }
        Collection<NBinding> result = Collections.emptySet();
        return result;
    }

    public Set<Entry<String, NBinding>> entrySet() {
        if (table != null) {
            return table.entrySet();
        }
        Set<Entry<String, NBinding>> result = Collections.emptySet();
        return result;
    }

    public boolean isEmpty() {
        return table == null ? true : table.isEmpty();
    }

    /**
     * Dismantles all resources allocated by this scope.
     */
    public void clear() {
        if (table != null) {
            table.clear();
            table = null;
        }
        parent = null;
        if (supers != null) {
            supers.clear();
            supers = null;
        }
        if (globalNames != null) {
            globalNames.clear();
            globalNames = null;
        }
    }

    public String newLambdaName() {
        return "lambda%" + (++lambdaCounter);
    }

    /**
     * Generates a qname for a parameter of a function or method.
     * There is not enough context for {@link #extendPath} to differentiate
     * params from locals, so callers must use this method when the name is
     * known to be a parameter name.
     */
    public String extendPathForParam(String name) {
        if (path.equals("")) {
            throw new IllegalStateException("Not inside a function");
        }
        return path + "@" + name;
    }

    /**
     * Constructs a qualified name by appending {@code name} to this scope's qname. <p>
     *
     * The indexer uses globally unique fully qualified names to address
     * identifier definition sites.  Many Python identifiers are already
     * globally addressable using dot-separated package, class and attribute
     * names. <p>
     *
     * Function variables and parameters are not globally addressable in the
     * language, so the indexer uses a special path syntax for creating globally
     * unique qualified names for them.  By convention the syntax is "@" for
     * parameters and "&amp;" for local variables.
     *
     * @param name a name to append to the current qname
     * @return the qname for {@code name}.  Does not change this scope's path.
     */
    public String extendPath(String name) {
        if (name.endsWith(".py")) {
            name = Util.moduleNameFor(name);
        }
        if (path.equals("")) {
            return name;
        }
        String sep = null;
        switch (scopeType) {
            case MODULE:
            case CLASS:
            case INSTANCE:
            case SCOPE:
                sep = ".";
                break;
            case FUNCTION:
                sep = "&";
                break;
            default:
                System.err.println("unsupported context for extendPath: " + scopeType);
                return path;
        }
        return path + sep + name;
    }

    private void ensureTable() {
        if (table == null) {
            table = new LinkedHashMap<String, NBinding>();
        }
    }

    @Override
    public String toString() {
        return "<Scope:" + getScopeType() + ":" + path + ":" +
                (table == null ? "{}" : table.keySet()) + ">";
    }

    public String toShortString() {
        return "<Scope:" + getScopeType() + ":" + path + ">";
    }
}
