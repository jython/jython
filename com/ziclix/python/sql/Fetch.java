
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

	/** Field rowcount */
	protected int rowcount;

	/** Field cursor */
	protected PyCursor cursor;

	/** Field description */
	protected PyObject description;

	/** True if a CallableStatement was added, false otherwise. */
	protected boolean callable;

	/** Field callableResults */
	protected PyObject callableResults;

	/**
	 * Constructor Fetch
	 *
	 * @param PyCursor cursor
	 *
	 */
	public Fetch(PyCursor cursor) {

		this.cursor = cursor;
		this.description = Py.None;
		this.rowcount = -1;
		this.callable = false;
		this.callableResults = Py.None;
	}

	/**
	 * Method newDynamicFetch
	 *
	 * @param PyCursor cursor
	 *
	 * @return Fetch
	 *
	 */
	static Fetch newDynamicFetch(PyCursor cursor) {
		return new DynamicFetch(cursor);
	}

	/**
	 * Method newStaticFetch
	 *
	 * @param PyCursor cursor
	 *
	 * @return Fetch
	 *
	 */
	static Fetch newStaticFetch(PyCursor cursor) {
		return new StaticFetch(cursor);
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
	 * @param Set skipCols
	 *
	 */
	abstract public void add(ResultSet resultSet, Set skipCols);

	/**
	 * Method add
	 *
	 * @param CallableStatement callableStatement
	 * @param Procedure procedure
	 *
	 */
	public void add(CallableStatement callableStatement, Procedure procedure) {

		// set this regardless of whether the statement has results
		this.callable = true;

		try {
			createDescription(procedure);

			if (description.__len__() == 0) {
				return;
			}

			PyObject[] row = new PyObject[description.__len__()];

			for (int i = 0, j = 0, len = procedure.columns.__len__(); i < len; i++) {
				PyObject column = procedure.columns.__getitem__(i);
				int colType = column.__getitem__(Procedure.COLUMN_TYPE).__int__().getValue();
				int dataType = column.__getitem__(Procedure.DATA_TYPE).__int__().getValue();

				switch (colType) {

					case DatabaseMetaData.procedureColumnOut :
					case DatabaseMetaData.procedureColumnInOut :
					case DatabaseMetaData.procedureColumnReturn :
						row[j++] = cursor.getDataHandler().getPyObject(callableStatement, i + 1, dataType);
						break;
				}
			}

			this.callableResults = new PyList();

			((PyList)this.callableResults).append(new PyTuple(row));

			this.rowcount = this.callableResults.__len__();
		} catch (PyException e) {
			throw e;
		} catch (Exception e) {
			throw zxJDBC.newError(e);
		}

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

		PyObject sequence = fetchmany(1);

		if (sequence != Py.None) {
			sequence = sequence.__getitem__(0);
		}

		return sequence;
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
	public final PyObject fetchall() {

		if (callable) {
			PyObject tmp = this.callableResults;

			this.callableResults = Py.None;

			return tmp;
		} else {
			return doFetchall();
		}
	}

	/**
	 * Method doFetchall
	 *
	 * @return PyObject
	 *
	 */
	abstract protected PyObject doFetchall();

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
	public final PyObject fetchmany(int size) {

		if (callable) {
			PyObject tmp = this.callableResults;

			this.callableResults = Py.None;

			return tmp;
		} else {
			return doFetchmany(size);
		}
	}

	/**
	 * Method doFetchmany
	 *
	 * @param int size
	 *
	 * @return PyObject
	 *
	 */
	abstract protected PyObject doFetchmany(int size);

	/**
	 * Move the result pointer to the next set if available.
	 *
	 * @return true if more sets exist, else None
	 */
	public final PyObject nextset() {

		// no support for result sets within callable statements
		return callable ? Py.None : doNextset();
	}

	/**
	 * Method doNextset
	 *
	 * @return PyObject
	 *
	 */
	abstract protected PyObject doNextset();

	/**
	 * Cleanup any resources.
	 */
	public final void close() throws SQLException {

		doClose();

		this.callableResults = Py.None;
	}

	/**
	 * Method doClose
	 *
	 * @throws SQLException
	 *
	 */
	abstract public void doClose() throws SQLException;

	/**
	 * Builds a tuple containing the meta-information about each column.
	 *
	 * (name, type_code, display_size, internal_size, precision, scale, null_ok)
	 *
	 * precision and scale are only available for numeric types
	 */
	protected void createDescription(ResultSetMetaData meta) throws SQLException {

		this.description = new PyList();

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

			((PyList)this.description).append(new PyTuple(a));
		}
	}

	/**
	 * Builds a tuple containing the meta-information about each column.
	 *
	 * (name, type_code, display_size, internal_size, precision, scale, null_ok)
	 *
	 * precision and scale are only available for numeric types
	 */
	protected void createDescription(Procedure procedure) throws SQLException {

		this.description = new PyList();

		for (int i = 0, len = procedure.columns.__len__(); i < len; i++) {
			PyObject column = procedure.columns.__getitem__(i);
			int colType = column.__getitem__(Procedure.COLUMN_TYPE).__int__().getValue();

			switch (colType) {

				case DatabaseMetaData.procedureColumnOut :
				case DatabaseMetaData.procedureColumnInOut :
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

					((PyList)this.description).append(new PyTuple(a));
					break;
			}
		}
	}

	/**
	 * Creates the results of a query.  Iterates through the list and builds the tuple.
	 *
	 * @param set result set
	 * @param skipCols set of JDBC-indexed columns to automatically set to None
	 * @return a list of tuples of the results
	 * @throws SQLException
	 */
	protected PyList createResults(ResultSet set, Set skipCols) throws SQLException {

		PyObject tuple = Py.None;
		PyList res = new PyList();

		while (set.next()) {
			tuple = createResult(set, skipCols);

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
	protected PyTuple createResult(ResultSet set, Set skipCols) throws SQLException {

		int descriptionLength = description.__len__();
		PyObject[] row = new PyObject[descriptionLength];

		for (int i = 0; i < descriptionLength; i++) {
			if ((skipCols != null) && skipCols.contains(new Integer(i + 1))) {
				row[i] = Py.None;
			} else {
				int type = ((PyInteger)description.__getitem__(i).__getitem__(1)).getValue();

				row[i] = this.cursor.getDataHandler().getPyObject(set, i + 1, type);
			}
		}

		this.cursor.addWarning(set.getWarnings());

		return new PyTuple(row);
	}

	/**
	 * Return the total row count.  Note: since JDBC provides no means to get this information
	 * without iterating the entire result set, only those fetches which build the result
	 * statically will have an accurate row count.
	 */
	public int getRowCount() {
		return this.rowcount;
	}

	/**
	 * Return the description of the result.
	 */
	public PyObject getDescription() {
		return this.description;
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

	/** Field counter */
	protected int counter;

	/** Field results */
	protected List results;

	/**
	 * Construct a static fetch.  The entire result set is iterated as it
	 * is added and the result set is immediately closed.
	 */
	public StaticFetch(PyCursor cursor) {

		super(cursor);

		this.results = new LinkedList();
		this.counter = -1;
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

		PyObject result = Py.None;

		try {
			if ((resultSet != null) && (resultSet.getMetaData() != null)) {
				if (this.description == Py.None) {
					this.createDescription(resultSet.getMetaData());
				}

				result = this.createResults(resultSet, skipCols);

				this.results.add(result);

				this.rowcount = ((PyObject)this.results.get(0)).__len__();
			}
		} catch (PyException e) {
			throw e;
		} catch (Exception e) {
			throw zxJDBC.newError(e);
		} finally {
			try {
				resultSet.close();
			} catch (Exception e) {}
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
	public PyObject doFetchall() {
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
	 * @return a sequence of sequences from the result set, or None when no more data is available
	 */
	public PyObject doFetchmany(int size) {

		PyObject res = Py.None, current = Py.None;

		if ((results != null) && (results.size() > 0)) {
			current = (PyObject)results.get(0);
		} else {
			return current;
		}

		if (size <= 0) {
			size = this.rowcount;
		}

		if ((counter + 1) < this.rowcount) {
			int start = counter + 1;

			counter += size;
			res = current.__getslice__(Py.newInteger(start), Py.newInteger(counter + 1), Py.newInteger(1));
		}

		return res;
	}

	/**
	 * Move the result pointer to the next set if available.
	 *
	 * @return true if more sets exist, else None
	 */
	public PyObject doNextset() {

		PyObject next = Py.None;

		if ((results != null) && (results.size() > 1)) {
			this.results.remove(0);

			next = (PyObject)this.results.get(0);
			this.rowcount = next.__len__();
			this.counter = -1;
		}

		return (next == Py.None) ? Py.None : Py.One;
	}

	/**
	 * Remove the results.
	 */
	public void doClose() throws SQLException {

		this.counter = -1;

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
	public DynamicFetch(PyCursor cursor) {
		super(cursor);
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
			throw zxJDBC.newError(zxJDBC.getString("onlyOneResultSet"));
		}

		try {
			if ((resultSet != null) && (resultSet.getMetaData() != null)) {
				if (this.description == Py.None) {
					this.createDescription(resultSet.getMetaData());
				}

				this.resultSet = resultSet;
				this.skipCols = skipCols;
			}
		} catch (PyException e) {
			throw e;
		} catch (Exception e) {
			throw zxJDBC.newError(e);
		}
	}

	/**
	 * Iterate the remaining contents of the ResultSet and return.
	 */
	public PyObject doFetchall() {
		return fetch(0, true);
	}

	/**
	 * Iterate up to size rows remaining in the ResultSet and return.
	 */
	public PyObject doFetchmany(int size) {
		return fetch(size, false);
	}

	/**
	 * Internal use only.  If <i>all</i> is true, return everything
	 * that's left in the result set, otherwise return up to size.  Fewer
	 * than size may be returned if fewer than size results are left in
	 * the set.
	 */
	private PyObject fetch(int size, boolean all) {

		if (this.resultSet == null) {
			return Py.None;
		}

		PyList res = new PyList();

		try {
			all = (size < 0) ? true : all;

			while (((size-- > 0) || all) && this.resultSet.next()) {
				PyTuple tuple = createResult(this.resultSet, this.skipCols);

				res.append(tuple);

				// since the rowcount == -1 initially, bump it to one the first time through
				this.rowcount = (this.rowcount == -1) ? 1 : this.rowcount + 1;
			}
		} catch (PyException e) {
			throw e;
		} catch (Exception e) {
			throw zxJDBC.newError(e);
		}

		return (res.__len__() == 0) ? Py.None : res;
	}

	/**
	 * Always returns None.
	 */
	public PyObject doNextset() {
		return Py.None;
	}

	/**
	 * Close the underlying ResultSet.
	 */
	public void doClose() throws SQLException {

		if (this.resultSet == null) {
			return;
		}

		this.resultSet.close();

		this.resultSet = null;
	}
}
