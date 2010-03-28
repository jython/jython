/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.types;

import org.python.indexer.Scope;

/**
 * Represents an instance of a class.  Currently there is nothing in
 * the indexer that needs to distinguish instances from classes -- both
 * classes and their instances are merged into a single type.
 */
public class NInstanceType extends NType {

    private NType classType;

    public NInstanceType() {
        classType = new NUnknownType();
    }

    public NInstanceType(NType c) {
        this.setTable(c.getTable().copy(Scope.Type.INSTANCE));
        this.getTable().addSuper(c.getTable());
        this.getTable().setPath(c.getTable().getPath());
        classType = c;
    }

    public NType getClassType() {
        return classType;
    }

    @Override
    public void printKids(CyclicTypeRecorder ctr, StringBuilder sb) {
        classType.print(ctr, sb);
    }
}
