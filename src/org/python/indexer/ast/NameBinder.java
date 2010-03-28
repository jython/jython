/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Indexer;
import org.python.indexer.NBinding;
import org.python.indexer.types.NListType;
import org.python.indexer.types.NTupleType;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnknownType;
import org.python.indexer.Scope;

import java.util.List;

import static org.python.indexer.NBinding.Kind.ATTRIBUTE;
import static org.python.indexer.NBinding.Kind.CLASS;
import static org.python.indexer.NBinding.Kind.CONSTRUCTOR;
import static org.python.indexer.NBinding.Kind.FUNCTION;
import static org.python.indexer.NBinding.Kind.METHOD;
import static org.python.indexer.NBinding.Kind.MODULE;
import static org.python.indexer.NBinding.Kind.PARAMETER;
import static org.python.indexer.NBinding.Kind.SCOPE;
import static org.python.indexer.NBinding.Kind.VARIABLE;

/**
 * Handles binding names to scopes, including destructuring assignment.
 */
public class NameBinder {

    static private final NameBinder DEFAULT_BINDER = new NameBinder();
    static private final NameBinder ATTRIBUTE_BINDER = new NameBinder(ATTRIBUTE);
    static private final NameBinder CLASS_BINDER = new NameBinder(CLASS);
    static private final NameBinder CONSTRUCTOR_BINDER = new NameBinder(CONSTRUCTOR);
    static private final NameBinder FUNCTION_BINDER = new NameBinder(FUNCTION);
    static private final NameBinder METHOD_BINDER = new NameBinder(METHOD);
    static private final NameBinder MODULE_BINDER = new NameBinder(MODULE);
    static private final NameBinder PARAMETER_BINDER = new NameBinder(PARAMETER);
    static private final NameBinder VARIABLE_BINDER = new NameBinder(VARIABLE);

    private NBinding.Kind kind;

    /**
     * Factory method for creating instances.
     */
    public static NameBinder make() {
        return DEFAULT_BINDER;
    }

    /**
     * Factory method for creating instances.
     * @return a {@code NameBinder} that will create bindings of {@code kind},
     * overriding the default choices.
     */
    public static NameBinder make(NBinding.Kind kind) {
        switch (kind) {
            case ATTRIBUTE:
                return ATTRIBUTE_BINDER;
            case CLASS:
                return CLASS_BINDER;
            case CONSTRUCTOR:
                return CONSTRUCTOR_BINDER;
            case FUNCTION:
                return FUNCTION_BINDER;
            case METHOD:
                return METHOD_BINDER;
            case MODULE:
                return MODULE_BINDER;
            case PARAMETER:
                return PARAMETER_BINDER;
            case VARIABLE:
                return VARIABLE_BINDER;
            default:
                return DEFAULT_BINDER;
        }
    }

    private NameBinder() {
    }

    private NameBinder(NBinding.Kind kind) {
        this.kind = kind;
    }

    public void bind(Scope s, NNode target, NType rvalue) throws Exception {
        if (target instanceof NName) {
            bindName(s, (NName)target, rvalue);
            return;
        }
        if (target instanceof NTuple) {
            bind(s, ((NTuple)target).elts, rvalue);
            return;
        }
        if (target instanceof NList) {
            bind(s, ((NList)target).elts, rvalue);
            return;
        }
        if (target instanceof NAttribute) {
            // This causes various problems if we let it happen during the
            // name-binding pass.  I believe the only name-binding context
            // in which an NAttribute can be an lvalue is in an assignment.
            // Assignments are statements, so they can only appear in blocks.
            // Hence the scope for the top-level name is unambiguous; we can
            // safely leave binding it until the resolve pass.
            if (!s.isNameBindingPhase()) {
                ((NAttribute)target).setAttr(s, rvalue);
            }
            return;
        }
        if (target instanceof NSubscript) {
            // Ditto.  No resolving is allowed during the name-binding phase.
            if (!s.isNameBindingPhase()) {
                target.resolveExpr(target, s);
            }
            return;
        }
        Indexer.idx.putProblem(target, "invalid location for assignment");
    }

    public void bind(Scope s, List<NNode> xs, NType rvalue) throws Exception {
        if (rvalue.isTupleType()) {
            List<NType> vs = rvalue.asTupleType().getElementTypes();
            if (xs.size() != vs.size()) {
                reportUnpackMismatch(xs, vs.size());
            } else {
                for (int i = 0; i < xs.size(); i++) {
                    bind(s, xs.get(i), vs.get(i));
                }
            }
            return;
        }

        if (rvalue.isListType()) {
            bind(s, xs, rvalue.asListType().toTupleType(xs.size()));
            return;
        }

        if (rvalue.isDictType()) {
            bind(s, xs, rvalue.asDictType().toTupleType(xs.size()));
            return;
        }

        if (!rvalue.isUnknownType()) {
            Indexer.idx.putProblem(xs.get(0).getFile(),
                                   xs.get(0).start(),
                                   xs.get(xs.size()-1).end(),
                                   "unpacking non-iterable: " + rvalue);
        }
        for (int i = 0; i < xs.size(); i++) {
            bind(s, xs.get(i), new NUnknownType());
        }
    }

    public NBinding bindName(Scope s, NName name, NType rvalue) throws Exception {
        NBinding b;

        if (s.isGlobalName(name.id)) {
            b = s.getGlobalTable().put(name.id, name, rvalue, kindOr(SCOPE));
            Indexer.idx.putLocation(name, b);
        } else {
            Scope bindingScope = s.getScopeSymtab();
            b = bindingScope.put(name.id, name, rvalue,
                                 kindOr(bindingScope.isFunctionScope() ? VARIABLE : SCOPE));
        }

        name.setType(b.followType());

        // XXX: this seems like a bit of a hack; should at least figure out
        // and document what use cases require it.
        NType nameType = name.getType();
        if (!(nameType.isModuleType() || nameType.isClassType())) {
            nameType.getTable().setPath(b.getQname());
        }

        return b;
    }

    public void bindIter(Scope s, NNode target, NNode iter) throws Exception {
        NType iterType = NNode.resolveExpr(iter, s);

        if (iterType.isListType()) {
            bind(s, target, iterType.asListType().getElementType());
        } else if (iterType.isTupleType()) {
            bind(s, target, iterType.asTupleType().toListType().getElementType());
        } else {
            NBinding ent = iterType.getTable().lookupAttr("__iter__");
            if (ent == null || !ent.getType().isFuncType()) {
                if (!iterType.isUnknownType()) {
                    iter.addWarning("not an iterable type: " + iterType);
                }
                bind(s, target, new NUnknownType());
            } else {
                bind(s, target, ent.getType().asFuncType().getReturnType());
            }
        }
    }

    private void reportUnpackMismatch(List<NNode> xs, int vsize) {
        int xsize = xs.size();
        int beg = xs.get(0).start();
        int end = xs.get(xs.size() - 1).end();
        int diff = xsize - vsize;
        String msg;
        if (diff > 0) {
            msg = "ValueError: need more than " + vsize + " values to unpack";
        } else {
            msg = "ValueError: too many values to unpack";
        }
        Indexer.idx.putProblem(xs.get(0).getFile(), beg, end, msg);
    }

    private NBinding.Kind kindOr(NBinding.Kind k) {
        return kind != null ? kind : k;
    }
}
