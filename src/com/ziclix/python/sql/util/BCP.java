/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql.util;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyBuiltinMethodSet;
import org.python.core.PyClass;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import com.ziclix.python.sql.PyConnection;
import com.ziclix.python.sql.zxJDBC;
import com.ziclix.python.sql.pipe.Pipe;
import com.ziclix.python.sql.pipe.db.DBSink;
import com.ziclix.python.sql.pipe.db.DBSource;

/**
 * A class to perform efficient Bulk CoPy of database tables.
 */
public class BCP extends PyObject implements ClassDictInit {

    /**
     * Field sourceDH, destDH
     */
    protected Class sourceDH, destDH;

    /**
     * Field batchsize, queuesize
     */
    protected int batchsize, queuesize;

    /**
     * Field source, destination
     */
    protected PyConnection source, destination;

    /**
     * The source connection will produce the rows while the destination
     * connection will consume the rows and coerce as necessary for the
     * destination database.
     */
    public BCP(PyConnection source, PyConnection destination) {
        this(source, destination, -1);
    }

    /**
     * The source connection will produce the rows while the destination
     * connection will consume the rows and coerce as necessary for the
     * destination database.
     *
     * @param batchsize used to batch the inserts on the destination
     */
    public BCP(PyConnection source, PyConnection destination, int batchsize) {

        this.source = source;
        this.destination = destination;
        this.destDH = null;
        this.sourceDH = null;
        this.batchsize = batchsize;
        this.queuesize = 0;
    }
    
    /**
     * Field __methods__
     */
    protected static PyList __methods__;

    /**
     * Field __members__
     */
    protected static PyList __members__;

    static {
        PyObject[] m = new PyObject[1];

        m[0] = new PyString("bcp");
        __methods__ = new PyList(m);
        m = new PyObject[6];
        m[0] = new PyString("source");
        m[1] = new PyString("destination");
        m[2] = new PyString("batchsize");
        m[3] = new PyString("queuesize");
        m[4] = new PyString("sourceDataHandler");
        m[5] = new PyString("destinationDataHandler");
        __members__ = new PyList(m);
    }

    /**
     * String representation of the object.
     *
     * @return a string representation of the object.
     */
    public String toString() {
        return "<BCP object instance at " + hashCode() + ">";
    }

    /**
     * Sets the attribute name to value.
     *
     * @param name
     * @param value
     */
    public void __setattr__(String name, PyObject value) {

        if ("destinationDataHandler".equals(name)) {
            this.destDH = (Class) value.__tojava__(Class.class);
        } else if ("sourceDataHandler".equals(name)) {
            this.sourceDH = (Class) value.__tojava__(Class.class);
        } else if ("batchsize".equals(name)) {
            this.batchsize = ((Number) value.__tojava__(Number.class)).intValue();
        } else if ("queuesize".equals(name)) {
            this.queuesize = ((Number) value.__tojava__(Number.class)).intValue();
        } else {
            super.__setattr__(name, value);
        }
    }

    /**
     * Gets the value of the attribute name.
     *
     * @param name
     * @return the attribute for the given name
     */
    public PyObject __findattr_ex__(String name) {

        if ("destinationDataHandler".equals(name)) {
            return Py.java2py(this.destDH);
        } else if ("sourceDataHandler".equals(name)) {
            return Py.java2py(this.sourceDH);
        } else if ("batchsize".equals(name)) {
            return Py.newInteger(this.batchsize);
        } else if ("queuesize".equals(name)) {
            return Py.newInteger(this.queuesize);
        }

        return super.__findattr_ex__(name);
    }

    /**
     * Initializes the object's namespace.
     *
     * @param dict
     */
    static public void classDictInit(PyObject dict) {

        dict.__setitem__("__version__", Py.newString("$Revision$").__getslice__(Py.newInteger(11), Py.newInteger(-2), null));
        dict.__setitem__("bcp", new BCPFunc("bcp", 0, 1, 2, zxJDBC.getString("bcp")));
        dict.__setitem__("batchsize", Py.newString(zxJDBC.getString("batchsize")));
        dict.__setitem__("queuesize", Py.newString(zxJDBC.getString("queuesize")));

        // hide from python
        dict.__setitem__("classDictInit", null);
        dict.__setitem__("toString", null);
        dict.__setitem__("PyClass", null);
        dict.__setitem__("getPyClass", null);
        dict.__setitem__("sourceDH", null);
        dict.__setitem__("destDH", null);
    }

    /**
     * Bulkcopy data from one database to another.
     *
     * @param fromTable the table in question on the source database
     * @param where     an optional where clause, defaults to '(1=1)' if null
     * @param params    optional params to substituted in the where clause
     * @param include   the columns to be queried from the source, '*' if None
     * @param exclude   the columns to be excluded from insertion on the destination, all if None
     * @param toTable   if non-null, the table in the destination db, otherwise the same table name as the source
     * @param bindings  the optional bindings for the destination, this allows morphing of types during the copy
     * @return the count of the total number of rows bulk copied, -1 if the query returned no rows
     */
    protected PyObject bcp(String fromTable, String where, PyObject params, PyObject include, PyObject exclude, String toTable, PyObject bindings) {

        Pipe pipe = new Pipe();
        String _toTable = (toTable == null) ? fromTable : toTable;
        DBSource source = new DBSource(this.source, sourceDH, fromTable, where, include, params);
        DBSink sink = new DBSink(this.destination, destDH, _toTable, exclude, bindings, this.batchsize);

        return pipe.pipe(source, sink).__sub__(Py.newInteger(1));
    }
}

/**
 * @author last modified by $Author$
 * @version $Revision$
 * @date last modified on $Date$
 * @copyright 2001 brian zimmer
 */
class BCPFunc extends PyBuiltinMethodSet {

    BCPFunc(String name, int index, int argcount, String doc) {
        this(name, index, argcount, argcount, doc);
    }

    BCPFunc(String name, int index, int minargs, int maxargs, String doc) {
        super(name, index, minargs, maxargs, doc, BCP.class);
    }

    /**
     * Method __call__
     *
     * @param arg
     * @return PyObject
     */
    public PyObject __call__(PyObject arg) {

        BCP bcp = (BCP) __self__;

        switch (index) {

            case 0:
                String table = (String) arg.__tojava__(String.class);

                if (table == null) {
                    throw Py.ValueError(zxJDBC.getString("invalidTableName"));
                }

                PyObject count = bcp.bcp(table, null, Py.None, Py.None, Py.None, null, Py.None);

                return count;

            default :
                throw info.unexpectedCall(1, false);
        }
    }

    public PyObject __call__(PyObject arga, PyObject argb) {

        BCP bcp = (BCP) __self__;

        switch (index) {

            case 0:
                String table = (String) arga.__tojava__(String.class);

                if (table == null) {
                    throw Py.ValueError(zxJDBC.getString("invalidTableName"));
                }

                String where = (String) argb.__tojava__(String.class);
                PyObject count = bcp.bcp(table, where, Py.None, Py.None, Py.None, null, Py.None);

                return count;

            default :
                throw info.unexpectedCall(2, false);
        }
    }

    public PyObject __call__(PyObject arga, PyObject argb, PyObject argc) {

        BCP bcp = (BCP) __self__;

        switch (index) {

            case 0:
                String table = (String) arga.__tojava__(String.class);

                if (table == null) {
                    throw Py.ValueError(zxJDBC.getString("invalidTableName"));
                }

                String where = (String) argb.__tojava__(String.class);
                PyObject count = bcp.bcp(table, where, argc, Py.None, Py.None, null, Py.None);

                return count;

            default :
                throw info.unexpectedCall(3, false);
        }
    }

    public PyObject __call__(PyObject[] args, String[] keywords) {

        BCP bcp = (BCP) __self__;

        switch (index) {

            case 0:

                /*
                 * B.bcp(table, [where=None, params=None, include=None, exclude=None, toTable=None, bindings=None])
                 */
                String where = null;
                PyObject params = Py.None;
                PyArgParser parser = new PyArgParser(args, keywords);
                String table = (String) parser.arg(0, Py.None).__tojava__(String.class);

                if (table == null) {
                    throw Py.ValueError(zxJDBC.getString("invalidTableName"));
                }

                // 'where' can be the second argument or a keyword
                if (parser.numArg() >= 2) {
                    where = (String) parser.arg(1, Py.None).__tojava__(String.class);
                }

                if (where == null) {
                    where = (String) parser.kw("where", Py.None).__tojava__(String.class);
                }

                // 'params' can be the third argument or a keyword
                if (parser.numArg() >= 3) {
                    params = parser.arg(2, Py.None);
                }

                if (params == Py.None) {
                    params = parser.kw("params", Py.None);
                }

                String toTable = (String) parser.kw("toTable", Py.None).__tojava__(String.class);
                PyObject include = parser.kw("include", Py.None);
                PyObject exclude = parser.kw("exclude", Py.None);
                PyObject bindings = parser.kw("bindings", Py.None);
                PyObject count = bcp.bcp(table, where, params, include, exclude, toTable, bindings);

                return count;

            default :
                throw info.unexpectedCall(3, false);
        }
    }
}
