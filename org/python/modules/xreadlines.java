// Copyright (c) 2001 Finn Bock.

package org.python.modules;

import org.python.core.*;

public class xreadlines {
    private final static int CHUNKSIZE = 8192;

    public static PyString __doc__xreadlines = new PyString(
        "xreadlines(f)\n" +
        "\n" +
        "Return an xreadlines object for the file f."
    );

    public static PyObject xreadlines$(PyObject file) {
        return new XReadlineObj(file);
    }

    public static class XReadlineObj extends PyObject {
        private PyObject file;
        private PyObject lines = null;
        private int lineslen = 0;
        private int lineno = 0;
        private int abslineno = 0;

        public XReadlineObj(PyObject file) {
            this.file = file;
        }

        public PyObject __iter__() {
            return new PySequenceIter(this);
        }

        public PyObject __finditem__(PyObject idx) {
            return __finditem__(idx.__int__().getValue());
        }

        public PyObject __finditem__(int idx) {
            if (idx != abslineno) {
                throw Py.RuntimeError(
                            "xreadlines object accessed out of order");
            }

            if (lineno >= lineslen) {
                lines = file.invoke("readlines", Py.newInteger(CHUNKSIZE));
                lineno = 0;
                lineslen = lines.__len__();
            }
            abslineno++;
            return lines.__finditem__(lineno++);
        }

        public String toString() {
            return "<xreadlines object " + Py.idstr(this) + ">";
        }

        // __class__ boilerplate -- see PyObject for details
        public static PyClass __class__;

        protected PyClass getPyClass() {
            return __class__;
        }

    }
}
