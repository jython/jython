//copyright 2000 Finn Bock

package org.python.modules;

import org.python.core.*;

public class sha {
    public int blocksize = 1;
    public int digestsize = 20;

    public static String __doc__ =
        "* Cryptix General License\n" +
        "* Copyright © 1995, 1996, 1997, 1998, 1999, 2000 The Cryptix"+
        " Foundation\n" +
        "* Limited. All rights reserved.\n" +
        "* \n" +
        "* Redistribution and use in source and binary forms, with or\n" +
        "* without modification, are permitted provided that the\n" +
        "* following conditions are met:\n" +
        "*\n" +
        "* - Redistributions of source code must retain the copyright\n" +
        "*   notice, this list of conditions and the following disclaimer.\n"+
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
        ArgParser ap = new ArgParser("sha", args, kws, "string");
        String cp = ap.getString(0, null);

        SHA1 n = new SHA1();
        if (cp != null)
            n.update(cp.getBytes());
        return n;
    }


    public static SHA1 sha$(PyObject[] args, String[] kws) {
        return new$(args, kws);
    }
}
