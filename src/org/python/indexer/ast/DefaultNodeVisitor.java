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
     * nodes, the visitor can throw a {@link NNodeVisitor.StopIterationException}.
     */
    public void stopTraversal() {
        traverseIntoNodes = false;
    }

    @Override
    public boolean visit(NAlias n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NAssert n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NAssign n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NAttribute n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NAugAssign n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NBinOp n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NBlock n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NBoolOp n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NBreak n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NCall n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NClassDef n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NCompare n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NComprehension n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NContinue n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NDelete n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NDict n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NEllipsis n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NExceptHandler n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NExec n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NFor n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NFunctionDef n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NGeneratorExp n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NGlobal n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NIf n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NIfExp n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NImport n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NImportFrom n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NIndex n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NKeyword n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NLambda n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NList n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NListComp n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NModule n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NName n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NNum n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NPass n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NPlaceHolder n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NPrint n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NQname n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NRaise n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NRepr n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NReturn n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NExprStmt n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NSlice n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NStr n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NSubscript n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NTryExcept n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NTryFinally n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NTuple n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NUnaryOp n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NUrl n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NWhile n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NWith n) {
        return traverseIntoNodes;
    }

    @Override
    public boolean visit(NYield n) {
        return traverseIntoNodes;
    }
}
