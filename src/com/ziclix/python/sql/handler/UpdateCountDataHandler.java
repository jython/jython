/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql.handler;

import com.ziclix.python.sql.DataHandler;
import com.ziclix.python.sql.FilterDataHandler;
import com.ziclix.python.sql.zxJDBC;
import org.python.core.Py;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * A data handler that keeps track of the update count for each execution of a
 * Statement.
 * <p/>
 * <p><b>Note:</b> MySql does not return the correct count for a
 * <a href="http://www.mysql.com/doc/D/E/DELETE.html">delete</a> statement that has
 * no <code>where</code> clause. Therefore, to assure the correct update count is returned,
 * either include a <code>where</code> clause, or understand that the value will always be
 * <code>0</code>.</p>
 *
 * @author brian zimmer
 * @author last revised by $Author$
 * @version $Revision$
 * @see java.sql.Statement#getUpdateCount()
 */
public class UpdateCountDataHandler extends FilterDataHandler {

    private static boolean once = false;

    /**
     * The update count for the last executed statement.
     */
    public int updateCount;

    /**
     * Handle capturing the update count.  The initial value of the updateCount is
     * <code>-1</code>.
     */
    public UpdateCountDataHandler(DataHandler datahandler) {

        super(datahandler);

        if (!once) {
            Py.writeError("UpdateCountDataHandler", zxJDBC.getString("updateCountDeprecation"));
            once = true;
        }

        this.updateCount = -1;
    }

    /**
     * Sets the update count to <code>-1</code> prior to the statement being executed.
     */
    public void preExecute(Statement stmt) throws SQLException {

        super.preExecute(stmt);

        this.updateCount = -1;
    }

    /**
     * Gets the update count from the statement after successfully executing.
     */
    public void postExecute(Statement stmt) throws SQLException {

        super.postExecute(stmt);

        this.updateCount = stmt.getUpdateCount();
    }
}
