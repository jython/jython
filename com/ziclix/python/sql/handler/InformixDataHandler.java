
/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql.handler;

import java.io.*;
import java.sql.*;
import java.math.*;
import org.python.core.*;
import com.ziclix.python.sql.*;

/**
 * Informix specific data handling.
 *
 * @author brian zimmer
 * @author last revised by $Author$
 * @version $Revision$
 */
public class InformixDataHandler extends FilterDataHandler {

	/**
	 * Decorator for handling Informix specific issues.
	 *
	 * @param datahandler the delegate DataHandler
	 */
	public InformixDataHandler(DataHandler datahandler) {
		super(datahandler);
	}

	/**
	 * Returns the serial for the statement.
	 *
	 * @param Statement stmt
	 *
	 * @return PyObject
	 *
	 * @throws SQLException
	 *
	 */
	public PyObject getRowId(Statement stmt) throws SQLException {

		if (stmt instanceof com.informix.jdbc.IfmxStatement) {
			return Py.newInteger(((com.informix.jdbc.IfmxStatement)stmt).getSerial());
		}

		return super.getRowId(stmt);
	}

	/**
	 * Provide fixes for Ifx driver.
	 *
	 * @param stmt
	 * @param index
	 * @param object
	 * @param type
	 * @throws SQLException
	 */
	public void setJDBCObject(PreparedStatement stmt, int index, PyObject object, int type) throws SQLException {

		if (DataHandler.checkNull(stmt, index, object, type)) {
			return;
		}

		switch (type) {

			case Types.LONGVARCHAR :

				// Ifx driver can't handle the setCharacterStream() method so use setObject() instead
				if (object instanceof PyFile) {
					object = ((PyFile)object).read();
				}

				String varchar = (String)object.__tojava__(String.class);

				stmt.setObject(index, varchar, type);
				break;

			case Types.OTHER :

				// this is most likely an Informix boolean
				stmt.setBoolean(index, object.__nonzero__());
				break;

			default :
				super.setJDBCObject(stmt, index, object, type);
		}
	}

	/**
	 * Provide fixes for Ifx driver.
	 *
	 * @param stmt
	 * @param index
	 * @param object
	 * @throws SQLException
	 */
	public void setJDBCObject(PreparedStatement stmt, int index, PyObject object) throws SQLException {

		// there is a bug in the Ifx driver when using setObject() with a String for a prepared statement
		if (object instanceof PyString) {
			super.setJDBCObject(stmt, index, object, Types.VARCHAR);
		} else {
			super.setJDBCObject(stmt, index, object);
		}
	}

	/**
	 * Override to handle Informix related issues.
	 *
	 * @param set the result set
	 * @param col the column number
	 * @param type the SQL type
	 * @return the mapped Python object
	 * @throws SQLException thrown for a sql exception
	 */
	public PyObject getPyObject(ResultSet set, int col, int type) throws SQLException {

		PyObject obj = Py.None;

		switch (type) {

			case Types.OTHER :
				try {

					// informix returns boolean as OTHERs, so let's give that a try
					obj = set.getBoolean(col) ? Py.One : Py.Zero;
				} catch (SQLException e) {
					obj = super.getPyObject(set, col, type);
				}
				break;

			case Types.BLOB :
				int major = set.getStatement().getConnection().getMetaData().getDriverMajorVersion();
				int minor = set.getStatement().getConnection().getMetaData().getDriverMinorVersion();

				if ((major <= 2) && (minor <= 11)) {
					Blob blob = set.getBlob(col);

					if (blob == null) {
						obj = Py.None;
					} else {
						InputStream is = null;

						try {

							// the available() bug means we CANNOT buffer this stream
							is = blob.getBinaryStream();
							obj = Py.java2py(DataHandler.read(is));
						} finally {
							try {
								is.close();
							} catch (Exception e) {}
						}
					}

					break;
				}
			default :
				obj = super.getPyObject(set, col, type);
		}

		return (set.wasNull() || (obj == null)) ? Py.None : obj;
	}
}
