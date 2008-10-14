/*
 * Jython Database Specification API 2.0
 *
 * $Id: DataHandler.java 3708 2007-11-20 20:03:46Z pjenvey $
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql;

import org.python.core.Py;
import org.python.core.PyFile;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyList;
import org.python.core.PyString;
import org.python.core.util.StringUtil;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;

/**
 * A copy of the DataHandler class as it was before Jython 2.5. By that version,
 * some backward-incompatible changes was made, as returning datetime.*
 * objects for DATE, TIME and TIMESTAMP columns, instead of java.sql.* classes.
 *
 * @author brian zimmer
 * @author last revised by $Author: pjenvey $
 * @version $Revision: 3708 $
 */
public class Jython22DataHandler extends DataHandler {
    /**
     * Handle most generic Java data types.
     */
    public Jython22DataHandler() {}

    /**
     * Some database vendors are case sensitive on calls to DatabaseMetaData,
     * most notably Oracle.  This callback allows a DataHandler to affect the
     * name.
     */
    public String getMetaDataName(PyObject name) {
        return ((name == Py.None) ? null : name.__str__().toString());
    }

    /**
     * A factory method for determing the correct procedure class to use
     * per the cursor type.
     * @param cursor an open cursor
     * @param name the name of the procedure to invoke
     * @return an instance of a Procedure
     * @throws SQLException
     */
    public Procedure getProcedure(PyCursor cursor, PyObject name) throws SQLException {
        return new Procedure(cursor, name);
    }

    /**
     * Returns the row id of the last executed statement.
     *
     * @param stmt the current statement
     * @return the row id of the last executed statement or None
     * @throws SQLException thrown if an exception occurs
     *
     */
    public PyObject getRowId(Statement stmt) throws SQLException {
        return Py.None;
    }

    /**
     * A callback prior to each execution of the statement.  If the statement is
     * a PreparedStatement, all the parameters will have been set.
     */
    public void preExecute(Statement stmt) throws SQLException {
        return;
    }

    /**
     * A callback after successfully executing the statement.
     */
    public void postExecute(Statement stmt) throws SQLException {
        return;
    }

    /**
     * Any .execute() which uses prepared statements will receive a callback for deciding
     * how to map the PyObject to the appropriate JDBC type.
     *
     * @param stmt the current PreparedStatement
     * @param index the index for which this object is bound
     * @param object the PyObject in question
     * @throws SQLException
     */
    public void setJDBCObject(PreparedStatement stmt, int index, PyObject object) throws SQLException {

        try {
            stmt.setObject(index, object.__tojava__(Object.class));
        } catch (Exception e) {
            SQLException cause = null, ex = new SQLException("error setting index [" + index + "]");

            if (e instanceof SQLException) {
                cause = (SQLException) e;
            } else {
                cause = new SQLException(e.getMessage());
            }

            ex.setNextException(cause);

            throw ex;
        }
    }

    /**
     * Any .execute() which uses prepared statements will receive a callback for deciding
     * how to map the PyObject to the appropriate JDBC type.  The <i>type</i> is the JDBC
     * type as obtained from <i>java.sql.Types</i>.
     *
     * @param stmt the current PreparedStatement
     * @param index the index for which this object is bound
     * @param object the PyObject in question
     * @param type the <i>java.sql.Types</i> for which this PyObject should be bound
     * @throws SQLException
     */
    public void setJDBCObject(PreparedStatement stmt, int index, PyObject object, int type) throws SQLException {

        try {
            if (checkNull(stmt, index, object, type)) {
                return;
            }

            switch (type) {

                case Types.DATE:
                    Date date = (Date) object.__tojava__(Date.class);

                    stmt.setDate(index, date);
                    break;

                case Types.TIME:
                    Time time = (Time) object.__tojava__(Time.class);

                    stmt.setTime(index, time);
                    break;

                case Types.TIMESTAMP:
                    Timestamp timestamp = (Timestamp) object.__tojava__(Timestamp.class);

                    stmt.setTimestamp(index, timestamp);
                    break;

                case Types.LONGVARCHAR:
                    if (object instanceof PyFile) {
                        object = ((PyFile) object).read();
                    }

                    String varchar = (String) object.__tojava__(String.class);
                    Reader reader = new BufferedReader(new StringReader(varchar));

                    stmt.setCharacterStream(index, reader, varchar.length());
                    break;

                case Types.BIT:
                    stmt.setBoolean(index, object.__nonzero__());
                    break;

                default :
                    if (object instanceof PyFile) {
                        object = ((PyFile) object).read();
                    }

                    stmt.setObject(index, object.__tojava__(Object.class), type);
                    break;
            }
        } catch (Exception e) {
            SQLException cause = null, ex = new SQLException("error setting index [" + index + "], type [" + type + "]");

            if (e instanceof SQLException) {
                cause = (SQLException) e;
            } else {
                cause = new SQLException(e.getMessage());
            }

            ex.setNextException(cause);

            throw ex;
        }
    }

    /**
     * Given a ResultSet, column and type, return the appropriate
     * Jython object.
     *
     * <p>Note: DO NOT iterate the ResultSet.
     *
     * @param set the current ResultSet set to the current row
     * @param col the column number (adjusted properly for JDBC)
     * @param type the column type
     * @throws SQLException if the type is unmappable
     */
    public PyObject getPyObject(ResultSet set, int col, int type) throws SQLException {

        PyObject obj = Py.None;

        switch (type) {

            case Types.CHAR:
            case Types.VARCHAR:
                String string = set.getString(col);

                obj = (string == null) ? Py.None : Py.newString(string);
                break;

            case Types.LONGVARCHAR:
                InputStream longvarchar = set.getAsciiStream(col);

                if (longvarchar == null) {
                    obj = Py.None;
                } else {
                    try {
                        longvarchar = new BufferedInputStream(longvarchar);

                        byte[] bytes = Jython22DataHandler.read(longvarchar);

                        if (bytes != null) {
                            obj = Py.newString(StringUtil.fromBytes(bytes));
                        }
                    } finally {
                        try {
                            longvarchar.close();
                        } catch (Throwable t) {}
                    }
                }
                break;

            case Types.NUMERIC:
            case Types.DECIMAL:
                BigDecimal bd = null;

                try {
                    bd = set.getBigDecimal(col, set.getMetaData().getPrecision(col));
                } catch (Throwable t) {
                    bd = set.getBigDecimal(col, 10);
                }

                obj = (bd == null) ? Py.None : Py.newFloat(bd.doubleValue());
                break;

            case Types.BIT:
                obj = set.getBoolean(col) ? Py.One : Py.Zero;
                break;

            case Types.INTEGER:
            case Types.TINYINT:
            case Types.SMALLINT:
                obj = Py.newInteger(set.getInt(col));
                break;

            case Types.BIGINT:
                obj = new PyLong(set.getLong(col));
                break;

            case Types.FLOAT:
            case Types.REAL:
                obj = Py.newFloat(set.getFloat(col));
                break;

            case Types.DOUBLE:
                obj = Py.newFloat(set.getDouble(col));
                break;

            case Types.TIME:
                obj = Py.java2py(set.getTime(col));
                break;

            case Types.TIMESTAMP:
                obj = Py.java2py(set.getTimestamp(col));
                break;

            case Types.DATE:
                obj = Py.java2py(set.getDate(col));
                break;

            case Types.NULL:
                obj = Py.None;
                break;

            case Types.OTHER:
                obj = Py.java2py(set.getObject(col));
                break;

            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                obj = Py.java2py(set.getBytes(col));
                break;

            default :
                Integer[] vals = {new Integer(col), new Integer(type)};
                String msg = zxJDBC.getString("errorGettingIndex", vals);

                throw new SQLException(msg);
        }

        return (set.wasNull() || (obj == null)) ? Py.None : obj;
    }

    /**
     * Given a CallableStatement, column and type, return the appropriate
     * Jython object.
     *
     * @param stmt the CallableStatement
     * @param col the column number (adjusted properly for JDBC)
     * @param type the column type
     * @throws SQLException if the type is unmappable
     */
    public PyObject getPyObject(CallableStatement stmt, int col, int type) throws SQLException {

        PyObject obj = Py.None;

        switch (type) {

            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
                String string = stmt.getString(col);

                obj = (string == null) ? Py.None : Py.newString(string);
                break;

            case Types.NUMERIC:
            case Types.DECIMAL:
                BigDecimal bd = stmt.getBigDecimal(col, 10);

                obj = (bd == null) ? Py.None : Py.newFloat(bd.doubleValue());
                break;

            case Types.BIT:
                obj = stmt.getBoolean(col) ? Py.One : Py.Zero;
                break;

            case Types.INTEGER:
            case Types.TINYINT:
            case Types.SMALLINT:
                obj = Py.newInteger(stmt.getInt(col));
                break;

            case Types.BIGINT:
                obj = new PyLong(stmt.getLong(col));
                break;

            case Types.FLOAT:
            case Types.REAL:
                obj = Py.newFloat(stmt.getFloat(col));
                break;

            case Types.DOUBLE:
                obj = Py.newFloat(stmt.getDouble(col));
                break;

            case Types.TIME:
                obj = Py.java2py(stmt.getTime(col));
                break;

            case Types.TIMESTAMP:
                obj = Py.java2py(stmt.getTimestamp(col));
                break;

            case Types.DATE:
                obj = Py.java2py(stmt.getDate(col));
                break;

            case Types.NULL:
                obj = Py.None;
                break;

            case Types.OTHER:
                obj = Py.java2py(stmt.getObject(col));
                break;

            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                obj = Py.java2py(stmt.getBytes(col));
                break;

            default :
                Integer[] vals = {new Integer(col), new Integer(type)};
                String msg = zxJDBC.getString("errorGettingIndex", vals);

                throw new SQLException(msg);
        }

        return (stmt.wasNull() || (obj == null)) ? Py.None : obj;
    }

    /**
     * Called when a stored procedure or function is executed and OUT parameters
     * need to be registered with the statement.
     *
     * @param statement
     * @param index the JDBC offset column number
     * @param colType the column as from DatabaseMetaData (eg, procedureColumnOut)
     * @param dataType the JDBC datatype from Types
     * @param dataTypeName the JDBC datatype name
     *
     * @throws SQLException
     *
     */
    public void registerOut(CallableStatement statement, int index, int colType, int dataType, String dataTypeName) throws SQLException {

        try {
            statement.registerOutParameter(index, dataType);
        } catch (Throwable t) {
            SQLException cause = null;
            SQLException ex = new SQLException("error setting index ["
              + index + "], coltype [" + colType + "], datatype [" + dataType
              + "], datatypename [" + dataTypeName + "]");

            if (t instanceof SQLException) {
                cause = (SQLException)t;
            } else {
                cause = new SQLException(t.getMessage());
            }
            ex.setNextException(cause);
            throw ex;
        }
    }

    /**
     * Returns a list of datahandlers chained together through the use of delegation.
     *
     * @return a list of datahandlers
     */
    public PyObject __chain__() {
        return new PyList(new PyObject[] { Py.java2py(this) });
    }

}

