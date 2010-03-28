/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.types;

import org.python.indexer.Builtins;
import org.python.indexer.Indexer;
import org.python.indexer.Scope;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class NType {

    private Scope table;

    protected static final String LIBRARY_URL = Builtins.LIBRARY_URL;
    protected static final String TUTORIAL_URL = Builtins.TUTORIAL_URL;
    protected static final String REFERENCE_URL = Builtins.REFERENCE_URL;

    private static Pattern INSTANCE_TAG = Pattern.compile("(.+?)=#([0-9]+)");

    public NType() {
    }

    public void setTable(Scope table) {
        this.table = table;
    }

    public Scope getTable() {
        if (table == null) {
            table = new Scope(null, Scope.Type.SCOPE);
        }
        return table;
    }

    /**
     * Returns {@link NUnknownType#follow} of this type.
     */
    public NType follow() {
        return NUnknownType.follow(this);
    }

    /**
     * Returns {@code true} if this Python type is implemented in native code
     * (i.e., C, Java, C# or some other host language.)
     */
    public boolean isNative() {
        return Indexer.idx.builtins.isNative(this);
    }

    public boolean isClassType() {
        return this instanceof NClassType;
    }

    public boolean isDictType() {
        return this instanceof NDictType;
    }

    public boolean isFuncType() {
        return this instanceof NFuncType;
    }

    public boolean isInstanceType() {
        return this instanceof NInstanceType;
    }

    public boolean isListType() {
        return this instanceof NListType;
    }

    public boolean isModuleType() {
        return this instanceof NModuleType;
    }

    public boolean isNumType() {
        return this == Indexer.idx.builtins.BaseNum;
    }

    public boolean isStrType() {
        return this == Indexer.idx.builtins.BaseStr;
    }

    public boolean isTupleType() {
        return this instanceof NTupleType;
    }

    public boolean isUnionType() {
        return this instanceof NUnionType;
    }

    public boolean isUnknownType() {
        return this instanceof NUnknownType;
    }

    public NClassType asClassType() {
        return (NClassType)this;
    }

    public NDictType asDictType() {
        return (NDictType)this;
    }

    public NFuncType asFuncType() {
        return (NFuncType)this;
    }

    public NInstanceType asInstanceType() {
        return (NInstanceType)this;
    }

    public NListType asListType() {
        return (NListType)this;
    }

    public NModuleType asModuleType() {
        return (NModuleType)this;
    }

    public NTupleType asTupleType() {
        return (NTupleType)this;
    }

    public NUnionType asUnionType() {
        return (NUnionType)this;
    }

    public NUnknownType asUnknownType() {
        return (NUnknownType)this;
    }

    @Override
    public String toString() {
        StringBuilder input = new StringBuilder();
        print(new CyclicTypeRecorder(), input);

        // Postprocess to remove unused instance reference numbers.
        StringBuilder sb = new StringBuilder(input.length());
        Matcher m = INSTANCE_TAG.matcher(input.toString());
        int end = -1;
        while (m.find()) {
            end = m.end();
            int num = Integer.parseInt(m.group(2));
            if (input.indexOf("<#" + num + ">") == -1) {  // referenced?
                sb.append(m.group(1));  // skip tag
            } else {
                sb.append(m.group());  // whole thing
            }
        }
        if (end != -1) {
            sb.append(input.substring(end));
        }

        return sb.toString();
    }

    /**
     * Internal class to support printing in the presence of type-graph cycles.
     */
    protected class CyclicTypeRecorder {
        int count = 0;
        private Map<NType, Integer> elements = new HashMap<NType, Integer>();

        /**
         * Get the instance number for the specified type.
         * @return the instance number:  positive if the type was already recorded,
         * or its negative if the type was just recorded and assigned a number.
         */
        public int fetch(NType t) {
            Integer i = elements.get(t);
            if (i != null) {
                return i;
            }
            i = ++count;
            elements.put(t, i);
            return -i;
        }
    }

    /**
     * Internal method to support printing in the presence of type-graph cycles.
     */
    protected void print(CyclicTypeRecorder ctr, StringBuilder sb) {
        int num = ctr.fetch(this);
        if (num > 0) {
            sb.append("<#").append(num).append(">");
        } else {
            String tag = getClass().getName();
            tag = tag.substring(tag.lastIndexOf(".") + 2);
            sb.append("<").append(tag).append("=#").append(-num).append(":");
            printKids(ctr, sb);
            sb.append(">");
        }
    }

    /**
     * Internal method to support printing in the presence of type-graph cycles.
     */
    protected abstract void printKids(CyclicTypeRecorder ctr, StringBuilder sb);
}
