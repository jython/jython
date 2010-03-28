/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.ast;

import org.python.indexer.Def;
import org.python.indexer.Indexer;
import org.python.indexer.NBinding;
import org.python.indexer.Scope;
import org.python.indexer.Util;
import org.python.indexer.types.NModuleType;
import org.python.indexer.types.NType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class NModule extends NNode {

    static final long serialVersionUID = -7737089963380450802L;

    public String name;
    public NBody body;

    private String file;  // input source file path
    private String md5;   // input source file md5

    public NModule() {
    }

    public NModule(String name) {
        this.name = name;
    }

    public NModule(NBlock body, int start, int end) {
        super(start, end);
        this.body = new NBody(body);
        addChildren(this.body);
    }

    public void setFile(String file) throws Exception {
        this.file = file;
        this.name = Util.moduleNameFor(file);
        this.md5 = Util.getMD5(new File(file));
    }

    public void setFile(File path) throws Exception {
        file = path.getCanonicalPath();
        name = Util.moduleNameFor(file);
        md5 = Util.getMD5(path);
    }

    /**
     * Used when module is parsed from an in-memory string.
     * @param path file path
     * @param md5 md5 message digest for source contents
     */
    public void setFileAndMD5(String path, String md5) throws Exception {
        file = path;
        name = Util.moduleNameFor(file);
        this.md5 = md5;
    }

    @Override
    public String getFile() {
        return file;
    }

    public String getMD5() {
        return md5;
    }

    @Override
    public NType resolve(Scope s) throws Exception {
        NBinding mb = Indexer.idx.moduleTable.lookupLocal(file);
        if (mb == null ) {
            Indexer.idx.reportFailedAssertion("No module for " + name + ": " + file);
            setType(new NModuleType(name, file, s));
        } else {
            setType(mb.getType());
        }

        resolveExpr(body, getTable());

        resolveExportedNames();
        return getType();
    }

    /**
     * If the module defines an {@code __all__} variable, resolve references
     * to each of the elements.
     */
    private void resolveExportedNames() throws Exception {
        NModuleType mtype = null;
        NType thisType = getType();
        if (thisType.isModuleType()) {
            mtype = thisType.asModuleType();
        } else if (thisType.isUnionType()) {
            for (NType u : thisType.asUnionType().getTypes()) {
                if (u.isModuleType()) {
                    mtype = u.asModuleType();
                    break;
                }
            }
        }

        if (mtype == null) {
            Indexer.idx.reportFailedAssertion("Found non-module type for "
                                              + this + " in " + getFile() + ": " + thisType);
            return;
        }

        Scope table = mtype.getTable();
        for (NStr nstr : getExportedNameNodes()) {
            String name = nstr.n.toString();
            NBinding b = table.lookupLocal(name);
            if (b != null) {
                Indexer.idx.putLocation(nstr, b);
            }
        }
    }

    /**
     * Attempt to determine the actual value of the "__all__" variable in the
     * target module.  If we can parse it, return the list of exported names.<p>
     *
     * @return the list of exported names.  Returns {@code null} if __all__ is
     *         missing, or if its initializer is not a simple list of strings.
     *         We don't generate a warning, since complex expressions such as
     *         {@code __all__ = [name for name in dir() if name[0] == "e"]}
     *         are valid provided the expr result is a string list.
     */
    public List<String> getExportedNames() throws Exception {
        List<String> exports = new ArrayList<String>();
        if (!getType().isModuleType()) {
            return exports;
        }
        for (NStr nstr : getExportedNameNodes()) {
            exports.add(nstr.n.toString());
        }
        return exports;
    }

    /**
     * If the module defines an {@code __all__} variable, returns the string
     * elements of the variable's list value.
     * @return any exported name nodes found, or an empty list if none found
     */
    public List<NStr> getExportedNameNodes() throws Exception {
        List<NStr> exports = new ArrayList<NStr>();
        if (!getType().isModuleType()) {
            return exports;
        }
        NBinding all = getTable().lookupLocal("__all__");
        if (all== null) {
            return exports;
        }
        Def def = all.getSignatureNode();
        if (def == null) {
            return exports;
        }
        NNode __all__ = getDeepestNodeAtOffset(def.start());
        if (!(__all__ instanceof NName)) {
            return exports;
        }
        NNode assign = __all__.getParent();
        if (!(assign instanceof NAssign)) {
            return exports;
        }
        NNode rvalue = ((NAssign)assign).rvalue;
        if (!(rvalue instanceof NList)) {
            return exports;
        }
        for (NNode elt : ((NList)rvalue).elts) {
            if (elt instanceof NStr) {
                NStr nstr = (NStr)elt;
                if (nstr.n != null) {
                    exports.add(nstr);
                }
            }
        }
        return exports;
    }

    public String toLongString() {
        return "<Module:" + body + ">";
    }

    @Override
    public String toString() {
        return "<Module:" + getFile() + ">";
    }

    @Override
    public void visit(NNodeVisitor v) {
        if (v.visit(this)) {
            visitNode(body, v);
        }
    }
}
