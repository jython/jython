/*
 * Copyright 1998 Finn Bock.
 * 
 * This program contains material copyrighted by:
 * Copyright © 1991-1995 by Stichting Mathematisch Centrum, Amsterdam, 
 * The Netherlands. 
 */

package org.python.modules;

import java.io.*;
import java.util.*;

import org.python.core.*;

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
     * @return		a new StringIO object.
     */
    public static StringIO StringIO() {
	return new StringIO();
    }

    /**
     * Create a StringIO object, initialized by the value.
     * @param buf	The initial value.
     * @return		a new StringIO object.
     */
    public static StringIO StringIO(String buf) {
	return new StringIO(buf);
    }


    /**
     * The StringIO object
     * @see cStringIO#StringIO()	
     * @see cStringIO#StringIO(String)
     */
    public static class StringIO extends PyObject {
	transient public boolean softspace = false;
	transient public String name = "<cStringIO>";
	transient public String mode = "w";
	transient public boolean closed = false;

	transient private char[] buf;
	transient private int count;
	transient private int pos;


	StringIO() {
	    this.buf = new char[16];
	}


	StringIO(String buf) {
	    this.buf = new char[buf.length() + 16];
	    write(buf);
	    seek(0);
	}


	/** 
	 * Free the memory buffer.
	 */
	public void close() {
	    buf = null;
	    closed = true;
	}


	/**
	 * Return false.
	 * @return	false.
	 */ 
	public boolean isatty() {
	    return false;
	}


	/** 
	 * Position the file pointer to the absolute position.
	 * @param	pos the position in the file.
	 */
	public void seek(long pos) {
	    seek(pos, 0);
	}


	/** 
	 * Position the file pointer to the position in the .
	 * @param	pos the position in the file.
	 * @param	mode; 0=from the start, 1=relative, 2=from the end.
	 */
	public void seek(long pos, int mode) {
	    if (mode == 1)
		this.pos = (int)pos + this.pos;
	    else if (mode == 2)
		this.pos = (int)pos + count;
	    this.pos = Math.max(0, (int)pos);
	}


	/** 
	 * Return the file position.
	 * @returns	the position in the file.
	 */
	public long tell() {
	    return pos;
	}



	/**
	 * Read all data until EOF is reached. 
	 * An empty string is returned when EOF is encountered immediately.
	 * @returns	A string containing the data.
	 */
	public String read() {
	    return read(-1);
	}


	/**
	 * Read at most size bytes from the file (less if the read hits EOF).
	 * If the size argument is negative, read all data until EOF is reached.
	 * An empty string is returned when EOF is encountered immediately. 
	 * @param size	the number of characters to read.
	 * @returns	A string containing the data read.
	 */
	public String read(int size) {
            opencheck();
	    int newpos = (size < 0) ? count : Math.min(pos+size, count);
	    String r = null;
	    if (size == 1) {
		r = cStringIO.getString(buf[pos]);
	    } else {
		r = new String(buf, pos, newpos-pos);
	    }
	    pos = newpos;
	    return r;
	}


	private int indexOf(char ch, int pos) {
	    for (int i = pos; i < count; i++) {
		if (buf[i] == ch)
		    return i;
	    }
	    return -1;
	}


	/**
	 * Read one entire line from the file. A trailing newline character 
         * is kept in the string (but may be absent when a file ends with 
	 * an incomplete line). 
	 * An empty string is returned when EOF is hit immediately. 
	 * @returns data from the file up to and including the newline.
	 */
	public String readline() {
	    return readline(-1);
	}


	/**
	 * Read one entire line from the file. A trailing newline character 
	 * is kept in the string (but may be absent when a file ends with an
	 * incomplete line). 
	 * If the size argument is non-negative, it is a maximum byte count 
	 * (including the trailing newline) and an incomplete line may be returned.
	 * @returns data from the file up to and including the newline.
	 */
	public String readline(int length) {
            opencheck();
	    int i = indexOf('\n', pos);
	    int newpos = (i < 0) ? count : i+1;
	    if (length != -1 && pos + length < newpos)
		newpos = pos + length;
	    String r = new String(buf, pos, newpos-pos);
	    pos = newpos;
	    return r;
	}


	/**
	 * Read and return a line without the trailing newling.
	 * Usind by cPickle as an optimization.
	 */
	public String readlineNoNl() {
	    int i = indexOf('\n', pos);
	    int newpos = (i < 0) ? count : i;
	    String r = new String(buf, pos, newpos-pos);
	    pos = newpos;
	    if (pos  < count) // Skip the newline
		pos++;
	    return r;
	}



	/**
	 * Read until EOF using readline() and return a list containing 
	 * the lines thus read.
	 * @return 	a list of the lines.
	 */
	public PyObject readlines() {
            return readlines(0);
        }


	/**
	 * Read until EOF using readline() and return a list containing 
	 * the lines thus read.
	 * @return 	a list of the lines.
	 */
	public PyObject readlines(int sizehint) {
            opencheck();
            int total = 0;
	    PyList lines = new PyList();
	    String line = readline();
	    while (line.length() > 0) {
		lines.append(new PyString(line));
                total += line.length();
                if (0 < sizehint  && sizehint <= total)
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
        public void truncate(int pos) {
            opencheck();
            if (pos < 0)
                pos = this.pos;
            if (count > pos) 
                count = pos;
        }


	private void expandCapacity(int newLength) {
	    int newCapacity = (buf.length + 1) * 2;
	    if (newLength > newCapacity) {
		newCapacity = newLength;
	    }

	    char newBuf[] = new char[newCapacity];
	    System.arraycopy(buf, 0, newBuf, 0, count);
	    buf = newBuf;
	    //System.out.println("newleng:" + newCapacity);
	}


	/**
	 * Write a string to the file.
	 * @param s	The data to write.
	 */
	public void write(String s) {
            opencheck();
	    int newpos = pos + s.length();

	    if (newpos >= buf.length)
		expandCapacity(newpos);
	    if (newpos > count)
		count = newpos;

	    s.getChars(0, s.length(), buf, pos);
	    pos = newpos;
	}


	/**
	 * Write a char to the file. Used by cPickle as an optimization.
	 * @param ch	The data to write.
	 */
	public void writeChar(char ch) {
	    if (pos+1 >= buf.length)
		expandCapacity(pos+1);
	    buf[pos++] = ch;
	    if (pos > count)
		count = pos;
	}


	/**
	 * Write a list of strings to the file.
	 */
	public void writelines(String[] lines) {
	    for (int i = 0; i < lines.length; i++) {
		write(lines[i]);
	    }
	}


	/**
	 * Flush the internal buffer. Does nothing.
	 */
	public void flush() {
            opencheck();
        }


	/**
	 * Retrieve the entire contents of the ``file'' at any time 
	 * before the StringIO object's close() method is called.
	 * @return	the contents of the StringIO.
	 */
	public String getvalue() {
            opencheck();
	    return new String(buf, 0, count);
	}


        private final void opencheck() {
            if (buf == null)
                throw Py.ValueError("I/O operation on closed file");
        }
    }


    private static String[]   strings = new String[256];
    static String getString(char ch) {
        if ((int)ch > 255) {
            return new String(new char[] { ch });
        }

      String s = strings[(int)ch];

      if (s == null) {
          s = new String(new char[] { ch });
          strings[(int)ch] = s;
      }
      return s;
   }
}
