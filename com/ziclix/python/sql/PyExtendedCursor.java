
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
import java.util.*;
import org.python.core.*;

/**
 * A cursor with extensions to the DB API 2.0.
 *
 * @author brian zimmer
 * @author last revised by $Author$
 * @version $Revision$
 */
public class PyExtendedCursor extends PyCursor {

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

	/** Field __members__ */
	protected static PyList __members__;

	/** Field __methods__ */
	protected static PyList __methods__;

	static {
		PyObject[] m = new PyObject[7];

		m[0] = new PyString("tables");
		m[1] = new PyString("columns");
		m[2] = new PyString("primarykeys");
		m[3] = new PyString("foreignkeys");
		m[4] = new PyString("procedures");
		m[5] = new PyString("procedurecolumns");
		m[6] = new PyString("statistics");
		__methods__ = new PyList(m);
		m = new PyObject[0];
		__members__ = new PyList(m);
	}

	/**
	 * Constructor PyExtendedCursor
	 *
	 * @param Connection connection
	 *
	 */
	PyExtendedCursor(Connection connection) {
		super(connection);
	}

	/**
	 * Constructor PyExtendedCursor
	 *
	 * @param Connection connection
	 * @param boolean dynamicFetch
	 *
	 */
	PyExtendedCursor(Connection connection, boolean dynamicFetch) {
		super(connection, dynamicFetch);
	}

	/**
	 * String representation of the object.
	 *
	 * @return a string representation of the object.
	 */
	public String toString() {
		return "<PyExtendedCursor object instance at " + hashCode() + ">";
	}

	/**
	 * Initializes the module.
	 *
	 * @param dict
	 */
	static public void classDictInit(PyObject dict) {

		PyCursor.classDictInit(dict);
		dict.__setitem__("__version__", Py.newString("$Revision$").__getslice__(Py.newInteger(11), Py.newInteger(-2), null));
		dict.__setitem__("tables", new ExtendedCursorFunc("tables", 100, 4, 4, "query for table information"));
		dict.__setitem__("columns", new ExtendedCursorFunc("columns", 101, 4, 4, "query for column information"));
		dict.__setitem__("primarykeys", new ExtendedCursorFunc("primarykeys", 102, 3, 3, "query for primary keys"));
		dict.__setitem__("foreignkeys", new ExtendedCursorFunc("foreignkeys", 103, 6, 6, "query for foreign keys"));
		dict.__setitem__("procedures", new ExtendedCursorFunc("procedures", 104, 3, 3, "query for procedures"));
		dict.__setitem__("procedurecolumns", new ExtendedCursorFunc("procedurecolumns", 105, 4, 4, "query for procedures columns"));
		dict.__setitem__("statistics", new ExtendedCursorFunc("statistics", 106, 5, 5, "description of a table's indices and statistics"));
		dict.__setitem__("gettypeinfo", new ExtendedCursorFunc("gettypeinfo", 107, 0, 1, "query for sql type info"));
		dict.__setitem__("gettabletypeinfo", new ExtendedCursorFunc("gettabletypeinfo", 108, 0, 1, "query for table types"));

		// hide from python
		dict.__setitem__("classDictInit", null);
		dict.__setitem__("toString", null);
	}

	/**
	 * Only table descriptions matching the catalog, schema, table name and type
	 * criteria are returned. They are ordered by TABLE_TYPE, TABLE_SCHEM and
	 * TABLE_NAME.
	 *
	 * @param qualifier
	 * @param owner
	 * @param table
	 * @param type
	 */
	protected void tables(PyObject qualifier, PyObject owner, PyObject table, PyObject type) {

		clear();

		try {
			String q = (qualifier == Py.None) ? null : (String)qualifier.__tojava__(String.class);
			String o = (owner == Py.None) ? null : (String)owner.__tojava__(String.class);
			String t = (table == Py.None) ? null : (String)table.__tojava__(String.class);
			String[] y = null;

			if (type != Py.None) {
				if (isSeq(type)) {
					int len = type.__len__();

					y = new String[len];

					for (int i = 0; i < len; i++) {
						y[i] = (String)type.__getitem__(i).__tojava__(String.class);
					}
				} else {
					y = new String[1];
					y[0] = (String)type.__tojava__(String.class);
				}
			}

			create(this.connection.getMetaData().getTables(q, o, t, y));
		} catch (SQLException e) {
			throw zxJDBC.newError(e);
		}
	}

	/**
	 * Returns the columns for a table.
	 *
	 * @param qualifier
	 * @param owner
	 * @param table
	 * @param column
	 */
	protected void columns(PyObject qualifier, PyObject owner, PyObject table, PyObject column) {

		clear();

		try {
			String q = (qualifier == Py.None) ? null : (String)qualifier.__tojava__(String.class);
			String o = (owner == Py.None) ? null : (String)owner.__tojava__(String.class);
			String t = (table == Py.None) ? null : (String)table.__tojava__(String.class);
			String c = (column == Py.None) ? null : (String)column.__tojava__(String.class);

			create(this.connection.getMetaData().getColumns(q, o, t, c));
		} catch (SQLException e) {
			throw zxJDBC.newError(e);
		}
	}

	/**
	 * Gets a description of the stored procedures available for the qualifier and owner.
	 *
	 * @param qualifier
	 * @param owner
	 * @param procedure
	 */
	protected void procedures(PyObject qualifier, PyObject owner, PyObject procedure) {

		clear();

		try {
			String q = (qualifier == Py.None) ? null : (String)qualifier.__tojava__(String.class);
			String o = (owner == Py.None) ? null : (String)owner.__tojava__(String.class);
			String p = (procedure == Py.None) ? null : (String)procedure.__tojava__(String.class);

			create(this.connection.getMetaData().getProcedures(q, o, p));
		} catch (SQLException e) {
			throw zxJDBC.newError(e);
		}
	}

	/**
	 * Gets the columns for the procedure.
	 *
	 * @param qualifier
	 * @param owner
	 * @param procedure
	 * @param column
	 */
	protected void procedurecolumns(PyObject qualifier, PyObject owner, PyObject procedure, PyObject column) {

		clear();

		try {
			String q = (qualifier == Py.None) ? null : (String)qualifier.__tojava__(String.class);
			String o = (owner == Py.None) ? null : (String)owner.__tojava__(String.class);
			String p = (procedure == Py.None) ? null : (String)procedure.__tojava__(String.class);
			String c = (column == Py.None) ? null : (String)column.__tojava__(String.class);

			create(this.connection.getMetaData().getProcedureColumns(q, o, p, c));
		} catch (SQLException e) {
			throw zxJDBC.newError(e);
		}
	}

	/**
	 * Gets a description of a table's primary key columns. They are ordered by
	 * COLUMN_NAME.
	 *
	 * @param qualifier a schema name
	 * @param owner an owner name
	 * @param table a table name
	 */
	protected void primarykeys(PyObject qualifier, PyObject owner, PyObject table) {

		clear();

		try {
			String q = (qualifier == Py.None) ? null : (String)qualifier.__tojava__(String.class);
			String o = (owner == Py.None) ? null : (String)owner.__tojava__(String.class);
			String t = (table == Py.None) ? null : (String)table.__tojava__(String.class);

			create(this.connection.getMetaData().getPrimaryKeys(q, o, t));
		} catch (SQLException e) {
			throw zxJDBC.newError(e);
		}
	}

	/**
	 * Gets a description of the foreign key columns in the foreign key table
	 * that reference the primary key columns of the primary key table (describe
	 * how one table imports another's key.) This should normally return a single
	 * foreign key/primary key pair (most tables only import a foreign key from a
	 * table once.) They are ordered by FKTABLE_CAT, FKTABLE_SCHEM, FKTABLE_NAME,
	 * and KEY_SEQ.
	 *
	 * @param primaryQualifier
	 * @param primaryOwner
	 * @param primaryTable
	 * @param foreignQualifier
	 * @param foreignOwner
	 * @param foreignTable
	 */
	protected void foreignkeys(PyObject primaryQualifier, PyObject primaryOwner, PyObject primaryTable, PyObject foreignQualifier, PyObject foreignOwner, PyObject foreignTable) {

		clear();

		try {
			String pq = (primaryQualifier == Py.None) ? null : (String)primaryQualifier.__tojava__(String.class);
			String po = (primaryOwner == Py.None) ? null : (String)primaryOwner.__tojava__(String.class);
			String pt = (primaryTable == Py.None) ? null : (String)primaryTable.__tojava__(String.class);
			String fq = (foreignQualifier == Py.None) ? null : (String)foreignQualifier.__tojava__(String.class);
			String fo = (foreignOwner == Py.None) ? null : (String)foreignOwner.__tojava__(String.class);
			String ft = (foreignTable == Py.None) ? null : (String)foreignTable.__tojava__(String.class);

			create(this.connection.getMetaData().getCrossReference(pq, po, pt, fq, fo, ft));
		} catch (SQLException e) {
			throw zxJDBC.newError(e);
		}
	}

	/**
	 * Gets a description of a table's indices and statistics. They are ordered by
	 * NON_UNIQUE, TYPE, INDEX_NAME, and ORDINAL_POSITION.
	 *
	 * @param qualifier
	 * @param owner
	 * @param table
	 * @param unique
	 * @param accuracy
	 */
	protected void statistics(PyObject qualifier, PyObject owner, PyObject table, PyObject unique, PyObject accuracy) {

		clear();

		try {
			String q = (qualifier == Py.None) ? null : (String)qualifier.__tojava__(String.class);
			String o = (owner == Py.None) ? null : (String)owner.__tojava__(String.class);
			String t = (table == Py.None) ? null : (String)table.__tojava__(String.class);
			boolean u = (unique == Py.None) ? false : ((Boolean)unique.__tojava__(Boolean.class)).booleanValue();
			boolean a = (accuracy == Py.None) ? false : ((Boolean)accuracy.__tojava__(Boolean.class)).booleanValue();
			Set skipCols = new HashSet();

			skipCols.add(new Integer(12));
			create(this.connection.getMetaData().getIndexInfo(q, o, t, u, a), skipCols);
		} catch (SQLException e) {
			throw zxJDBC.newError(e);
		}
	}

	/**
	 * Gets a description of the type information for a given type.
	 *
	 * @param type data type for which to provide information
	 */
	protected void typeinfo(PyObject type) {

		clear();

		try {
			Set skipCols = new HashSet();

			skipCols.add(new Integer(16));
			skipCols.add(new Integer(17));
			create(this.connection.getMetaData().getTypeInfo(), skipCols);

			// if the type is non-null, then trim the result list down to that type only
			// if(type != null &&!Py.None.equals(type)) {
			// for(int i = 0; i < ((PyObject) this.results.get(0)).__len__(); i++) {
			// PyObject row = ((PyObject) this.results.get(0)).__getitem__(new PyInteger(i));
			// PyObject sqlType = row.__getitem__(new PyInteger(1));
			// if(type.equals(sqlType)) {
			// this.results.remove(0);
			// this.results.add(0, new PyList(new PyObject[] {
			// row
			// }));
			// }
			// }
			// }
		} catch (SQLException e) {
			throw zxJDBC.newError(e);
		}
	}

	/**
	 * Gets a description of possible table types.
	 */
	protected void tabletypeinfo() {

		clear();

		try {
			create(this.connection.getMetaData().getTableTypes());
		} catch (SQLException e) {
			throw zxJDBC.newError(e);
		}
	}
}

/**
 * Class ExtendedCursorFunc
 *
 * @author
 * @date $today.date$
 * @author last modified by $Author$
 * @date last modified on $Date$
 * @version $Revision$
 * @copyright 2001 brian zimmer
 */
class ExtendedCursorFunc extends PyBuiltinFunctionSet {

	/**
	 * Constructor ExtendedCursorFunc
	 *
	 * @param String name
	 * @param int index
	 * @param int argcount
	 * @param String doc
	 *
	 */
	ExtendedCursorFunc(String name, int index, int argcount, String doc) {
		super(name, index, argcount, argcount, true, doc);
	}

	/**
	 * Constructor ExtendedCursorFunc
	 *
	 * @param String name
	 * @param int index
	 * @param int minargs
	 * @param int maxargs
	 * @param String doc
	 *
	 */
	ExtendedCursorFunc(String name, int index, int minargs, int maxargs, String doc) {
		super(name, index, minargs, maxargs, true, doc);
	}

	/**
	 * Method __call__
	 *
	 * @return PyObject
	 *
	 */
	public PyObject __call__() {

		PyExtendedCursor cursor = (PyExtendedCursor)__self__;

		switch (index) {

			case 107 :
				cursor.typeinfo(Py.None);

				return Py.None;

			case 108 :
				cursor.tabletypeinfo();

				return Py.None;

			default :
				throw argCountError(0);
		}
	}

	/**
	 * Method __call__
	 *
	 * @param PyObject arga
	 *
	 * @return PyObject
	 *
	 */
	public PyObject __call__(PyObject arga) {

		PyExtendedCursor cursor = (PyExtendedCursor)__self__;

		switch (index) {

			case 107 :
				cursor.typeinfo(arga);

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
	 * @param PyObject argc
	 *
	 * @return PyObject
	 *
	 */
	public PyObject __call__(PyObject arga, PyObject argb, PyObject argc) {

		PyExtendedCursor cursor = (PyExtendedCursor)__self__;

		switch (index) {

			case 102 :
				cursor.primarykeys(arga, argb, argc);

				return Py.None;

			case 104 :
				cursor.procedures(arga, argb, argc);

				return Py.None;

			default :
				throw argCountError(3);
		}
	}

	/**
	 * Method fancyCall
	 *
	 * @param PyObject[] args
	 *
	 * @return PyObject
	 *
	 */
	public PyObject fancyCall(PyObject[] args) {

		PyExtendedCursor cursor = (PyExtendedCursor)__self__;

		switch (index) {

			case 103 :
				cursor.foreignkeys(args[0], args[1], args[2], args[3], args[4], args[5]);

				return Py.None;

			case 106 :
				cursor.statistics(args[0], args[1], args[2], args[3], args[4]);

				return Py.None;

			default :
				throw argCountError(args.length);
		}
	}

	/**
	 * Method __call__
	 *
	 * @param PyObject arg1
	 * @param PyObject arg2
	 * @param PyObject arg3
	 * @param PyObject arg4
	 *
	 * @return PyObject
	 *
	 */
	public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3, PyObject arg4) {

		PyExtendedCursor cursor = (PyExtendedCursor)__self__;

		switch (index) {

			case 100 :
				cursor.tables(arg1, arg2, arg3, arg4);

				return Py.None;

			case 101 :
				cursor.columns(arg1, arg2, arg3, arg4);

				return Py.None;

			case 105 :
				cursor.procedurecolumns(arg1, arg2, arg3, arg4);

				return Py.None;

			default :
				throw argCountError(4);
		}
	}
}
