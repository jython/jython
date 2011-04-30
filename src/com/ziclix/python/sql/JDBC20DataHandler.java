/*
 * Jython Database Specification API 2.0
 *
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.python.core.Py;
import org.python.core.PyFile;
import org.python.core.PyObject;
import org.python.core.util.StringUtil;

/**
 * Support for JDBC 2.x type mappings, including Arrays, CLOBs and BLOBs.
 *
 * @author brian zimmer
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
    @SuppressWarnings("fallthrough")
    @Override
    public void setJDBCObject(PreparedStatement stmt, int index, PyObject object, int type)
        throws SQLException {
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
                    lob = read((InputStream) jobject);
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
    @Override
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
                Reader reader = set.getCharacterStream(col);
                obj = reader == null ? Py.None : Py.newUnicode(read(reader));
                break;

            case Types.BLOB:
                Blob blob = set.getBlob(col);
                obj = blob == null ? Py.None : Py.java2py(read(blob.getBinaryStream()));
                break;

            case Types.ARRAY:
                Array array = set.getArray(col);
                obj = array == null ? Py.None : Py.java2py(array.getArray());
                break;

            default :
                return super.getPyObject(set, col, type);
        }

        return (set.wasNull() || (obj == null)) ? Py.None : obj;
    }
}
