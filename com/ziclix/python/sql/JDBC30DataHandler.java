/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2002 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql;

import org.python.core.PyObject;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ParameterMetaData;

/**
 * Support for JDBC 3.x additions, notably ParameterMetaData.
 *
 * @author brian zimmer
 * @author last revised by $Author$
 * @version $Revision$
 */
public class JDBC30DataHandler extends FilterDataHandler {

  static {
    try {
      Class.forName("java.sql.ParameterMetaData");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("JDBC3.0 required to use this DataHandler");
    }
  }

  /**
   * Handle JDBC 3.0 additions.
   *
   */
  public JDBC30DataHandler(DataHandler datahandler) {
      super(datahandler);
  }

  /**
   * Use ParameterMetaData if available to dynamically cast to the appropriate
   * JDBC type.
   *
   * @param stmt the prepared statement
   * @param index the index currently being used
   * @param object the object to be set on the statement
   * @throws SQLException
   */
  public void setJDBCObject(PreparedStatement stmt, int index, PyObject object) throws SQLException {
    ParameterMetaData meta = stmt.getParameterMetaData();
    super.setJDBCObject(stmt, index, object, meta.getParameterType(index));
  }
}

