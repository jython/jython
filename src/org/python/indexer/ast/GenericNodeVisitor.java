/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

/**
 * A visitor that passes every visited node to a single function.
 * Subclasses need only implement {@link #dispatch} to receive
 * every node as a generic {@link NNode}.
 */
public abstract class GenericNodeVisitor extends DefaultNodeVisitor {

    /**
     * Every visited node is passed to this method.  The semantics
     * for halting traversal are the same as for {@link DefaultNodeVisitor}.
     * @return {@code true} to traverse this node's children
     */
    public boolean dispatch(NNode n) {
        return traverseIntoNodes;
    }

    public boolean visit(NAlias n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NAssert n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NAssign n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NAttribute n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NAugAssign n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NBinOp n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NBlock n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NBoolOp n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NBreak n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NCall n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NClassDef n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NCompare n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NComprehension n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NContinue n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NDelete n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NDict n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NEllipsis n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NExceptHandler n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NExec n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NFor n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NFunctionDef n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NGeneratorExp n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NGlobal n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NIf n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NIfExp n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NImport n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NImportFrom n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NIndex n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NKeyword n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NLambda n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NList n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NListComp n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NModule n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NName n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NNum n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NPass n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NPlaceHolder n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NPrint n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NQname n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NRaise n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NRepr n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NReturn n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NExprStmt n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NSlice n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NStr n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NSubscript n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NTryExcept n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NTryFinally n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NTuple n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NUnaryOp n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NUrl n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NWhile n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NWith n) {
        return traverseIntoNodes && dispatch(n);
    }

    public boolean visit(NYield n) {
        return traverseIntoNodes && dispatch(n);
    }
}
