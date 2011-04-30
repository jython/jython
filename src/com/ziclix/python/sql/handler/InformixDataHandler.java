/*
 * Jython Database Specification API 2.0
 *
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql.handler;

import com.informix.jdbc.IfmxStatement;

import com.ziclix.python.sql.DataHandler;
import com.ziclix.python.sql.FilterDataHandler;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import org.python.core.Py;
import org.python.core.PyFile;
import org.python.core.PyObject;
import org.python.core.PyString;

/**
 * Informix specific data handling.
 *
 * @author brian zimmer
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
     * @param stmt
     * @return PyObject
     * @throws SQLException
     */
    @Override
    public PyObject getRowId(Statement stmt) throws SQLException {
        if (stmt instanceof IfmxStatement) {
            return Py.newInteger(((IfmxStatement) stmt).getSerial());
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
    @Override
    public void setJDBCObject(PreparedStatement stmt, int index, PyObject object, int type)
        throws SQLException {
        if (DataHandler.checkNull(stmt, index, object, type)) {
            return;
        }

        switch (type) {

            case Types.LONGVARCHAR:

                String varchar;
                // Ifx driver can't handle the setCharacterStream() method so use setObject() instead
                if (object instanceof PyFile) {
                    varchar = ((PyFile) object).read().toString();
                } else {
                    varchar = (String) object.__tojava__(String.class);
                }
                stmt.setObject(index, varchar, type);
                break;

            case Types.OTHER:

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
    @Override
    public void setJDBCObject(PreparedStatement stmt, int index, PyObject object)
        throws SQLException {
        // there is a bug in the Ifx driver when using setObject() with a String for a
        // prepared statement
        if (object instanceof PyString) {
            super.setJDBCObject(stmt, index, object, Types.VARCHAR);
        } else {
            super.setJDBCObject(stmt, index, object);
        }
    }

    /**
     * Override to handle Informix related issues.
     *
     * @param set  the result set
     * @param col  the column number
     * @param type the SQL type
     * @return the mapped Python object
     * @throws SQLException thrown for a sql exception
     */
    @SuppressWarnings("fallthrough")
    @Override
    public PyObject getPyObject(ResultSet set, int col, int type) throws SQLException {

        PyObject obj = Py.None;

        switch (type) {

            case Types.OTHER:
                try {
                    // informix returns boolean as OTHERs, so let's give that a try
                    obj = set.getBoolean(col) ? Py.One : Py.Zero;
                } catch (SQLException e) {
                    obj = super.getPyObject(set, col, type);
                }
                break;

            case Types.BLOB:
                int major = set.getStatement().getConnection().getMetaData().getDriverMajorVersion();
                int minor = set.getStatement().getConnection().getMetaData().getDriverMinorVersion();

                if (major <= 2 && minor <= 11) {
                    Blob blob = set.getBlob(col);
                    obj = blob == null ? Py.None : Py.java2py(read(blob.getBinaryStream()));
                    break;
                }
            default :
                obj = super.getPyObject(set, col, type);
        }

        return set.wasNull() || obj == null ? Py.None : obj;
    }
}
