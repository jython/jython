
/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql.pipe.db;

import java.util.*;
import java.lang.reflect.*;
import org.python.core.*;
import com.ziclix.python.sql.*;
import com.ziclix.python.sql.handler.*;

/**
 * Abstract class to assist in generating cursors.
 *
 * @author brian zimmer
 * @version $Revision$
 */
public abstract class BaseDB {

	/** Field cursor */
	protected PyCursor cursor;

	/** Field dataHandler */
	protected Class dataHandler;

	/** Field tableName */
	protected String tableName;

	/** Field connection */
	protected PyConnection connection;

	/**
	 * Construct the helper.
	 */
	public BaseDB(PyConnection connection, Class dataHandler, String tableName) {

		this.tableName = tableName;
		this.dataHandler = dataHandler;
		this.connection = connection;
		this.cursor = this.cursor();
	}

	/**
	 * Create a new constructor and optionally bind a new DataHandler.  The new DataHandler must act as
	 * a Decorator, having a single argument constructor of another DataHandler.  The new DataHandler is
	 * then expected to delegate all calls to the original while enhancing the functionality in any matter
	 * desired.  This allows additional functionality without losing any previous work or requiring any
	 * complicated inheritance dependencies.
	 */
	protected PyCursor cursor() {

		PyCursor cursor = this.connection.cursor(true);
		DataHandler origDataHandler = cursor.getDataHandler(), newDataHandler = null;

		if ((origDataHandler != null) && (this.dataHandler != null)) {
			Constructor cons = null;

			try {
				Class[] args = new Class[1];

				args[0] = DataHandler.class;
				cons = this.dataHandler.getConstructor(args);
			} catch (Exception e) {
				return cursor;
			}

			if (cons == null) {
				String msg = zxJDBC.getString("invalidCons", new Object[]{ this.dataHandler.getName() });

				throw zxJDBC.makeException(msg);
			}

			try {
				Object[] args = new Object[1];

				args[0] = origDataHandler;
				newDataHandler = (DataHandler)cons.newInstance(args);
			} catch (Exception e) {
				return cursor;
			}

			if (newDataHandler != null) {
				cursor.__setattr__("datahandler", Py.java2py(newDataHandler));
			}
		}

		return cursor;
	}
}
