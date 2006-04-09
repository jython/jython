/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql;

import java.sql.SQLWarning;
import java.util.EventObject;

/**
 * An event signalling the a SQLWarning was encountered
 * while building results from a ResultSet.
 */
public class WarningEvent extends EventObject {

    private SQLWarning warning;

    public WarningEvent(Object source, SQLWarning warning) {

        super(source);

        this.warning = warning;
    }

    public SQLWarning getWarning() {
        return this.warning;
    }
}
