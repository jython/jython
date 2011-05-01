/*
 * Jython Database Specification API 2.0
 *
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql;

import org.python.core.codecs;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyUnicode;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Class PyStatement
 *
 * @author brian zimmer
 */
public class PyStatement extends PyObject {

    /** Denotes a simple Statement with no parameters. */
    public static final int STATEMENT_STATIC = 2;

    /** Denotes a PreparedStatement either explicitly created by the user, or from a
     * cursor (due to the presence of bind parameters). */
    public static final int STATEMENT_PREPARED = 4;

    /** Denotes a stored procedure call. */
    public static final int STATEMENT_CALLABLE = 8;

    /** One of the above styles. */
    private int style;

    /** The underlying sql, a String or a Procedure. */
    private Object sql;

    /** Whether this statement is closed. */
    private boolean closed;

    /** The underlying java.sql.Statement. */
    Statement statement;

    /** Field __methods__ */
    protected static PyList __methods__;

    /** Field __members__ */
    protected static PyList __members__;

    static {
        PyObject[] m = new PyObject[1];

        m[0] = new PyString("close");
        __methods__ = new PyList(m);
        m = new PyObject[3];
        m[0] = new PyString("style");
        m[1] = new PyString("closed");
        m[2] = new PyString("__statement__");
        __members__ = new PyList(m);
    }

    /**
     * Constructor PyStatement
     *
     * @param statement
     * @param sql
     * @param style
     */
    public PyStatement(Statement statement, Object sql, int style) {
        this.statement = statement;
        this.sql = sql;
        this.style = style;
        closed = false;
    }

    /**
     * Constructor PyStatement
     *
     * @param statement
     * @param procedure
     */
    public PyStatement(Statement statement, Procedure procedure) {
        this(statement, procedure, STATEMENT_CALLABLE);
    }

    @Override
    public PyUnicode __unicode__() {
        if (sql instanceof String) {
            return Py.newUnicode((String) sql);
        } else if (sql instanceof Procedure) {
            try {
                return Py.newUnicode(((Procedure) sql).toSql());
            } catch (SQLException e) {
                throw zxJDBC.makeException(e);
            }
        }
        return super.__unicode__();
    }

    @Override
    public PyString __str__() {
        return Py.newString(__unicode__().encode(codecs.getDefaultEncoding(), "replace"));
    }

    @Override
    public String toString() {
        return String.format("<PyStatement object at %s for [%s]", Py.idstr(this), __unicode__());
    }

    /**
     * Gets the value of the attribute name.
     *
     * @param name
     * @return the attribute for the given name
     */
    @Override
    public PyObject __findattr_ex__(String name) {
        if ("style".equals(name)) {
            return Py.newInteger(style);
        } else if ("closed".equals(name)) {
            return Py.newBoolean(closed);
        } else if ("__statement__".equals(name)) {
            return Py.java2py(statement);
        } else if ("__methods__".equals(name)) {
            return __methods__;
        } else if ("__members__".equals(name)) {
            return __members__;
        }

        return super.__findattr_ex__(name);
    }

    /**
     * Initializes the object's namespace.
     *
     * @param dict
     */
    static public void classDictInit(PyObject dict) {
        dict.__setitem__("__version__", Py.newString("7290"));

        // hide from python
        dict.__setitem__("classDictInit", null);
        dict.__setitem__("statement", null);
        dict.__setitem__("execute", null);
        dict.__setitem__("prepare", null);
        dict.__setitem__("STATEMENT_STATIC", null);
        dict.__setitem__("STATEMENT_PREPARED", null);
        dict.__setitem__("STATEMENT_CALLABLE", null);
    }

    /**
     * Delete the statement.
     */
    public void __del__() {
        close();
    }

    /**
     * Method execute
     *
     * @param cursor
     * @param params
     * @param bindings
     * @throws SQLException
     */
    public void execute(PyCursor cursor, PyObject params, PyObject bindings) throws SQLException {
        if (closed) {
            throw zxJDBC.makeException(zxJDBC.ProgrammingError, "statement is closed");
        }

        prepare(cursor, params, bindings);

        Fetch fetch = cursor.fetch;
        switch (style) {
            case STATEMENT_STATIC:
                if (statement.execute((String) sql)) {
                    fetch.add(statement.getResultSet());
                }
                break;

            case STATEMENT_PREPARED:
                final PreparedStatement preparedStatement = (PreparedStatement) statement;

                if (preparedStatement.execute()) {
                    fetch.add(preparedStatement.getResultSet());
                }
                break;

            case STATEMENT_CALLABLE:
                final CallableStatement callableStatement = (CallableStatement) statement;

                if (callableStatement.execute()) {
                    fetch.add(callableStatement.getResultSet());
                }

                fetch.add(callableStatement, (Procedure) sql, params);
                break;

            default:
                throw zxJDBC.makeException(zxJDBC.ProgrammingError,
                                           zxJDBC.getString("invalidStyle"));
        }
    }

    /**
     * Method prepare
     *
     * @param cursor
     * @param params
     * @param bindings
     * @throws SQLException
     */
    private void prepare(PyCursor cursor, PyObject params, PyObject bindings) throws SQLException {
        if (params == Py.None || style == STATEMENT_STATIC) {
            return;
        }

        // [3, 4] or (3, 4)
        final DataHandler datahandler = cursor.datahandler;
        int columns = 0, column = 0, index = params.__len__();
        final PreparedStatement preparedStatement = (PreparedStatement) statement;
        final Procedure procedure = style == STATEMENT_CALLABLE ? (Procedure) sql : null;

        if (style != STATEMENT_CALLABLE) {
            columns = params.__len__();
            // clear the statement so all new bindings take affect only if not a callproc
            // this is because Procedure already registered the OUT parameters and we
            // don't want to lose those
            preparedStatement.clearParameters();
        } else {
            columns = procedure.columns == Py.None ? 0 : procedure.columns.__len__();
        }

        // count backwards through all the columns
        while (columns-- > 0) {
            column = columns + 1;

            if (procedure != null && !procedure.isInput(column)) {
                continue;
            }

            // working from right to left
            PyObject param = params.__getitem__(--index);
            if (bindings != Py.None) {
                PyObject binding = bindings.__finditem__(Py.newInteger(index));

                if (binding != null) {
                    try {
                        int bindingValue = binding.asInt();
                        datahandler.setJDBCObject(preparedStatement, column, param, bindingValue);
                    } catch (PyException e) {
                        throw zxJDBC.makeException(zxJDBC.ProgrammingError,
                                                   zxJDBC.getString("bindingValue"));
                    }
                    continue;
                }
            }

            datahandler.setJDBCObject(preparedStatement, column, param);
        }
    }

    /**
     * Method close
     */
    public void close() {
        try {
            statement.close();
        } catch (SQLException e) {
            throw zxJDBC.makeException(e);
        } finally {
            closed = true;
        }
    }
}
