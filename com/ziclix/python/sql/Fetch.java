
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
import java.util.*;
import org.python.core.*;

/**
 * <p>The responsibility of a Fetch instance is to manage the iteration of a
 * ResultSet.  Two different alogorithms are available: static or dynamic.</p>
 *
 * <p><b>Static</b> The static variety iterates the entire set immediately,
 * creating the necessary Jython objects and storing them.  It is able to
 * immediately close the ResultSet so a call to close() is essentially a no-op
 * from a database resource perspective (it does clear the results list however).
 * This approach also allows for the correct rowcount to be determined since
 * the entire result set has been iterated.</p>
 *
 * <p><b>Dynamic</b> The dynamic variety iterates the result set only as requested.
 * This holds a bit truer to the intent of the API as the fetch*() methods actually
 * fetch when instructed.  This is especially useful for managing exeedingly large
 * results, but is unable to determine the rowcount without having worked through
 * the entire result set.  The other disadvantage is the ResultSet remains open
 * throughout the entire iteration.  So the tradeoff is in open database resources
 * versus JVM resources since the application can keep constant space if it doesn't
 * require the entire result set be presented as one.</p>
 *
 * @author brian zimmer
 * @version $Revision$
 */
abstract public class Fetch {

	/**
	 * The total number of rows in the result set.
	 *
	 * Note: since JDBC provides no means to get this information without iterating
	 * the entire result set, only those fetches which build the result statically
	 * will have an accurate row count.
	 */
	protected int rowcount;

	/** The current row of the cursor (-1 if off either end). */
	protected int rownumber;

	/** Field cursor */
	private DataHandler datahandler;

	/** Field description */
	protected PyObject description;

	/** A list of warning listeners. */
	private List listeners;

	/**
	 * Constructor Fetch
	 *
	 * @param datahandler
	 *
	 */
	protected Fetch(DataHandler datahandler) {

		this.rowcount = -1;
		this.rownumber = -1;
		this.description = Py.None;
		this.datahandler = datahandler;
		this.listeners = new ArrayList(3);
	}

	/**
	 * Method newFetch
	 *
	 * @param datahandler
	 * @param dynamic
	 *
	 * @return Fetch
	 *
	 */
	public static Fetch newFetch(DataHandler datahandler, boolean dynamic) {

		if (dynamic) {
			return new DynamicFetch(datahandler);
		} else {
			return new StaticFetch(datahandler);
		}
	}

	/**
	 * The number of rows in the current result set.
	 */
	public int getRowCount() {
		return this.rowcount;
	}

	/**
	 * The description of each column, in order, for the data in the result
	 * set.
	 */
	public PyObject getDescription() {
		return this.description;
	}

	/**
	 * Create the results after a successful execution and manages the result set.
	 *
	 * @param ResultSet resultSet
	 *
	 */
	abstract public void add(ResultSet resultSet);

	/**
	 * Create the results after a successful execution and manages the result set.
	 * Optionally takes a set of JDBC-indexed columns to automatically set to None
	 * primarily to support getTypeInfo() which sets a column type of a number but
	 * doesn't use the value so a driver is free to put anything it wants there.
	 *
	 * @param ResultSet resultSet
	 * @param Set skipCols JDBC-indexed set of columns to be skipped
	 *
	 */
	abstract public void add(ResultSet resultSet, Set skipCols);

	/**
	 * Method add
	 *
	 * @param CallableStatement callableStatement
	 * @param Procedure procedure
	 * @param PyObject params
	 *
	 */
	abstract public void add(CallableStatement callableStatement, Procedure procedure, PyObject params);

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

		PyObject sequence = fetchmany(1);

		if (sequence.__len__() == 1) {
			return sequence.__getitem__(0);
		} else {
			return Py.None;
		}
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
	public abstract PyObject fetchall();

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
	public abstract PyObject fetchmany(int size);

	/**
	 * Move the result pointer to the next set if available.
	 *
	 * @return true if more sets exist, else None
	 */
	public abstract PyObject nextset();

	/**
	 * Scroll the cursor in the result set to a new position according
	 * to mode.
	 *
	 * If mode is 'relative' (default), value is taken as offset to
	 * the current position in the result set, if set to 'absolute',
	 * value states an absolute target position.
	 *
	 * An IndexError should be raised in case a scroll operation would
	 * leave the result set. In this case, the cursor position is left
	 * undefined (ideal would be to not move the cursor at all).
	 *
	 * Note: This method should use native scrollable cursors, if
	 * available, or revert to an emulation for forward-only
	 * scrollable cursors. The method may raise NotSupportedErrors to
	 * signal that a specific operation is not supported by the
	 * database (e.g. backward scrolling).
	 *
	 * @param int value
	 * @param String mode
	 *
	 */
	public abstract void scroll(int value, String mode);

	/**
	 * Cleanup any resources.
	 */
	public void close() throws SQLException {
		this.listeners.clear();
	}

	/**
	 * Builds a tuple containing the meta-information about each column.
	 *
	 * (name, type_code, display_size, internal_size, precision, scale, null_ok)
	 *
	 * precision and scale are only available for numeric types
	 */
	protected PyObject createDescription(ResultSetMetaData meta) throws SQLException {

		PyObject metadata = new PyList();

		for (int i = 1; i <= meta.getColumnCount(); i++) {
			PyObject[] a = new PyObject[7];

			a[0] = Py.newString(meta.getColumnName(i));
			a[1] = Py.newInteger(meta.getColumnType(i));
			a[2] = Py.newInteger(meta.getColumnDisplaySize(i));
			a[3] = Py.None;

			switch (meta.getColumnType(i)) {

				case Types.BIGINT :
				case Types.BIT :
				case Types.DECIMAL :
				case Types.DOUBLE :
				case Types.FLOAT :
				case Types.INTEGER :
				case Types.SMALLINT :
					a[4] = Py.newInteger(meta.getPrecision(i));
					a[5] = Py.newInteger(meta.getScale(i));
					break;

				default :
					a[4] = Py.None;
					a[5] = Py.None;
					break;
			}

			a[6] = Py.newInteger(meta.isNullable(i));

			((PyList)metadata).append(new PyTuple(a));
		}

		return metadata;
	}

	/**
	 * Builds a tuple containing the meta-information about each column.
	 *
	 * (name, type_code, display_size, internal_size, precision, scale, null_ok)
	 *
	 * precision and scale are only available for numeric types
	 */
	protected PyObject createDescription(Procedure procedure) throws SQLException {

		PyObject metadata = new PyList();

		for (int i = 0, len = procedure.columns.__len__(); i < len; i++) {
			PyObject column = procedure.columns.__getitem__(i);
			int colType = column.__getitem__(Procedure.COLUMN_TYPE).__int__().getValue();

			switch (colType) {

				case DatabaseMetaData.procedureColumnReturn :
					PyObject[] a = new PyObject[7];

					a[0] = column.__getitem__(Procedure.NAME);
					a[1] = column.__getitem__(Procedure.DATA_TYPE);
					a[2] = Py.newInteger(-1);
					a[3] = column.__getitem__(Procedure.LENGTH);

					switch (a[1].__int__().getValue()) {

						case Types.BIGINT :
						case Types.BIT :
						case Types.DECIMAL :
						case Types.DOUBLE :
						case Types.FLOAT :
						case Types.INTEGER :
						case Types.SMALLINT :
							a[4] = column.__getitem__(Procedure.PRECISION);
							a[5] = column.__getitem__(Procedure.SCALE);
							break;

						default :
							a[4] = Py.None;
							a[5] = Py.None;
							break;
					}

					int nullable = column.__getitem__(Procedure.NULLABLE).__int__().getValue();

					a[6] = (nullable == DatabaseMetaData.procedureNullable) ? Py.One : Py.Zero;

					((PyList)metadata).append(new PyTuple(a));
					break;
			}
		}

		return metadata;
	}

	/**
	 * Method createResults
	 *
	 * @param CallableStatement callableStatement
	 * @param Procedure procedure
	 * @param PyObject params
	 *
	 * @return PyObject
	 *
	 * @throws SQLException
	 *
	 */
	protected PyObject createResults(CallableStatement callableStatement, Procedure procedure, PyObject params) throws SQLException {

		PyList results = new PyList();

		for (int i = 0, j = 0, len = procedure.columns.__len__(); i < len; i++) {
			PyObject obj = Py.None;
			PyObject column = procedure.columns.__getitem__(i);
			int colType = column.__getitem__(Procedure.COLUMN_TYPE).__int__().getValue();
			int dataType = column.__getitem__(Procedure.DATA_TYPE).__int__().getValue();

			switch (colType) {

				case DatabaseMetaData.procedureColumnIn :
					j++;
					break;

				case DatabaseMetaData.procedureColumnOut :
				case DatabaseMetaData.procedureColumnInOut :
					obj = datahandler.getPyObject(callableStatement, i + 1, dataType);

					params.__setitem__(j++, obj);
					break;

				case DatabaseMetaData.procedureColumnReturn :
					obj = datahandler.getPyObject(callableStatement, i + 1, dataType);

					// Oracle sends ResultSets as a return value
					Object rs = obj.__tojava__(ResultSet.class);

					if (rs == Py.NoConversion) {
						results.append(obj);
					} else {
						add((ResultSet)rs);
					}
					break;
			}
		}

		if (results.__len__() == 0) {
			return results;
		}

		PyList ret = new PyList();

		ret.append(__builtin__.tuple(results));

		return ret;
	}

	/**
	 * Creates the results of a query.  Iterates through the list and builds the tuple.
	 *
	 * @param set result set
	 * @param skipCols set of JDBC-indexed columns to automatically set to None
	 * @return a list of tuples of the results
	 * @throws SQLException
	 */
	protected PyList createResults(ResultSet set, Set skipCols, PyObject metaData) throws SQLException {

		PyList res = new PyList();

		while (set.next()) {
			PyObject tuple = createResult(set, skipCols, metaData);

			res.append(tuple);
		}

		return res;
	}

	/**
	 * Creates the individual result row from the current ResultSet row.
	 *
	 * @param set result set
	 * @param skipCols set of JDBC-indexed columns to automatically set to None
	 * @return a tuple of the results
	 * @throws SQLException
	 */
	protected PyTuple createResult(ResultSet set, Set skipCols, PyObject metaData) throws SQLException {

		int descriptionLength = metaData.__len__();
		PyObject[] row = new PyObject[descriptionLength];

		for (int i = 0; i < descriptionLength; i++) {
			if ((skipCols != null) && skipCols.contains(new Integer(i + 1))) {
				row[i] = Py.None;
			} else {
				int type = ((PyInteger)metaData.__getitem__(i).__getitem__(1)).getValue();

				row[i] = datahandler.getPyObject(set, i + 1, type);
			}
		}

		SQLWarning warning = set.getWarnings();

		if (warning != null) {
			fireWarning(warning);
		}

		PyTuple tuple = new PyTuple(row);

		return tuple;
	}

	protected void fireWarning(SQLWarning warning) {

		WarningEvent event = new WarningEvent(this, warning);

		for (int i = listeners.size() - 1; i >= 0; i--) {
			try {
				((WarningListener)listeners.get(i)).warning(event);
			} catch (Throwable t) {}
		}
	}

	public void addWarningListener(WarningListener listener) {
		this.listeners.add(listener);
	}

	public boolean removeWarningListener(WarningListener listener) {
		return this.listeners.remove(listener);
	}
}

/**
 * This version of fetch builds the results statically.  This consumes more resources but
 * allows for efficient closing of database resources because the contents of the result
 * set are immediately consumed.  It also allows for an accurate rowcount attribute, whereas
 * a dynamic query is unable to provide this information until all the results have been
 * consumed.
 */
class StaticFetch extends Fetch {

	/** Field results */
	protected List results;

	/** Field descriptions */
	protected List descriptions;

	/**
	 * Construct a static fetch.  The entire result set is iterated as it
	 * is added and the result set is immediately closed.
	 */
	public StaticFetch(DataHandler datahandler) {

		super(datahandler);

		this.results = new LinkedList();
		this.descriptions = new LinkedList();
	}

	/**
	 * Method add
	 *
	 * @param ResultSet resultSet
	 *
	 */
	public void add(ResultSet resultSet) {
		this.add(resultSet, null);
	}

	/**
	 * Method add
	 *
	 * @param ResultSet resultSet
	 * @param Set skipCols
	 *
	 */
	public void add(ResultSet resultSet, Set skipCols) {

		try {
			if ((resultSet != null) && (resultSet.getMetaData() != null)) {
				PyObject metadata = this.createDescription(resultSet.getMetaData());
				PyObject result = this.createResults(resultSet, skipCols, metadata);

				if (result.__len__() > 0) {
					this.results.add(result);
					this.descriptions.add(metadata);

					// we want the rowcount of the first result set
					this.rowcount = ((PyObject)this.results.get(0)).__len__();

					// we want the description of the first result set
					this.description = ((PyObject)this.descriptions.get(0));

					// set the current rownumber
					this.rownumber = 0;
				}
			}
		} catch (PyException e) {
			throw e;
		} catch (Exception e) {
			throw zxJDBC.makeException(e);
		} finally {
			try {
				resultSet.close();
			} catch (Exception e) {}
		}
	}

	/**
	 * Method add
	 *
	 * @param CallableStatement callableStatement
	 * @param Procedure procedure
	 * @param PyObject params
	 *
	 */
	public void add(CallableStatement callableStatement, Procedure procedure, PyObject params) {

		try {
			PyObject result = this.createResults(callableStatement, procedure, params);

			if (result.__len__() > 0) {
				this.results.add(result);
				this.descriptions.add(this.createDescription(procedure));

				// we want the rowcount of the first result set
				this.rowcount = ((PyObject)this.results.get(0)).__len__();

				// we want the description of the first result set
				this.description = ((PyObject)this.descriptions.get(0));

				// set the current rownumber
				this.rownumber = 0;
			}
		} catch (PyException e) {
			throw e;
		} catch (Exception e) {
			throw zxJDBC.makeException(e);
		}
	}

	/**
	 * Fetch all (remaining) rows of a query result, returning them as a sequence
	 * of sequences (e.g. a list of tuples). Note that the cursor's arraysize attribute
	 * can affect the performance of this operation.
	 *
	 * An Error (or subclass) exception is raised if the previous call to executeXXX()
	 * did not produce any result set or no call was issued yet.
	 *
	 * @return a sequence of sequences from the result set, or an empty sequence when
	 *         no more data is available
	 */
	public PyObject fetchall() {
		return fetchmany(this.rowcount);
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
	 * @return a sequence of sequences from the result set, or an empty sequence when
	 *         no more data is available
	 */
	public PyObject fetchmany(int size) {

		PyObject res = new PyList();

		if ((results == null) || (results.size() == 0)) {
			return res;
		}

		PyObject current = (PyObject)results.get(0);

		if (size <= 0) {
			size = this.rowcount;
		}

		if (this.rownumber < this.rowcount) {
			res = current.__getslice__(Py.newInteger(this.rownumber), Py.newInteger(this.rownumber + size), Py.One);
			this.rownumber += size;
		}

		return res;
	}

	/**
	 * Method scroll
	 *
	 * @param int value
	 * @param String mode 'relative' or 'absolute'
	 *
	 */
	public void scroll(int value, String mode) {

		int pos;

		if ("relative".equals(mode)) {
			pos = this.rownumber + value;
		} else if ("absolute".equals(mode)) {
			pos = value;
		} else {
			throw zxJDBC.makeException(zxJDBC.ProgrammingError, "invalid cursor scroll mode [" + mode + "]");
		}

		if ((pos >= 0) && (pos < this.rowcount)) {
			this.rownumber = pos;
		} else {
			throw zxJDBC.makeException(Py.IndexError, "cursor index [" + pos + "] out of range");
		}
	}

	/**
	 * Move the result pointer to the next set if available.
	 *
	 * @return true if more sets exist, else None
	 */
	public PyObject nextset() {

		PyObject next = Py.None;

		if ((results != null) && (results.size() > 1)) {
			this.results.remove(0);
			this.descriptions.remove(0);

			next = (PyObject)this.results.get(0);
			this.description = (PyObject)this.descriptions.get(0);
			this.rowcount = next.__len__();
			this.rownumber = 0;
		}

		return (next == Py.None) ? Py.None : Py.One;
	}

	/**
	 * Remove the results.
	 */
	public void close() throws SQLException {

		super.close();

		this.rownumber = -1;

		this.results.clear();
	}
}

/**
 * Dynamically construct the results from an execute*().  The static version builds the entire
 * result set immediately upon completion of the query, however in some circumstances, this
 * requires far too many resources to be efficient.  In this version of the fetch the resources
 * remain constant.  The dis-advantage to this approach from an API perspective is its impossible
 * to generate an accurate rowcount since not all the rows have been consumed.
 */
class DynamicFetch extends Fetch {

	/** Field skipCols */
	protected Set skipCols;

	/** Field resultSet */
	protected ResultSet resultSet;

	/**
	 * Construct a dynamic fetch.
	 */
	public DynamicFetch(DataHandler datahandler) {
		super(datahandler);
	}

	/**
	 * Add the result set to the results.  If more than one result
	 * set is attempted to be added, an Error is raised since JDBC
	 * requires that only one ResultSet be iterated for one Statement
	 * at any one time.  Since this is a dynamic iteration, it precludes
	 * the addition of more than one result set.
	 */
	public void add(ResultSet resultSet) {
		add(resultSet, null);
	}

	/**
	 * Add the result set to the results.  If more than one result
	 * set is attempted to be added, an Error is raised since JDBC
	 * requires that only one ResultSet be iterated for one Statement
	 * at any one time.  Since this is a dynamic iteration, it precludes
	 * the addition of more than one result set.
	 */
	public void add(ResultSet resultSet, Set skipCols) {

		if (this.resultSet != null) {
			throw zxJDBC.makeException(zxJDBC.getString("onlyOneResultSet"));
		}

		try {
			if ((resultSet != null) && (resultSet.getMetaData() != null)) {
				if (this.description == Py.None) {
					this.description = this.createDescription(resultSet.getMetaData());
				}

				this.resultSet = resultSet;
				this.skipCols = skipCols;

				// it would be more compliant if we knew the resultSet actually
				// contained some rows, but since we don't make a stab at it so
				// everything else looks better
				this.rowcount = 0;
				this.rownumber = 0;
			}
		} catch (PyException e) {
			throw e;
		} catch (Exception e) {
			throw zxJDBC.makeException(e);
		}
	}

	/**
	 * Method add
	 *
	 * @param CallableStatement callableStatement
	 * @param Procedure procedure
	 * @param PyObject params
	 *
	 */
	public void add(CallableStatement callableStatement, Procedure procedure, PyObject params) {
		throw zxJDBC.makeException(zxJDBC.NotSupportedError, "dynamic cursor does not support callproc(); use static cursors instead");
	}

	/**
	 * Iterate the remaining contents of the ResultSet and return.
	 */
	public PyObject fetchall() {
		return fetch(0, true);
	}

	/**
	 * Iterate up to size rows remaining in the ResultSet and return.
	 */
	public PyObject fetchmany(int size) {
		return fetch(size, false);
	}

	/**
	 * Internal use only.  If <i>all</i> is true, return everything
	 * that's left in the result set, otherwise return up to size.  Fewer
	 * than size may be returned if fewer than size results are left in
	 * the set.
	 */
	private PyObject fetch(int size, boolean all) {

		PyList res = new PyList();

		if (this.resultSet == null) {
			return res;
		}

		try {
			all = (size < 0) ? true : all;

			while (((size-- > 0) || all) && this.resultSet.next()) {
				PyTuple tuple = createResult(this.resultSet, this.skipCols, this.description);

				res.append(tuple);

				this.rowcount++;

				this.rownumber = this.resultSet.getRow();
			}
		} catch (PyException e) {
			throw e;
		} catch (Exception e) {
			throw zxJDBC.makeException(e);
		}

		return res;
	}

	/**
	 * Always returns None.
	 */
	public PyObject nextset() {
		return Py.None;
	}

	/**
	 * Method scroll
	 *
	 * @param int value
	 * @param String mode
	 *
	 */
	public void scroll(int value, String mode) {

		try {
			int type = this.resultSet.getType();

			if ((type != ResultSet.TYPE_FORWARD_ONLY) || (value > 0)) {
				if ("relative".equals(mode)) {
					if (value < 0) {
						value = Math.abs(this.rownumber + value);
					} else if (value > 0) {
						value = this.rownumber + value + 1;
					}
				} else if ("absolute".equals(mode)) {
					if (value < 0) {
						throw zxJDBC.makeException(Py.IndexError, "cursor index [" + value + "] out of range");
					}
				} else {
					throw zxJDBC.makeException(zxJDBC.ProgrammingError, "invalid cursor scroll mode [" + mode + "]");
				}

				if (value == 0) {
					this.resultSet.beforeFirst();
				} else {
					if (!this.resultSet.absolute(value)) {
						throw zxJDBC.makeException(Py.IndexError, "cursor index [" + value + "] out of range");
					}
				}

				// since .rownumber is the *next* row, then the JDBC value suits us fine
				this.rownumber = this.resultSet.getRow();
			} else {
				String msg = "dynamic result set of type [" + type + "] does not support scrolling";

				throw zxJDBC.makeException(zxJDBC.NotSupportedError, msg);
			}
		} catch (SQLException e) {
			throw zxJDBC.makeException(e);
		}
	}

	/**
	 * Close the underlying ResultSet.
	 */
	public void close() throws SQLException {

		super.close();

		if (this.resultSet == null) {
			return;
		}

		this.rownumber = -1;

		try {
			this.resultSet.close();
		} finally {
			this.resultSet = null;
		}
	}
}
