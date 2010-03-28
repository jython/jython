/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.types;

import org.python.indexer.Indexer;
import org.python.indexer.NBinding;
import org.python.indexer.Scope;
import org.python.indexer.Util;
import org.python.indexer.ast.NAssign;
import org.python.indexer.ast.NList;
import org.python.indexer.ast.NName;
import org.python.indexer.ast.NNode;
import org.python.indexer.ast.NStr;

import java.util.ArrayList;
import java.util.List;

public class NModuleType extends NType {

    private String file;
    private String name;
    private String qname;

    public NModuleType() {
    }

    public NModuleType(String name, String file, Scope parent) {
        this.name = name;
        this.file = file;  // null for builtin modules
        if (file != null) {
            // This will return null iff specified file is not prefixed by
            // any path in the module search path -- i.e., the caller asked
            // the indexer to load a file not in the search path.
            qname = Util.moduleQname(file);
        }
        if (qname == null) {
            qname = name;
        }
        setTable(new Scope(parent, Scope.Type.MODULE));
        getTable().setPath(qname);

        // null during bootstrapping of built-in types
        if (Indexer.idx.builtins != null) {
            getTable().addSuper(Indexer.idx.builtins.BaseModule.getTable());
        }
    }

    public void setFile(String file) {
      this.file = file;
    }

    public String getFile() {
      return file;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void setQname(String qname) {
      this.qname = qname;
    }

    public String getQname() {
      return qname;
    }

    @Override
    public void printKids(CyclicTypeRecorder ctr, StringBuilder sb) {
        sb.append(qname);
    }
}
