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

import java.util.*;
import org.python.core.*;

public class PatternObject extends PyObject {
    char[] code; /* link to the code string object */
    public String pattern; /* link to the pattern source (or None) */
    public int groups;
    public org.python.core.PyObject groupindex;
    public int flags;
    org.python.core.PyObject indexgroup;
    public int codesize;


    public PatternObject(PyString pattern, int flags, char[] code,
            int groups, PyObject groupindex, PyObject indexgroup) {

        if (pattern != null)
            this.pattern = pattern.toString();
        this.flags   = flags;
        this.code    = code;
        this.codesize = code.length;
        this.groups  = groups;
        this.groupindex = groupindex;
        this.indexgroup = indexgroup;
    }

    public MatchObject match(String string) {
        return match(string, 0, Integer.MAX_VALUE);
    }

    public MatchObject match(String string, int start) {
        return match(string, start, Integer.MAX_VALUE);
    }

    public MatchObject match(String string, int start, int end) {
        SRE_STATE state = new SRE_STATE(string, start, end, flags);

        state.ptr = state.start;
        int status = state.SRE_MATCH(code, 0, 1);

        return _pattern_new_match(state, string, status);
    }




    public MatchObject search(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("search", args, kws,
                                     "pattern", "pos", "endpos");
        String string = ap.getString(0);
        int start = ap.getInt(1, 0);
        int end = ap.getInt(2, string.length());

        SRE_STATE state = new SRE_STATE(string, start, end, flags);

        int status = state.SRE_SEARCH(code, 0);

        return _pattern_new_match(state, string, status);
    }


    public PyObject sub(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("sub", args, kws,
                                     "repl", "string", "count");
        PyObject template = ap.getPyObject(0);
        String string = ap.getString(1);
        int count = ap.getInt(2, 0);

        return subx(template, string, count, false);
    }



    public PyObject subn(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("subn", args, kws,
                                     "repl", "string", "count");
        PyObject template = ap.getPyObject(0);
        String string = ap.getString(1);
        int count = ap.getInt(2, 0);

        return subx(template, string, count, true);
    }


    private PyObject subx(PyObject template, String string, int count,
                          boolean subn)
    {
        PyObject filter = null;
        boolean filter_is_callable = false;
        if (template.isCallable()) {
            filter = template;
            filter_is_callable = true;
        } else {
            boolean literal = false;
            if (template instanceof PyString) {
                literal = template.toString().indexOf('\\') < 0;
            }
            if (literal) {
                filter = template;
                filter_is_callable = false;
            } else {
                filter = call("sre", "_subx", new PyObject[] {
                    this, template});
                filter_is_callable = filter.isCallable();
            }
        }

        SRE_STATE state = new SRE_STATE(string, 0, Integer.MAX_VALUE, flags);

        StringBuffer buf = new StringBuffer();

        int n = 0;
        int i = 0;
        
        while (count == 0 || n < count) {
            state.state_reset();
            state.ptr = state.start;
            int status = state.SRE_SEARCH(code, 0);
            if (status <= 0) {
                if (status == 0)
                    break;
                _error(status);
            }
            int b = state.start;
            int e = state.ptr;

            if (i < b) {
                /* get segment before this match */
                buf.append(string.substring(i, b));
            }
            if (! (i == b && i == e && n > 0)) {
                PyObject item;
                if (filter_is_callable) {
                    /* pass match object through filter */
                    MatchObject match = _pattern_new_match(state, string, 1);
                    item = filter.__call__(match);
                } else {
                    item = filter;
                }
    
                if (item != Py.None) {
                    buf.append(item.toString());
                }
                i = e;
                n++;
            }

            /* move on */
            if (state.ptr == state.start)
                state.start = state.ptr + 1;
            else
                state.start = state.ptr;
        }
        if (i < state.endpos) {
            buf.append(string.substring(i, state.endpos));
        }

        if (subn)
            return new PyTuple(new PyObject[] {
                Py.newString(buf.toString()), Py.newInteger(n)
            });
        else
            return Py.newString(buf.toString());
    }


    public PyObject split(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("split", args, kws,
                                     "source", "maxsplit");
        String string = ap.getString(0);
        int maxsplit = ap.getInt(1, 0);

        SRE_STATE state = new SRE_STATE(string, 0, Integer.MAX_VALUE, flags);

        PyList list = new PyList();

        int n = 0;
        int last = state.start;
        while (maxsplit == 0 || n < maxsplit) {
            state.state_reset();
            state.ptr = state.start;
            int status = state.SRE_SEARCH(code, 0);
            if (status <= 0) {
                if (status == 0)
                    break;
                _error(status);
            }
            if (state.start == state.ptr) {
                if (last == state.end)
                    break;
                /* skip one character */
                state.start = state.ptr + 1;
                continue;
            }

            /* get segment before this match */
            PyObject item = Py.newString(string.substring(last, state.start));
            list.append(item);

            for (int i = 0; i < groups; i++) {
                String s = state.getslice(i+1, string, false);
                if (s != null)
                    list.append(Py.newString(s));
                else
                    list.append(Py.None);
            }
            n += 1;
            last = state.start = state.ptr;
        }

        PyObject item = Py.newString(string.substring(last, state.endpos));
        list.append(item);

        return list;
    }

    private PyObject call(String module, String function, PyObject[] args) {
        PyObject sre = imp.importName(module, true);
        return sre.invoke(function, args);
    }



    public PyObject findall(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("findall", args, kws,
                                     "source", "pos", "endpos");
        String string = ap.getString(0);
        int start = ap.getInt(1, 0);
        int end = ap.getInt(2, Integer.MAX_VALUE);

        SRE_STATE state = new SRE_STATE(string, start, end, flags);

        Vector list = new Vector();

        while (state.start <= state.end) {
            state.state_reset();
            state.ptr = state.start;
            int status = state.SRE_SEARCH(code, 0);
            if (status > 0) {
                PyObject item;

                /* don't bother to build a match object */
                switch (groups) {
                case 0:
                    item = Py.newString(
                                    string.substring(state.start, state.ptr));
                    break;
                case 1:
                    item = Py.newString(state.getslice(1, string, true));
                    break;
                default:
                    PyObject[] t = new PyObject[groups];
                    for (int i = 0; i < groups; i++)
                        t[i] = Py.newString(state.getslice(i+1, string, true));
                    item = new PyTuple(t);
                    break;
                }

                list.addElement(item);

                if (state.ptr == state.start)
                    state.start = state.ptr + 1;
                else
                    state.start = state.ptr;
            } else {

                if (status == 0)
                    break;

                _error(status);
            }
        }
        return new PyList(list);
    }


/* Enable "finditer" when iter support is added.
    public PyObject finditer(String string) {
        return finditer(string, 0, Integer.MAX_VALUE);
    }

    public PyObject finditer(String string, int start) {
        return finditer(string, start, Integer.MAX_VALUE);
    }

    public PyObject finditer(String string, int start, int end) {
        ScannerObject scanner = scanner(string, start, end);
        PyObject search = scanner.__findattr__("search");
        return new PyCallIterator(search, Py.None);
    }
*/

    public ScannerObject scanner(String string) {
        return scanner(string, 0, Integer.MAX_VALUE);
    }

    public ScannerObject scanner(String string, int start) {
        return scanner(string, start, Integer.MAX_VALUE);
    }

    public ScannerObject scanner(String string, int start, int end) {
        ScannerObject self = new ScannerObject();
        self.state = new SRE_STATE(string, start, end, flags);
        self.pattern = this;
        self.string = string;
        return self;
    }


    private void _error(int status) {
        if (status == SRE_STATE.SRE_ERROR_RECURSION_LIMIT)
            throw Py.RuntimeError("maximum recursion limit exceeded");

        throw Py.RuntimeError("internal error in regular expression engine");
    }


    MatchObject _pattern_new_match(SRE_STATE state, String string,
                                   int status)
    {
        /* create match object (from state object) */

        //System.out.println("status = " +  status + " " + string);

        if (status > 0) {
            /* create match object (with room for extra group marks) */
            MatchObject match = new MatchObject();
            match.pattern = this;
            match.string = string;
            match.regs = null;
            match.groups = groups+1;
            /* group zero */
            int base = state.beginning;

            match.mark = new int[match.groups*2];
            match.mark[0] = state.start - base;
            match.mark[1] = state.ptr - base;

            /* fill in the rest of the groups */
            int i, j;
            for (i = j = 0; i < groups; i++, j+=2) {
                if (j+1 <= state.lastmark && state.mark[j] != -1 &&
                                                    state.mark[j+1] != -1) {
                    match.mark[j+2] = state.mark[j] - base;
                    match.mark[j+3] = state.mark[j+1] - base;
                } else
                    match.mark[j+2] = match.mark[j+3] = -1;
            }
            match.pos = state.pos;
            match.endpos = state.endpos;
            match.lastindex = state.lastindex;

            return match;
        } else if (status == 0) {
            return null;
        }

        _error(status);
        return null;
    }
}


