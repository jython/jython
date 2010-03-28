/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

/**
 * Preorder-traversal node visitor interface.
 */
public interface NNodeVisitor {
    /**
     * Convenience exception for subclasses.  The caller that initiates
     * the visit should catch this exception if the subclass is expected
     * to throw it.
     */
    public static final class StopIterationException extends RuntimeException {
        public StopIterationException() {}
    }

    public boolean visit(NAlias m);
    public boolean visit(NAssert m);
    public boolean visit(NAssign m);
    public boolean visit(NAttribute m);
    public boolean visit(NAugAssign m);
    public boolean visit(NBinOp m);
    public boolean visit(NBlock m);
    public boolean visit(NBoolOp m);
    public boolean visit(NBreak m);
    public boolean visit(NCall m);
    public boolean visit(NClassDef m);
    public boolean visit(NCompare m);
    public boolean visit(NComprehension m);
    public boolean visit(NContinue m);
    public boolean visit(NDelete m);
    public boolean visit(NDict m);
    public boolean visit(NEllipsis m);
    public boolean visit(NExceptHandler m);
    public boolean visit(NExec m);
    public boolean visit(NFor m);
    public boolean visit(NFunctionDef m);
    public boolean visit(NGeneratorExp m);
    public boolean visit(NGlobal m);
    public boolean visit(NIf m);
    public boolean visit(NIfExp m);
    public boolean visit(NImport m);
    public boolean visit(NImportFrom m);
    public boolean visit(NIndex m);
    public boolean visit(NKeyword m);
    public boolean visit(NLambda m);
    public boolean visit(NList m);
    public boolean visit(NListComp m);
    public boolean visit(NModule m);
    public boolean visit(NName m);
    public boolean visit(NNum m);
    public boolean visit(NPass m);
    public boolean visit(NPlaceHolder m);
    public boolean visit(NPrint m);
    public boolean visit(NQname m);
    public boolean visit(NRaise m);
    public boolean visit(NRepr m);
    public boolean visit(NReturn m);
    public boolean visit(NExprStmt m);
    public boolean visit(NSlice m);
    public boolean visit(NStr m);
    public boolean visit(NSubscript m);
    public boolean visit(NTryExcept m);
    public boolean visit(NTryFinally m);
    public boolean visit(NTuple m);
    public boolean visit(NUnaryOp m);
    public boolean visit(NUrl m);
    public boolean visit(NWhile m);
    public boolean visit(NWith m);
    public boolean visit(NYield m);
}
