
/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql;

import java.sql.Statement;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.CallableStatement;
import org.python.core.*;

/**
 * Class PyStatement
 *
 * @author brian zimmer
 * @date 2002-04-19 08:58:22.11
 * @date last modified on $Date$
 * @version $Revision$
 */
public class PyStatement extends PyObject {

	/** Field STATEMENT_STATIC */
	public static final int STATEMENT_STATIC = 2;

	/** Field STATEMENT_PREPARED */
	public static final int STATEMENT_PREPARED = 4;

	/** Field STATEMENT_CALLABLE */
	public static final int STATEMENT_CALLABLE = 8;

	/** Field style */
	private int style;

	/** Field sql */
	private Object sql;

	/** Field closed */
	private boolean closed;

	/** Field statement */
	Statement statement;

	/**
	 * Constructor PyStatement
	 *
	 * @param statement
	 * @param sql
	 * @param style
	 *
	 */
	public PyStatement(Statement statement, Object sql, int style) {

		this.sql = sql;
		this.style = style;
		this.closed = false;
		this.statement = statement;
	}

	/**
	 * Constructor PyStatement
	 *
	 * @param statement
	 * @param procedure
	 *
	 */
	public PyStatement(Statement statement, Procedure procedure) {
		this(statement, procedure, STATEMENT_CALLABLE);
	}

	/** Field __class__ */
	public static PyClass __class__;

	/**
	 * Method getPyClass
	 *
	 * @return PyClass
	 *
	 */
	protected PyClass getPyClass() {
		return __class__;
	}

	/** Field __methods__ */
	protected static PyList __methods__;

	/** Field __members__ */
	protected static PyList __members__;

	static {
		PyObject[] m = new PyObject[1];

		m[0] = new PyString("close");
		__methods__ = new PyList(m);
		m = new PyObject[3];
		m[0] = new PyString("style");
		m[1] = new PyString("closed");
		m[2] = new PyString("__statement__");
		__members__ = new PyList(m);
	}

	/**
	 * Method __str__
	 *
	 * @return PyString
	 *
	 */
	public PyString __str__() {

		if (sql instanceof String) {
			return Py.newString((String)sql);
		} else if (sql instanceof Procedure) {
			try {
				return Py.newString(((Procedure)sql).toSql());
			} catch (SQLException e) {
				throw zxJDBC.makeException(e);
			}
		}

		return super.__str__();
	}

	/**
	 * Method __repr__
	 *
	 * @return PyString
	 *
	 */
	public PyString __repr__() {

		// care is taken not to display a rounded second value
		StringBuffer buf = new StringBuffer("<PyStatement object for [");

		buf.append(__str__().toString());
		buf.append("] at ").append(Py.id(this)).append(">");

		return Py.newString(buf.toString());
	}

	/**
	 * Method toString
	 *
	 * @return String
	 *
	 */
	public String toString() {
		return __repr__().toString();
	}

	/**
	 * Gets the value of the attribute name.
	 *
	 * @param name
	 * @return the attribute for the given name
	 */
	public PyObject __findattr__(String name) {

		if ("style".equals(name)) {
			return Py.newInteger(style);
		} else if ("closed".equals(name)) {
			return Py.newBoolean(closed);
		} else if ("__statement__".equals(name)) {
			return Py.java2py(statement);
		} else if ("__methods__".equals(name)) {
			return __methods__;
		} else if ("__members__".equals(name)) {
			return __members__;
		}

		return super.__findattr__(name);
	}

	/**
	 * Initializes the object's namespace.
	 *
	 * @param dict
	 */
	static public void classDictInit(PyObject dict) {

		dict.__setitem__("__version__", Py.newString("$Revision$").__getslice__(Py.newInteger(11), Py.newInteger(-2), null));

		// hide from python
		dict.__setitem__("classDictInit", null);
		dict.__setitem__("statement", null);
		dict.__setitem__("execute", null);
		dict.__setitem__("prepare", null);
		dict.__setitem__("STATEMENT_STATIC", null);
		dict.__setitem__("STATEMENT_PREPARED", null);
		dict.__setitem__("STATEMENT_CALLABLE", null);
	}

	/**
	 * Delete the statement.
	 *
	 */
	public void __del__() {
		close();
	}

	/**
	 * Method execute
	 *
	 * @param PyCursor cursor
	 * @param PyObject params
	 * @param PyObject bindings
	 *
	 * @throws SQLException
	 *
	 */
	public void execute(PyCursor cursor, PyObject params, PyObject bindings) throws SQLException {

		if (this.closed) {
			throw zxJDBC.makeException(zxJDBC.ProgrammingError, "statement is closed");
		}

		this.prepare(cursor, params, bindings);

		Fetch fetch = cursor.fetch;

		switch (this.style) {

			case STATEMENT_STATIC :
				if (this.statement.execute((String)this.sql)) {
					fetch.add(this.statement.getResultSet());
				}
				break;

			case STATEMENT_PREPARED :
				final PreparedStatement preparedStatement = (PreparedStatement)this.statement;

				if (preparedStatement.execute()) {
					fetch.add(preparedStatement.getResultSet());
				}
				break;

			case STATEMENT_CALLABLE :
				final CallableStatement callableStatement = (CallableStatement)this.statement;

				if (callableStatement.execute()) {
					fetch.add(callableStatement.getResultSet());
				}

				fetch.add(callableStatement, (Procedure)sql, params);
				break;

			default :
				throw zxJDBC.makeException(zxJDBC.ProgrammingError, zxJDBC.getString("invalidStyle"));
		}
	}

	/**
	 * Method prepare
	 *
	 * @param PyCursor cursor
	 * @param PyObject params
	 * @param PyObject bindings
	 *
	 * @throws SQLException
	 *
	 */
	private void prepare(PyCursor cursor, PyObject params, PyObject bindings) throws SQLException {

		if ((params == Py.None) || (this.style == STATEMENT_STATIC)) {
			return;
		}

		// [3, 4] or (3, 4)
		final DataHandler datahandler = cursor.datahandler;
		int columns = 0, column = 0, index = params.__len__();
		final PreparedStatement preparedStatement = (PreparedStatement)statement;
		final Procedure procedure = (this.style == STATEMENT_CALLABLE) ? (Procedure)this.sql : null;

		if (this.style != STATEMENT_CALLABLE) {
			columns = params.__len__();

			// clear the statement so all new bindings take affect only if not a callproc
			// this is because Procedure already registered the OUT parameters and we
			// don't want to lose those
			preparedStatement.clearParameters();
		} else {
			columns = (procedure.columns == Py.None) ? 0 : procedure.columns.__len__();
		}

		// count backwards through all the columns
		while (columns-- > 0) {
			column = columns + 1;

			if ((procedure != null) && (!procedure.isInput(column))) {
				continue;
			}

			// working from right to left
			PyObject param = params.__getitem__(--index);

			if (bindings != Py.None) {
				PyObject binding = bindings.__finditem__(Py.newInteger(index));

				if (binding != null) {
					try {
						int bindingValue = binding.__int__().getValue();

						datahandler.setJDBCObject(preparedStatement, column, param, bindingValue);
					} catch (PyException e) {
						throw zxJDBC.makeException(zxJDBC.ProgrammingError, zxJDBC.getString("bindingValue"));
					}

					continue;
				}
			}

			datahandler.setJDBCObject(preparedStatement, column, param);
		}

		return;
	}

	/**
	 * Method close
	 *
	 */
	public void close() {

		try {
			this.statement.close();
		} catch (SQLException e) {
			throw zxJDBC.makeException(e);
		} finally {
			this.closed = true;
		}
	}
}
