
/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql;

import org.python.core.PyClass;
import org.python.core.PyInteger;

/**
 * This class wraps the types from java.sql.Type in order for
 * PyCursor to differentiate between a regular PyInteger and
 * a SQL datatype value.
 *
 * @author brian zimmer
 * @author last modified by $Author$
 * @version $Revision$
 */
public final class DBApiType extends PyInteger {

	/** Field __class__ */
	public static PyClass __class__;

	/**
	 * Constructor DBApiType
	 *
	 * @param int type
	 *
	 */
	public DBApiType(int type) {
		super(type);
	}

	/**
	 * Constructor DBApiType
	 *
	 * @param Integer type
	 *
	 */
	public DBApiType(Integer type) {
		super(type.intValue());
	}

	/**
	 * Method getPyClass
	 *
	 * @return PyClass
	 *
	 */
	protected PyClass getPyClass() {
		return __class__;
	}
}
