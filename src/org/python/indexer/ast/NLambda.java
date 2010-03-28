/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Indexer;
import org.python.indexer.NBinding;
import org.python.indexer.Scope;
import org.python.indexer.types.NFuncType;
import org.python.indexer.types.NTupleType;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnknownType;

import java.util.List;

public class NLambda extends NFunctionDef {

    static final long serialVersionUID = 7737836525970653522L;

    private NName fname;

    public NLambda(List<NNode> args, NNode body, List<NNode> defaults,
                   NName varargs, NName kwargs) {
        this(args, body, defaults, varargs, kwargs, 0, 1);
    }

    public NLambda(List<NNode> args, NNode body, List<NNode> defaults,
                   NName varargs, NName kwargs, int start, int end) {
        super(null, args, null, defaults, varargs, kwargs, start, end);
        this.body = body instanceof NBlock ? new NBody((NBlock)body) : body;
        addChildren(this.body);
    }

    @Override
    public boolean isLambda() {
        return true;
    }

    /**
     * Returns the name of the function for indexing/qname purposes.
     */
    @Override
    protected String getBindingName(Scope s) {
        if (fname != null) {
            return fname.id;
        }
        String fn = s.newLambdaName();
        fname = new NName(fn, start(), start() + "lambda".length());
        fname.setParent(this);
        return fn;
    }

    @Override
    protected void bindFunctionName(Scope owner) throws Exception {
        NameBinder.make(NBinding.Kind.FUNCTION).bindName(owner, fname, getType());
    }

    @Override
    protected void bindMethodAttrs(Scope owner) throws Exception {
        // no-op
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        if (!getType().isFuncType()) {
            org.python.indexer.Indexer.idx.reportFailedAssertion(
                "Bad type on " + this + ": type=" + getType() +
                " in file " + getFile() + " at " + start());
        }
        NTupleType fromType = new NTupleType();
        NameBinder param = NameBinder.make(NBinding.Kind.PARAMETER);

        resolveList(defaults, s);

        Scope funcTable = getTable();
        int argnum = 0;
        for (NNode a : args) {
            NType argtype = NFunctionDef.getArgType(args, defaults, argnum++);
            param.bind(funcTable, a, argtype);
            fromType.add(argtype);
        }

        if (varargs != null) {
            NType u = new NUnknownType();
            param.bind(funcTable, varargs, u);
            fromType.add(u);
        }

        if (kwargs != null) {
            NType u = new NUnknownType();
            param.bind(funcTable, kwargs, u);
            fromType.add(u);
        }

        // A lambda body is not an NBody, so it doesn't undergo the two
        // pre-resolve passes for finding global statements and name-binding
        // constructs.  However, the lambda expression may itself contain
        // name-binding constructs (generally, other lambdas), so we need to
        // perform the name-binding pass on it before resolving.
        try {
            funcTable.setNameBindingPhase(true);
            body.visit(new BindingFinder(funcTable));
        } finally {
            funcTable.setNameBindingPhase(false);
        }

        NType toType = resolveExpr(body, funcTable);
        if (getType().isFuncType()) {  // else warning logged at method entry above
            getType().asFuncType().setReturnType(toType);
        }
        return getType();
    }

    @Override
    public String toString() {
        return "<Lambda:" + start() + ":" + args + ":" + body + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNodeList(args, v);
            visitNodeList(defaults, v);
            visitNode(varargs, v);
            visitNode(kwargs, v);
            visitNode(body, v);
        }
    }
}
