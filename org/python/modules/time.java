// Copyright © Corporation for National Research Initiatives

// An implementation of the Python standard time module.  Currently
// unimplemented:
//
// accept2dyear
// strptime()
//
// There may also be some incompatibilities in strftime(), because the Java 
// tools for creating those formats don't always map to C's strftime()
// function.

package org.python.modules;

import org.python.core.*;
import java.text.DateFormatSymbols;
import java.text.DateFormat;
import java.util.GregorianCalendar;
import java.util.Date;



class TimeFunctions extends PyBuiltinFunctionSet
{
    public PyObject __call__() {
        switch (index) {
	case 0:
	    return Py.newFloat(time.time$());
	default:
	    throw argCountError(0);
        }
    }
}



public class time implements InitModule
{
    public void initModule(PyObject dict) {
	dict.__setitem__("time", new TimeFunctions().init("time", 0, 0) );
	dict.__setitem__("clock", new TimeFunctions().init("clock", 0, 0) );
    }
		
    public static double time$() {
        return System.currentTimeMillis()/1000.0;
    }

    /*public static double clock() {
      return System.currentTimeMillis()/1000.0;
      }*/

    private static GregorianCalendar _tupletocal(PyTuple tup) {
	return new GregorianCalendar(
            tup.__getitem__(0).__int__().getValue(),
            tup.__getitem__(1).__int__().getValue()-1,
            tup.__getitem__(2).__int__().getValue(),
            tup.__getitem__(3).__int__().getValue(),
            tup.__getitem__(4).__int__().getValue(),
            tup.__getitem__(5).__int__().getValue());
    }

    private static Date _tupletodate(PyTuple tup) {
	return new Date(
            tup.__getitem__(0).__int__().getValue(),
            tup.__getitem__(1).__int__().getValue()-1,
            tup.__getitem__(2).__int__().getValue(),
            tup.__getitem__(3).__int__().getValue(),
            tup.__getitem__(4).__int__().getValue(),
            tup.__getitem__(5).__int__().getValue());
    }

    public static double mktime(PyTuple tup) {
        GregorianCalendar cal = _tupletocal(tup);
        return (double)cal.getTime().getTime()/1000.0;
    }

    protected static PyTuple _timefields(double secs, java.util.TimeZone tz) {
        java.util.GregorianCalendar cal =
            new java.util.GregorianCalendar(tz);
        cal.clear();
        cal.setTime(new java.util.Date((long)(secs*1000)));
        // This call used to be needed to work around JVM bugs.
        // It appears to break jdk1.2, so it's not removed.
        // cal.clear();
        int dow = cal.get(java.util.Calendar.DAY_OF_WEEK)-2;
        if (dow<0)
	    dow = dow+7;
        int isdst = 0; // is this date dst?
        return new PyTuple(new PyObject[] {
            new PyInteger(cal.get(java.util.Calendar.YEAR)),
	    new PyInteger(cal.get(java.util.Calendar.MONTH)+1),
	    new PyInteger(cal.get(java.util.Calendar.DAY_OF_MONTH)),
	    new PyInteger(cal.get(java.util.Calendar.HOUR)
			  + 12*cal.get(java.util.Calendar.AM_PM)),
	    new PyInteger(cal.get(java.util.Calendar.MINUTE)),
	    new PyInteger(cal.get(java.util.Calendar.SECOND)),
	    new PyInteger(dow),
	    new PyInteger(cal.get(java.util.Calendar.DAY_OF_YEAR)),
	    new PyInteger(0)
	});
    }

    public static PyTuple localtime(double secs) {
        return _timefields(secs, java.util.TimeZone.getDefault());
    }

    public static PyTuple gmtime(double secs) {
        return _timefields(secs, java.util.TimeZone.getTimeZone("GMT"));
    }

    public static String ctime(double secs) {
        return asctime(localtime(secs));
    }

    // Python's time module specifies use of current locale
    protected static DateFormatSymbols datesyms = new DateFormatSymbols();
    protected static String[] shortdays = null;
    protected static String[] shortmonths = null;

    private static void throwValueError(String msg) {
	throw new PyException(Py.ValueError, new PyString(msg));
    }

    private static String _shortday(int dow) {
	// we need to hand craft shortdays[] because Java and Python have
	// different specifications.  Java (undocumented) appears to be
	// first element "", followed by 0=Sun.  Python says 0=Mon
	try {
	    if (shortdays == null) {
		shortdays = new String[7];
		String[] names = datesyms.getShortWeekdays();
		for (int i=0; i<6; i++)
		    shortdays[i] = names[i+2];
		shortdays[6] = names[1];
	    }
	}
	catch (ArrayIndexOutOfBoundsException e) {
	    throwValueError("day of week out of range (0-6)");
	}
	return shortdays[dow];
    }

    private static String _shortmonth(int month0to11) {
	// getShortWeekdays() returns a 13 element array with the last item
	// being the empty string.  This is also undocumented ;-/
	try {
	    if (shortmonths == null) {
		shortmonths = new String[12];
		String[] names = datesyms.getShortMonths();
		for (int i=0; i<12; i++)
		    shortmonths[i] = names[i];
	    }
	}
	catch (ArrayIndexOutOfBoundsException e) {
	    throwValueError("month out of range (1-12)");
	}
	return shortmonths[month0to11];
    }

    private static String _twodigit(PyInteger i) {
        String s = i.toString();
        if (s.length() == 1)
	    s = "0" + s;
        return s;
    }

    public static String asctime(PyTuple tup) {
	int dayindex = tup.__getitem__(6).__int__().getValue();
	int monthindex = tup.__getitem__(1).__int__().getValue() - 1;
	String s = _shortday(dayindex);
	s = s + " " + _shortmonth(monthindex);
        s = s + " " + _twodigit(tup.__getitem__(2).__int__());
        s = s + " " + _twodigit(tup.__getitem__(3).__int__());
        s = s + ":" + _twodigit(tup.__getitem__(4).__int__());
        s = s + ":" + _twodigit(tup.__getitem__(5).__int__());
        s = s + " " + tup.__getitem__(0).__str__().toString();
        return s;
    }

    public static void sleep(double secs) {
        try {
            java.lang.Thread.sleep((long)(secs * 1000));
        } catch (java.lang.InterruptedException e) {
            throw new PyException(Py.KeyboardInterrupt, "interrupted sleep");
        }
    }

    // This doesn't do the right thing when DST is in effect yet
    public static int timezone =
        -java.util.TimeZone.getDefault().getRawOffset() / 1000;

    public static int altzone = timezone;

    public static int daylight = 0;

    public static PyTuple tzname = new PyTuple(new PyObject[] {
        new PyString(java.util.TimeZone.getDefault().getID()),
        new PyString(java.util.TimeZone.getDefault().getID())
    });

    public static String strftime(String format, PyTuple tup) {
	String s = "";
	int lastc = 0;
	int j;
	String[] syms;
	GregorianCalendar cal = null;
	String tmp;
	while (lastc < format.length()) {
	    int i = format.indexOf("%", lastc);
	    if (i < 0) {
		// the end of the format string
		s = s + format.substring(lastc);
		break;
	    }
	    if (i == format.length() - 1) {
		// there's a bare % at the end of the string.  Python lets
		// this go by just sticking a % at the end of the result
		// string
		s = s + "%";
		break;
	    }
	    s = s + format.substring(lastc, i);
	    i++;
	    switch (format.charAt(i)) {
	    case 'a':
		// abbrev weekday
		j = tup.__getitem__(6).__int__().getValue();
		s = s + _shortday(j);
		break;
	    case 'A':
		// full weekday
		// see _shortday()
		syms = datesyms.getWeekdays();
		j = tup.__getitem__(6).__int__().getValue();
		if (0 <= j && j < 6)
		    s = s + syms[j+2];
		else if (j== 6)
		    s = s + syms[1];
		else
		    throwValueError("day of week out of range (0 - 6)");
		break;
	    case 'b':
		// abbrev month
		j = tup.__getitem__(1).__int__().getValue() - 1;
		s = s + _shortmonth(j);
		break;
	    case 'B':
		// full month
		syms = datesyms.getMonths();
		j = tup.__getitem__(1).__int__().getValue() - 1;
		if (0 <= j && j < 12)
		    s = s + syms[j];
		else
		    throwValueError("month out of range (1-12)");
		break;
	    case 'c':
		// locale's date and time repr (essentially asctime()?)
		s = s + asctime(tup);
		break;
	    case 'd':
		// day of month (01-31)
		s = s + _twodigit(tup.__getitem__(2).__int__());
		break;
	    case 'H':
		// hour (00-23)
		s = s + _twodigit(tup.__getitem__(3).__int__());
		break;
	    case 'I':
		// hour (01-12)
		j = tup.__getitem__(3).__int__().getValue() % 12;
		if (j == 0)
		    j = 12;		     // midnight or noon
		s = s + _twodigit(new PyInteger(j));
		break;
	    case 'j':
		// day of year (001-366)
		tmp = tup.__getitem__(7).__int__().toString();
		if (tmp.length() == 1)
		    s = s + "00" + tmp;
		else if (tmp.length() == 2)
		    s = s + "0" + tmp;
		else
		    s = s + tmp;
		break;
	    case 'm':
		// month (01-12)
		s = s + _twodigit(tup.__getitem__(1).__int__());
		break;
	    case 'M':
		// minute (00-59)
		s = s + _twodigit(tup.__getitem__(4).__int__());
		break;
	    case 'p':
		// AM/PM
		j = tup.__getitem__(3).__int__().getValue();
		syms = datesyms.getAmPmStrings();
		if (0 <= j && j < 12)
		    s = s + syms[0];
		else if (12 <= j && j < 24)
		    s = s + syms[1];
		else
		    throwValueError("hour out of range (0-23)");
		break;
	    case 'S':
		// seconds (00-61)
		s = s + _twodigit(tup.__getitem__(5).__int__());
		break;
	    case 'U':
		// week of year (sunday is first day) (00-53).  all days in
		// new year preceding first sunday are considered to be in
		// week 0
		if (cal == null)
		    cal = _tupletocal(tup);
		cal.setFirstDayOfWeek(cal.SUNDAY);
		cal.setMinimalDaysInFirstWeek(7);
		j = cal.get(cal.WEEK_OF_YEAR);
		s = s + _twodigit(new PyInteger(j));
		break;
	    case 'w':
		// weekday as decimal (0=Sunday-6)
		// tuple format has monday=0
		j = (tup.__getitem__(6).__int__().getValue() + 1) % 7;
		s = s + _twodigit(new PyInteger(j));
		break;
	    case 'W':
		// week of year (monday is first day) (00-53).  all days in
		// new year preceding first sunday are considered to be in
		// week 0
		if (cal == null)
		    cal = _tupletocal(tup);
		cal.setFirstDayOfWeek(cal.MONDAY);
		cal.setMinimalDaysInFirstWeek(7);
		j = cal.get(cal.WEEK_OF_YEAR);
		s = s + _twodigit(new PyInteger(j));
		break;
	    case 'x':
		// locale's date repr
		s = s + DateFormat.getDateInstance().format(_tupletodate(tup));
		break;
	    case 'X':
		// locale's time repr
		//
		// TBD: This breaks test_strftime.py's test of %X, but I
		// think it is a valid interpretation of the spec.
		s = s + DateFormat.getTimeInstance().format(_tupletodate(tup));
		break;
	    case 'y':
		// year w/o century (00-99)
		tmp = _twodigit(tup.__getitem__(0).__int__());
		if (tmp.length() > 2)
		    s = s + tmp.substring(tmp.length()-2, tmp.length());
		else
		    s = s + tmp;
		break;
	    case 'Y':
		// year w/ century
		tmp = _twodigit(tup.__getitem__(0).__int__());
		if (tmp.length() == 2)
		    s = s + "00" + tmp;
		else if (tmp.length() == 3)
		    s = s + "0" + tmp;
		else
		    s = s + tmp;
		break;
	    case 'Z':
		// timezone name
		if (cal == null)
		    cal = _tupletocal(tup);
		// TBD: this isn't quite right
		s = s + cal.getTimeZone().getDisplayName();
		break;
	    case '%':
		// %
		s = s + "%";
		break;
	    default:
		// TBD: should this raise a ValueError?
		s = s + "%" + format.charAt(i);
		i++;
		break;
	    }
	    lastc = i+1;
	    i++;
	}
	return s;
    }
}
