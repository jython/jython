
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
import org.python.core.*;
import com.ziclix.python.sql.*;
import com.ziclix.python.sql.pipe.*;
import com.ziclix.python.sql.handler.*;

/**
 * A database consumer.  All data transferred will be inserted into the appropriate table.
 *
 * @author brian zimmer
 * @version $Revision$
 */
public class DBSink extends BaseDB implements Sink {

	/** Field sql */
	protected PyObject sql;

	/** Field exclude */
	protected Set exclude;

	/** Field rows */
	protected PyList rows;

	/** Field batchsize */
	protected int batchsize;

	/** Field bindings */
	protected PyObject bindings;

	/** Field indexedBindings */
	protected PyDictionary indexedBindings;

	/**
	 * Constructor for handling the consumption of data.
	 *
	 * @param connection the database connection
	 * @param dataHandler a custom DataHandler for the cursor, can be None
	 * @param tableName the table to insert the data
	 * @param exclude the columns to be excluded from insertion on the destination, all if None
	 * @param bindings the optional bindings for the destination, this allows morphing of types during the copy
	 * @param batchsize the optional batchsize for the inserts
	 */
	public DBSink(PyConnection connection, Class dataHandler, String tableName, PyObject exclude, PyObject bindings, int batchsize) {

		super(connection, dataHandler, tableName);

		this.sql = Py.None;
		this.rows = new PyList();
		this.bindings = bindings;
		this.batchsize = batchsize;
		this.exclude = new HashSet();
		this.indexedBindings = new PyDictionary();

		if (exclude != Py.None) {
			for (int i = 0; i < exclude.__len__(); i++) {
				PyObject lowered = Py.newString(((PyString)exclude.__getitem__(i)).lower());

				this.exclude.add(lowered);
			}
		}
	}

	/**
	 * Return true if the key (converted to lowercase) is not found in the exclude list.
	 */
	protected boolean excluded(PyObject key) {

		PyObject lowered = Py.newString(((PyString)key).lower());

		return this.exclude.contains(lowered);
	}

	/**
	 * Create the insert statement given the header row.
	 */
	protected void createSql(PyObject row) {

		// this should be the column info
		if ((row == Py.None) || (row.__len__() == 0)) {

			// if there are no columns, what's the point?
			throw zxJDBC.makeException(zxJDBC.getString("noColInfo"));
		}

		int index = 0, len = row.__len__();
		PyObject entry = Py.None, col = Py.None, pyIndex = Py.None;
		StringBuffer sb = new StringBuffer("insert into ").append(this.tableName).append(" (");

		/*
		 * Iterate through the columns and pull out the names for use in the insert
		 * statement and the types for use in the bindings.  The tuple is of the form
		 * (column name, column type).
		 */
		for (int i = 0; i < len - 1; i++) {
			entry = row.__getitem__(i);
			col = entry.__getitem__(0);

			if (!this.excluded(col)) {

				// add to the list
				sb.append(col).append(",");

				// add the binding
				pyIndex = Py.newInteger(index++);

				try {
					this.indexedBindings.__setitem__(pyIndex, this.bindings.__getitem__(col));
				} catch (Exception e) {

					// either a KeyError or this.bindings is None or null
					this.indexedBindings.__setitem__(pyIndex, entry.__getitem__(1));
				}
			}
		}

		entry = row.__getitem__(len - 1);
		col = entry.__getitem__(0);

		if (!this.excluded(col)) {
			sb.append(col);

			pyIndex = Py.newInteger(index++);

			try {
				this.indexedBindings.__setitem__(pyIndex, this.bindings.__getitem__(col));
			} catch (Exception e) {

				// either a KeyError or this.bindings is None or null
				this.indexedBindings.__setitem__(pyIndex, entry.__getitem__(1));
			}
		}

		sb.append(") values (");

		for (int i = 1; i < len; i++) {
			sb.append("?,");
		}

		sb.append("?)");

		if (index == 0) {
			throw zxJDBC.makeException(zxJDBC.ProgrammingError, zxJDBC.getString("excludedAllCols"));
		}

		this.sql = Py.newString(sb.toString());
	}

	/**
	 * Handle the row.  Insert the data into the correct table and columns.  No updates are done.
	 */
	public void row(PyObject row) {

		if (this.sql != Py.None) {
			if (this.batchsize <= 0) {

				// no batching, just go ahead each time
				this.cursor.execute(this.sql, row, this.indexedBindings, Py.None);
				this.connection.commit();
			} else {
				this.rows.append(row);

				int len = rows.__len__();

				if (len % this.batchsize == 0) {
					this.cursor.execute(this.sql, this.rows, this.indexedBindings, Py.None);
					this.connection.commit();

					this.rows = new PyList();
				}
			}
		} else {
			this.createSql(row);
		}
	}

	/**
	 * Method start
	 *
	 */
	public void start() {}

	/**
	 * Handles flushing any buffers and closes the cursor.
	 */
	public void end() {

		// finish what we started
		try {
			int len = this.rows.__len__();

			if (len > 0) {
				this.cursor.execute(this.sql, this.rows, this.indexedBindings, Py.None);
				this.connection.commit();
			}
		} finally {
			this.cursor.close();
		}
	}
}
