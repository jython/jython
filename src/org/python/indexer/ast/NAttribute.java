/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Indexer;
import org.python.indexer.NBinding;
import org.python.indexer.Scope;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnionType;
import org.python.indexer.types.NUnknownType;

import static org.python.indexer.NBinding.Kind.ATTRIBUTE;

public class NAttribute extends NNode {

    static final long serialVersionUID = -1120979305017812255L;

    public NNode target;
    public NName attr;

    public NAttribute(NNode target, NName attr) {
        this(target, attr, 0, 1);
    }

    public NAttribute(NNode target, NName attr, int start, int end) {
        super(start, end);
        setTarget(target);
        setAttr(attr);
        addChildren(target, attr);
    }

    public String getAttributeName() {
        return attr.id;
    }

    /**
     * Sets the attribute node.  Used when constructing the AST.
     * @throws IllegalArgumentException if the param is null
     */
    public void setAttr(NName attr) {
        if (attr == null) {
            throw new IllegalArgumentException("param cannot be null");
        }
        this.attr = attr;
    }

    public NName getAttr() {
        return attr;
    }

    /**
     * Sets the target node.  Used when constructing the AST.
     * @throws IllegalArgumentException if the param is null
     */
    public void setTarget(NNode target) {
        if (target == null) {
            throw new IllegalArgumentException("param cannot be null");
        }
        this.target = target;
    }

    public NNode getTarget() {
        return target;
    }

    /**
     * Assign some definite value to the attribute.  Used during the name
     * resolution pass.  This method is called when this node is in the lvalue of
     * an assignment, in which case it is called in lieu of {@link #resolve}.<p>
     */
    public void setAttr(Scope s, NType v) throws Exception {
        setType(new NUnknownType());

        NType targetType = resolveExpr(target, s);
        if (targetType.isUnionType()) {
            targetType = targetType.asUnionType().firstKnownNonNullAlternate();
            if (targetType == null) {
                return;
            }
        }

        targetType = targetType.follow();
        if (targetType == Indexer.idx.builtins.None) {
            return;
        }
        NBinding b = targetType.getTable().putAttr(attr.id, attr, v, ATTRIBUTE);
        if (b != null) {
            setType(attr.setType(b.followType()));
        }
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        setType(new NUnknownType());

        NType targetType = resolveExpr(target, s);

        if (targetType.isUnionType()) {
            NType ret = new NUnknownType();
            for (NType tp : targetType.asUnionType().getTypes()) {
                resolveAttributeOnType(tp);
                ret = NUnionType.union(ret, getType());
            }
            setType(attr.setType(ret.follow()));
        } else {
            resolveAttributeOnType(targetType);
        }

        return getType();
    }

    private void resolveAttributeOnType(NType targetType) {
        NType ttype = targetType.follow();
        NBinding b = ttype.getTable().lookupAttr(attr.id);
        if (b == null) {
            b = makeProvisionalBinding(ttype);
        }
        if (b != null) {
            Indexer.idx.putLocation(attr, b);
            setType(attr.setType(b.getType()));
        }
    }

    /**
     * If we can't find the attribute in the target type, create a temp binding
     * for the attribute.  If later on the target type defines the attribute,
     * that definition replaces the temp binding, and any references to the temp
     * binding are updated to refer to the new definition.
     *
     * <p>We never create temp bindings for attributes of native types, as
     * the attributes of native types are expected to be fully resolved.
     *
     * <p>This strategy is a temporary solution for inferring the types of
     * attributes on targets that are not yet resolved.  This whole approach
     * needs to be replaced by dataflow analysis.
     */
    private NBinding makeProvisionalBinding(NType targetType) {
        if (targetType.isNative()) {
            return null;
        }

        Scope targetScope = targetType.getTable();

        // XXX:  Eventually we need to fix out all the cases where the path is
        // is empty here.  For now, avoid an IndexingException.
        if ("".equals(targetScope.getPath())) {
            return null;
        }

        NType utype = new NUnknownType();
        NBinding b = targetScope.putAttr(attr.id, null, utype, ATTRIBUTE);
        if (b != null) {
            b.setProvisional(true);
            utype.getTable().setPath(b.getQname());
        }
        return b;
    }

    @Override
    public String toString() {
        return "<Attribute:" + start() + ":" + target + "." + getAttributeName() + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(target, v);
            visitNode(attr, v);
        }
    }
}
