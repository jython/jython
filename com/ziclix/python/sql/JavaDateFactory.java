/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2003 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql;

import org.python.core.Py;
import org.python.core.PyObject;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 * Produce java.[util|sql] type dates.
 *
 * @author brian zimmer
 * @author last revised by $Author$
 * @version $Revision$
 */
public class JavaDateFactory implements DateFactory {

    /**
     * This function constructs an object holding a date value.
     *
     * @param year
     * @param month
     * @param day
     * @return PyObject
     */
    public PyObject Date(int year, int month, int day) {

        Calendar c = Calendar.getInstance();

        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month - 1);
        c.set(Calendar.DATE, day);

        return DateFromTicks(c.getTime().getTime() / 1000);
    }

    /**
     * This function constructs an object holding a time value.
     *
     * @param hour
     * @param minute
     * @param second
     * @return PyObject
     */
    public PyObject Time(int hour, int minute, int second) {

        Calendar c = Calendar.getInstance();

        c.set(Calendar.HOUR, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, second);

        return TimeFromTicks(c.getTime().getTime() / 1000);
    }

    /**
     * This function constructs an object holding a time stamp value.
     *
     * @param year
     * @param month
     * @param day
     * @param hour
     * @param minute
     * @param second
     * @return PyObject
     */
    public PyObject Timestamp(int year, int month, int day, int hour, int minute, int second) {

        Calendar c = Calendar.getInstance();

        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month - 1);
        c.set(Calendar.DATE, day);
        c.set(Calendar.HOUR, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, second);
        c.set(Calendar.MILLISECOND, 0);

        return TimestampFromTicks(c.getTime().getTime() / 1000);
    }

    /**
     * This function constructs an object holding a date value from the
     * given ticks value (number of seconds since the epoch; see the
     * documentation of the standard Python <i>time</i> module for details).
     * <p/>
     * <i>Note:</i> The DB API 2.0 spec calls for time in seconds since the epoch
     * while the Java Date object returns time in milliseconds since the epoch.
     * This module adheres to the python API and will therefore use time in
     * seconds rather than milliseconds, so adjust any Java code accordingly.
     *
     * @param ticks number of seconds since the epoch
     * @return PyObject
     */
    public PyObject DateFromTicks(long ticks) {

        Calendar c = Calendar.getInstance();

        c.setTime(new java.util.Date(ticks * 1000));
        c.set(Calendar.HOUR, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        return Py.java2py(new java.sql.Date(c.getTime().getTime()));
    }

    /**
     * This function constructs an object holding a time value from the
     * given ticks value (number of seconds since the epoch; see the
     * documentation of the standard Python <i>time</i> module for details).
     * <p/>
     * <i>Note:</i> The DB API 2.0 spec calls for time in seconds since the epoch
     * while the Java Date object returns time in milliseconds since the epoch.
     * This module adheres to the python API and will therefore use time in
     * seconds rather than milliseconds, so adjust any Java code accordingly.
     *
     * @param ticks number of seconds since the epoch
     * @return PyObject
     */
    public PyObject TimeFromTicks(long ticks) {
        return Py.java2py(new Time(ticks * 1000));
    }

    /**
     * This function constructs an object holding a time stamp value from
     * the given ticks value (number of seconds since the epoch; see the
     * documentation of the standard Python <i>time</i> module for details).
     * <p/>
     * <i>Note:</i> The DB API 2.0 spec calls for time in seconds since the epoch
     * while the Java Date object returns time in milliseconds since the epoch.
     * This module adheres to the python API and will therefore use time in
     * seconds rather than milliseconds, so adjust any Java code accordingly.
     *
     * @param ticks number of seconds since the epoch
     * @return PyObject
     */
    public PyObject TimestampFromTicks(long ticks) {
        return Py.java2py(new Timestamp(ticks * 1000));
    }
}
