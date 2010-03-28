/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.types;

import org.python.indexer.Scope;
import org.python.indexer.Util;

public class NClassType extends NType {

    private String name;

    public NClassType() {
        this("<unknown>", null);
    }

    public NClassType(String name, Scope parent) {
        this.name = name;
        this.setTable(new Scope(parent, Scope.Type.CLASS));
        if (parent != null) {
            this.getTable().setPath(parent.extendPath(name));
        } else {
            this.getTable().setPath(name);
        }
    }

    public NClassType(String name, Scope parent, NClassType superClass) {
        this(name, parent);
        if (superClass != null) {
            addSuper(superClass);
        }
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void addSuper(NType sp) {
        getTable().addSuper(sp.getTable());
    }

    @Override
    public void printKids(CyclicTypeRecorder ctr, StringBuilder sb) {
        sb.append(name);
    }
}
