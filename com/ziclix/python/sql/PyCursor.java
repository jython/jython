
/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql;

import java.io.*;
import java.sql.*;
import java.math.*;
import java.util.*;
import org.python.core.*;
import com.ziclix.python.sql.util.*;

/**
 * These objects represent a database cursor, which is used to manage the
 * context of a fetch operation.
 *
 * @author brian zimmer
 * @author last revised by $Author$
 * @version $Revision$
 */
public class PyCursor extends PyObject implements ClassDictInit {

	/** Field fetch */
	protected Fetch fetch;

	/** Field arraysize */
	protected int arraysize;

	/** Field warnings */
	protected PyObject warnings;

	/** Field warnings */
	protected PyObject rowid;

	/** Field dynamicFetch */
	protected boolean dynamicFetch;

	/** Field connection */
	protected Connection connection;

	/** Field datahandler */
	protected DataHandler datahandler;

	/** Field sqlStatement */
	protected Statement sqlStatement;

	// they are stateless instances, so we only need to instantiate it once
	private static DataHandler DATAHANDLER = null;

	// discern the correct datahandler
	static {
		DATAHANDLER = new DataHandler();

		try {
			DATAHANDLER = new JDBC20DataHandler(DATAHANDLER);
		} catch (Throwable t) {}
	}

	/**
	 * Create the cursor with a static fetch.
	 */
	PyCursor(Connection connection) {
		this(connection, false);
	}

	/**
	 * Create the cursor, optionally choosing the type of fetch (static or dynamic).
	 * If dynamicFetch is true, then use a dynamic fetch.
	 */
	PyCursor(Connection connection, boolean dynamicFetch) {

		this.arraysize = 1;
		this.connection = connection;
		this.datahandler = DATAHANDLER;
		this.dynamicFetch = dynamicFetch;

		// constructs the appropriate Fetch among other things
		this.clear();
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
		PyObject[] m = new PyObject[7];

		m[0] = new PyString("close");
		m[1] = new PyString("execute");
		m[2] = new PyString("executemany");
		m[3] = new PyString("fetchone");
		m[4] = new PyString("fetchall");
		m[5] = new PyString("fetchmany");
		m[6] = new PyString("callproc");
		__methods__ = new PyList(m);
		m = new PyObject[6];
		m[0] = new PyString("arraysize");
		m[1] = new PyString("rowcount");
		m[2] = new PyString("description");
		m[3] = new PyString("datahandler");
		m[4] = new PyString("warnings");
		m[5] = new PyString("rowid");
		__members__ = new PyList(m);
	}

	/**
	 * String representation of the object.
	 *
	 * @return a string representation of the object.
	 */
	public String toString() {
		return "<PyCursor object instance at " + hashCode() + ">";
	}

	/**
	 * Sets the attribute name to value.
	 *
	 * @param name
	 * @param value
	 */
	public void __setattr__(String name, PyObject value) {

		if ("arraysize".equals(name)) {
			arraysize = value.__int__().getValue();
		} else if ("datahandler".equals(name)) {
			this.datahandler = (DataHandler)value.__tojava__(DataHandler.class);
		} else {
			super.__setattr__(name, value);
		}
	}

	/**
	 * Gets the value of the attribute name.
	 *
	 * @param name
	 * @return the attribute for the given name
	 */
	public PyObject __findattr__(String name) {

		if ("arraysize".equals(name)) {
			return Py.newInteger(arraysize);
		} else if ("__methods__".equals(name)) {
			return __methods__;
		} else if ("__members__".equals(name)) {
			return __members__;
		} else if ("description".equals(name)) {
			return this.fetch.getDescription();
		} else if ("rowcount".equals(name)) {
			return Py.newInteger(this.fetch.getRowCount());
		} else if ("warnings".equals(name)) {
			return warnings;
		} else if ("rowid".equals(name)) {
			return rowid;
		} else if ("datahandler".equals(name)) {
			return Py.java2py(this.datahandler);
		} else if ("dynamic".equals(name)) {
			return this.dynamicFetch ? Py.One : Py.Zero;
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
		dict.__setitem__("fetchmany", new CursorFunc("fetchmany", 0, 1, 2, "fetch specified number of rows"));
		dict.__setitem__("close", new CursorFunc("close", 1, 1, "close the cursor"));
		dict.__setitem__("fetchall", new CursorFunc("fetchall", 2, 1, "fetch all results"));
		dict.__setitem__("fetchone", new CursorFunc("fetchone", 3, 1, "fetch the next result"));
		dict.__setitem__("nextset", new CursorFunc("nextset", 4, 1, "return next set or None"));
		dict.__setitem__("execute", new CursorFunc("execute", 5, 1, 4, "execute the sql expression"));
		dict.__setitem__("setinputsizes", new CursorFunc("setinputsizes", 6, 1, "not implemented"));
		dict.__setitem__("setoutputsize", new CursorFunc("setoutputsize", 7, 1, 2, "not implemented"));
		dict.__setitem__("callproc", new CursorFunc("callproc", 8, 1, 2, "executes a stored procedure"));
		dict.__setitem__("executemany", new CursorFunc("executemany", 9, 1, 3, "execute sql with the parameter list"));

		// hide from python
		dict.__setitem__("classDictInit", null);
		dict.__setitem__("toString", null);
		dict.__setitem__("connection", null);
		dict.__setitem__("getDataHandler", null);
		dict.__setitem__("addWarning", null);
		dict.__setitem__("fetch", null);
		dict.__setitem__("newFetch", null);
		dict.__setitem__("sqlStatement", null);
		dict.__setitem__("dynamicFetch", null);
		dict.__setitem__("getPyClass", null);
	}

	/**
	 * Delete the cursor.
	 *
	 */
	public void __del__() {
		close();
	}

	/**
	 * Close the cursor now (rather than whenever __del__ is called).
	 * The cursor will be unusable from this point forward; an Error
	 * (or subclass) exception will be raised if any operation is
	 * attempted with the cursor.
	 *
	 */
	public void close() {
		this.clear();
	}

	/**
	 * Return the currently bound DataHandler.
	 */
	public DataHandler getDataHandler() {
		return this.datahandler;
	}

	/**
	 * Method newFetch
	 *
	 */
	protected void newFetch() {

		if (this.dynamicFetch) {
			this.fetch = Fetch.newDynamicFetch(this);
		} else {
			this.fetch = Fetch.newStaticFetch(this);
		}
	}

	/**
	 * Prepare a callable statement (stored procedure).
	 *
	 * @param sqlString
	 * @param maxRows max number of rows to be returned
	 * @throws SQLException
	 */
	protected void callableStatement(String sqlString, PyObject maxRows) throws SQLException {

		this.sqlStatement = this.connection.prepareCall(sqlString);

		if (maxRows != Py.None) {
			this.sqlStatement.setMaxRows(maxRows.__int__().getValue());
		}
	}

	/**
	 * Prepare a statement ready for executing.
	 *
	 * @param sqlString
	 * @param maxRows max number of rows to be returned
	 * @param prepared if true, prepare the statement, otherwise create a normal statement
	 * @throws SQLException
	 */
	protected void prepareStatement(String sqlString, PyObject maxRows, boolean prepared) throws SQLException {

		if (prepared) {
			this.sqlStatement = this.connection.prepareStatement(sqlString);
		} else {
			this.sqlStatement = this.connection.createStatement();
		}

		if (maxRows != Py.None) {
			this.sqlStatement.setMaxRows(maxRows.__int__().getValue());
		}
	}

	/**
	 * This method is optional since not all databases provide stored procedures.
	 *
	 * Call a stored database procedure with the given name. The sequence of parameters
	 * must contain one entry for each argument that the procedure expects. The result of
	 * the call is returned as modified copy of the input sequence. Input parameters are
	 * left untouched, output and input/output parameters replaced with possibly new values.
	 *
	 * The procedure may also provide a result set as output. This must then be made available
	 * through the standard fetchXXX() methods.
	 */
	public void callproc(String sqlString, PyObject params, PyObject bindings, PyObject maxRows) {

		clear();

		try {
			if (this.connection.getMetaData().supportsStoredProcedures()) {
				callableStatement(sqlString, maxRows);
				execute(params, bindings);
			} else {
				throw zxJDBC.makeException(zxJDBC.NotSupportedError, zxJDBC.getString("noStoredProc"));
			}
		} catch (PyException e) {
			throw e;
		} catch (Exception e) {
			throw zxJDBC.newError(e);
		}
	}

	/**
	 * Prepare a database operation (query or command) and then execute it against all
	 * parameter sequences or mappings found in the sequence seq_of_parameters.
	 * Modules are free to implement this method using multiple calls to the execute()
	 * method or by using array operations to have the database process the sequence as
	 * a whole in one call.
	 *
	 * The same comments as for execute() also apply accordingly to this method.
	 *
	 * Return values are not defined.
	 */
	public void executemany(String sqlString, PyObject params, PyObject bindings, PyObject maxRows) {
		execute(sqlString, params, bindings, maxRows);
	}

	/**
	 * Prepare and execute a database operation (query or command).
	 * Parameters may be provided as sequence or mapping and will
	 * be bound to variables in the operation. Variables are specified
	 * in a database-specific notation (see the module's paramstyle
	 * attribute for details).
	 *
	 * A reference to the operation will be retained by the cursor.
	 * If the same operation object is passed in again, then the cursor
	 * can optimize its behavior. This is most effective for algorithms
	 * where the same operation is used, but different parameters are
	 * bound to it (many times).
	 *
	 * For maximum efficiency when reusing an operation, it is best to
	 * use the setinputsizes() method to specify the parameter types and
	 * sizes ahead of time. It is legal for a parameter to not match the
	 * predefined information; the implementation should compensate, possibly
	 * with a loss of efficiency.
	 *
	 * The parameters may also be specified as list of tuples to e.g. insert
	 * multiple rows in a single operation, but this kind of usage is
	 * deprecated: executemany() should be used instead.
	 *
	 * Return values are not defined.
	 *
	 * @param sqlString sql string
	 * @param params params for a prepared statement
	 * @param bindings dictionary of (param index : SQLType binding)
	 * @param maxRows integer value of max rows
	 */
	public void execute(String sqlString, PyObject params, PyObject bindings, PyObject maxRows) {

		clear();

		boolean hasParams = hasParams(params);

		try {
			prepareStatement(sqlString, maxRows, hasParams);

			if (hasParams) {
				execute(params, bindings);
			} else {
				this.datahandler.preExecute(this.sqlStatement);

				if (this.sqlStatement.execute(sqlString)) {
					create(this.sqlStatement.getResultSet());
				}

				this.rowid = this.datahandler.getRowId(this.sqlStatement);

				addWarning(this.sqlStatement.getWarnings());
				this.datahandler.postExecute(this.sqlStatement);
			}
		} catch (PyException e) {
			throw e;
		} catch (Exception e) {
			throw zxJDBC.newError(e);
		}
	}

	/**
	 * The workhorse for properly executing a prepared statement.
	 *
	 * @param PyObject params a non-None seq of sequences or entities
	 * @param PyObject bindings an optional dictionary of index:DBApiType mappings
	 *
	 * @throws SQLException
	 *
	 */
	protected void execute(PyObject params, PyObject bindings) throws SQLException {

		// if we have a sequence of sequences, let's run through them and finish
		if (isSeqSeq(params)) {

			// [(3, 4)] or [(3, 4), (5, 6)]
			for (int i = 0, len = params.__len__(); i < len; i++) {
				PyObject param = params.__getitem__(i);

				execute(param, bindings);
			}

			// we've recursed through everything, so we're done
			return;
		}

		// [3, 4] or (3, 4)
		PreparedStatement preparedStatement = (PreparedStatement)this.sqlStatement;

		// clear the statement so all new bindings take affect
		preparedStatement.clearParameters();

		for (int i = 0, len = params.__len__(); i < len; i++) {
			PyObject param = params.__getitem__(i);

			if (bindings != Py.None) {
				PyObject binding = bindings.__finditem__(Py.newInteger(i));

				if (binding != null) {
					int bindingValue = 0;

					try {
						bindingValue = binding.__int__().getValue();
					} catch (PyException e) {
						throw zxJDBC.makeException(zxJDBC.ProgrammingError, zxJDBC.getString("bindingValue"));
					}

					this.datahandler.setJDBCObject(preparedStatement, i + 1, param, bindingValue);

					continue;
				}
			}

			this.datahandler.setJDBCObject(preparedStatement, i + 1, param);
		}

		this.datahandler.preExecute(preparedStatement);
		preparedStatement.execute();
		create(preparedStatement.getResultSet());

		this.rowid = this.datahandler.getRowId(preparedStatement);

		addWarning(preparedStatement.getWarnings());
		this.datahandler.postExecute(preparedStatement);

		return;
	}

	/**
	 * Fetch the next row of a query result set, returning a single sequence,
	 * or None when no more data is available.
	 *
	 * An Error (or subclass) exception is raised if the previous call to
	 * executeXXX() did not produce any result set or no call was issued yet.
	 *
	 * @return a single sequence from the result set, or None when no more data is available
	 */
	public PyObject fetchone() {
		return this.fetch.fetchone();
	}

	/**
	 * Fetch all (remaining) rows of a query result, returning them as a sequence
	 * of sequences (e.g. a list of tuples). Note that the cursor's arraysize attribute
	 * can affect the performance of this operation.
	 *
	 * An Error (or subclass) exception is raised if the previous call to executeXXX()
	 * did not produce any result set or no call was issued yet.
	 *
	 * @return a sequence of sequences from the result set, or None when no more data is available
	 */
	public PyObject fetchall() {
		return this.fetch.fetchall();
	}

	/**
	 * Fetch the next set of rows of a query result, returning a sequence of
	 * sequences (e.g. a list of tuples). An empty sequence is returned when
	 * no more rows are available.
	 *
	 * The number of rows to fetch per call is specified by the parameter. If
	 * it is not given, the cursor's arraysize determines the number of rows
	 * to be fetched. The method should try to fetch as many rows as indicated
	 * by the size parameter. If this is not possible due to the specified number
	 * of rows not being available, fewer rows may be returned.
	 *
	 * An Error (or subclass) exception is raised if the previous call to executeXXX()
	 * did not produce any result set or no call was issued yet.
	 *
	 * Note there are performance considerations involved with the size parameter.
	 * For optimal performance, it is usually best to use the arraysize attribute.
	 * If the size parameter is used, then it is best for it to retain the same value
	 * from one fetchmany() call to the next.
	 *
	 * @return a sequence of sequences from the result set, or None when no more data is available
	 */
	public PyObject fetchmany(int size) {
		return this.fetch.fetchmany(size);
	}

	/**
	 * Move the result pointer to the next set if available.
	 *
	 * @return true if more sets exist, else None
	 */
	public PyObject nextset() {
		return this.fetch.nextset();
	}

	/**
	 * Create the results after a successful execution and closes the result set.
	 *
	 * @param rs A ResultSet.
	 */
	protected void create(ResultSet rs) {
		create(rs, null);
	}

	/**
	 * Create the results after a successful execution and closes the result set.
	 * Optionally takes a set of JDBC-indexed columns to automatically set to None
	 * primarily to support getTypeInfo() which sets a column type of a number but
	 * doesn't use the value so a driver is free to put anything it wants there.
	 *
	 * @param rs A ResultSet.
	 * @param skipCols set of JDBC-indexed columns to automatically set as None
	 */
	protected void create(ResultSet rs, Set skipCols) {
		this.fetch.add(rs, skipCols);
	}

	/**
	 * Adds a warning to the tuple and will follow the chain as necessary.
	 */
	void addWarning(SQLWarning warning) {

		if (warning == null) {
			return;
		}

		if (this.warnings == Py.None) {
			this.warnings = new PyList();
		}

		PyTuple warn = new PyTuple();

		// there are three parts: (reason, state, vendorCode)
		warn.__add__(Py.java2py(warning.getMessage()));
		warn.__add__(Py.java2py(warning.getSQLState()));
		warn.__add__(Py.newInteger(warning.getErrorCode()));

		// add the warning to the list
		((PyList)this.warnings).append(warn);

		SQLWarning next = warning.getNextWarning();

		if (next != null) {
			addWarning(next);
		}

		return;
	}

	/**
	 * Reset the cursor state.
	 */
	protected void clear() {

		this.warnings = Py.None;
		this.rowid = Py.None;

		try {
			this.fetch.close();
		} catch (Exception e) {}
		finally {
			this.newFetch();
		}

		try {
			this.sqlStatement.close();
		} catch (Exception e) {}
		finally {
			this.sqlStatement = null;
		}
	}

	/**
	 * Method isSeq
	 *
	 * @param PyObject object
	 *
	 * @return true for any PyList, PyTuple or java.util.List
	 *
	 */
	protected boolean isSeq(PyObject object) {

		if ((object == null) || (object == Py.None)) {
			return false;
		}

		if (object.__tojava__(List.class) != Py.NoConversion) {
			return true;
		}

		// originally checked for __getitem__ and __len__, but this is true for PyString
		// and we don't want to insert one character at a time
		return (object instanceof PyList) || (object instanceof PyTuple);
	}

	/**
	 * Method hasParams
	 *
	 * @param PyObject params
	 *
	 * @return boolean
	 *
	 */
	protected boolean hasParams(PyObject params) {

		if (Py.None == params) {
			return false;
		}

		boolean isSeq = isSeq(params);

		// the optional argument better be a sequence
		if (!isSeq) {
			throw zxJDBC.makeException(zxJDBC.ProgrammingError, zxJDBC.getString("optionalSecond"));
		}

		return params.__len__() > 0;
	}

	/**
	 * Method isSeqSeq
	 *
	 * @param PyObject object
	 *
	 * @return true is a sequence of sequences
	 *
	 */
	protected boolean isSeqSeq(PyObject object) {

		if (isSeq(object) && (object.__len__() > 0)) {
			for (int i = 0; i < object.__len__(); i++) {
				if (!isSeq(object.__finditem__(i))) {
					return false;
				}
			}

			return true;
		}

		return false;
	}
}

/**
 * Class CursorFunc
 *
 * @date $today.date$
 * @author last modified by $Author$
 * @date last modified on $Date$
 * @version $Revision$
 * @copyright 2001 brian zimmer
 */
class CursorFunc extends PyBuiltinFunctionSet {

	/**
	 * Constructor CursorFunc
	 *
	 * @param String name
	 * @param int index
	 * @param int argcount
	 * @param String doc
	 *
	 */
	CursorFunc(String name, int index, int argcount, String doc) {
		super(name, index, argcount, argcount, true, doc);
	}

	/**
	 * Constructor CursorFunc
	 *
	 * @param String name
	 * @param int index
	 * @param int minargs
	 * @param int maxargs
	 * @param String doc
	 *
	 */
	CursorFunc(String name, int index, int minargs, int maxargs, String doc) {
		super(name, index, minargs, maxargs, true, doc);
	}

	/**
	 * Method __call__
	 *
	 * @return PyObject
	 *
	 */
	public PyObject __call__() {

		PyCursor cursor = (PyCursor)__self__;

		switch (index) {

			case 0 :
				return cursor.fetchmany(cursor.arraysize);

			case 1 :
				cursor.close();

				return Py.None;

			case 2 :
				return cursor.fetchall();

			case 3 :
				return cursor.fetchone();

			case 4 :
				return cursor.nextset();

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

		PyCursor cursor = (PyCursor)__self__;

		switch (index) {

			case 0 :
				return cursor.fetchmany(arg.__int__().getValue());

			case 5 :
				cursor.execute(arg.__str__().toString(), Py.None, Py.None, Py.None);

				return Py.None;

			case 6 :
			case 7 :
				return Py.None;

			case 8 :
				cursor.callproc(arg.__str__().toString(), Py.None, Py.None, Py.None);

				return Py.None;

			case 9 :
				cursor.executemany(arg.__str__().toString(), Py.None, Py.None, Py.None);

				return Py.None;

			default :
				throw argCountError(1);
		}
	}

	/**
	 * Method __call__
	 *
	 * @param PyObject arga
	 * @param PyObject argb
	 *
	 * @return PyObject
	 *
	 */
	public PyObject __call__(PyObject arga, PyObject argb) {

		PyCursor cursor = (PyCursor)__self__;

		switch (index) {

			case 5 :
				cursor.execute(arga.__str__().toString(), argb, Py.None, Py.None);

				return Py.None;

			case 7 :
				return Py.None;

			case 8 :
				cursor.callproc(arga.__str__().toString(), argb, Py.None, Py.None);

				return Py.None;

			case 9 :
				cursor.executemany(arga.__str__().toString(), argb, Py.None, Py.None);

				return Py.None;

			default :
				throw argCountError(2);
		}
	}

	/**
	 * Method __call__
	 *
	 * @param PyObject arga
	 * @param PyObject argb
	 * @param PyObject argc
	 *
	 * @return PyObject
	 *
	 */
	public PyObject __call__(PyObject arga, PyObject argb, PyObject argc) {

		PyCursor cursor = (PyCursor)__self__;

		switch (index) {

			case 5 :
				cursor.execute(arga.__str__().toString(), argb, argc, Py.None);

				return Py.None;

			case 8 :
				cursor.callproc(arga.__str__().toString(), argb, argc, Py.None);

				return Py.None;

			case 9 :
				cursor.executemany(arga.__str__().toString(), argb, argc, Py.None);

				return Py.None;

			default :
				throw argCountError(3);
		}
	}

	/**
	 * Method __call__
	 *
	 * @param PyObject[] args
	 * @param String[] keywords
	 *
	 * @return PyObject
	 *
	 */
	public PyObject __call__(PyObject[] args, String[] keywords) {

		PyCursor cursor = (PyCursor)__self__;
		PyArgParser parser = new PyArgParser(args, keywords);
		String sql = parser.arg(0).__str__().toString();
		PyObject params = parser.kw("params", Py.None);
		PyObject bindings = parser.kw("bindings", Py.None);
		PyObject maxrows = parser.kw("maxrows", Py.None);

		params = (parser.numArg() >= 2) ? parser.arg(1) : params;
		bindings = (parser.numArg() >= 3) ? parser.arg(2) : bindings;
		maxrows = (parser.numArg() >= 4) ? parser.arg(3) : maxrows;

		switch (index) {

			case 5 :
				cursor.execute(sql, params, bindings, maxrows);

				return Py.None;

			case 8 :
				cursor.callproc(sql, params, bindings, maxrows);

				return Py.None;

			case 9 :
				cursor.executemany(sql, params, bindings, maxrows);

				return Py.None;

			default :
				throw argCountError(4);
		}
	}
}
