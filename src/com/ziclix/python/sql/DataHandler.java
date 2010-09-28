/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;

import org.python.core.Py;
import org.python.core.PyFile;
import org.python.core.PyList;
import org.python.core.PyObject;

/**
 * The DataHandler is responsible mapping the JDBC data type to
 * a Jython object.  Depending on the version of the JDBC
 * implementation and the particulars of the driver, the type
 * mapping can be significantly different.
 *
 * This interface can also be used to change the behaviour of
 * the default mappings provided by the cursor.  This might be
 * useful in handling more complicated data types such as BLOBs,
 * CLOBs and Arrays.
 *
 * @author brian zimmer
 * @author last revised by $Author$
 * @version $Revision$
 */
public class DataHandler {

    /** Default size for buffers. */
    private static final int INITIAL_SIZE = 1024 * 4;

    private static final String[] SYSTEM_DATAHANDLERS = {
        "com.ziclix.python.sql.JDBC20DataHandler"
    };

    /**
     * Handle most generic Java data types.
     */
    public DataHandler() {}

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
            Object o = object.__tojava__(Object.class);
            if (o instanceof BigInteger) {
                //XXX: This is in here to specifically fix passing a PyLong into Postgresql.
                stmt.setObject(index, o, Types.BIGINT);
            } else {
                stmt.setObject(index, o);
            }
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
    public void setJDBCObject(PreparedStatement stmt, int index, PyObject object, int type)
        throws SQLException {
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
            SQLException cause = null, ex = new SQLException("error setting index [" + index
                                                             + "], type [" + type + "]");

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
                obj = string == null ? Py.None : Py.newUnicode(string);
                break;

            case Types.LONGVARCHAR:
                Reader reader = set.getCharacterStream(col);
                obj = reader == null ? Py.None : Py.newUnicode(read(reader));
                break;

            case Types.NUMERIC:
            case Types.DECIMAL:
                BigDecimal bd = set.getBigDecimal(col);
                obj = (bd == null) ? Py.None : Py.newFloat(bd.doubleValue());
                break;

            case Types.BIT:
            case Types.BOOLEAN:
                obj = set.getBoolean(col) ? Py.True : Py.False;
                break;

            case Types.INTEGER:
            case Types.TINYINT:
            case Types.SMALLINT:
                obj = Py.newInteger(set.getInt(col));
                break;

            case Types.BIGINT:
                obj = Py.newLong(set.getLong(col));
                break;

            case Types.FLOAT:
            case Types.REAL:
                obj = Py.newFloat(set.getFloat(col));
                break;

            case Types.DOUBLE:
                obj = Py.newFloat(set.getDouble(col));
                break;

            case Types.TIME:
                obj = Py.newTime(set.getTime(col));
                break;

            case Types.TIMESTAMP:
                obj = Py.newDatetime(set.getTimestamp(col));
                break;

            case Types.DATE:
                Object date = set.getObject(col);
                // don't newDate mysql YEAR columns
                obj = date instanceof Date ? Py.newDate((Date)date) : Py.java2py(date);
                break;

            case Types.NULL:
                obj = Py.None;
                break;

            case Types.OTHER:
            case Types.JAVA_OBJECT:
                obj = Py.java2py(set.getObject(col));
                break;

            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                obj = Py.java2py(set.getBytes(col));
                break;

            case Types.BLOB:
                Blob blob = set.getBlob(col);
                obj = blob == null ? Py.None : Py.java2py(read(blob.getBinaryStream()));
                break;

            case Types.CLOB:
                Clob clob = set.getClob(col);
                obj = clob == null ? Py.None : Py.java2py(read(clob.getCharacterStream()));
                break;
                
            // TODO can we support these?
            case Types.ARRAY:
                throw createUnsupportedTypeSQLException("ARRAY", col);
            case Types.DATALINK:
                throw createUnsupportedTypeSQLException("DATALINK", col);
            case Types.DISTINCT:
                throw createUnsupportedTypeSQLException("DISTINCT", col);
            case Types.REF:
                throw createUnsupportedTypeSQLException("REF", col);
            case Types.STRUCT:
                throw createUnsupportedTypeSQLException("STRUCT", col);
                
            default :
                throw createUnsupportedTypeSQLException(new Integer(type), col);
        }

        return set.wasNull() || obj == null ? Py.None : obj;
    }

    protected final SQLException createUnsupportedTypeSQLException(Object type, int col) {
        Object[] vals = {type, new Integer(col)};
        String msg = zxJDBC.getString("unsupportedTypeForColumn", vals);
        return new SQLException(msg);
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
                obj = (string == null) ? Py.None : Py.newUnicode(string);
                break;

            case Types.NUMERIC:
            case Types.DECIMAL:
                BigDecimal bd = stmt.getBigDecimal(col);
                obj = (bd == null) ? Py.None : Py.newFloat(bd.doubleValue());
                break;

            case Types.BIT:
                obj = stmt.getBoolean(col) ? Py.True : Py.False;
                break;

            case Types.INTEGER:
            case Types.TINYINT:
            case Types.SMALLINT:
                obj = Py.newInteger(stmt.getInt(col));
                break;

            case Types.BIGINT:
                obj = Py.newLong(stmt.getLong(col));
                break;

            case Types.FLOAT:
            case Types.REAL:
                obj = Py.newFloat(stmt.getFloat(col));
                break;

            case Types.DOUBLE:
                obj = Py.newFloat(stmt.getDouble(col));
                break;

            case Types.TIME:
                obj = Py.newTime(stmt.getTime(col));
                break;

            case Types.TIMESTAMP:
                obj = Py.newDatetime(stmt.getTimestamp(col));
                break;

            case Types.DATE:
                obj = Py.newDate(stmt.getDate(col));
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
                createUnsupportedTypeSQLException(type, col);
        }

        return stmt.wasNull() || obj == null ? Py.None : obj;
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
    public void registerOut(CallableStatement statement, int index, int colType, int dataType,
                            String dataTypeName) throws SQLException {

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
     * Handles checking if the object is null or None and setting it on the statement.
     *
     * @return true if the object is null and was set on the statement, false otherwise
     */
    public static final boolean checkNull(PreparedStatement stmt, int index, PyObject object,
                                          int type) throws SQLException {

        if ((object == null) || (Py.None == object)) {
            stmt.setNull(index, type);
            return true;
        }
        return false;
    }

    /**
     * Consume the InputStream into an byte array and close the InputStream.
     *
     * @return the contents of the InputStream a byte[]
     */
    public static final byte[] read(InputStream stream) {
        int size = 0;
        byte[] buffer = new byte[INITIAL_SIZE];
        ByteArrayOutputStream baos = new ByteArrayOutputStream(INITIAL_SIZE);

        try {
            while ((size = stream.read(buffer)) != -1) {
                baos.write(buffer, 0, size);
            }
        } catch (IOException ioe) {
            throw zxJDBC.makeException(ioe);
        } finally {
            try {
                stream.close();
            } catch (IOException ioe) {
                throw zxJDBC.makeException(ioe);
            }
        }

        return baos.toByteArray();
    }

    /**
     * Consume the Reader into a String and close the Reader.
     *
     * @return the contents of the Reader as a String
     */
    public static String read(Reader reader) {
        int size = 0;
        char[] buffer = new char[INITIAL_SIZE];
        StringBuilder builder = new StringBuilder(INITIAL_SIZE);

        try {
            while ((size = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, size);
            }
        } catch (IOException ioe) {
            throw zxJDBC.makeException(ioe);
        } finally {
            try {
                reader.close();
            } catch (IOException ioe) {
                throw zxJDBC.makeException(ioe);
            }
        }

        return builder.toString();
    }

    /**
     * Build the DataHandler chain depending on the VM.  This guarentees a DataHandler
     * but might additionally chain a JDBC2.0 or JDBC3.0 implementation.
     * @return a DataHandler configured for the VM version
     */
    public static final DataHandler getSystemDataHandler() {
        DataHandler dh = new DataHandler();

        for (String element : SYSTEM_DATAHANDLERS) {
            try {
                Class<?> c = Class.forName(element);
                Constructor<?> cons = c.getConstructor(new Class<?>[]{DataHandler.class});
                dh = (DataHandler) cons.newInstance(new Object[]{dh});
            } catch (Throwable t) {}
        }

        return dh;
    }

    /**
     * Returns a list of datahandlers chained together through the use of delegation.
     *
     * @return a list of datahandlers
     */
    public PyObject __chain__() {
        return new PyList(Py.javas2pys(this));
    }

    /**
     * Returns the classname of this datahandler.
     */
    @Override
    public String toString() {
        return getClass().getName();
    }
}

