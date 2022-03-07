/*
 * Copyright 2000 Finn Bock
 *
 * This program contains material copyrighted by:
 * Copyright (c) 1997-2000 by Secret Labs AB.  All rights reserved.
 *
 * This version of the SRE library can be redistributed under CNRI's
 * Python 1.6 license.  For any other use, please contact Secret Labs
 * AB (info@pythonware.com).
 *
 * Portions of this engine have been developed in cooperation with
 * CNRI.  Hewlett-Packard provided funding for 1.6 integration and
 * other compatibility work.
 */

package org.python.modules.sre;

import org.python.core.*;

import java.math.BigInteger;


public class MatchObject extends PyObject implements Traverseproc {
    public PyString string; /* link to the target string */
    public PyObject regs; /* cached list of matching spans */
    PatternObject pattern; /* link to the regex (pattern) object */
    int pos, endpos; /* current target slice */
    int lastindex; /* last index marker seen by the engine (-1 if none) */
    int groups; /* number of groups (start/end marks) */
    int[] mark;

    
    public PyObject expand(PyObject[] args) {
        if(args.length == 0) {
            throw Py.TypeError("expand() takes exactly 1 argument (0 given)");
        }
        PyObject mod = imp.importName("re", true);
        PyObject func = mod.__getattr__("_expand");
        return func.__call__(new PyObject[] {pattern, this, args[0]});
    }

    public PyObject group(PyObject[] args) {
        switch (args.length) {
        case 0:
            return getslice(Py.Zero, Py.None);
        case 1:
            return getslice(args[0], Py.None);
        default:
            PyObject[] result = new PyObject[args.length];
            for (int i = 0; i < args.length; i++)
                result[i] = getslice(args[i], Py.None);
            return new PyTuple(result);
        }
    }

    public PyObject groups(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("groups", args, kws, "default");
        PyObject def = ap.getPyObject(0, Py.None);

        PyObject[] result = new PyObject[groups-1];
        for (int i = 1; i < groups; i++) {
            result[i-1] = getslice_by_index(i, def);
        }
        return new PyTuple(result);
    }

    public PyObject groupdict(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("groupdict", args, kws, "default");
        PyObject def = ap.getPyObject(0, Py.None);

        PyObject result = new PyDictionary();

        if (pattern.groupindex == null)
            return result;

        PyObject keys = pattern.groupindex.invoke("keys");

        PyObject key;
        for (int i = 0; (key = keys.__finditem__(i)) != null; i++) {
            PyObject item = getslice(key, def);
            result.__setitem__(key, item);
        }
        return result;
    }

    public PyObject start() {
        return start(Py.Zero);
    }

    public PyObject start(PyObject index_) {
        int index = getindex(index_);

        if (index < 0 || index >= groups)
            throw Py.IndexError("no such group");

        return Py.newInteger(mark[index*2]);
    }

    public PyObject end() {
        return end(Py.Zero);
    }

    public PyObject end(PyObject index_) {
        int index = getindex(index_);

        if (index < 0 || index >= groups)
            throw Py.IndexError("no such group");

        return Py.newInteger(mark[index*2+1]);
    }

    public PyTuple span() {
        return span(Py.Zero);
    }

    public PyTuple span(PyObject index_) {
        int index = getindex(index_);

        if (index < 0 || index >= groups)
            throw Py.IndexError("no such group");

        int start = mark[index*2];
        int end = mark[index*2+1];

        return _pair(start, end);
    }

    public PyObject regs() {

        PyObject[] regs = new PyObject[groups];

        for (int index = 0; index < groups; index++) {
            regs[index] = _pair(mark[index*2], mark[index*2+1]);
        }

        return new PyTuple(regs);
    }


    PyTuple _pair(int i1, int i2) {
        return new PyTuple(Py.newInteger(i1), Py.newInteger(i2));
    }

    private PyObject getslice(PyObject index, PyObject def) {
        return getslice_by_index(getindex(index), def);
    }

    private int getindex(PyObject index) {
        if (index instanceof PyInteger)
            return ((PyInteger) index).getValue();
        if (index instanceof PyLong) {
            BigInteger idx = ((PyLong) index).getValue();
            if (idx.compareTo(PyInteger.MAX_INT) == 1) {
                throw Py.IndexError("no such group");
            } else {
                return idx.intValue();
            }
        }

        int i = -1;

        if (pattern.groupindex != null) {
            index = pattern.groupindex.__finditem__(index);
            if (index != null)
                if (index instanceof PyInteger)
                    return ((PyInteger) index).getValue();
        }
        return i;
    }

    private PyObject getslice_by_index(int index, PyObject def) {
        if (index < 0 || index >= groups)
            throw Py.IndexError("no such group");

        index *= 2;
        int start = mark[index];
        int end = mark[index+1];

        //System.out.println("group:" + index + " " + start + " " +
        //                   end + " l:" + string.length());

        if (string == null || start < 0)
            return def;
        return string.__getslice__(Py.newInteger(start), Py.newInteger(end));

    }

    public PyObject __findattr_ex__(String key) {
        if (key == null) {
            return null;
        }
        //System.out.println("__findattr__:" + key);
        switch (key) {
            case "flags":
                return Py.newInteger(pattern.flags);
            case "groupindex":
                return pattern.groupindex;
            case "re":
                return pattern;
            case "pos":
                return Py.newInteger(pos);
            case "endpos":
                return Py.newInteger(endpos);
            case "lastindex":
                return lastindex == -1 ? Py.None : Py.newInteger(lastindex);
            case "lastgroup":
                if (pattern.indexgroup != null && lastindex >= 0) {
                    return pattern.indexgroup.__getitem__(lastindex);
                }
                return Py.None;
            case "regs":
                return regs();
            default:
                return super.__findattr_ex__(key);
        }
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        int retVal;
        if (pattern != null) {
            retVal = visit.visit(pattern, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (string != null) {
            retVal = visit.visit(string, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        return regs != null ? visit.visit(regs, arg) : 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && (ob == pattern || ob == string || ob == regs);
    }
}
