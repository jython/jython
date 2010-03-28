/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.types;

import org.python.indexer.Indexer;
import org.python.indexer.NBinding;

import java.util.HashSet;
import java.util.Set;

/**
 * A union type is a set of several other types. During a union operation,
 * destructuring happens and unknown types are unified.
 */
public class NUnionType extends NType {

    /**
     * Union types can lead to infinite recursion in the occurs check.  Until
     * we've got a handle on these cases, I'm limiting the recursion depth.
     *
     * @see http://www.cs.kuleuven.ac.be/~dtai/projects/ALP/newsletter/archive_93_96/net/impl/occur.html
     *      for an interesting and highly relevant discussion.
     */
    private static final int MAX_RECURSION_DEPTH = 15;

    private Set<NType> types;

    public NUnionType() {
        this.types = new HashSet<NType>();
    }

    public NUnionType(NType... initialTypes) {
        this();
        for (NType nt : initialTypes) {
            addType(nt);
        }
    }

    public void setTypes(Set<NType> types) {
      this.types = types;
    }

    public Set<NType> getTypes() {
      return types;
    }

    public void addType(NType t) {
        if (t == null) {
            throw new IllegalArgumentException("null type");
        }
        if (t.isUnionType()) {
            types.addAll(t.asUnionType().types);
        } else {
            types.add(t);
        }
    }

    public boolean contains(NType t) {
        return types.contains(t);
    }

    public static NType union(NType u, NType v) {
        NType wu = NUnknownType.follow(u);
        NType wv = NUnknownType.follow(v);
        if (wu == wv) {
            return u;
        }

        // This is a bit unconventional, as most type inferencers try to
        // determine whether a given name can ever take a null value.  However,
        // doing so complicates the logic and proliferates union types, arguably
        // with little benefit for Python.  So for now, X|None => X.
        if (wu == Indexer.idx.builtins.None) {
            return v;
        }
        if (wv == Indexer.idx.builtins.None) {
            return u;
        }

        if (wu.isUnknownType() && !occurs(wu, wv, 0)) {
            NUnknownType.point(wu, wv);
            return u;
        }

        if (wv.isUnknownType() && !occurs(wv, wu, 0)) {
            NUnknownType.point(wv, wu);
            return v;
        }
        if (wu.isTupleType() && wv.isTupleType()) {
            NTupleType tu = (NTupleType)wu;
            NTupleType tv = (NTupleType)wv;
            if (tu.getElementTypes().size() == tv.getElementTypes().size()) {
                NTupleType ret = new NTupleType();
                for (int i = 0; i < tu.getElementTypes().size(); i++) {
                    ret.add(union(tu.getElementTypes().get(i), tv.getElementTypes().get(i)));
                }
                return ret;
            }
            return newUnion(wu, wv);
        }
        if (wu.isListType() && wv.isListType()) {
            return new NListType(union(wu.asListType().getElementType(),
                                       wv.asListType().getElementType()));
        }
        if (wu.isDictType() && wv.isDictType()) {
            NDictType du = (NDictType)wu;
            NDictType dv = (NDictType)wv;
            return new NDictType(union(du.getKeyType(), dv.getKeyType()),
                                 union(du.getValueType(), dv.getValueType()));
        }
        if (wu.isFuncType() && wv.isFuncType()) {
            return new NFuncType(NUnionType.union(wu.asFuncType().getReturnType(),
                                                  wv.asFuncType().getReturnType()));
        }

        // XXX:  see comments in NInstanceType
        if (wu.isFuncType() && wv.isClassType()) {
            // NUnknownType.point(wu.asFuncType().getReturnType(), new NInstanceType(wv));
            NUnknownType.point(wu.asFuncType().getReturnType(), wv);
            NUnknownType.point(u, wv);
            return u;
        }
        if (wu.isClassType() && wv.isFuncType()) {
            // NUnknownType.point(wv.asFuncType().getReturnType(), new NInstanceType(wu));
            NUnknownType.point(wv.asFuncType().getReturnType(), wu);
            NUnknownType.point(v, wu);
            return v;
        }

        return newUnion(wu, wv);
    }

    /**
     * @see http://en.wikipedia.org/wiki/Occurs_check
     */
    private static boolean occurs(NType u, NType v, int depth) {
        if (depth++ > MAX_RECURSION_DEPTH) {
            return true;
        }

        u = NUnknownType.follow(u);
        v = NUnknownType.follow(v);
        if (u == v) {
            return true;
        }

        if (v.isTupleType()) {
            for (NType vv : v.asTupleType().getElementTypes()) {
                if (occurs(u, vv, depth)) {
                    return true;
                }
            }
            return false;
        }

        if (v.isListType()) {
            return occurs(u, v.asListType().getElementType(), depth);
        }

        if (v.isDictType()) {
            return occurs(u, v.asDictType().getKeyType(), depth)
                || occurs(u, v.asDictType().getValueType(), depth);
        }

        if (v.isFuncType()) {
            // A function type appearing in its own return type can happen
            // (e.g. def foo(): return [foo]), and causes infinite recursion if
            // we don't check for it
            NType ret = v.asFuncType().getReturnType();
            if (occurs(v, ret, depth)) {
                return true;
            }
            return occurs(u, ret, depth);
        }

        if (v.isUnionType()) {
            for (NType vv : v.asUnionType().types) {
                if (occurs(u, vv, depth)) {
                    return true;
                }
            }
            return false;
        }

        return false;
    }

    public static NUnionType newUnion(NType... types) {
        NUnionType ret = new NUnionType();
        for (NType type : types) {
            ret.addType(type);
        }
        return ret;
    }

    /**
     * Returns the first alternate whose type is not unknown.
     * @return the first non-unknown alternate, or {@code null} if none found
     */
    public NType firstKnownAlternate() {
        for (NType type : types) {
            if (!type.follow().isUnknownType()) {
                return type;
            }
        }
        return null;
    }

    /**
     * Returns the first alternate whose type is not unknown and
     * is not {@link Indexer.idx.builtins.None}.
     * @return the first non-unknown, non-{@code None} alternate, or {@code null} if none found
     */
    public NType firstKnownNonNullAlternate() {
        for (NType type : types) {
            NType tt = type.follow();
            if (!tt.isUnknownType() && tt != Indexer.idx.builtins.None) {
                return type;
            }
        }
        return null;
    }

    @Override
    public void printKids(CyclicTypeRecorder ctr, StringBuilder sb) {
        sb.append("[");
        for (NType u : types) {
            u.print(ctr, sb);
            sb.append(",");
        }
        sb.setLength(sb.length() - 1);  // pop last comma
        sb.append("]");
    }
}
