package com.ziclix.python.sql;

import org.python.core.PyObject;
import org.python.core.Py;

/**
 * Provide an extensible way to create dates for zxJDBC.
 *
 * @author brian zimmer
 * @author last revised by $Author$
 * @version $Revision$
 */
public interface DateFactory {

    /**
	 * This function constructs an object holding a date value.
	 *
	 * @param year
	 * @param month
	 * @param day
	 *
	 * @return PyObject
	 */
	public PyObject Date(int year, int month, int day);

	/**
	 * This function constructs an object holding a time value.
	 *
	 * @param hour
	 * @param minute
	 * @param second
	 *
	 * @return PyObject
	 */
	public PyObject Time(int hour, int minute, int second);

	/**
	 * This function constructs an object holding a time stamp value.
	 *
	 * @param year
	 * @param month
	 * @param day
	 * @param hour
	 * @param minute
	 * @param second
	 *
	 * @return PyObject
	 */
	public PyObject Timestamp(int year, int month, int day, int hour, int minute, int second);

	/**
	 * This function constructs an object holding a date value from the
	 * given ticks value (number of seconds since the epoch; see the
	 * documentation of the standard Python <i>time</i> module for details).
	 *
	 * <i>Note:</i> The DB API 2.0 spec calls for time in seconds since the epoch
	 * while the Java Date object returns time in milliseconds since the epoch.
	 * This module adheres to the python API and will therefore use time in
	 * seconds rather than milliseconds, so adjust any Java code accordingly.
	 *
	 * @param ticks number of seconds since the epoch
	 *
	 * @return PyObject
	 */
	public PyObject DateFromTicks(long ticks);

	/**
	 * This function constructs an object holding a time value from the
	 * given ticks value (number of seconds since the epoch; see the
	 * documentation of the standard Python <i>time</i> module for details).
	 *
	 * <i>Note:</i> The DB API 2.0 spec calls for time in seconds since the epoch
	 * while the Java Date object returns time in milliseconds since the epoch.
	 * This module adheres to the python API and will therefore use time in
	 * seconds rather than milliseconds, so adjust any Java code accordingly.
	 *
	 * @param ticks number of seconds since the epoch
	 *
	 * @return PyObject
	 */
	public PyObject TimeFromTicks(long ticks);

	/**
	 * This function constructs an object holding a time stamp value from
	 * the given ticks value (number of seconds since the epoch; see the
	 * documentation of the standard Python <i>time</i> module for details).
	 *
	 * <i>Note:</i> The DB API 2.0 spec calls for time in seconds since the epoch
	 * while the Java Date object returns time in milliseconds since the epoch.
	 * This module adheres to the python API and will therefore use time in
	 * seconds rather than milliseconds, so adjust any Java code accordingly.
	 *
	 * @param ticks number of seconds since the epoch
	 *
	 * @return PyObject
	 */
	public PyObject TimestampFromTicks(long ticks);

}
