/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql;

import org.python.core.Py;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;

import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.BitSet;

/**
 * This class provides the necessary functionality to call stored
 * procedures.  It handles managing the database metadata and binding
 * the appropriate parameters.
 *
 * @author brian zimmer
 * @author last modified by $Author$
 * @version $Revision$
 */
public class Procedure extends Object {

    /**
     * Field NAME
     */
    protected static final int NAME = 3;

    /**
     * Field COLUMN_TYPE
     */
    protected static final int COLUMN_TYPE = 4;

    /**
     * Field DATA_TYPE
     */
    protected static final int DATA_TYPE = 5;

    /**
     * Field DATA_TYPE_NAME
     */
    protected static final int DATA_TYPE_NAME = 6;

    /**
     * Field PRECISION
     */
    protected static final int PRECISION = 7;

    /**
     * Field LENGTH
     */
    protected static final int LENGTH = 8;

    /**
     * Field SCALE
     */
    protected static final int SCALE = 9;

    /**
     * Field NULLABLE
     */
    protected static final int NULLABLE = 11;

    /**
     * Field cursor
     */
    protected PyCursor cursor;

    /**
     * Field columns
     */
    protected PyObject columns;

    /**
     * Field procedureCatalog
     */
    protected PyObject procedureCatalog;

    /**
     * Field procedureSchema
     */
    protected PyObject procedureSchema;

    /**
     * Field procedureName
     */
    protected PyObject procedureName;

    /**
     * Field inputSet
     */
    protected BitSet inputSet;

    /**
     * Constructor Procedure
     *
     * @param cursor cursor an open cursor
     * @param name   name a string or tuple representing the name
     * @throws SQLException
     */
    public Procedure(PyCursor cursor, PyObject name) throws SQLException {

        this.cursor = cursor;
        this.inputSet = new BitSet();

        if (name instanceof PyString) {
            this.procedureCatalog = getDefault();
            this.procedureSchema = getDefault();
            this.procedureName = name;
        } else if (PyCursor.isSeq(name)) {
            if (name.__len__() == 3) {
                this.procedureCatalog = name.__getitem__(0);
                this.procedureSchema = name.__getitem__(1);
                this.procedureName = name.__getitem__(2);
            } else {

                // throw an exception
            }
        } else {

            // throw an exception
        }

        fetchColumns();
    }

    /**
     * Prepares the statement and registers the OUT/INOUT parameters (if any).
     *
     * @return CallableStatement
     * @throws SQLException
     */
    public CallableStatement prepareCall() throws SQLException {
        return prepareCall(Py.None, Py.None);
    }

    /**
     * Prepares the statement and registers the OUT/INOUT parameters (if any).
     *
     * @param rsType   the value of to be created ResultSet type
     * @param rsConcur the value of the to be created ResultSet concurrency
     * @return CallableStatement
     * @throws SQLException
     */
    public CallableStatement prepareCall(PyObject rsType, PyObject rsConcur) throws SQLException {

        // prepare the statement
        CallableStatement statement = null;
        boolean normal = ((rsType == Py.None) && (rsConcur == Py.None));

        try {

            // build the full call syntax
            String sqlString = toSql();

            if (normal) {
                statement = cursor.connection.connection.prepareCall(sqlString);
            } else {
                int t = rsType.asInt();
                int c = rsConcur.asInt();

                statement = cursor.connection.connection.prepareCall(sqlString, t, c);
            }

            // prepare the OUT parameters
            registerOutParameters(statement);
        } catch (SQLException e) {
            if (statement != null) {
                try {
                    statement.close();
                } catch (Exception ex) {
                }
            }

            throw e;
        }

        return statement;
    }

    /**
     * Prepare the binding dictionary with the correct datatypes.
     *
     * @param params   a non-None list of params
     * @param bindings a dictionary of bindings
     */
    public void normalizeInput(PyObject params, PyObject bindings) throws SQLException {

        if (this.columns == Py.None) {
            return;
        }

        // do nothing with params at the moment
        for (int i = 0, len = this.columns.__len__(), binding = 0; i < len; i++) {
            PyObject column = this.columns.__getitem__(i);
            int colType = column.__getitem__(COLUMN_TYPE).asInt();

            switch (colType) {

                case DatabaseMetaData.procedureColumnIn:
                case DatabaseMetaData.procedureColumnInOut:

                    // bindings are Python-indexed
                    PyInteger key = Py.newInteger(binding++);

                    if (bindings.__finditem__(key) == null) {
                        int dataType = column.__getitem__(DATA_TYPE).asInt();
                        bindings.__setitem__(key, Py.newInteger(dataType));
                    }

                    // inputs are JDBC-indexed
                    this.inputSet.set(i + 1);
                    break;
            }
        }
    }

    /**
     * This method determines whether the param at the specified index is an
     * IN or INOUT param for a stored procedure.  This is only configured properly
     * AFTER a call to normalizeInput().
     *
     * @param index JDBC indexed column index (1, 2, ...)
     * @return true if the column is an input, false otherwise
     * @throws SQLException
     */
    public boolean isInput(int index) throws SQLException {
        return this.inputSet.get(index);
    }

    /**
     * Returns the call in the syntax:
     * <p/>
     * {? = call <procedure-name>(?, ?, ...)}
     * {call <procedure-name>(?, ?, ...)}
     * <p/>
     * As of now, all parameters variables are created and no support for named variable
     * calling is supported.
     *
     * @return String
     */
    public String toSql() throws SQLException {

        int colParam = 0;
        int colReturn = 0;

        if (this.columns != Py.None) {
            for (int i = 0, len = this.columns.__len__(); i < len; i++) {
                PyObject column = this.columns.__getitem__(i);
                int colType = column.__getitem__(COLUMN_TYPE).asInt();

                switch (colType) {

                    case DatabaseMetaData.procedureColumnUnknown:
                        throw zxJDBC.makeException(zxJDBC.NotSupportedError, "procedureColumnUnknown");
                    case DatabaseMetaData.procedureColumnResult:
                        throw zxJDBC.makeException(zxJDBC.NotSupportedError, "procedureColumnResult");

                        // these go on the right hand side
                    case DatabaseMetaData.procedureColumnIn:
                    case DatabaseMetaData.procedureColumnInOut:
                    case DatabaseMetaData.procedureColumnOut:
                        colParam++;
                        break;

                        // these go on the left hand side
                    case DatabaseMetaData.procedureColumnReturn:
                        colReturn++;
                        break;

                    default :
                        throw zxJDBC.makeException(zxJDBC.DataError, "unknown column type [" + colType + "]");
                }
            }
        }

        StringBuffer sql = new StringBuffer("{");

        if (colReturn > 0) {
            PyList list = new PyList();

            for (; colReturn > 0; colReturn--) {
                list.append(Py.newString("?"));
            }

            sql.append(Py.newString(",").join(list)).append(" = ");
        }

        String name = this.getProcedureName();

        sql.append("call ").append(name).append("(");

        if (colParam > 0) {
            PyList list = new PyList();

            for (; colParam > 0; colParam--) {
                list.append(Py.newString("?"));
            }

            sql.append(Py.newString(",").join(list));
        }

        return sql.append(")}").toString();
    }

    /**
     * Registers the OUT/INOUT parameters of the statement.
     *
     * @param statement statement
     * @throws SQLException
     */
    protected void registerOutParameters(CallableStatement statement) throws SQLException {

        if (this.columns == Py.None) {
            return;
        }

        for (int i = 0, len = this.columns.__len__(); i < len; i++) {
            PyObject column = this.columns.__getitem__(i);
            int colType = column.__getitem__(COLUMN_TYPE).asInt();
            int dataType = column.__getitem__(DATA_TYPE).asInt();
            String dataTypeName = column.__getitem__(DATA_TYPE_NAME).toString();

            switch (colType) {

                case DatabaseMetaData.procedureColumnInOut:
                case DatabaseMetaData.procedureColumnOut:
                case DatabaseMetaData.procedureColumnReturn:
                    cursor.datahandler.registerOut(statement, i + 1, colType, dataType, dataTypeName);
                    break;
            }
        }
    }

    /**
     * Get the columns for the stored procedure.
     *
     * @throws SQLException
     */
    protected void fetchColumns() throws SQLException {

        PyExtendedCursor pec = (PyExtendedCursor) cursor.connection.cursor();

        try {
            pec.datahandler = this.cursor.datahandler;

            pec.procedurecolumns(procedureCatalog, procedureSchema, procedureName, Py.None);

            this.columns = pec.fetchall();
        } finally {
            pec.close();
        }
    }

    /**
     * The value for a missing schema or catalog.  This value is used to find
     * the column names for the procedure.  Not all DBMS use the same default
     * value; for instance Oracle uses an empty string and SQLServer a null.
     * This implementation returns the empty string.
     *
     * @return the default value (the empty string)
     * @see java.sql.DatabaseMetaData#getProcedureColumns
     */
    protected PyObject getDefault() {
        return Py.EmptyString;
    }

    /**
     * Construct a procedure name for the relevant schema and catalog information.
     */
    protected String getProcedureName() {

        StringBuffer proc = new StringBuffer();

        if (this.procedureCatalog.__nonzero__()) {
            proc.append(this.procedureCatalog.toString()).append(".");
        }

        return proc.append(this.procedureName.toString()).toString();
    }
}
