
/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2002 brian zimmer <mailto:bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql.procedure;

import java.sql.*;
import org.python.core.*;
import com.ziclix.python.sql.*;

/**
 * Stored procedure support for SQLServer.
 *
 * @author brian zimmer
 * @author last revised by $Author$
 * @version $Revision$
 */
public class SQLServerProcedure extends Procedure {

	public SQLServerProcedure(PyCursor cursor, PyObject name) throws SQLException {
		super(cursor, name);
	}

	protected PyObject getDefault() {
		return Py.None;
	}

	protected String getProcedureName() {

		StringBuffer proc = new StringBuffer();

		if (this.procedureSchema.__nonzero__()) {
			proc.append(this.procedureSchema.toString()).append(".");
		}

		return proc.append(this.procedureName.toString()).toString();
	}
}
