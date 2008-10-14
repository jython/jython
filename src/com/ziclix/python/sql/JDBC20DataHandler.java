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
import org.python.core.PyFile;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.util.StringUtil;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Support for JDBC 2.x type mappings, including Arrays, CLOBs and BLOBs.
 *
 * @author brian zimmer
 * @author last revised by $Author$
 * @version $Revision$
 */
public class JDBC20DataHandler extends FilterDataHandler {

    /**
     * Handle JDBC 2.0 datatypes.
     */
    public JDBC20DataHandler(DataHandler datahandler) {
        super(datahandler);
    }

    /**
     * Handle CLOBs and BLOBs.
     *
     * @param stmt
     * @param index
     * @param object
     * @param type
     * @throws SQLException
     */
    public void setJDBCObject(PreparedStatement stmt, int index, PyObject object, int type) throws SQLException {

        if (DataHandler.checkNull(stmt, index, object, type)) {
            return;
        }

        switch (type) {

            case Types.CLOB:
                if (object instanceof PyFile) {
                    object = ((PyFile) object).read();
                }

                String clob = (String) object.__tojava__(String.class);
                int length = clob.length();
                InputStream stream = new ByteArrayInputStream(StringUtil.toBytes(clob));

                stream = new BufferedInputStream(stream);

                stmt.setBinaryStream(index, stream, length);

                // Reader reader = new StringReader(clob);
                // reader = new BufferedReader(reader);
                // stmt.setCharacterStream(index, reader, length);
                break;

            case Types.BLOB:
                byte[] lob = null;
                Object jobject = null;

                if (object instanceof PyFile) {
                    jobject = object.__tojava__(InputStream.class);
                } else {
                    jobject = object.__tojava__(Object.class);
                }

                // it really is unfortunate that I need to send the length of the stream
                if (jobject instanceof InputStream) {
                    lob = DataHandler.read(new BufferedInputStream((InputStream) jobject));
                } else if (jobject instanceof byte[]) {
                    lob = (byte[]) jobject;
                }

                if (lob != null) {
                    stmt.setBytes(index, lob);

                    break;
                }
            default :
                super.setJDBCObject(stmt, index, object, type);
                break;
        }
    }

    /**
     * Get the object from the result set.
     *
     * @param set
     * @param col
     * @param type
     * @return a Python object
     * @throws SQLException
     */
    public PyObject getPyObject(ResultSet set, int col, int type) throws SQLException {

        PyObject obj = Py.None;

        switch (type) {

            case Types.NUMERIC:
            case Types.DECIMAL:

                // in JDBC 2.0, use of a scale is deprecated
                try {
                    BigDecimal bd = set.getBigDecimal(col);

                    obj = (bd == null) ? Py.None : Py.newFloat(bd.doubleValue());
                } catch (SQLException e) {
                    obj = super.getPyObject(set, col, type);
                }
                break;

            case Types.CLOB:

                /*
                 * It seems some drivers (well at least Informix) don't clean up after themselves
                 * if the Clob is requested.  The engine keeps a handle to an open table for each
                 * row requested and cleans up fully only when the ResultSet or Connection is closed.
                 * While this generally will never be noticed because the number of CLOBs or BLOBs
                 * queried will likely be small in the event a large number are queried, it is a huge
                 * problem.  So, handle it as low as possible by managing the stream directly.  I've
                 * decided to leave this in the generic JDBC20 handler because it works for all engines
                 * I've tested and seems to perform quite well to boot.
                 */
                Reader reader = null;

                try {
                    InputStream stream = set.getBinaryStream(col);

                    if (stream == null) {
                        obj = Py.None;
                    } else {
                        reader = new InputStreamReader(stream);
                        reader = new BufferedReader(reader);
                        obj = Py.newString(DataHandler.read(reader));
                    }
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Exception e) {
                        }
                    }
                }
                break;

            case Types.BLOB:
                Blob blob = set.getBlob(col);

                if (blob == null) {
                    obj = Py.None;
                } else {
                    InputStream stream = null;

                    try {
                        stream = blob.getBinaryStream();
                        stream = new BufferedInputStream(stream);
                        obj = Py.java2py(DataHandler.read(stream));
                    } finally {
                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (Exception e) {
                            }
                        }
                    }
                }
                break;

            case Types.ARRAY:
                obj = Py.java2py(set.getArray(col).getArray());
                break;

            default :
                return super.getPyObject(set, col, type);
        }

        return (set.wasNull() || (obj == null)) ? Py.None : obj;
    }
}
