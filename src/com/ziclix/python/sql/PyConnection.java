/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql;

import com.ziclix.python.sql.util.PyArgParser;
import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyBuiltinFunctionSet;
import org.python.core.PyClass;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A connection to the database.
 *
 * @author brian zimmer
 * @author last revised by $Author$
 * @version $Revision$
 */
public class PyConnection extends PyObject implements ClassDictInit {

    /**
     * Field closed
     */
    protected boolean closed;

    /**
     * Field connection
     */
    protected Connection connection;

    /**
     * Field supportsTransactions
     */
    protected boolean supportsTransactions;

    /**
     * Field cursors
     */
    private Set cursors;

    /**
     * Field statements
     */
    private Set statements;

    /**
     * Field __class__
     */
    public static PyClass __class__;

    /**
     * Method getPyClass
     *
     * @return PyClass
     */
    protected PyClass getPyClass() {
        return __class__;
    }

    /**
     * Field __members__
     */
    protected static PyList __members__;

    /**
     * Field __methods__
     */
    protected static PyList __methods__;

    static {
        PyObject[] m = new PyObject[5];

        m[0] = new PyString("close");
        m[1] = new PyString("commit");
        m[2] = new PyString("cursor");
        m[3] = new PyString("rollback");
        m[4] = new PyString("nativesql");
        __methods__ = new PyList(m);
        m = new PyObject[10];
        m[0] = new PyString("autocommit");
        m[1] = new PyString("dbname");
        m[2] = new PyString("dbversion");
        m[3] = new PyString("drivername");
        m[4] = new PyString("driverversion");
        m[5] = new PyString("url");
        m[6] = new PyString("__connection__");
        m[7] = new PyString("__cursors__");
        m[8] = new PyString("__statements__");
        m[9] = new PyString("closed");
        __members__ = new PyList(m);
    }

    /**
     * Create a PyConnection with the open connection.
     *
     * @param connection
     * @throws SQLException
     */
    public PyConnection(Connection connection) throws SQLException {

        this.closed = false;
        this.cursors = new HashSet();
        this.connection = connection;
        this.statements = new HashSet();
        this.supportsTransactions = this.connection.getMetaData().supportsTransactions();

        if (this.supportsTransactions) {
            this.connection.setAutoCommit(false);
        }
    }

    /**
     * Produces a string representation of the object.
     *
     * @return string representation of the object.
     */
    public String toString() {

        try {
            return "<PyConnection user='" + this.connection.getMetaData().getUserName() + "', url='" + this.connection.getMetaData().getURL() + "'>";
        } catch (SQLException e) {
            return "<PyConnection at " + hashCode() + ">";
        }
    }

    /**
     * Method classDictInit
     *
     * @param dict
     */
    static public void classDictInit(PyObject dict) {

        dict.__setitem__("autocommit", new PyInteger(0));
        dict.__setitem__("__version__", Py.newString("$Revision$").__getslice__(Py.newInteger(11), Py.newInteger(-2), null));
        dict.__setitem__("close", new ConnectionFunc("close", 0, 0, 0, zxJDBC.getString("close")));
        dict.__setitem__("commit", new ConnectionFunc("commit", 1, 0, 0, zxJDBC.getString("commit")));
        dict.__setitem__("cursor", new ConnectionFunc("cursor", 2, 0, 4, zxJDBC.getString("cursor")));
        dict.__setitem__("rollback", new ConnectionFunc("rollback", 3, 0, 0, zxJDBC.getString("rollback")));
        dict.__setitem__("nativesql", new ConnectionFunc("nativesql", 4, 1, 1, zxJDBC.getString("nativesql")));

        // hide from python
        dict.__setitem__("initModule", null);
        dict.__setitem__("toString", null);
        dict.__setitem__("setConnection", null);
        dict.__setitem__("getPyClass", null);
        dict.__setitem__("connection", null);
        dict.__setitem__("classDictInit", null);
        dict.__setitem__("cursors", null);
    }

    /**
     * Sets the attribute.
     *
     * @param name
     * @param value
     */
    public void __setattr__(String name, PyObject value) {

        if ("autocommit".equals(name)) {
            try {
                if (this.supportsTransactions) {
                    this.connection.setAutoCommit(value.__nonzero__());
                }
            } catch (SQLException e) {
                throw zxJDBC.makeException(zxJDBC.DatabaseError, e);
            }

            return;
        }

        super.__setattr__(name, value);
    }

    /**
     * Finds the attribute.
     *
     * @param name the name of the attribute of interest
     * @return the value for the attribute of the specified name
     */
    public PyObject __findattr__(String name) {

        if ("autocommit".equals(name)) {
            try {
                return connection.getAutoCommit() ? Py.One : Py.Zero;
            } catch (SQLException e) {
                throw zxJDBC.makeException(zxJDBC.DatabaseError, e);
            }
        } else if ("dbname".equals(name)) {
            try {
                return Py.newString(this.connection.getMetaData().getDatabaseProductName());
            } catch (SQLException e) {
                throw zxJDBC.makeException(zxJDBC.DatabaseError, e);
            }
        } else if ("dbversion".equals(name)) {
            try {
                return Py.newString(this.connection.getMetaData().getDatabaseProductVersion());
            } catch (SQLException e) {
                throw zxJDBC.makeException(zxJDBC.DatabaseError, e);
            }
        } else if ("drivername".equals(name)) {
            try {
                return Py.newString(this.connection.getMetaData().getDriverName());
            } catch (SQLException e) {
                throw zxJDBC.makeException(zxJDBC.DatabaseError, e);
            }
        } else if ("driverversion".equals(name)) {
            try {
                return Py.newString(this.connection.getMetaData().getDriverVersion());
            } catch (SQLException e) {
                throw zxJDBC.makeException(zxJDBC.DatabaseError, e);
            }
        } else if ("url".equals(name)) {
            try {
                return Py.newString(this.connection.getMetaData().getURL());
            } catch (SQLException e) {
                throw zxJDBC.makeException(zxJDBC.DatabaseError, e);
            }
        } else if ("__connection__".equals(name)) {
            return Py.java2py(this.connection);
        } else if ("__cursors__".equals(name)) {
            return Py.java2py(Collections.unmodifiableSet(this.cursors));
        } else if ("__statements__".equals(name)) {
            return Py.java2py(Collections.unmodifiableSet(this.statements));
        } else if ("__methods__".equals(name)) {
            return __methods__;
        } else if ("__members__".equals(name)) {
            return __members__;
        } else if ("closed".equals(name)) {
            return Py.newBoolean(closed);
        }

        return super.__findattr__(name);
    }

    /**
     * Close the connection now (rather than whenever __del__ is called).
     * The connection will be unusable from this point forward; an Error
     * (or subclass) exception will be raised if any operation is attempted
     * with the connection. The same applies to all cursor objects trying
     * to use the connection.
     */
    public void close() {

        if (closed) {
            throw zxJDBC.makeException(zxJDBC.ProgrammingError, "connection is closed");
        }

        // mark ourselves closed now so that any callbacks we
        // get from closing down cursors and statements to not
        // try and modify our internal sets
        this.closed = true;

        synchronized (this.cursors) {

            // close the cursors
            for (Iterator i = this.cursors.iterator(); i.hasNext();) {
                ((PyCursor) i.next()).close();
            }

            this.cursors.clear();
        }

        synchronized (this.statements) {

            // close the cursors
            for (Iterator i = this.statements.iterator(); i.hasNext();) {
                ((PyStatement) i.next()).close();
            }

            this.statements.clear();
        }

        try {
            this.connection.close();
        } catch (SQLException e) {
            throw zxJDBC.makeException(e);
        }
    }

    /**
     * Commit any pending transaction to the database. Note that if the
     * database supports an auto-commit feature, this must be initially
     * off. An interface method may be provided to turn it back on.
     * <p/>
     * Database modules that do not support transactions should implement
     * this method with void functionality.
     */
    public void commit() {

        if (closed) {
            throw zxJDBC.makeException(zxJDBC.ProgrammingError, "connection is closed");
        }

        if (!this.supportsTransactions) {
            return;
        }

        try {
            this.connection.commit();
        } catch (SQLException e) {
            throw zxJDBC.makeException(e);
        }
    }

    /**
     * <i>This method is optional since not all databases provide transaction
     * support.</i>
     * <p/>
     * In case a database does provide transactions this method causes the database
     * to roll back to the start of any pending transaction. Closing a connection
     * without committing the changes first will cause an implicit rollback to be
     * performed.
     */
    public void rollback() {

        if (closed) {
            throw zxJDBC.makeException(zxJDBC.ProgrammingError, "connection is closed");
        }

        if (!this.supportsTransactions) {
            return;
        }

        try {
            this.connection.rollback();
        } catch (SQLException e) {
            throw zxJDBC.makeException(e);
        }
    }

    /**
     * Converts the given SQL statement into the system's native SQL grammar. A
     * driver may convert the JDBC sql grammar into its system's native SQL grammar
     * prior to sending it; this method returns the native form of the statement
     * that the driver would have sent.
     *
     * @param nativeSQL
     * @return the native form of this statement
     */
    public PyObject nativesql(PyObject nativeSQL) {

        if (closed) {
            throw zxJDBC.makeException(zxJDBC.ProgrammingError, "connection is closed");
        }

        if (nativeSQL == Py.None) {
            return Py.None;
        }

        try {
            return Py.newString(this.connection.nativeSQL(nativeSQL.__str__().toString()));
        } catch (SQLException e) {
            throw zxJDBC.makeException(e);
        }
    }

    /**
     * Return a new Cursor Object using the connection. If the database does not
     * provide a direct cursor concept, the module will have to emulate cursors
     * using other means to the extent needed by this specification.
     *
     * @return a new cursor using this connection
     */
    public PyCursor cursor() {
        return cursor(false);
    }

    /**
     * Return a new Cursor Object using the connection. If the database does not
     * provide a direct cursor concept, the module will have to emulate cursors
     * using other means to the extent needed by this specification.
     *
     * @param dynamicFetch if true, dynamically iterate the result
     * @return a new cursor using this connection
     */
    public PyCursor cursor(boolean dynamicFetch) {
        return this.cursor(dynamicFetch, Py.None, Py.None);
    }

    /**
     * Return a new Cursor Object using the connection. If the database does not
     * provide a direct cursor concept, the module will have to emulate cursors
     * using other means to the extent needed by this specification.
     *
     * @param dynamicFetch if true, dynamically iterate the result
     * @param rsType       the type of the underlying ResultSet
     * @param rsConcur     the concurrency of the underlying ResultSet
     * @return a new cursor using this connection
     */
    public PyCursor cursor(boolean dynamicFetch, PyObject rsType, PyObject rsConcur) {

        if (closed) {
            throw zxJDBC.makeException(zxJDBC.ProgrammingError, "connection is closed");
        }

        PyCursor cursor = new PyExtendedCursor(this, dynamicFetch, rsType, rsConcur);

        this.cursors.add(cursor);

        return cursor;
    }

    /**
     * Remove an open PyCursor.
     *
     * @param cursor
     */
    void remove(PyCursor cursor) {

        if (closed) {
            return;
        }

        this.cursors.remove(cursor);
    }

    /**
     * Method register
     *
     * @param statement statement
     */
    void add(PyStatement statement) {

        if (closed) {
            return;
        }

        this.statements.add(statement);
    }

    /**
     * Method contains
     *
     * @param statement statement
     * @return boolean
     */
    boolean contains(PyStatement statement) {

        if (closed) {
            return false;
        }

        return this.statements.contains(statement);
    }
}

class ConnectionFunc extends PyBuiltinFunctionSet {
    ConnectionFunc(String name, int index, int minargs, int maxargs, String doc) {
        super(name, index, minargs, maxargs, true, doc);
    }

    public PyObject __call__() {
        PyConnection c = (PyConnection) __self__;
        switch (index) {
            case 0:
                c.close();
                return Py.None;
            case 1:
                c.commit();
                return Py.None;
            case 2:
                return c.cursor();
            case 3:
                c.rollback();
                return Py.None;
            default :
                throw argCountError(0);
        }
    }

    public PyObject __call__(PyObject arg) {
        PyConnection c = (PyConnection) __self__;
        switch (index) {
            case 2:
                return c.cursor(arg.__nonzero__());
            case 4:
                return c.nativesql(arg);
            default :
                throw argCountError(1);
        }
    }

    public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3) {
        PyConnection c = (PyConnection) __self__;
        switch (index) {
            case 2:
                return c.cursor(arg1.__nonzero__(), arg2, arg3);
            default :
                throw argCountError(3);
        }
    }

    public PyObject __call__(PyObject[] args, String[] keywords) {
        PyConnection c = (PyConnection) __self__;
        PyArgParser parser = new PyArgParser(args, keywords);
        switch (index) {
            case 2:
                PyObject dynamic = parser.kw("dynamic", Py.None);
                PyObject rstype = parser.kw("rstype", Py.None);
                PyObject rsconcur = parser.kw("rsconcur", Py.None);

                dynamic = (parser.numArg() >= 1) ? parser.arg(0) : dynamic;
                rstype = (parser.numArg() >= 2) ? parser.arg(1) : rstype;
                rsconcur = (parser.numArg() >= 3) ? parser.arg(2) : rsconcur;

                return c.cursor(dynamic.__nonzero__(), rstype, rsconcur);

            default :
                throw argCountError(args.length);
        }
    }
}
