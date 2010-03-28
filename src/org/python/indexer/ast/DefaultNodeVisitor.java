/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

/**
 * A visitor that by default visits every node in the tree.
 * Subclasses can override specific node visiting methods
 * and decide whether to visit the children.
 */
public class DefaultNodeVisitor implements NNodeVisitor {

    protected boolean traverseIntoNodes = true;

    /**
     * Once this is called, all {@code visit} methods will return {@code false}.
     * If the current node's children are being visited, all remaining top-level
     * children of the node will be visited (without visiting their children),
     * and then tree traversal halts. <p>
     *
     * If the traversal should be halted immediately without visiting any further
     * nodes, the visitor can throw a {@link StopIterationException}.
     */
    public void stopTraversal() {
        traverseIntoNodes = false;
    }

    public boolean visit(NAlias n) {
        return traverseIntoNodes;
    }

    public boolean visit(NAssert n) {
        return traverseIntoNodes;
    }

    public boolean visit(NAssign n) {
        return traverseIntoNodes;
    }

    public boolean visit(NAttribute n) {
        return traverseIntoNodes;
    }

    public boolean visit(NAugAssign n) {
        return traverseIntoNodes;
    }

    public boolean visit(NBinOp n) {
        return traverseIntoNodes;
    }

    public boolean visit(NBlock n) {
        return traverseIntoNodes;
    }

    public boolean visit(NBoolOp n) {
        return traverseIntoNodes;
    }

    public boolean visit(NBreak n) {
        return traverseIntoNodes;
    }

    public boolean visit(NCall n) {
        return traverseIntoNodes;
    }

    public boolean visit(NClassDef n) {
        return traverseIntoNodes;
    }

    public boolean visit(NCompare n) {
        return traverseIntoNodes;
    }

    public boolean visit(NComprehension n) {
        return traverseIntoNodes;
    }

    public boolean visit(NContinue n) {
        return traverseIntoNodes;
    }

    public boolean visit(NDelete n) {
        return traverseIntoNodes;
    }

    public boolean visit(NDict n) {
        return traverseIntoNodes;
    }

    public boolean visit(NEllipsis n) {
        return traverseIntoNodes;
    }

    public boolean visit(NExceptHandler n) {
        return traverseIntoNodes;
    }

    public boolean visit(NExec n) {
        return traverseIntoNodes;
    }

    public boolean visit(NFor n) {
        return traverseIntoNodes;
    }

    public boolean visit(NFunctionDef n) {
        return traverseIntoNodes;
    }

    public boolean visit(NGeneratorExp n) {
        return traverseIntoNodes;
    }

    public boolean visit(NGlobal n) {
        return traverseIntoNodes;
    }

    public boolean visit(NIf n) {
        return traverseIntoNodes;
    }

    public boolean visit(NIfExp n) {
        return traverseIntoNodes;
    }

    public boolean visit(NImport n) {
        return traverseIntoNodes;
    }

    public boolean visit(NImportFrom n) {
        return traverseIntoNodes;
    }

    public boolean visit(NIndex n) {
        return traverseIntoNodes;
    }

    public boolean visit(NKeyword n) {
        return traverseIntoNodes;
    }

    public boolean visit(NLambda n) {
        return traverseIntoNodes;
    }

    public boolean visit(NList n) {
        return traverseIntoNodes;
    }

    public boolean visit(NListComp n) {
        return traverseIntoNodes;
    }

    public boolean visit(NModule n) {
        return traverseIntoNodes;
    }

    public boolean visit(NName n) {
        return traverseIntoNodes;
    }

    public boolean visit(NNum n) {
        return traverseIntoNodes;
    }

    public boolean visit(NPass n) {
        return traverseIntoNodes;
    }

    public boolean visit(NPlaceHolder n) {
        return traverseIntoNodes;
    }

    public boolean visit(NPrint n) {
        return traverseIntoNodes;
    }

    public boolean visit(NQname n) {
        return traverseIntoNodes;
    }

    public boolean visit(NRaise n) {
        return traverseIntoNodes;
    }

    public boolean visit(NRepr n) {
        return traverseIntoNodes;
    }

    public boolean visit(NReturn n) {
        return traverseIntoNodes;
    }

    public boolean visit(NExprStmt n) {
        return traverseIntoNodes;
    }

    public boolean visit(NSlice n) {
        return traverseIntoNodes;
    }

    public boolean visit(NStr n) {
        return traverseIntoNodes;
    }

    public boolean visit(NSubscript n) {
        return traverseIntoNodes;
    }

    public boolean visit(NTryExcept n) {
        return traverseIntoNodes;
    }

    public boolean visit(NTryFinally n) {
        return traverseIntoNodes;
    }

    public boolean visit(NTuple n) {
        return traverseIntoNodes;
    }

    public boolean visit(NUnaryOp n) {
        return traverseIntoNodes;
    }

    public boolean visit(NUrl n) {
        return traverseIntoNodes;
    }

    public boolean visit(NWhile n) {
        return traverseIntoNodes;
    }

    public boolean visit(NWith n) {
        return traverseIntoNodes;
    }

    public boolean visit(NYield n) {
        return traverseIntoNodes;
    }
}
