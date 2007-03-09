/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import org.python.core.Py;
import org.python.core.PyBuiltinMethodSet;
import org.python.core.PyClass;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;

/**
 * A cursor with extensions to the DB API 2.0.
 *
 * @author brian zimmer
 * @author last revised by $Author$
 * @version $Revision$
 */
public class PyExtendedCursor extends PyCursor {

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
        PyObject[] m = new PyObject[9];

        m[0] = new PyString("tables");
        m[1] = new PyString("columns");
        m[2] = new PyString("primarykeys");
        m[3] = new PyString("foreignkeys");
        m[4] = new PyString("procedures");
        m[5] = new PyString("procedurecolumns");
        m[6] = new PyString("statistics");
        m[7] = new PyString("bestrow");
        m[8] = new PyString("versioncolumns");
        __methods__ = new PyList(m);

        __methods__.extend(PyCursor.__methods__);

        m = new PyObject[0];
        __members__ = new PyList(m);

        __members__.extend(PyCursor.__members__);
    }

    /**
     * Constructor PyExtendedCursor
     *
     * @param connection
     */
    PyExtendedCursor(PyConnection connection) {
        super(connection);
    }

    /**
     * Constructor PyExtendedCursor
     *
     * @param connection
     * @param dynamicFetch
     */
    PyExtendedCursor(PyConnection connection, boolean dynamicFetch) {
        super(connection, dynamicFetch);
    }

    /**
     * Constructor PyExtendedCursor
     *
     * @param connection
     * @param dynamicFetch
     * @param rsType
     * @param rsConcur
     */
    PyExtendedCursor(PyConnection connection, boolean dynamicFetch, PyObject rsType, PyObject rsConcur) {
        super(connection, dynamicFetch, rsType, rsConcur);
    }

    /**
     * String representation of the object.
     *
     * @return a string representation of the object.
     */
    public String toString() {
        return "<PyExtendedCursor object instance at " + Py.id(this) + ">";
    }

    /**
     * Initializes the module.
     *
     * @param dict
     */
    static public void classDictInit(PyObject dict) {

        PyCursor.classDictInit(dict);
        dict.__setitem__("__version__", Py.newString("$Revision$").__getslice__(Py.newInteger(11), Py.newInteger(-2), null));
        dict.__setitem__("tables", new ExtendedCursorFunc("tables", 100, 4, 4, "query for table information"));
        dict.__setitem__("columns", new ExtendedCursorFunc("columns", 101, 4, 4, "query for column information"));
        dict.__setitem__("primarykeys", new ExtendedCursorFunc("primarykeys", 102, 3, 3, "query for primary keys"));
        dict.__setitem__("foreignkeys", new ExtendedCursorFunc("foreignkeys", 103, 6, 6, "query for foreign keys"));
        dict.__setitem__("procedures", new ExtendedCursorFunc("procedures", 104, 3, 3, "query for procedures"));
        dict.__setitem__("procedurecolumns", new ExtendedCursorFunc("procedurecolumns", 105, 4, 4, "query for procedures columns"));
        dict.__setitem__("statistics", new ExtendedCursorFunc("statistics", 106, 5, 5, "description of a table's indices and statistics"));
        dict.__setitem__("gettypeinfo", new ExtendedCursorFunc("gettypeinfo", 107, 0, 1, "query for sql type info"));
        dict.__setitem__("gettabletypeinfo", new ExtendedCursorFunc("gettabletypeinfo", 108, 0, 1, "query for table types"));
        dict.__setitem__("bestrow", new ExtendedCursorFunc("bestrow", 109, 3, 3, "optimal set of columns that uniquely identifies a row"));
        dict.__setitem__("versioncolumns", new ExtendedCursorFunc("versioncolumns", 110, 3, 3, "columns that are automatically updated when any value in a row is updated"));

        // hide from python
        dict.__setitem__("classDictInit", null);
        dict.__setitem__("toString", null);
    }

    /**
     * Finds the attribute.
     *
     * @param name the name of the attribute of interest
     * @return the value for the attribute of the specified name
     */
    public PyObject __findattr__(String name) {

        if ("__methods__".equals(name)) {
            return __methods__;
        } else if ("__members__".equals(name)) {
            return __members__;
        }

        return super.__findattr__(name);
    }

    /**
     * Only table descriptions matching the catalog, schema, table name and type
     * criteria are returned. They are ordered by TABLE_TYPE, TABLE_SCHEM and
     * TABLE_NAME.
     *
     * @param qualifier
     * @param owner
     * @param table
     * @param type
     */
    protected void tables(PyObject qualifier, PyObject owner, PyObject table, PyObject type) {

        clear();

        String q = getMetaDataName(qualifier);
        String o = getMetaDataName(owner);
        String t = getMetaDataName(table);
        String[] y = null;

        // postgresql interprets the types to be uppercase exclusively
        // so we'll force this on everyone else as well
        if (type != Py.None) {
            String typeName = null;

            if (isSeq(type)) {
                int len = type.__len__();

                y = new String[len];

                for (int i = 0; i < len; i++) {
                    typeName = getMetaDataName(type.__getitem__(i));
                    y[i] = (typeName == null) ? null : typeName.toUpperCase();
                }
            } else {
                typeName = getMetaDataName(type.__getitem__(type));
                y = new String[]{(typeName == null) ? null : typeName.toUpperCase()};
            }
        }

        try {
            this.fetch.add(getMetaData().getTables(q, o, t, y));
        } catch (SQLException e) {
            throw zxJDBC.makeException(e);
        }
    }

    /**
     * Returns the columns for a table.
     *
     * @param qualifier
     * @param owner
     * @param table
     * @param column
     */
    protected void columns(PyObject qualifier, PyObject owner, PyObject table, PyObject column) {

        clear();

        String q = getMetaDataName(qualifier);
        String o = getMetaDataName(owner);
        String t = getMetaDataName(table);
        String c = getMetaDataName(column);

        try {
            this.fetch.add(getMetaData().getColumns(q, o, t, c));
        } catch (SQLException e) {
            throw zxJDBC.makeException(e);
        }
    }

    /**
     * Gets a description of the stored procedures available for the qualifier and owner.
     *
     * @param qualifier
     * @param owner
     * @param procedure
     */
    protected void procedures(PyObject qualifier, PyObject owner, PyObject procedure) {

        clear();

        String q = getMetaDataName(qualifier);
        String o = getMetaDataName(owner);
        String p = getMetaDataName(procedure);

        try {
            this.fetch.add(getMetaData().getProcedures(q, o, p));
        } catch (SQLException e) {
            throw zxJDBC.makeException(e);
        }
    }

    /**
     * Gets the columns for the procedure.
     *
     * @param qualifier
     * @param owner
     * @param procedure
     * @param column
     */
    protected void procedurecolumns(PyObject qualifier, PyObject owner, PyObject procedure, PyObject column) {

        clear();

        String q = getMetaDataName(qualifier);
        String o = getMetaDataName(owner);
        String p = getMetaDataName(procedure);
        String c = getMetaDataName(column);

        try {
            this.fetch.add(getMetaData().getProcedureColumns(q, o, p, c));
        } catch (SQLException e) {
            throw zxJDBC.makeException(e);
        }
    }

    /**
     * Gets a description of a table's primary key columns. They are ordered by
     * COLUMN_NAME.
     *
     * @param qualifier a schema name
     * @param owner     an owner name
     * @param table     a table name
     */
    protected void primarykeys(PyObject qualifier, PyObject owner, PyObject table) {

        clear();

        String q = getMetaDataName(qualifier);
        String o = getMetaDataName(owner);
        String t = getMetaDataName(table);

        try {
            this.fetch.add(getMetaData().getPrimaryKeys(q, o, t));
        } catch (SQLException e) {
            throw zxJDBC.makeException(e);
        }
    }

    /**
     * Gets a description of the foreign key columns in the foreign key table
     * that reference the primary key columns of the primary key table (describe
     * how one table imports another's key.) This should normally return a single
     * foreign key/primary key pair (most tables only import a foreign key from a
     * table once.) They are ordered by FKTABLE_CAT, FKTABLE_SCHEM, FKTABLE_NAME,
     * and KEY_SEQ.
     *
     * @param primaryQualifier
     * @param primaryOwner
     * @param primaryTable
     * @param foreignQualifier
     * @param foreignOwner
     * @param foreignTable
     */
    protected void foreignkeys(PyObject primaryQualifier, PyObject primaryOwner, PyObject primaryTable, PyObject foreignQualifier, PyObject foreignOwner, PyObject foreignTable) {

        clear();

        String pq = getMetaDataName(primaryQualifier);
        String po = getMetaDataName(primaryOwner);
        String pt = getMetaDataName(primaryTable);
        String fq = getMetaDataName(foreignQualifier);
        String fo = getMetaDataName(foreignOwner);
        String ft = getMetaDataName(foreignTable);

        try {
            this.fetch.add(getMetaData().getCrossReference(pq, po, pt, fq, fo, ft));
        } catch (SQLException e) {
            throw zxJDBC.makeException(e);
        }
    }

    /**
     * Gets a description of a table's indices and statistics. They are ordered by
     * NON_UNIQUE, TYPE, INDEX_NAME, and ORDINAL_POSITION.
     *
     * @param qualifier
     * @param owner
     * @param table
     * @param unique
     * @param accuracy
     */
    protected void statistics(PyObject qualifier, PyObject owner, PyObject table, PyObject unique, PyObject accuracy) {

        clear();

        Set skipCols = new HashSet();

        skipCols.add(new Integer(12));

        String q = getMetaDataName(qualifier);
        String o = getMetaDataName(owner);
        String t = getMetaDataName(table);
        boolean u = unique.__nonzero__();
        boolean a = accuracy.__nonzero__();

        try {
            this.fetch.add(getMetaData().getIndexInfo(q, o, t, u, a), skipCols);
        } catch (SQLException e) {
            throw zxJDBC.makeException(e);
        }
    }

    /**
     * Gets a description of the type information for a given type.
     *
     * @param type data type for which to provide information
     */
    protected void typeinfo(PyObject type) {

        clear();

        Set skipCols = new HashSet();

        skipCols.add(new Integer(16));
        skipCols.add(new Integer(17));

        try {
            this.fetch.add(getMetaData().getTypeInfo(), skipCols);
        } catch (SQLException e) {
            throw zxJDBC.makeException(e);
        }

        // if the type is non-null, then trim the result list down to that type only
        // if(type != null &&!Py.None.equals(type)) {
        // for(int i = 0; i < ((PyObject) this.results.get(0)).__len__(); i++) {
        // PyObject row = ((PyObject) this.results.get(0)).__getitem__(new PyInteger(i));
        // PyObject sqlType = row.__getitem__(new PyInteger(1));
        // if(type.equals(sqlType)) {
        // this.results.remove(0);
        // this.results.add(0, new PyList(new PyObject[] {
        // row
        // }));
        // }
        // }
        // }
    }

    /**
     * Gets a description of possible table types.
     */
    protected void tabletypeinfo() {

        clear();

        try {
            this.fetch.add(getMetaData().getTableTypes());
        } catch (SQLException e) {
            throw zxJDBC.makeException(e);
        }
    }

    /**
     * Gets a description of a table's optimal set of columns that uniquely
     * identifies a row. They are ordered by SCOPE.
     *
     * @param qualifier
     * @param owner
     * @param table
     */
    protected void bestrow(PyObject qualifier, PyObject owner, PyObject table) {

        clear();

        String c = getMetaDataName(qualifier);
        String s = getMetaDataName(owner);
        String t = getMetaDataName(table);
        int p = DatabaseMetaData.bestRowSession; // scope
        boolean n = true; // nullable

        try {
            this.fetch.add(getMetaData().getBestRowIdentifier(c, s, t, p, n));
        } catch (SQLException e) {
            throw zxJDBC.makeException(e);
        }
    }

    /**
     * Gets a description of a table's columns that are automatically
     * updated when any value in a row is updated. They are unordered.
     *
     * @param qualifier a schema name
     * @param owner     an owner name
     * @param table     a table name
     */
    protected void versioncolumns(PyObject qualifier, PyObject owner, PyObject table) {

        clear();

        String q = getMetaDataName(qualifier);
        String o = getMetaDataName(owner);
        String t = getMetaDataName(table);

        try {
            this.fetch.add(getMetaData().getVersionColumns(q, o, t));
        } catch (SQLException e) {
            throw zxJDBC.makeException(e);
        }
    }

    /**
     * Method getMetaDataName
     *
     * @param name
     * @return String
     */
    protected String getMetaDataName(PyObject name) {

        if (name == Py.None) {
            return null;
        }

        String string = name.__str__().toString();

        // see if the driver can help us
        try {
            if (getMetaData().storesLowerCaseIdentifiers()) {
                return string.toLowerCase();
            } else if (getMetaData().storesUpperCaseIdentifiers()) {
                return string.toUpperCase();
            }
        } catch (SQLException e) {
        }

        // well we don't know yet so give it to the datahandler
        return datahandler.getMetaDataName(name);
    }
}

class ExtendedCursorFunc extends PyBuiltinMethodSet {

    ExtendedCursorFunc(String name, int index, int argcount, String doc) {
        super(name, index, argcount, argcount, doc);
    }

    ExtendedCursorFunc(String name, int index, int minargs, int maxargs, String doc) {
        super(name, index, minargs, maxargs, doc);
    }

    public PyObject __call__() {

        PyExtendedCursor cursor = (PyExtendedCursor) __self__;

        switch (index) {

            case 107:
                cursor.typeinfo(Py.None);

                return Py.None;

            case 108:
                cursor.tabletypeinfo();

                return Py.None;

            default :
                throw info.unexpectedCall(0, false);
        }
    }

    public PyObject __call__(PyObject arga) {

        PyExtendedCursor cursor = (PyExtendedCursor) __self__;

        switch (index) {

            case 107:
                cursor.typeinfo(arga);

                return Py.None;

            default :
                throw info.unexpectedCall(1, false);
        }
    }

    public PyObject __call__(PyObject arga, PyObject argb, PyObject argc) {

        PyExtendedCursor cursor = (PyExtendedCursor) __self__;

        switch (index) {

            case 102:
                cursor.primarykeys(arga, argb, argc);

                return Py.None;

            case 104:
                cursor.procedures(arga, argb, argc);

                return Py.None;

            case 109:
                cursor.bestrow(arga, argb, argc);

                return Py.None;

            case 110:
                cursor.versioncolumns(arga, argb, argc);

                return Py.None;

            default :
                throw info.unexpectedCall(3, false);
        }
    }

    public PyObject fancyCall(PyObject[] args) {

        PyExtendedCursor cursor = (PyExtendedCursor) __self__;

        switch (index) {

            case 103:
                cursor.foreignkeys(args[0], args[1], args[2], args[3], args[4], args[5]);

                return Py.None;

            case 106:
                cursor.statistics(args[0], args[1], args[2], args[3], args[4]);

                return Py.None;

            default :
                throw info.unexpectedCall(args.length, true);
        }
    }

    public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3, PyObject arg4) {

        PyExtendedCursor cursor = (PyExtendedCursor) __self__;

        switch (index) {

            case 100:
                cursor.tables(arg1, arg2, arg3, arg4);

                return Py.None;

            case 101:
                cursor.columns(arg1, arg2, arg3, arg4);

                return Py.None;

            case 105:
                cursor.procedurecolumns(arg1, arg2, arg3, arg4);

                return Py.None;

            default :
                throw info.unexpectedCall(4, false);
        }
    }
}
