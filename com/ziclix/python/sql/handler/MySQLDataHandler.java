
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
import org.python.core.*;
import com.ziclix.python.sql.*;

/**
 * MySQL specific data handling.
 *
 * @author brian zimmer
 * @author last revised by $Author$
 * @version $Revision$
 */
public class MySQLDataHandler extends FilterDataHandler {

	/**
	 * Decorator for handling MySql specific issues.
	 *
	 * @param datahandler the delegate DataHandler
	 */
	public MySQLDataHandler(DataHandler datahandler) {
		super(datahandler);
	}

	/**
	 * Returns the last insert id for the statement.
	 *
	 * @param Statement stmt
	 *
	 * @return PyObject
	 *
	 * @throws SQLException
	 *
	 */
	public PyObject getRowId(Statement stmt) throws SQLException {

		if (stmt instanceof org.gjt.mm.mysql.Statement) {
			return Py.newInteger(((org.gjt.mm.mysql.Statement)stmt).getLastInsertID());
		}

		return super.getRowId(stmt);
	}

	/**
	 * Handle LONGVARCHAR.
	 */
	public void setJDBCObject(PreparedStatement stmt, int index, PyObject object, int type) throws SQLException {

		if (DataHandler.checkNull(stmt, index, object, type)) {
			return;
		}

		switch (type) {

			case Types.LONGVARCHAR :
				if (object instanceof PyFile) {
					object = ((PyFile)object).read();
				}

				String varchar = (String)object.__tojava__(String.class);
				InputStream stream = new ByteArrayInputStream(varchar.getBytes());

				stream = new BufferedInputStream(stream);

				stmt.setAsciiStream(index, stream, varchar.length());
				break;

			default :
				super.setJDBCObject(stmt, index, object, type);
				break;
		}
	}
}
