
/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql.util;

import java.io.*;
import java.sql.*;
import java.math.*;
import java.util.*;
import org.python.core.*;

/**
 * Parse the args and kws for a method call.
 *
 * @author brian zimmer
 * @version $Revision$
 */
public class PyArgParser extends Object {

	/** Field keywords */
	protected Map keywords;

	/** Field arguments */
	protected PyObject[] arguments;

	/**
	 * Construct a parser with the arguments and keywords.
	 */
	public PyArgParser(PyObject[] args, String[] kws) {

		this.keywords = new HashMap();
		this.arguments = null;

		parse(args, kws);
	}

	/**
	 * Method parse
	 *
	 * @param PyObject[] args
	 * @param String[] kws
	 *
	 */
	protected void parse(PyObject[] args, String[] kws) {

		// walk backwards through the kws and build the map
		int largs = args.length;

		if (kws != null) {
			for (int i = kws.length - 1; i >= 0; i--) {
				keywords.put(kws[i], args[--largs]);
			}
		}

		this.arguments = new PyObject[largs];

		System.arraycopy(args, 0, this.arguments, 0, largs);
	}

	/**
	 * How many keywords?
	 */
	public int numKw() {
		return this.keywords.keySet().size();
	}

	/**
	 * Does the keyword exist?
	 */
	public boolean hasKw(String kw) {
		return this.keywords.containsKey(kw);
	}

	/**
	 * Return the value for the keyword, raise a KeyError if the keyword does
	 * not exist.
	 */
	public PyObject kw(String kw) {

		if (!hasKw(kw)) {
			throw Py.KeyError(kw);
		}

		return (PyObject)this.keywords.get(kw);
	}

	/**
	 * Return the value for the keyword, return the default if the keyword does
	 * not exist.
	 */
	public PyObject kw(String kw, PyObject def) {

		if (!hasKw(kw)) {
			return def;
		}

		return (PyObject)this.keywords.get(kw);
	}

	/**
	 * Get the array of keywords.
	 */
	public String[] kws() {
		return (String[])this.keywords.keySet().toArray(new String[0]);
	}

	/**
	 * Get the number of arguments.
	 */
	public int numArg() {
		return this.arguments.length;
	}

	/**
	 * Return the argument at the given index, raise an IndexError if out of range.
	 */
	public PyObject arg(int index) {

		if ((index >= 0) && (index <= this.arguments.length - 1)) {
			return this.arguments[index];
		}

		throw Py.IndexError("index out of range");
	}

	/**
	 * Return the argument at the given index, or the default if the index is out of range.
	 */
	public PyObject arg(int index, PyObject def) {

		if ((index >= 0) && (index <= this.arguments.length - 1)) {
			return this.arguments[index];
		}

		return def;
	}
}
