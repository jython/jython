/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql.handler;

import com.ziclix.python.sql.FilterDataHandler;
import com.ziclix.python.sql.DataHandler;

import java.util.Map;
import java.util.HashMap;
import java.sql.Statement;
import java.sql.SQLException;
import java.lang.reflect.Method;

import org.python.core.PyObject;
import org.python.core.Py;

/**
 * Handle the rowid methods since the API is not available until JDBC 3.0.
 *
 * @author brian zimmer
 * @author last revised by $Author$
 * @version $Revision$
 */
public abstract class RowIdHandler extends FilterDataHandler {

  private static Map ROWIDS = new HashMap();
  private static Object CHECKED = new Object();

  public RowIdHandler(DataHandler handler) {
    super(handler);
  }

  /**
   * Return the name of the method that returns the last row id.  The
   * method can take no arguments but the return type is flexible and
   * will be figured out by the Jython runtime system.
   * @return
   */
  protected abstract String getRowIdMethodName();

  /**
   * Return the row id of the last insert statement.
   * @param stmt
   * @return an object representing the last row id
   * @throws SQLException
   */
  public PyObject getRowId(Statement stmt) throws SQLException {

    Class c = stmt.getClass();
    Object o = ROWIDS.get(c);

    if (o == null) {
      synchronized (ROWIDS) {
        try {
          o = c.getMethod(getRowIdMethodName(), null);
          ROWIDS.put(c, o);
        } catch (Throwable t) {
          ROWIDS.put(c, CHECKED);
        }
      }
    }

    if (!(o == null || o == CHECKED)) {
      try {
        return Py.java2py(((Method) o).invoke(stmt, null));
      } catch (Throwable t) {}
    }

    return super.getRowId(stmt);
  }

}
