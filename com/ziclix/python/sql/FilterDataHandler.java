
/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql;

import org.python.core.PyObject;
import org.python.core.PyList;
import org.python.core.PyString;
import org.python.core.Py;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A FilterDataHandler contains some other DataHandler, which it uses
 * as its basic source of functionality, possibly transforming the calls
 * along the way or providing additional functionality. The class FilterDataHandler
 * itself simply overrides all methods of DataHandler with versions that
 * pass all requests to the contained data handler.
 *
 * @author brian zimmer
 * @author last revised by $Author$
 * @version $Revision$
 */
public abstract class FilterDataHandler extends DataHandler {

	private DataHandler delegate;

	/**
	 * Constructor FilterDataHandler
	 *
	 * @param DataHandler delegate
	 *
	 */
	public FilterDataHandler(DataHandler delegate) {
		this.delegate = delegate;
	}

	/**
	 * Returns the row id of the last executed statement.
	 *
	 * @param Statement stmt
	 *
	 * @return PyObject
	 *
	 * @throws SQLException
	 *
	 */
	public PyObject getRowId(Statement stmt) throws SQLException {
		return this.delegate.getRowId(stmt);
	}

	/**
	 * Method preExecute
	 *
	 * @param Statement stmt
	 *
	 * @throws SQLException
	 *
	 */
	public void preExecute(Statement stmt) throws SQLException {
		this.delegate.preExecute(stmt);
	}

	/**
	 * Method postExecute
	 *
	 * @param Statement stmt
	 *
	 * @throws SQLException
	 *
	 */
	public void postExecute(Statement stmt) throws SQLException {
		this.delegate.postExecute(stmt);
	}

	/**
	 * Method setJDBCObject
	 *
	 * @param PreparedStatement stmt
	 * @param int index
	 * @param PyObject object
	 *
	 * @throws SQLException
	 *
	 */
	public void setJDBCObject(PreparedStatement stmt, int index, PyObject object) throws SQLException {
		this.delegate.setJDBCObject(stmt, index, object);
	}

	/**
	 * Method setJDBCObject
	 *
	 * @param PreparedStatement stmt
	 * @param int index
	 * @param PyObject object
	 * @param int type
	 *
	 * @throws SQLException
	 *
	 */
	public void setJDBCObject(PreparedStatement stmt, int index, PyObject object, int type) throws SQLException {
		this.delegate.setJDBCObject(stmt, index, object, type);
	}

	/**
	 * Method getPyObject
	 *
	 * @param ResultSet set
	 * @param int col
	 * @param int type
	 *
	 * @return PyObject
	 *
	 * @throws SQLException
	 *
	 */
	public PyObject getPyObject(ResultSet set, int col, int type) throws SQLException {
		return this.delegate.getPyObject(set, col, type);
	}

    /**
     * Returns a list of datahandlers chained together through the use of delegation.
     *
     * @return
     */
    public PyObject __chain__() {
        PyList list = new PyList();
        DataHandler handler = this;
        while(handler != null) {
            list.append(Py.java2py(handler));
            if(handler instanceof FilterDataHandler) {
                handler = ((FilterDataHandler)handler).delegate;
            } else {
                handler = null;
            }
        }
        return list;
    }
}
