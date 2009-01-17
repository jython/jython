/*
 * Copyright 1998 Finn Bock.
 *
 * This program contains material copyrighted by:
 * Copyright (c) 1991-1995 by Stichting Mathematisch Centrum, Amsterdam,
 * The Netherlands.
 */

// cStringIO with StringBuilder semantics - don't use without using external
// synchronization. Java does provide other alternatives.

package org.python.modules;

import org.python.core.Py;
import org.python.core.PyIterator;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;

/**
 * This module implements a file-like class, StringIO, that reads and
 * writes a string buffer (also known as memory files).
 * See the description on file objects for operations.
 * @author Finn Bock, bckfnn@pipmail.dknet.dk
 * @version cStringIO.java,v 1.10 1999/05/20 18:03:20 fb Exp
 */
public class cStringIO {
    /**
     * Create an empty StringIO object
     * @return          a new StringIO object.
     */

    // would be nicer if we directly imported from os, but crazy to do so
    // since in python code itself
    private class os {
        public static final int SEEK_SET = 0;
        public static final int SEEK_CUR = 1;
        public static final int SEEK_END = 2;
    }

    public static PyType InputType = PyType.fromClass(StringIO.class);
    public static PyType OutputType = PyType.fromClass(StringIO.class);

    public static StringIO StringIO() {
        return new StringIO();
    }

    /**
     * Create a StringIO object, initialized by the value.
     * @param buf       The initial value.
     * @return          a new StringIO object.
     */
    public static StringIO StringIO(String buffer) {
        return new StringIO(buffer);
    }


    /**
     * The StringIO object
     * @see cStringIO#StringIO()
     * @see cStringIO#StringIO(String)
     */
    public static class StringIO extends PyIterator {
        public boolean softspace = false;
        public boolean closed = false;
        public int pos = 0;

        private final StringBuilder buf;

        public StringIO() {
            buf = new StringBuilder();
        }


        public StringIO(String buffer) {
            buf = new StringBuilder(buffer);
        }

        private void _complain_ifclosed() {
            if (closed)
                throw Py.ValueError("I/O operation on closed file");
        }

        private int _convert_to_int(long val) {
            if (val > Integer.MAX_VALUE) {
                throw Py.OverflowError("long int too large to convert to int");
            }
            return (int)val;
        }

        public void __setattr__(String name, PyObject value) {
            if (name == "softspace") {
                softspace = value.__nonzero__();
                return;
            }
            super.__setattr__(name, value);
        }

        public PyObject __iternext__() {
            _complain_ifclosed();
            PyString r = readline();
            if (r.__len__() == 0)
                return null;
            return r;
        }

        /**
         * Free the memory buffer.
         */
        public void close() {
            closed = true;
        }


        /**
         * Return false.
         * @return      false.
         */
        public boolean isatty() {
            _complain_ifclosed();
            return false;
        }


        /**
         * Position the file pointer to the absolute position.
         * @param       pos the position in the file.
         */
        public void seek(long pos) {
            seek(pos, os.SEEK_SET);
        }


        /**
         * Position the file pointer to the position in the .
         * @param       pos the position in the file.
         * @param       mode; 0=from the start, 1=relative, 2=from the end.
         */
        public synchronized void seek(long pos, int mode) {
            _complain_ifclosed();
            switch (mode) {
                case os.SEEK_CUR:
                    this.pos += pos;
                    break;
                case os.SEEK_END:
                    this.pos = _convert_to_int(pos + buf.length());
                    break;
                case os.SEEK_SET:
                default:
                    this.pos = _convert_to_int(pos);
                    break;
            }
        }

        /**
         * Reset the file position to the beginning of the file.
         */
        public synchronized void reset() {
            pos = 0;
        }

        /**
         * Return the file position.
         * @returns     the position in the file.
         */
        public synchronized int tell() {
            _complain_ifclosed();
            return pos;
        }



        /**
         * Read all data until EOF is reached.
         * An empty string is returned when EOF is encountered immediately.
         * @returns     A string containing the data.
         */
        public PyString read() {
            return read(-1);
        }


        /**
         * Read at most size bytes from the file (less if the read hits EOF).
         * If the size argument is negative, read all data until EOF is
         * reached. An empty string is returned when EOF is encountered
         * immediately.
         * @param size  the number of characters to read.
         * @returns     A string containing the data read.
         */

        public synchronized PyString read(long size) {
            _complain_ifclosed();
            _convert_to_int(size);
            int len = buf.length();
            String substr;
            if (size < 0) {
                substr = pos >= len ? "" : buf.substring(pos);
                pos = len;
            } else {
                // ensure no overflow
                int newpos = _convert_to_int(Math.min(pos + size, len));
                substr = buf.substring(pos, newpos);
                pos = newpos;
            }
            return new PyString(substr);
        }

        /**
         * Read one entire line from the file. A trailing newline character
         * is kept in the string (but may be absent when a file ends with
         * an incomplete line).
         * An empty string is returned when EOF is hit immediately.
         * @returns data from the file up to and including the newline.
         */
        public PyString readline() {
            return readline(-1);
        }


        /**
         * Read one entire line from the file. A trailing newline character
         * is kept in the string (but may be absent when a file ends with an
         * incomplete line).
         * If the size argument is non-negative, it is a maximum byte count
         * (including the trailing newline) and an incomplete line may be
         * returned.
         * @returns data from the file up to and including the newline.
         */
        public synchronized PyString readline(long size) {
            _complain_ifclosed();
            _convert_to_int(size);
            int len = buf.length();
            if (pos == len) {
                return new PyString("");
            }
            int i = buf.indexOf("\n", pos);
            int newpos = (i < 0) ? len : i + 1;
            if (size >= 0) {
                newpos = _convert_to_int(Math.min(newpos - pos, size) + pos);
            }
            String r = buf.substring(pos, newpos);
            pos = newpos;
            return new PyString(r);
        }


        /**
         * Read and return a line without the trailing newline.
         * Usind by cPickle as an optimization.
         */
        public synchronized PyString readlineNoNl() {
            _complain_ifclosed();
            int len = buf.length();
            int i = buf.indexOf("\n", pos);
            int newpos = (i < 0) ? len : i;
            String r = buf.substring(pos, newpos);
            pos = newpos;
            if (pos  < len) // Skip the newline
                pos++;
            return new PyString(r);
        }



        /**
         * Read until EOF using readline() and return a list containing
         * the lines thus read.
         * @return      a list of the lines.
         */
        public PyObject readlines() {
            return readlines(0);
        }


        /**
         * Read until EOF using readline() and return a list containing
         * the lines thus read.
         * @return      a list of the lines.
         */
        public PyObject readlines(long sizehint) {
            _complain_ifclosed();

            int sizehint_int = (int)sizehint;
            int total = 0;
            PyList lines = new PyList();
            PyString line = readline();
            while (line.__len__() > 0) {
                lines.append(line);
                total += line.__len__();
                if (0 < sizehint_int  && sizehint_int <= total)
                    break;
                line = readline();
            }
            return lines;
        }

        /**
         * truncate the file at the current position.
         */
        public void truncate() {
            truncate(-1);
        }

        /**
         * truncate the file at the position pos.
         */
        public synchronized void truncate(long pos) {
            int pos_int = _convert_to_int(pos);
            if (pos_int < 0)
                pos_int = this.pos;
            buf.setLength(pos_int);
            this.pos = pos_int;
        }


        /**
         * Write a string to the file.
         * @param s     The data to write.
         */
        public void write(PyObject obj) {
            write(obj.toString());
        }

        public synchronized void write(String s) {
            _complain_ifclosed();

            int spos = pos;
            int slen = buf.length();

            if (spos == slen) {
                buf.append(s);
                buf.setLength(slen + s.length());
                pos = spos + s.length();

                return;
            }

            if (spos > slen) {
                int l = spos - slen;
                char[] bytes = new char[l];

                for (int i = 0; i < l - 1; i++)
                    bytes[i] = '\0';

                buf.append(bytes);
                slen = spos;
            }

            int newpos = spos + s.length();

            if (newpos > slen) {
                buf.replace(spos, slen - spos, s);
                buf.append(s.substring(slen));
                slen = newpos;
            } else {
                buf.replace(spos, spos + s.length(), s);
            }

            buf.setLength(slen);
            pos = newpos;
        }

        /**
         * Write a char to the file. Used by cPickle as an optimization.
         * @param ch    The data to write.
         */
        public synchronized void writeChar(char ch) {
            int len = buf.length();
            if (len <= pos)
                buf.setLength(pos + 1);
            buf.setCharAt(pos++, ch);
        }


        /**
         * Write a list of strings to the file.
         */
        public void writelines(PyObject lines) {
            for (PyObject line : lines.asIterable()) {
                write(line);
            }
        }


        /**
         * Flush the internal buffer. Does nothing.
         */
        public void flush() {
            _complain_ifclosed();
        }


        /**
         * Retrieve the entire contents of the ``file'' at any time
         * before the StringIO object's close() method is called.
         * @return      the contents of the StringIO.
         */
        public synchronized PyString getvalue() {
            return new PyString(buf.toString());
        }

    }

    private static String[] strings = new String[256];
    static String getString(char ch) {
        if (ch > 255) {
            return new String(new char[] { ch });
        }

      String s = strings[ch];

      if (s == null) {
          s = new String(new char[] { ch });
          strings[ch] = s;
      }
      return s;
   }

}
