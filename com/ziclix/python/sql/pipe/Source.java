
/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql.pipe;

import org.python.core.PyObject;

/**
 * A Source produces data to be consumed by a Sink.  The data can be generated
 * from anywhere, but must follow the format detail in next().
 *
 * @author brian zimmer
 * @version $Revision$
 * @see #next
 * @see Sink
 */
public interface Source {

	/**
	 * Invoked at the start of processing.
	 */
	public void start();

	/**
	 * Return the next row from the source.
	 * The following format:<br>
	 * &nbsp;&nbsp;[(colName, colType), (colName, colType), ...]
	 * for headers and:<br/>
	 * &nbsp;&nbsp;[(col), (colName, colType), ...]
	 * for all other data must be used.
	 */
	public PyObject next();

	/**
	 * Invoked at the end of processing.  This method is guarenteed to be called.
	 */
	public void end();
}
