
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
import java.text.*;
import java.lang.reflect.*;
import org.python.core.*;

/**
 * Creates database connections.
 *
 * <pre>
 * from com.ziclix.python.sql import zxJDBC
 * db = zxJDBC.connect("jdbc:mysql://localhost:3306/MySql", None, None, "org.gjt.mm.mysql.Driver")
 * </pre>
 *
 * @author brian zimmer
 * @author last revised by $Author$
 * @version $Revision$
 */
public class zxJDBC extends PyObject implements ClassDictInit {

	/** Field Error */
	public static PyObject Error = Py.None;

	/** Field Warning */
	public static PyObject Warning = Py.None;

	/** Field InterfaceError */
	public static PyObject InterfaceError = Py.None;

	/** Field DatabaseError */
	public static PyObject DatabaseError = Py.None;

	/** Field InternalError */
	public static PyObject InternalError = Py.None;

	/** Field OperationalError */
	public static PyObject OperationalError = Py.None;

	/** Field ProgrammingError */
	public static PyObject ProgrammingError = Py.None;

	/** Field IntegrityError */
	public static PyObject IntegrityError = Py.None;

	/** Field DataError */
	public static PyObject DataError = Py.None;

	/** Field NotSupportedError */
	public static PyObject NotSupportedError = Py.None;

	/** The ResourceBundle with error messages and doc strings */
	private static ResourceBundle resourceBundle = null;

	static {
		try {
			resourceBundle = ResourceBundle.getBundle("com.ziclix.python.sql.resource.zxJDBCMessages");
		} catch (MissingResourceException e) {
			throw new RuntimeException("missing zxjdbc resource bundle");
		}
	}

	/**
	 * Initializes the module.
	 *
	 * @param dict
	 */
	public static void classDictInit(PyObject dict) {

		dict.__setitem__("apilevel", new PyString("2.0"));
		dict.__setitem__("threadsafety", new PyInteger(1));
		dict.__setitem__("paramstyle", new PyString("qmark"));
		dict.__setitem__("__version__", Py.newString("$Revision$").__getslice__(Py.newInteger(11), Py.newInteger(-2), null));
		dict.__setitem__("Date", new zxJDBCFunc("Date", 1, 3, 3, false, "construct a Date from year, month, day"));
		dict.__setitem__("Time", new zxJDBCFunc("Time", 2, 3, 3, false, "construct a Date from hour, minute, second"));
		dict.__setitem__("Timestamp", new zxJDBCFunc("Timestamp", 3, 6, 6, false, "construct a Timestamp from year, month, day, hour, minute, second"));
		dict.__setitem__("DateFromTicks", new zxJDBCFunc("DateFromTicks", 4, 1, 1, false, "construct a Date from seconds since the epoch"));
		dict.__setitem__("TimeFromTicks", new zxJDBCFunc("TimeFromTicks", 5, 1, 1, false, "construct a Time from seconds since the epoch"));
		dict.__setitem__("TimestampFromTicks", new zxJDBCFunc("TimestampFromTicks", 6, 1, 1, false, "construct a Timestamp from seconds since the epoch"));
		zxJDBC._addSqlTypes(dict);
		zxJDBC._addConnectors(dict);
		zxJDBC._buildExceptions(dict);

		// hide from python
		dict.__setitem__("initModule", null);
		dict.__setitem__("toString", null);
		dict.__setitem__("getPyClass", null);
		dict.__setitem__("classDictInit", null);
		dict.__setitem__("_addSqlTypes", null);
		dict.__setitem__("_addConnectors", null);
		dict.__setitem__("_buildExceptions", null);
		dict.__setitem__("_empty__init__", null);
		dict.__setitem__("buildClass", null);
		dict.__setitem__("createExceptionMessage", null);
		dict.__setitem__("newError", null);
		dict.__setitem__("resourceBundle", null);
		dict.__setitem__("getString", null);
		dict.__setitem__("makeException", null);
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

	/**
	 * Add the types from java.sql.Types
	 *
	 * @param PyObject dict
	 *
	 * @throws PyException
	 *
	 */
	protected static void _addSqlTypes(PyObject dict) throws PyException {

		PyDictionary sqltype = new PyDictionary();

		dict.__setitem__("sqltype", sqltype);

		try {
			Class c = Class.forName("java.sql.Types");
			Field[] fields = c.getFields();

			for (int i = 0; i < fields.length; i++) {
				Field f = fields[i];
				PyString name = Py.newString(f.getName());
				PyObject value = new DBApiType(f.getInt(c));

				dict.__setitem__(name, value);
				sqltype.__setitem__(value, name);
			}
		} catch (Throwable t) {
			throw newError(t);
		}

		return;
	}

	/**
	 * Add all the possible connectors
	 *
	 * @param PyObject dict
	 *
	 * @throws PyException
	 *
	 */
	protected static void _addConnectors(PyObject dict) throws PyException {

		PyObject connector = Py.None;
		Properties props = new Properties();

		props.put("connect", "com.ziclix.python.sql.connect.Connect");
		props.put("lookup", "com.ziclix.python.sql.connect.Lookup");
		props.put("connectx", "com.ziclix.python.sql.connect.Connectx");

		java.util.Enumeration names = props.propertyNames();

		while (names.hasMoreElements()) {
			String name = ((String)names.nextElement()).trim();
			String className = ((String)props.getProperty(name)).trim();

			try {
				connector = (PyObject)Class.forName(className).newInstance();

				dict.__setitem__(name, connector);
				Py.writeComment("zxJDBC", "loaded connector [" + className + "] as [" + name + "]");
			} catch (Throwable t) {
				Py.writeComment("zxJDBC", "failed to load connector [" + name + "] using class [" + className + "]");
			}
		}

		return;
	}

	/**
	 * Create the exception classes and get their descriptions from the resource bundle.
	 */
	protected static void _buildExceptions(PyObject dict) {

		Error = buildClass("Error", Py.StandardError, "_empty__init__");
		Warning = buildClass("Warning", Py.StandardError, "_empty__init__");
		InterfaceError = buildClass("InterfaceError", Error, "_empty__init__");
		DatabaseError = buildClass("DatabaseError", Error, "_empty__init__");
		InternalError = buildClass("InternalError", DatabaseError, "_empty__init__");
		OperationalError = buildClass("OperationalError", DatabaseError, "_empty__init__");
		ProgrammingError = buildClass("ProgrammingError", DatabaseError, "_empty__init__");
		IntegrityError = buildClass("IntegrityError", DatabaseError, "_empty__init__");
		DataError = buildClass("DataError", DatabaseError, "_empty__init__");
		NotSupportedError = buildClass("NotSupportedError", DatabaseError, "_empty__init__");
	}

	/**
	 * Method _empty__init__
	 *
	 * @param PyObject[] arg
	 * @param String[] kws
	 *
	 * @return PyObject
	 *
	 */
	public static PyObject _empty__init__(PyObject[] arg, String[] kws) {

		PyObject dict = new PyStringMap();

		dict.__setitem__("__module__", new PyString("zxJDBC"));

		return dict;
	}

	/**
	 * Return the string associated with the key for the default resource bundle.  It
	 * first checks for 'key.N' where N starts at 0 and increments by one.  If any indexed
	 * key is found, the results of all the indexed values are concatenated with the line
	 * separator.  If no indexed key is found, it defaults to checking the bundle by the
	 * key value alone.
	 */
	public static String getString(String key) {

		int i = 0;
		List lines = null;
		String resource = null;

		while (true) {
			try {
				resource = resourceBundle.getString(key + "." + (i++));

				if (lines == null) {
					lines = new ArrayList();
				}

				lines.add(resource);
			} catch (MissingResourceException e) {
				break;
			}
		}

		if ((lines == null) || (lines.size() == 0)) {
			resource = resourceBundle.getString(key);
		} else {
			String sep = System.getProperty("line.separator");
			StringBuffer sb = new StringBuffer();

			for (i = 0; i < lines.size() - 1; i++) {
				sb.append(lines.get(i)).append(sep);
			}

			sb.append(lines.get(lines.size() - 1));

			resource = sb.toString();
		}

		return resource;
	}

	/**
	 * Return a formatted string.  The key is used to get the format and the values
	 * are passed, along with the format, to a MessageFormat who formats it appropriately.
	 */
	public static String getString(String key, Object[] values) {

		String format = getString(key);

		return MessageFormat.format(format, values);
	}

	/**
	 * Return a newly instantiated PyException.
	 */
	public static PyException makeException(PyObject type, String msg) {
		return Py.makeException(type, Py.newString((msg == null) ? "" : msg));
	}

	/**
	 * Return a newly instantiated Error.
	 */
	public static PyException newError(String msg) {
		return zxJDBC.makeException(Error, msg);
	}

	/**
	 * Return a newly instantiated Error.
	 */
	public static PyException newError(Throwable e) {

		if (e instanceof PyException) {
			throw (PyException)e;
		}

		try {
			StringBuffer buffer = new StringBuffer();

			createExceptionMessage((SQLException)e, buffer, 0);

			return newError(buffer.toString());
		} catch (ClassCastException ex) {}

		return newError(e.getMessage());
	}

	/**
	 * Create SQL exception messages
	 *
	 * @param SQLException exception
	 * @param StringBuffer buffer
	 * @param int level
	 *
	 */
	protected static void createExceptionMessage(SQLException exception, StringBuffer buffer, int level) {

		buffer.append(exception.getMessage());
		buffer.append(" [SQLCode: " + exception.getErrorCode() + "]");

		if (exception.getSQLState() != null) {
			buffer.append(", [SQLState: " + exception.getSQLState() + "]");
		}

		if (exception.getNextException() != null) {
			buffer.append(System.getProperty("line.separator"));
			createExceptionMessage(exception.getNextException(), buffer, level + 1);
		}
	}

	/**
	 * This function constructs an object holding a date value.
	 */
	protected static PyObject Date(int year, int month, int day) {

		Calendar c = Calendar.getInstance();

		c.set(Calendar.YEAR, year);
		c.set(Calendar.MONTH, month - 1);
		c.set(Calendar.DATE, day);

		return DateFromTicks(c.getTime().getTime() / 1000);
	}

	/**
	 * This function constructs an object holding a time value.
	 */
	protected static PyObject Time(int hour, int minute, int second) {

		Calendar c = Calendar.getInstance();

		c.set(Calendar.HOUR, hour);
		c.set(Calendar.MINUTE, minute);
		c.set(Calendar.SECOND, second);

		return TimeFromTicks(c.getTime().getTime() / 1000);
	}

	/**
	 * This function constructs an object holding a time stamp value.
	 */
	protected static PyObject Timestamp(int year, int month, int day, int hour, int minute, int second) {

		Calendar c = Calendar.getInstance();

		c.set(Calendar.YEAR, year);
		c.set(Calendar.MONTH, month - 1);
		c.set(Calendar.DATE, day);
		c.set(Calendar.HOUR, hour);
		c.set(Calendar.MINUTE, minute);
		c.set(Calendar.SECOND, second);
		c.set(Calendar.MILLISECOND, 0);

		return TimestampFromTicks(c.getTime().getTime() / 1000);
	}

	/**
	 * This function constructs an object holding a date value from the
	 * given ticks value (number of seconds since the epoch; see the
	 * documentation of the standard Python <i>time</i> module for details).
	 *
	 * <i>Note:</i> The DB API 2.0 spec calls for time in seconds since the epoch
	 * while the Java Date object returns time in milliseconds since the epoch.
	 * This module adheres to the python API and will therefore use time in
	 * seconds rather than milliseconds, so adjust any Java code accordingly.
	 *
	 * @param ticks number of seconds since the epoch
	 */
	protected static PyObject DateFromTicks(long ticks) {

		Calendar c = Calendar.getInstance();

		c.setTime(new java.util.Date(ticks * 1000));
		c.set(Calendar.HOUR, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);

		return Py.java2py(new java.sql.Date(c.getTime().getTime()));
	}

	/**
	 * This function constructs an object holding a time value from the
	 * given ticks value (number of seconds since the epoch; see the
	 * documentation of the standard Python <i>time</i> module for details).
	 *
	 * <i>Note:</i> The DB API 2.0 spec calls for time in seconds since the epoch
	 * while the Java Date object returns time in milliseconds since the epoch.
	 * This module adheres to the python API and will therefore use time in
	 * seconds rather than milliseconds, so adjust any Java code accordingly.
	 *
	 * @param ticks number of seconds since the epoch
	 */
	protected static PyObject TimeFromTicks(long ticks) {
		return Py.java2py(new Time(ticks * 1000));
	}

	/**
	 * This function constructs an object holding a time stamp value from
	 * the given ticks value (number of seconds since the epoch; see the
	 * documentation of the standard Python <i>time</i> module for details).
	 *
	 * <i>Note:</i> The DB API 2.0 spec calls for time in seconds since the epoch
	 * while the Java Date object returns time in milliseconds since the epoch.
	 * This module adheres to the python API and will therefore use time in
	 * seconds rather than milliseconds, so adjust any Java code accordingly.
	 *
	 * @param ticks number of seconds since the epoch
	 */
	protected static PyObject TimestampFromTicks(long ticks) {
		return Py.java2py(new Timestamp(ticks * 1000));
	}

	/**
	 * Method buildClass
	 *
	 * @param String classname
	 * @param PyObject superclass
	 * @param String classCodeName
	 *
	 * @return PyObject
	 *
	 */
	protected static PyObject buildClass(String classname, PyObject superclass, String classCodeName) {

		PyObject[] parents = (superclass == null) ? Py.EmptyObjects : new PyObject[]{ superclass };
		PyString doc = Py.newString(getString(classname));
		PyObject cls = Py.makeClass(classname, parents, Py.newJavaCode(zxJDBC.class, classCodeName), doc);

		return cls;
	}
}

/**
 * Class zxJDBCFunc
 *
 * @date $today.date$
 * @author last modified by $Author$
 * @date last modified on $Date$
 * @version $Revision$
 * @copyright 2001 brian zimmer
 */
class zxJDBCFunc extends PyBuiltinFunctionSet {

	/**
	 * Constructor zxJDBCFunc
	 *
	 * @param String name
	 * @param int index
	 * @param int minargs
	 * @param int maxargs
	 * @param boolean func
	 * @param String doc
	 *
	 */
	zxJDBCFunc(String name, int index, int minargs, int maxargs, boolean func, String doc) {
		super(name, index, minargs, maxargs, func, doc);
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

		long ticks = ((Number)arg.__tojava__(Number.class)).longValue();

		switch (index) {

			case 4 :
				return zxJDBC.DateFromTicks(ticks);

			case 5 :
				return zxJDBC.TimeFromTicks(ticks);

			case 6 :
				return zxJDBC.TimestampFromTicks(ticks);

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

		switch (index) {

			case 1 :
				int year = ((Number)arga.__tojava__(Number.class)).intValue();
				int month = ((Number)argb.__tojava__(Number.class)).intValue();
				int day = ((Number)argc.__tojava__(Number.class)).intValue();

				return zxJDBC.Date(year, month, day);

			case 2 :
				int hour = ((Number)arga.__tojava__(Number.class)).intValue();
				int minute = ((Number)argb.__tojava__(Number.class)).intValue();
				int second = ((Number)argc.__tojava__(Number.class)).intValue();

				return zxJDBC.Time(hour, minute, second);

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

		switch (index) {

			case 3 :
				int year = ((Number)args[0].__tojava__(Number.class)).intValue();
				int month = ((Number)args[1].__tojava__(Number.class)).intValue();
				int day = ((Number)args[2].__tojava__(Number.class)).intValue();
				int hour = ((Number)args[3].__tojava__(Number.class)).intValue();
				int minute = ((Number)args[4].__tojava__(Number.class)).intValue();
				int second = ((Number)args[5].__tojava__(Number.class)).intValue();

				return zxJDBC.Timestamp(year, month, day, hour, minute, second);

			default :
				throw argCountError(args.length);
		}
	}
}
