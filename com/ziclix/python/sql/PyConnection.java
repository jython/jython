
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
 * A connection to the database.
 *
 * @author brian zimmer
 * @author last revised by $Author$
 * @version $Revision$
 */
public class PyConnection extends PyObject implements ClassDictInit {

	/** Field connection */
	protected Connection connection;

	/** Field supportsTransactions */
	protected boolean supportsTransactions;

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

	/**
	 * Create a PyConnection with the open connection.
	 */
	public PyConnection(Connection connection) throws SQLException {

		this.connection = connection;
		this.supportsTransactions = this.connection.getMetaData().supportsTransactions();

		if (this.supportsTransactions) {
			this.connection.setAutoCommit(false);
		}
	}

	/**
	 * Produces a string representation of the object.
	 *
	 * @return string representation of the object.
	 */
	public String toString() {

		try {
			return "<PyConnection user='" + this.connection.getMetaData().getUserName() + "', url='" + this.connection.getMetaData().getURL() + "'>";
		} catch (SQLException e) {
			return "<PyConnection at " + hashCode() + ">";
		}
	}

	/**
	 * Method classDictInit
	 *
	 * @param PyObject dict
	 *
	 */
	static public void classDictInit(PyObject dict) {

		dict.__setitem__("autocommit", new PyInteger(0));
		dict.__setitem__("__version__", Py.newString("$Revision$").__getslice__(Py.newInteger(11), Py.newInteger(-2), null));
		dict.__setitem__("close", new ConnectionFunc("close", 0, 0, 0, zxJDBC.getString("close")));
		dict.__setitem__("commit", new ConnectionFunc("commit", 1, 0, 0, zxJDBC.getString("commit")));
		dict.__setitem__("cursor", new ConnectionFunc("cursor", 2, 0, 1, zxJDBC.getString("cursor")));
		dict.__setitem__("rollback", new ConnectionFunc("rollback", 3, 0, 0, zxJDBC.getString("rollback")));

		// hide from python
		dict.__setitem__("initModule", null);
		dict.__setitem__("toString", null);
		dict.__setitem__("setConnection", null);
		dict.__setitem__("getPyClass", null);
		dict.__setitem__("connection", null);
		dict.__setitem__("classDictInit", null);
	}

	/**
	 * Sets the attribute.
	 *
	 * @param name
	 * @param value
	 */
	public void __setattr__(String name, PyObject value) {

		if ("autocommit".equals(name)) {
			try {
				if (this.supportsTransactions) {
					this.connection.setAutoCommit(value.__nonzero__());
				}
			} catch (SQLException e) {
				throw zxJDBC.makeException(zxJDBC.DatabaseError, e.getMessage());
			}

			return;
		}

		super.__setattr__(name, value);
	}

	/**
	 * Finds the attribute.
	 *
	 * @param name the name of the attribute of interest
	 * @return the value for the attribute of the specified name
	 */
	public PyObject __findattr__(String name) {

		if ("autocommit".equals(name)) {
			try {
				return connection.getAutoCommit() ? Py.One : Py.Zero;
			} catch (SQLException e) {
				throw zxJDBC.makeException(zxJDBC.DatabaseError, e.getMessage());
			}
		} else if ("dbname".equals(name)) {
			try {
				return Py.newString(this.connection.getMetaData().getDatabaseProductName());
			} catch (SQLException e) {
				throw zxJDBC.makeException(zxJDBC.DatabaseError, e.getMessage());
			}
		} else if ("dbversion".equals(name)) {
			try {
				return Py.newString(this.connection.getMetaData().getDatabaseProductVersion());
			} catch (SQLException e) {
				throw zxJDBC.makeException(zxJDBC.DatabaseError, e.getMessage());
			}
		} else if ("driverversion".equals(name)) {
			try {
				return Py.newString(this.connection.getMetaData().getDriverVersion());
			} catch (SQLException e) {
				throw zxJDBC.makeException(zxJDBC.DatabaseError, e.getMessage());
			}
		} else if ("url".equals(name)) {
			try {
				return Py.newString(this.connection.getMetaData().getURL());
			} catch (SQLException e) {
				throw zxJDBC.makeException(zxJDBC.DatabaseError, e.getMessage());
			}
		} else if ("__connection__".equals(name)) {
			return Py.java2py(this.connection);
		}

		return super.__findattr__(name);
	}

	/**
	 * Close the connection now (rather than whenever __del__ is called).
	 * The connection will be unusable from this point forward; an Error
	 * (or subclass) exception will be raised if any operation is attempted
	 * with the connection. The same applies to all cursor objects trying
	 * to use the connection.
	 *
	 */
	public void close() {

		try {

			// close the cursors too?
			this.connection.close();
		} catch (SQLException e) {
			throw zxJDBC.newError(e);
		}
	}

	/**
	 * Commit any pending transaction to the database. Note that if the
	 * database supports an auto-commit feature, this must be initially
	 * off. An interface method may be provided to turn it back on.
	 *
	 * Database modules that do not support transactions should implement
	 * this method with void functionality.
	 *
	 */
	public void commit() {

		if (!this.supportsTransactions) {
			return;
		}

		try {
			this.connection.commit();
		} catch (SQLException e) {
			throw zxJDBC.newError(e);
		}
	}

	/**
	 * <i>This method is optional since not all databases provide transaction
	 * support.</i>
	 *
	 * In case a database does provide transactions this method causes the database
	 * to roll back to the start of any pending transaction. Closing a connection
	 * without committing the changes first will cause an implicit rollback to be
	 * performed.
	 *
	 */
	void rollback() {

		if (!this.supportsTransactions) {
			return;
		}

		try {
			this.connection.rollback();
		} catch (SQLException e) {
			throw zxJDBC.newError(e);
		}
	}

	/**
	 * Return a new Cursor Object using the connection. If the database does not
	 * provide a direct cursor concept, the module will have to emulate cursors
	 * using other means to the extent needed by this specification.
	 *
	 * @return a new cursor using this connection
	 */
	public PyCursor cursor() {
		return new PyExtendedCursor(this.connection);
	}

	/**
	 * Return a new Cursor Object using the connection. If the database does not
	 * provide a direct cursor concept, the module will have to emulate cursors
	 * using other means to the extent needed by this specification.
	 *
	 * @param dynamicFetch if true, dynamically iterate the result
	 * @return a new cursor using this connection
	 */
	public PyCursor cursor(boolean dynamicFetch) {
		return new PyExtendedCursor(this.connection, dynamicFetch);
	}
}

/**
 * Class ConnectionFunc
 *
 * @date $today.date$
 * @author last modified by $Author$
 * @date last modified on $Date$
 * @version $Revision$
 * @copyright 2001 brian zimmer
 */
class ConnectionFunc extends PyBuiltinFunctionSet {

	/**
	 * Constructor ConnectionFunc
	 *
	 * @param String name
	 * @param int index
	 * @param int minargs
	 * @param int maxargs
	 * @param String doc
	 *
	 */
	ConnectionFunc(String name, int index, int minargs, int maxargs, String doc) {
		super(name, index, minargs, maxargs, true, doc);
	}

	/**
	 * Method __call__
	 *
	 * @return PyObject
	 *
	 */
	public PyObject __call__() {

		PyConnection c = (PyConnection)__self__;

		switch (index) {

			case 0 :
				c.close();

				return Py.None;

			case 1 :
				c.commit();

				return Py.None;

			case 2 :
				return c.cursor();

			case 3 :
				c.rollback();

				return Py.None;

			default :
				throw argCountError(0);
		}
	}

	/**
	 * Method __call__
	 *
	 * @param PyObject arg
	 *
	 * @return PyObject
	 *
	 */
	public PyObject __call__(PyObject arg) {

		PyConnection c = (PyConnection)__self__;

		switch (index) {

			case 2 :
				return c.cursor(arg.__nonzero__());

			default :
				throw argCountError(1);
		}
	}
}
