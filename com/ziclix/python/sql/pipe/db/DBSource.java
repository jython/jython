
/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql.pipe.db;

import org.python.core.*;
import com.ziclix.python.sql.*;
import com.ziclix.python.sql.pipe.*;
import com.ziclix.python.sql.handler.*;

/**
 * A database source.  Given a PyConnection and information about the query, produce the data.
 *
 * @author brian zimmer
 * @version $Revision$
 */
public class DBSource extends BaseDB implements Source {

	/** Field sql */
	protected String sql;

	/** Field sentHeader */
	protected boolean sentHeader;

	/** Field params, include */
	protected PyObject params, include;

	/**
	 * Constructor for handling the generation of data.
	 *
	 * @param connection the database connection
	 * @param dataHandler a custom DataHandler for the cursor, can be None
	 * @param tableName the table in question on the source database
	 * @param where an optional where clause, defaults to '(1=1)' if null
	 * @param include the columns to be queried from the source, '*' if None
	 * @param params optional params to substituted in the where clause
	 */
	public DBSource(PyConnection connection, Class dataHandler, String tableName, String where, PyObject include, PyObject params) {

		super(connection, dataHandler, tableName);

		this.params = params;
		this.include = include;
		this.sentHeader = false;
		this.sql = this.createSql(where);
	}

	/**
	 * Create the sql string given the where clause.
	 */
	protected String createSql(String where) {

		// create the sql statement, using the columns if available
		StringBuffer sb = new StringBuffer("select ");

		if ((this.include == Py.None) || (this.include.__len__() == 0)) {
			sb.append("*");
		} else {
			for (int i = 1; i < this.include.__len__(); i++) {
				sb.append(this.include.__getitem__(i)).append(",");
			}

			sb.append(this.include.__getitem__(this.include.__len__() - 1));
		}

		sb.append(" from ").append(this.tableName);
		sb.append(" where ").append((where == null) ? "(1=1)" : where);

		String sql = sb.toString();

		return sql;
	}

	/**
	 * Return the next row in the result set.  The first row returned will be column information.
	 */
	public PyObject next() {

		if (this.sentHeader) {

			// Py.None will be sent when all done, so this will close down the queue
			return this.cursor.fetchone();
		} else {
			this.cursor.execute(this.sql, this.params, Py.None, Py.None);

			PyObject description = this.cursor.__findattr__("description");

			// we can't insert if we don't know column names
			if ((description == Py.None) || (description.__len__() == 0)) {

				// let the destination worry about handling the empty set
				return Py.None;
			}

			int len = description.__len__();
			PyObject[] columns = new PyObject[len];

			for (int i = 0; i < len; i++) {
				PyObject[] colInfo = new PyObject[2];

				// col name
				colInfo[0] = description.__getitem__(i).__getitem__(0);

				// col type
				colInfo[1] = description.__getitem__(i).__getitem__(1);
				columns[i] = new PyTuple(colInfo);
			}

			PyObject row = new PyTuple(columns);

			Py.writeDebug("db-source", row.toString());

			this.sentHeader = true;

			return row;
		}
	}

	/**
	 * Method start
	 *
	 */
	public void start() {}

	/**
	 * Close the cursor.
	 */
	public void end() {

		if (this.cursor != null) {
			this.cursor.close();
		}
	}
}
