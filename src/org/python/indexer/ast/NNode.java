/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Indexer;
import org.python.indexer.IndexingException;
import org.python.indexer.NBinding;
import org.python.indexer.Scope;
import org.python.indexer.types.NClassType;
import org.python.indexer.types.NFuncType;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnionType;
import org.python.indexer.types.NUnknownType;

import java.util.List;

public abstract class NNode implements java.io.Serializable {

    static final long serialVersionUID = 3682719481356964898L;

    private int start = 0;
    private int end = 1;

    protected NNode parent = null;

    /**
     * This is marked transient to prevent serialization.  We re-resolve ASTs
     * after deserializing them.  It is private to ensure that the type is never
     * {@code null}, as much code in the indexer assumes this precondition.
     */
    private transient NType type = Indexer.idx.builtins.None;

    public NNode() {
    }

    public NNode(int start, int end) {
        setStart(start);
        setEnd(end);
    }

    public void setParent(NNode parent) {
        this.parent = parent;
    }

    public NNode getParent() {
        return parent;
    }

    public NNode getAstRoot() {
        if (parent == null) {
            return this;
        }
        return parent.getAstRoot();
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }

    public int length() {
        return end - start;
    }

    /**
     * Utility alias for {@code getType().getTable()}.
     */
    public Scope getTable() {
        return getType().getTable();
    }

    /**
     * Returns the type for this node.  It is never {@code null}.
     * If the node has not been resolved, the type will default to
     * {@link Indexer.idx.builtins.None}.
     */
    public NType getType() {
        if (type == null) {
            type = Indexer.idx.builtins.None;
        }
        return type;
    }

    /**
     * Sets the type for the node.
     * @param newType the new type
     * @return {@code newType}
     * @throws IllegalArgumentException if {@code newType} is {@code null}
     */
    public NType setType(NType newType) {
        if (newType == null) {
            throw new IllegalArgumentException();
        }
        return type = newType;
    }

    /**
     * Adds a new type for the node, creating a union of the previous type
     * and the new type.
     * @param newType the new type
     * @return the resulting type for the node
     * @throws IllegalArgumentException if {@code newType} is {@code null}
     */
    public NType addType(NType newType) {
        if (newType == null) {
            throw new IllegalArgumentException();
        }
        return type = NUnionType.union(getType(), newType);
    }

    /**
     * Returns {@code true} if this is a name-binding node.
     * Includes functions/lambdas, function/lambda params, classes,
     * assignments, imports, and implicit assignment via for statements
     * and except clauses.
     * @see http://www.python.org/dev/peps/pep-0227
     */
    public boolean bindsName() {
        return false;
    }

    /**
     * Called by resolver to bind names into the passed scope.
     */
    protected void bindNames(Scope s) throws Exception {
        throw new UnsupportedOperationException("Not a name-binding node type");
    }

    /**
     * @return the path to the code that generated this AST
     */
    public String getFile() {
        return parent != null ? parent.getFile() : null;
    }

    public void addChildren(NNode... nodes) {
        if (nodes != null) {
            for (NNode n : nodes) {
                if (n != null) {
                    n.setParent(this);
                }
            }
        }
    }

    public void addChildren(List<? extends NNode> nodes) {
        if (nodes != null) {
            for (NNode n : nodes) {
                if (n != null) {
                    n.setParent(this);
                }
            }
        }
    }

    private static NType handleExceptionInResolve(NNode n, Throwable t) {
        Indexer.idx.handleException("Unable to resolve: " + n + " in " + n.getFile(), t);
        return new NUnknownType();
    }

    public static NType resolveExpr(NNode n, Scope s) {
        if (n == null) {
            return new NUnknownType();
        }
        // This try-catch enables error recovery when there are bugs in
        // the indexer.  Rather than unwinding all the way up to the module
        // level (and failing to load the module), we record an error for this
        // node and continue.
        try {
            NType result = n.resolve(s);
            if (result == null) {
                Indexer.idx.warn(n + " resolved to a null type");
                return n.setType(new NUnknownType());
            }
            return result;
        } catch (IndexingException ix) {
            throw ix;
        } catch (Exception x) {
            return handleExceptionInResolve(n, x);
        } catch (StackOverflowError soe) {
            String msg = "Unable to resolve: " + n + " in " + n.getFile() + " (stack overflow)";
            Indexer.idx.warn(msg);
            return handleExceptionInResolve(n, soe);
        }
    }

    /**
     * Node should set the resolved type in its {@link #type} field
     * and also return it.
     */
    public NType resolve(Scope s) throws Exception {
        return getType();
    }

    public boolean isCall() {
        return this instanceof NCall;
    }

    public boolean isModule() {
        return this instanceof NModule;
    }

    public boolean isClassDef() {
        return false;
    }

    public boolean isFunctionDef() {
        return false;
    }

    public boolean isLambda() {
        return false;
    }

    public boolean isName() {
        return this instanceof NName;
    }

    protected void visitNode(NNode n, NNodeVisitor v) {
        if (n != null) {
            n.visit(v);
        }
    }

    protected void visitNodeList(List<? extends NNode> nodes, NNodeVisitor v) {
        if (nodes != null) {
            for (NNode n : nodes) {
                if (n != null) {
                    n.visit(v);
                }
            }
        }
    }

    /**
     * Visits this node and optionally its children. <p>
     *
     * @param visitor the object to call with this node.
     *        If the visitor returns {@code true}, the node also
     *        passes its children to the visitor.
     */
    public abstract void visit(NNodeVisitor visitor);

    /**
     * Returns the innermost enclosing scope for doing (non-attribute) name
     * lookups.  If the current node defines a scope, it returns the parent
     * scope for name lookups.
     *
     * @return the enclosing function, class, instance, module or builtin scope.
     *         If this node has not yet been resolved, returns the builtin
     *         namespace.
     */
    public Scope getEnclosingNamespace() {
        if (parent == null || this.isModule()) {
            return Indexer.idx.globaltable;
        }
        NNode up = this;
        while ((up = up.parent) != null) {
            if (up.isFunctionDef() || up.isClassDef() || up.isModule()) {
                NType type = up.getType();
                if (type == null || type.getTable() == null) {
                    return Indexer.idx.globaltable;
                }
                return type.getTable();
            }
        }
        return Indexer.idx.globaltable;
    }

    protected void addWarning(String msg) {
        Indexer.idx.putProblem(this, msg);
    }

    protected void addWarning(NNode loc, String msg) {
        Indexer.idx.putProblem(loc, msg);
    }

    protected void addError(String msg) {
        Indexer.idx.putProblem(this, msg);
    }

    protected void addError(NNode loc, String msg) {
        Indexer.idx.putProblem(loc, msg);
    }

    /**
     * Utility method to resolve every node in {@code nodes} and
     * return the union of their types.  If {@code nodes} is empty or
     * {@code null}, returns a new {@link NUnknownType}.
     */
    protected NType resolveListAsUnion(List<? extends NNode> nodes, Scope s) {
        if (nodes == null || nodes.isEmpty()) {
            return new NUnknownType();
        }

        NType result = null;
        for (NNode node : nodes) {
            NType nodeType = resolveExpr(node, s);
            if (result == null) {
                result = nodeType;
            } else {
                result = NUnionType.union(result, nodeType);
            }
        }
        return result;
    }

    /**
     * Resolves each element of a node list in the passed scope.
     * Node list may be empty or {@code null}.
     */
    protected void resolveList(List<? extends NNode> nodes, Scope s) {
        if (nodes != null) {
            for (NNode n : nodes) {
                resolveExpr(n, s);
            }
        }
    }

    /**
     * Assumes nodes are always traversed in increasing order of their start
     * positions.
     */
    static class DeepestOverlappingNodeFinder extends GenericNodeVisitor {
        private int offset;
        private NNode deepest;

        public DeepestOverlappingNodeFinder(int offset) {
            this.offset = offset;
        }

        /**
         * Returns the deepest node overlapping the desired source offset.
         * @return the node, or {@code null} if no node overlaps the offset
         */
        public NNode getNode() {
            return deepest;
        }

        public boolean dispatch(NNode node) {
            // This node ends before the offset, so don't look inside it.
            if (offset > node.end) {
                return false;  // don't traverse children, but do keep going
            }

            if (offset >= node.start && offset <= node.end) {
                deepest = node;
                return true;  // visit kids
            }

            // this node starts after the offset, so we're done
            throw new NNodeVisitor.StopIterationException();
        }
    }

    /**
     * Searches the AST for the deepest node that overlaps the specified source
     * offset.  Can be called from any node in the AST, as it traverses to the
     * parent before beginning the search.
     * @param sourceOffset the spot at which to look for a node
     * @return the deepest AST node whose start is greater than or equal to the offset,
     *         and whose end is less than or equal to the offset.  Returns {@code null}
     *         if no node overlaps {@code sourceOffset}.
     */
    public NNode getDeepestNodeAtOffset(int sourceOffset) {
        NNode ast = getAstRoot();
        DeepestOverlappingNodeFinder finder = new DeepestOverlappingNodeFinder(sourceOffset);
        try {
            ast.visit(finder);
        } catch (NNodeVisitor.StopIterationException six) {
            // expected
        }
        return finder.getNode();
    }
}
