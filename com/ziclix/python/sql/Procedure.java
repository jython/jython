
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
import org.python.core.*;

/**
 * Class Procedure
 *
 * @author brian zimmer
 * @date $today.date$
 * @author last modified by $Author$
 * @date last modified on $Date$
 * @version $Revision$
 * @copyright 2001 brian zimmer
 */
public class Procedure extends Object {

	/** Field COLUMN_TYPE */
	protected static final int COLUMN_TYPE = 4;

	/** Field DATA_TYPE */
	protected static final int DATA_TYPE = 5;

	/** Field PLACEHOLDER */
	public static final PyObject PLACEHOLDER = new PyObject();

	/** Field cursor */
	protected PyCursor cursor;

	/** Field name */
	protected PyObject name;

	/** Field columns */
	protected PyObject columns;

	/**
	 * Constructor Procedure
	 *
	 * @param PyCursor cursor an open cursor
	 * @param PyObject name a string or tuple representing the name
	 *
	 */
	public Procedure(PyCursor cursor, PyObject name) {
		this.cursor = cursor;
		this.name = name;
	}

	/**
	 * Method prepareCall
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

			// fetch the column information
			fetchColumns();

			// build the full call syntax
			String sqlString = buildSql();

			statement = cursor.connection.connection.prepareCall(sqlString);

			// register the OUT parameters
			registerOutParameters(statement);
		} catch (SQLException e) {
			if (statement == null) {
				try {
					statement.close();
				} catch (Exception ex) {}
			}

			throw e;
		}

		return statement;
	}

	/**
	 * Construct a list of the params in the proper order for the .setXXX methods of
	 * a PreparedStatement.  In the special case for a CallableStatement, insert
	 * Procedure.PLACEHOLDER to notify the cursor to skip the slot.
	 *
	 * @param params a non-None list of params
	 *
	 * @return PyObject a list of params with Procedure.PLACEHOLDER in index for all
	 * non IN and INOUT parameters
	 *
	 */
	public PyObject normalizeParams(PyObject params) {

		if (columns == Py.None) {
			throw zxJDBC.makeException(zxJDBC.ProgrammingError, "too many params for input");
		}

		if (params == Py.None) {
			return Py.None;
		}

		int j = 0, plen = params.__len__();
		PyList population = new PyList();

		for (int i = 0, len = columns.__len__(); i < len; i++) {
			PyObject column = columns.__getitem__(i);
			int colType = column.__getitem__(COLUMN_TYPE).__int__().getValue();

			switch (colType) {

				case DatabaseMetaData.procedureColumnIn :
				case DatabaseMetaData.procedureColumnInOut :
					if (j + 1 > plen) {
						throw zxJDBC.makeException(zxJDBC.ProgrammingError, "too few params for input, attempting [" + (j + 1) + "] found [" + plen + "]");
					}

					population.append(params.__getitem__(j++));
					break;

				default :
					population.append(PLACEHOLDER);
					break;
			}
		}

		if (j != plen) {
			throw zxJDBC.makeException(zxJDBC.ProgrammingError, "too many params for input");
		}

		return population;
	}

	/**
	 * Returns the call in the syntax:
	 *
	 * {? = call <procedure-name>(?, ?, ...)}
	 * {call <procedure-name>(?, ?, ...)}
	 *
	 * As of now, all parameters variables are created and no support for named variable
	 * calling is supported.
	 * @return String
	 *
	 */
	protected String buildSql() throws SQLException {

		int colParam = 0;
		int colReturn = 0;

		if (columns != Py.None) {
			for (int i = 0, len = columns.__len__(); i < len; i++) {
				PyObject column = columns.__getitem__(i);
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
	 * Method registerOutParameters
	 *
	 * @param CallableStatement statement
	 *
	 * @throws SQLException
	 *
	 */
	private final void registerOutParameters(CallableStatement statement) throws SQLException {

		if (columns == Py.None) {
			return;
		}

		for (int i = 0, len = columns.__len__(); i < len; i++) {
			PyObject column = columns.__getitem__(i);
			int colType = column.__getitem__(COLUMN_TYPE).__int__().getValue();
			int dataType = column.__getitem__(DATA_TYPE).__int__().getValue();

			switch (colType) {

				case DatabaseMetaData.procedureColumnIn :
				case DatabaseMetaData.procedureColumnInOut :
				case DatabaseMetaData.procedureColumnOut :
				case DatabaseMetaData.procedureColumnReturn :
					doRegister(statement, i + 1, colType, dataType);
					break;
			}
		}
	}

	/**
	 * Method doRegister
	 *
	 * @param CallableStatement statement
	 * @param int index
	 * @param int colType
	 * @param int dataType
	 *
	 * @throws SQLException
	 *
	 */
	protected void doRegister(CallableStatement statement, int index, int colType, int dataType) throws SQLException {
		statement.registerOutParameter(index, dataType);
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

			pec.procedurecolumns(Py.newString(""), Py.newString(""), name, Py.None);

			this.columns = pec.fetchall();
		} finally {
			pec.close();
		}
	}
}
