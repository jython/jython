//copyright 2000 Finn Bock

package org.python.modules;

import org.python.core.*;


public class sha {
    public int blocksize = 1;
    public int digestsize = 20;

    public static String __doc__ =
        "* Cryptix General License\n" +
        "* Copyright © 1995, 1996, 1997, 1998, 1999, 2000 The Cryptix Foundation\n"+
        "* Limited. 	All rights reserved.\n" +
        "* \n" +
        "* Redistribution and use in source and binary forms, with or\n" +
        "* without modification, are permitted provided that the\n" + 
        "* following conditions are met:\n" +
        "*\n" +
        "* - Redistributions of source code must retain the copyright\n" +
        "*   notice, this list of conditions and the following disclaimer.\n" +
        "* - Redistributions in binary form must reproduce the above\n" +
        "*   copyright notice, this list of conditions and the following\n" + 
        "*   disclaimer in the documentation and/or other materials\n" +
        "*   provided with the distribution.\n" +
        "*\n" +
        "* THIS SOFTWARE IS PROVIDED BY THE CRYPTIX FOUNDATION LIMITED\n" +
        "* AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS OR IMPLIED\n" +
        "* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED\n" +
        "* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR\n" +
        "* PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE CRYPTIX\n" +
        "* FOUNDATION LIMITED OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,\n" +
        "* INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL\n" +
        "* DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF\n" +
        "* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;\n" +
        "* OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY\n" +
        "* OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT\n" +
        "* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF\n" +
        "* THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY\n" +
        "* OF SUCH DAMAGE.\n";


    public static SHA1 new$(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser(args, kws, "string");
        String cp = ap.getString(0);

        SHA1 n = new SHA1();
        if (cp != null)
            n.update(cp.getBytes());
        return n;
    }


    public static SHA1 sha$(PyObject[] args, String[] kws) {
        return new$(args, kws);
    }
}



// This is the moral equivalent of PyArg_ParseTupleAndKeywords. If it
// turns out to work as I hope, it will become a utility class in core.
// In any case, it will soon be removed from this source.

class ArgParser {
    PyObject[] args;
    String[] kws;
    String[] params = null;

    public ArgParser(PyObject[] args, String[] kws) {
        this.args = args;
        this.kws = kws;
    }

    public ArgParser(PyObject[] args, String[] kws, String p0) {
        this(args, kws);
        this.params = new String[] { p0 };
        check();
    }

    public ArgParser(PyObject[] args, String[] kws, String p0, String p1) {
        this(args, kws);
        this.params = new String[] { p0, p1 };
        check();
    }

    public ArgParser(PyObject[] args, String[] kws, String p0, String p1, String p2) {
        this(args, kws);
        this.params = new String[] { p0, p1, p2 };
        check();
    }

    public ArgParser(PyObject[] args, String[] kws, String[] paramnames) {
        this(args, kws);
        this.params = paramnames;
        check();
    }


    public String getString(int pos) {
         return (String) getArg(pos, String.class, "string");
    }

    public int getInt(int pos) {
         return ((Integer) getArg(pos, Integer.class, "int")).intValue();
    }


    private void check() {
        l1:
        for (int i = 0; i < kws.length; i++) {
            for (int j = 0; j < params.length; j++) {
                if (kws[i].equals(params[j]))
                    continue l1;
            }
            throw Py.TypeError(
                  kws[i] +  " is an invalid keyword argument for this function");
        }
    }

    private PyObject getArg(int pos) {
        int kws_start = args.length - kws.length;
        if (pos < kws_start)
            return args[pos];
        for (int i = 0; i < kws.length; i++) {
            if (kws[i].equals(params[pos]))
                return args[kws_start + i];
        }
        return null;
    }

    private Object getArg(int pos, Class clss, String classname) {
        PyObject pyret = getArg(pos);
        if (pyret == null)
            return null;
        Object ret = pyret.__tojava__(clss);
        if (ret == Py.NoConversion) 
            throw Py.TypeError("argument " + (pos+1) + ": expected " +
                               classname + ", " + Py.safeRepr(pyret) + " found");
        return ret;
    }
}
            