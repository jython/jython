/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Builtins;
import org.python.indexer.Indexer;
import org.python.indexer.NBinding;
import org.python.indexer.Scope;
import org.python.indexer.types.NDictType;
import org.python.indexer.types.NFuncType;
import org.python.indexer.types.NListType;
import org.python.indexer.types.NTupleType;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnknownType;

import java.util.ArrayList;
import java.util.List;

import static org.python.indexer.NBinding.Kind.ATTRIBUTE;
import static org.python.indexer.NBinding.Kind.CLASS;
import static org.python.indexer.NBinding.Kind.CONSTRUCTOR;
import static org.python.indexer.NBinding.Kind.FUNCTION;
import static org.python.indexer.NBinding.Kind.METHOD;
import static org.python.indexer.NBinding.Kind.PARAMETER;

public class NFunctionDef extends NNode {

    static final long serialVersionUID = 5495886181960463846L;

    public NName name;
    public List<NNode> args;
    public List<NNode> defaults;
    public NName varargs;  // *args
    public NName kwargs;   // **kwargs
    public NNode body;
    private List<NNode> decoratorList;

    public NFunctionDef(NName name, List<NNode> args, NBlock body, List<NNode> defaults,
                        NName varargs, NName kwargs) {
        this(name, args, body, defaults, kwargs, varargs, 0, 1);
    }

    public NFunctionDef(NName name, List<NNode> args, NBlock body, List<NNode> defaults,
                        NName varargs, NName kwargs, int start, int end) {
        super(start, end);
        this.name = name;
        this.args = args;
        this.body = body != null ? new NBody(body) : new NBlock(null);
        this.defaults = defaults;
        this.varargs = varargs;
        this.kwargs = kwargs;
        addChildren(name);
        addChildren(args);
        addChildren(defaults);
        addChildren(varargs, kwargs, this.body);
    }

    public void setDecoratorList(List<NNode> decoratorList) {
        this.decoratorList = decoratorList;
        addChildren(decoratorList);
    }

    public List<NNode> getDecoratorList() {
        if (decoratorList == null) {
            decoratorList = new ArrayList<NNode>();
        }
        return decoratorList;
    }

    @Override
    public boolean isFunctionDef() {
        return true;
    }

    @Override
    public boolean bindsName() {
        return true;
    }

    /**
     * Returns the name of the function for indexing/qname purposes.
     * Lambdas will return a generated name.
     */
    protected String getBindingName(Scope s) {
        return name.id;
    }

    @Override
    protected void bindNames(Scope s) throws Exception {
        Scope owner = s.getScopeSymtab();  // enclosing class, function or module

        setType(new NFuncType());
        Scope funcTable = new Scope(s.getEnclosingLexicalScope(), Scope.Type.FUNCTION);
        getType().setTable(funcTable);
        funcTable.setPath(owner.extendPath(getBindingName(owner)));

        // If we already defined this function in this scope, don't try it again.
        NType existing = owner.lookupType(getBindingName(owner), true /* local scope */);
        if (existing != null && existing.isFuncType()) {
            return;
        }

        bindFunctionName(owner);
        bindFunctionParams(funcTable);
        bindFunctionDefaults(s);
        bindMethodAttrs(owner);
    }

    protected void bindFunctionName(Scope owner) throws Exception {
        NBinding.Kind funkind = FUNCTION;
        if (owner.getScopeType() == Scope.Type.CLASS) {
            if ("__init__".equals(name.id)) {
                funkind = CONSTRUCTOR;
            } else {
                funkind = METHOD;
            }
        }
        NameBinder.make(funkind).bindName(owner, name, getType());
    }

    protected void bindFunctionParams(Scope funcTable) throws Exception {
        NameBinder param = NameBinder.make(PARAMETER);
        for (NNode a : args) {
            param.bind(funcTable, a, new NUnknownType());
        }
        if (varargs != null) {
            param.bind(funcTable, varargs, new NListType());
        }
        if (kwargs != null) {
            param.bind(funcTable, kwargs, new NDictType());
        }
    }

    /**
     * Processes any name-binding constructs appearing as parameter defaults.
     * For instance, in {@code def foo(converter=lambda name: name.upper()): ...}
     * the lambda is a name-binding construct.
     */
    protected void bindFunctionDefaults(Scope s) throws Exception {
        for (NNode n : defaults) {
            if (n.bindsName()) {
                n.bindNames(s);
            }
        }
    }

    protected void bindMethodAttrs(Scope owner) throws Exception {
        NType cls = Indexer.idx.lookupQnameType(owner.getPath());
        if (cls == null || !cls.isClassType()) {
            return;
        }
        // We don't currently differentiate between classes and instances.
        addReadOnlyAttr("im_class", cls, CLASS);
        addReadOnlyAttr("__class__", cls, CLASS);
        addReadOnlyAttr("im_self", cls, ATTRIBUTE);
        addReadOnlyAttr("__self__", cls, ATTRIBUTE);
    }

    protected NBinding addSpecialAttr(String name, NType atype, NBinding.Kind kind) {
        NBinding b = getTable().update(name,
                                       Builtins.newDataModelUrl("the-standard-type-hierarchy"),
                                       atype, kind);
        b.markSynthetic();
        b.markStatic();
        return b;
    }

    protected NBinding addReadOnlyAttr(String name, NType type, NBinding.Kind kind) {
        NBinding b = addSpecialAttr(name, type, kind);
        b.markReadOnly();
        return b;
    }

    @Override
    public NType resolve(Scope outer) throws Exception {
        resolveList(defaults, outer);
        resolveList(decoratorList, outer);

        Scope funcTable = getTable();
        NBinding selfBinding = funcTable.lookup("__self__");
        if (selfBinding != null && !selfBinding.getType().isClassType()) {
            selfBinding = null;
        }

        if (selfBinding != null) {
            if (args.size() < 1) {
                addWarning(name, "method should have at least one argument (self)");
            } else if (!(args.get(0) instanceof NName)) {
                addError(name, "self parameter must be an identifier");
            }
        }

        NTupleType fromType = new NTupleType();
        bindParamsToDefaults(selfBinding, fromType);

        if (varargs != null) {
            NBinding b = funcTable.lookupLocal(varargs.id);
            if (b != null) {
                fromType.add(b.getType());
            }
        }

        if (kwargs != null) {
            NBinding b = funcTable.lookupLocal(kwargs.id);
            if (b != null) {
                fromType.add(b.getType());
            }
        }

        NType toType = resolveExpr(body, funcTable);
        getType().asFuncType().setReturnType(toType);
        return getType();
    }

    private void bindParamsToDefaults(NBinding selfBinding, NTupleType fromType) throws Exception {
        NameBinder param = NameBinder.make(PARAMETER);
        Scope funcTable = getTable();

        for (int i = 0; i < args.size(); i++) {
            NNode arg = args.get(i);
            NType argtype = ((i == 0 && selfBinding != null)
                             ? selfBinding.getType()
                             : getArgType(args, defaults, i));
            param.bind(funcTable, arg, argtype);
            fromType.add(argtype);
        }
    }

    static NType getArgType(List<NNode> args, List<NNode> defaults, int argnum) {
        if (defaults == null) {
            return new NUnknownType();
        }
        int firstDefault = args.size() - defaults.size();
        if (firstDefault >= 0 && argnum >= firstDefault) {
            return defaults.get(argnum - firstDefault).getType();
        }
        return new NUnknownType();
    }

    @Override
    public String toString() {
        return "<Function:" + start() + ":" + name + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(name, v);
            visitNodeList(args, v);
            visitNodeList(defaults, v);
            visitNode(kwargs, v);
            visitNode(varargs, v);
            visitNode(body, v);
        }
    }
}
