
/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql;

import java.sql.*;
import java.util.BitSet;
import org.python.core.*;

/**
 * This class provides the necessary functionality to call stored
 * procedures.  It handles managing the database metadata and binding
 * the appropriate parameters.
 *
 * @author brian zimmer
 * @author last modified by $Author$
 * @version $Revision$
 */
public class Procedure extends Object {

	/** Field NAME */
	protected static final int NAME = 3;

	/** Field COLUMN_TYPE */
	protected static final int COLUMN_TYPE = 4;

	/** Field DATA_TYPE */
	protected static final int DATA_TYPE = 5;

	/** Field DATA_TYPE_NAME */
	protected static final int DATA_TYPE_NAME = 6;

	/** Field PRECISION */
	protected static final int PRECISION = 7;

	/** Field LENGTH */
	protected static final int LENGTH = 8;

	/** Field SCALE */
	protected static final int SCALE = 9;

	/** Field NULLABLE */
	protected static final int NULLABLE = 11;

	/** Field cursor */
	protected PyCursor cursor;

	/** Field columns */
	protected PyObject columns;

	/** Field procedureCatalog */
	protected PyObject procedureCatalog;

	/** Field procedureSchema */
	protected PyObject procedureSchema;

	/** Field procedureName */
	protected PyObject procedureName;

	/** Field inputSet */
	protected BitSet inputSet;

	/**
	 * Constructor Procedure
	 *
	 * @param PyCursor cursor an open cursor
	 * @param PyObject name a string or tuple representing the name
	 *
	 * @throws SQLException
	 *
	 */
	public Procedure(PyCursor cursor, PyObject name) throws SQLException {

		this.cursor = cursor;
		this.inputSet = new BitSet();

		if (name instanceof PyString) {
			this.procedureCatalog = Py.EmptyString;
			this.procedureSchema = Py.EmptyString;
			this.procedureName = name;
		} else if (this.cursor.isSeq(name)) {
			if (name.__len__() == 3) {
				this.procedureCatalog = name.__getitem__(0);
				this.procedureSchema = name.__getitem__(1);
				this.procedureName = name.__getitem__(2);
			} else {

				// throw an exception
			}
		} else {

			// throw an exception
		}

		fetchColumns();
	}

	/**
	 * Prepares the statement and registers the OUT/INOUT parameters (if any).
	 *
	 * @return CallableStatement
	 *
	 * @throws SQLException
	 *
	 */
	public CallableStatement prepareCall() throws SQLException {

		// prepare the statement
		CallableStatement statement = null;

		try {

			// build the full call syntax
			String sqlString = toSql();

			statement = cursor.connection.connection.prepareCall(sqlString);

			// prepare the OUT parameters
			registerOutParameters(statement);
		} catch (SQLException e) {
			if (statement != null) {
				try {
					statement.close();
				} catch (Exception ex) {}
			}

			throw e;
		}

		return statement;
	}

	/**
	 * Prepare the binding dictionary with the correct datatypes.
	 *
	 * @param params a non-None list of params
	 * @param bindings a dictionary of bindings
	 *
	 */
	public void normalizeInput(PyObject params, PyObject bindings) throws SQLException {

		if (this.columns == Py.None) {
			return;
		}

		// do nothing with params at the moment
		for (int i = 0, len = this.columns.__len__(), binding = 0; i < len; i++) {
			PyObject column = this.columns.__getitem__(i);
			int colType = column.__getitem__(COLUMN_TYPE).__int__().getValue();

			switch (colType) {

				case DatabaseMetaData.procedureColumnIn :
				case DatabaseMetaData.procedureColumnInOut :

					// bindings are Python-indexed
					PyInteger key = Py.newInteger(binding++);

					if (bindings.__finditem__(key) == null) {
						int dataType = column.__getitem__(DATA_TYPE).__int__().getValue();
						String name = column.__getitem__(NAME).toString();

						bindings.__setitem__(key, Py.newInteger(dataType));
					}

					// inputs are JDBC-indexed
					this.inputSet.set(i + 1);
					break;
			}
		}
	}

	/**
	 * This method determines whether the param at the specified index is an
	 * IN or INOUT param for a stored procedure.  This is only configured properly
	 * AFTER a call to normalizeInput().
	 *
	 * @param index JDBC indexed column index (1, 2, ...)
	 *
	 * @return true if the column is an input, false otherwise
	 *
	 * @throws SQLException
	 *
	 */
	public boolean isInput(int index) throws SQLException {
		return this.inputSet.get(index);
	}

	/**
	 * Returns the call in the syntax:
	 *
	 * {? = call <procedure-name>(?, ?, ...)}
	 * {call <procedure-name>(?, ?, ...)}
	 *
	 * As of now, all parameters variables are created and no support for named variable
	 * calling is supported.
	 *
	 * @return String
	 *
	 */
	public String toSql() throws SQLException {

		int colParam = 0;
		int colReturn = 0;

		if (this.columns != Py.None) {
			for (int i = 0, len = this.columns.__len__(); i < len; i++) {
				PyObject column = this.columns.__getitem__(i);
				int colType = column.__getitem__(COLUMN_TYPE).__int__().getValue();

				switch (colType) {

					case DatabaseMetaData.procedureColumnUnknown :
						throw zxJDBC.makeException(zxJDBC.NotSupportedError, "procedureColumnUnknown");
					case DatabaseMetaData.procedureColumnResult :
						throw zxJDBC.makeException(zxJDBC.NotSupportedError, "procedureColumnResult");

					// these go on the right hand side
					case DatabaseMetaData.procedureColumnIn :
					case DatabaseMetaData.procedureColumnInOut :
					case DatabaseMetaData.procedureColumnOut :
						colParam++;
						break;

					// these go on the left hand side
					case DatabaseMetaData.procedureColumnReturn :
						colReturn++;
						break;

					default :
						throw zxJDBC.makeException(zxJDBC.DataError, "unknown column type [" + colType + "]");
				}
			}
		}

		StringBuffer sql = new StringBuffer("{");

		if (colReturn > 0) {
			PyList list = new PyList();

			for (; colReturn > 0; colReturn--) {
				list.append(Py.newString("?"));
			}

			sql.append(Py.newString(",").join(list)).append(" = ");
		}

		String name = cursor.datahandler.getProcedureName(procedureCatalog, procedureSchema, procedureName);

		sql.append("call ").append(name).append("(");

		if (colParam > 0) {
			PyList list = new PyList();

			for (; colParam > 0; colParam--) {
				list.append(Py.newString("?"));
			}

			sql.append(Py.newString(",").join(list));
		}

		return sql.append(")}").toString();
	}

	/**
	 * Registers the OUT/INOUT parameters of the statement.
	 *
	 * @param CallableStatement statement
	 *
	 * @throws SQLException
	 *
	 */
	protected void registerOutParameters(CallableStatement statement) throws SQLException {

		if (this.columns == Py.None) {
			return;
		}

		for (int i = 0, len = this.columns.__len__(); i < len; i++) {
			PyObject column = this.columns.__getitem__(i);
			int colType = column.__getitem__(COLUMN_TYPE).__int__().getValue();
			int dataType = column.__getitem__(DATA_TYPE).__int__().getValue();
			String dataTypeName = column.__getitem__(DATA_TYPE_NAME).toString();

			switch (colType) {

				case DatabaseMetaData.procedureColumnInOut :
				case DatabaseMetaData.procedureColumnOut :
				case DatabaseMetaData.procedureColumnReturn :
					cursor.datahandler.registerOut(statement, i + 1, colType, dataType, dataTypeName);
					break;
			}
		}
	}

	/**
	 * Method fetchColumns
	 *
	 * @throws SQLException
	 *
	 */
	protected void fetchColumns() throws SQLException {

		PyExtendedCursor pec = (PyExtendedCursor)cursor.connection.cursor();

		try {
			pec.datahandler = this.cursor.datahandler;

			pec.procedurecolumns(procedureCatalog, procedureSchema, procedureName, Py.None);

			this.columns = pec.fetchall();
		} finally {
			pec.close();
		}
	}
}
