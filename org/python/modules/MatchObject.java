// Copyright (c) Corporation for National Research Initiatives
package org.python.modules;

import org.python.core.*;
import org.apache.oro.text.regex.*;

public class MatchObject extends PyObject
{
    public String string;
    public int pos, endpos;
    public RegexObject re;
    private MatchResult match;

    public MatchObject(RegexObject re, String string,
                       int pos, int endpos, MatchResult match)
    {
        this.string = string;
        this.pos = pos;
        this.endpos = endpos;
        this.re = re;
        this.match = match;
    }

    public int start(int g) {
        return match.beginOffset(g);
    }

    public int start() {
        return start(0);
    }

    public int start(PyString s) {
        return start(getindex(s));
    }

    public int end(int g) {
        return match.endOffset(g);
    }

    public int end() {
        return end(0);
    }

    public int end(PyString s) {
        return end(getindex(s));
    }

    public PyTuple span(int g) {
        return new PyTuple(
            new PyObject[] {
                new PyInteger(start(g)),
                new PyInteger(end(g))
            });
    }

    public PyTuple span() {
        return span(0);
    }

    public PyTuple span(PyString s) {
        return span(getindex(s));
    }

    public PyTuple groups(PyObject defalt) {
        int n = match.groups()-1;
        PyObject[] ret = new PyObject[n];
        for(int i=0; i<n; i++) {
            String tmp = match.group(i+1);
            if (tmp == null) {
                ret[i] = defalt;
            } else {
                ret[i] = new PyString(tmp);
            }
        }
        return new PyTuple(ret);
    }

    public PyTuple groups() {
        return groups(Py.None);
    }

    private int getindex(PyString s) {
        PyInteger v = (PyInteger)re.groupindex.__finditem__(s);
        if (v == null)
            throw Py.IndexError("group '"+s+"' is undefined");
        return v.getValue();
    }

    private String group(int i) {
        if (i >= match.groups()) {
            throw Py.IndexError("group "+i+" is undefined");
        }
        return match.group(i);
    }

    private String group(PyString s) {
        return group(getindex(s));
    }

    private PyObject group(PyObject o) {
        String s;

        if (o instanceof PyInteger) {
            s = group(((PyInteger)o).getValue());
        } else if (o instanceof PyString) {
            s = group((PyString)o);
        } else {
            throw org.python.modules.re.ReError(
                "group index must be a string or integer");
        }
        if (s == null)
            return Py.None;
        else
            return new PyString(s);
    }

    public PyObject group(PyObject[] args) {
        int n = args.length;

        if (n == 0)
            return new PyString(group(0));
        if (n == 1)
            return group(args[0]);

        PyObject[] res = new PyObject[n];
        for(int i=0; i < n; i++) {
            res[i] = group(args[i]);
        }
        return new PyTuple(res);
    }

    public PyObject groupdict() {
        return groupdict(Py.None);
    }

    public PyObject groupdict(PyObject defalt) {
        PyDictionary dict = new PyDictionary();
        PyList items = re.groupindex.items();

        for (int i=0; i < items.__len__(); i++) {
            PyTuple t = (PyTuple)items.__getitem__(i);
            PyString name = (PyString)t.__getitem__(0);
            PyInteger index = (PyInteger)t.__getitem__(1);

            String s = group(index.getValue());
            if (s == null)
                dict.__setitem__(name, defalt);
            else
                dict.__setitem__(name, new PyString(s));
        }
        return dict;
    }
}
