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
//
// NOTE: This file is prepared for the JDK 1.2 APIs, however it is
// currently set up to compile cleanly under 1.1.
//
// If you would like to enable the JDK 1.2 behavior (perhaps because you
// are running under JDK 1.2 and would like to actually have stuff like
// time.tzname or time.altzone work correctly, just search for the string
// "XXXAPI" and stick a couple of double slashes at the beginning of each
// matching line.

package org.python.modules;

import org.python.core.*;
import java.text.DateFormatSymbols;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.SimpleTimeZone;



class TimeFunctions extends PyBuiltinFunctionSet
{
    public TimeFunctions(String name, int index, int argcount) {
        super(name, index, argcount, argcount, false, null);
    }

    public PyObject __call__() {
        switch (index) {
        case 0:
            return Py.newFloat(time.time$());
        default:
            throw argCountError(0);
        }
    }
}



public class time implements ClassDictInit
{
    public static void classDictInit(PyObject dict) {
        dict.__setitem__("time", new TimeFunctions("time", 0, 0));
        dict.__setitem__("clock", new TimeFunctions("clock", 0, 0));

        // calculate the static variables tzname, timezone, altzone, daylight
        TimeZone tz = TimeZone.getDefault();

        /* XXXAPI 1.2 START
        try {
            tzname = new PyTuple(
                new PyObject[] {
                    new PyString(tz.getDisplayName(false, TimeZone.SHORT)),
                    new PyString(tz.getDisplayName(true, TimeZone.SHORT))
                });
        }
        catch (NoSuchMethodError e) {}
        XXXAPI 1.2 END */

        // getDisplayName() is only available in Java 1.2.  This is the
        // next best thing, but it isn't really correct, or what the Python
        // time module spec wants, but it does work for Java 1.1
        if (tzname == null) {
            tzname = new PyTuple(
                new PyObject[] {
                    new PyString(tz.getID()),
                    new PyString(tz.getID())
                });
        }
        daylight = tz.useDaylightTime() ? 1 : 0;

        timezone = -tz.getRawOffset() / 1000;
        if (tz instanceof SimpleTimeZone) {
            /* XXXAPI 1.2 START
            try {
                SimpleTimeZone stz = (SimpleTimeZone)tz;
                altzone = timezone - stz.getDSTSavings() / 1000;
            }
            catch (NoSuchMethodError e) {}
            XXXAPI 1.2 END */
        }
        if (altzone == -1)
                // best we can do for Java 1.1.  This is wrong though.
                altzone = timezone;
    }

    public static double time$() {
        return System.currentTimeMillis()/1000.0;
    }

    /*public static double clock() {
      return System.currentTimeMillis()/1000.0;
      }*/

    private static void throwValueError(String msg) {
        throw new PyException(Py.ValueError, new PyString(msg));
    }

    private static int item(PyTuple tup, int i) {
        // knows about and asserts format on tuple items.  See
        // documentation for Python's time module for details.
        int val = tup.__getitem__(i).__int__().getValue();
        boolean valid = true;
        switch (i) {
        case 0: break;                                  // year
        case 1: valid = (1 <= val && val <= 12); break; // month 1-12
        case 2: valid = (1 <= val && val <= 31); break; // day 1 - 31
        case 3: valid = (0 <= val && val <= 23); break; // hour 0 - 23
        case 4: valid = (0 <= val && val <= 59); break; // minute 0 - 59
        case 5: valid = (0 <= val && val <= 59); break; // second 0 - 59
        case 6: valid = (0 <= val && val <= 6);  break; // weekday 0 - 6
        case 7: valid = (1 <= val && val < 367); break; // julian day 1 - 366
        case 8: valid = (-1 <= val && val <= 1); break; // d.s. flag, -1,0,1
        }
        // raise a ValueError if not within range
        if (!valid) {
            String msg;
            switch (i) {
            case 1:
                msg = "month out of range (1-12)";
                break;
            case 2:
                msg = "day out of range (1-31)";
                break;
            case 3:
                msg = "hour out of range (0-23)";
                break;
            case 4:
                msg = "minute out of range (0-59)";
                break;
            case 5:
                msg = "second out of range (0-59)";
                break;
            case 6:
                msg = "day of week out of range (0-6)";
                break;
            case 7:
                msg = "day of year out of range (1-366)";
                break;
            case 8:
                msg = "daylight savings flag out of range (-1,0,1)";
                break;
            default:
                // make compiler happy
                msg = "ignore";
                break;
            }
            throwValueError(msg);
        }
        // Java's months are usually 0-11
        if (i == 1)
            val--;
        return val;
    }

    private static GregorianCalendar _tupletocal(PyTuple tup) {
        return new GregorianCalendar(item(tup, 0),
                                     item(tup, 1),
                                     item(tup, 2),
                                     item(tup, 3),
                                     item(tup, 4),
                                     item(tup, 5));
    }

    public static double mktime(PyTuple tup) {
        GregorianCalendar cal;
        try {
            cal = _tupletocal(tup);
        }
        catch (PyException e) {
            // CPython's mktime raises OverflowErrors... yuck!
            e.type = Py.OverflowError;
            throw e;
        }
        return (double)cal.getTime().getTime()/1000.0;
    }

    protected static PyTuple _timefields(double secs, TimeZone tz) {
        GregorianCalendar cal = new GregorianCalendar(tz);
        cal.clear();
        cal.setTime(new Date((long)(secs*1000)));
        // This call used to be needed to work around JVM bugs.
        // It appears to break jdk1.2, so it's not removed.
        // cal.clear();
        int dow = cal.get(Calendar.DAY_OF_WEEK)-2;
        if (dow<0)
            dow = dow+7;
        // TBD: is this date dst?
        boolean isdst = tz.inDaylightTime(cal.getTime());
        return new PyTuple(new PyObject[] {
            new PyInteger(cal.get(Calendar.YEAR)),
            new PyInteger(cal.get(Calendar.MONTH)+1),
            new PyInteger(cal.get(Calendar.DAY_OF_MONTH)),
            new PyInteger(cal.get(Calendar.HOUR) +
                          12*cal.get(Calendar.AM_PM)),
            new PyInteger(cal.get(Calendar.MINUTE)),
            new PyInteger(cal.get(Calendar.SECOND)),
            new PyInteger(dow),
            new PyInteger(cal.get(Calendar.DAY_OF_YEAR)),
            new PyInteger(isdst ? 1 : 0)
        });
    }

    public static PyTuple localtime(double secs) {
        return _timefields(secs, TimeZone.getDefault());
    }

    public static PyTuple gmtime(double secs) {
        return _timefields(secs, TimeZone.getTimeZone("GMT"));
    }

    public static String ctime(double secs) {
        return asctime(localtime(secs));
    }

    // Python's time module specifies use of current locale
    protected static DateFormatSymbols datesyms = new DateFormatSymbols();
    protected static String[] shortdays = null;
    protected static String[] shortmonths = null;

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

    private static String _padint(int i, int target) {
        String s = Integer.toString(i);
        int sz = s.length();
        if (target <= sz)
            // no truncation
            return s;
        if (target == sz+1)
            return "0"+s;
        if (target == sz+2)
            return "00"+s;
        else {
            char[] c = new char[target-sz];
            while (target > sz) {
                c[target-sz] = '0';
                target--;
            }
            return new String(c) + s;
        }
    }

    private static String _twodigit(int i) {
        return _padint(i, 2);
    }

    private static String _truncyear(int year) {
        String yearstr = _padint(year, 4);
        return yearstr.substring(yearstr.length()-2, yearstr.length());
    }

    public static String asctime(PyTuple tup) {
        int day = item(tup, 6);
        int mon = item(tup, 1);
        return _shortday(day) + " " + _shortmonth(mon) + " " +
            _twodigit(item(tup, 2)) + " " +
            _twodigit(item(tup, 3)) + ":" +
            _twodigit(item(tup, 4)) + ":" +
            _twodigit(item(tup, 5)) + " " +
            item(tup, 0);
    }

    public static void sleep(double secs) {
        try {
            java.lang.Thread.sleep((long)(secs * 1000));
        }
        catch (java.lang.InterruptedException e) {
            throw new PyException(Py.KeyboardInterrupt, "interrupted sleep");
        }
    }

    // set by classDictInit()
    public static int timezone;
    public static int altzone = -1;
    public static int daylight;
    public static PyTuple tzname = null;
    // TBD: should we accept 2 digit years?  should we make this attribute
    // writable but ignore its value?
    public static final int accept2dyear = 0;

    public static String strftime(String format, PyTuple tup) {
        String s = "";
        int lastc = 0;
        int j;
        String[] syms;
        GregorianCalendar cal = null;
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
                j = item(tup, 6);
                s = s + _shortday(j);
                break;
            case 'A':
                // full weekday
                // see _shortday()
                syms = datesyms.getWeekdays();
                j = item(tup, 6);
                if (0 <= j && j < 6)
                    s = s + syms[j+2];
                else if (j== 6)
                    s = s + syms[1];
                else
                    throwValueError("day of week out of range (0 - 6)");
                break;
            case 'b':
                // abbrev month
                j = item(tup, 1);
                s = s + _shortmonth(j);
                break;
            case 'B':
                // full month
                syms = datesyms.getMonths();
                j = item(tup, 1);
                s = s + syms[j];
                break;
            case 'c':
                // locale's date and time repr (essentially asctime()?)
                s = s + asctime(tup);
                break;
            case 'd':
                // day of month (01-31)
                s = s + _twodigit(item(tup, 2));
                break;
            case 'H':
                // hour (00-23)
                s = s + _twodigit(item(tup, 3));
                break;
            case 'I':
                // hour (01-12)
                j = item(tup, 3) % 12;
                if (j == 0)
                    j = 12;                  // midnight or noon
                s = s + _twodigit(j);
                break;
            case 'j':
                // day of year (001-366)
                s = _padint(item(tup, 7), 3);
                break;
            case 'm':
                // month (01-12)
                s = s + _twodigit(item(tup, 1) + 1);
                break;
            case 'M':
                // minute (00-59)
                s = s + _twodigit(item(tup, 4));
                break;
            case 'p':
                // AM/PM
                j = item(tup, 3);
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
                s = s + _twodigit(item(tup, 5));
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
                s = s + _twodigit(j);
                break;
            case 'w':
                // weekday as decimal (0=Sunday-6)
                // tuple format has monday=0
                j = (item(tup, 6) + 1) % 7;
                s = s + _twodigit(j);
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
                s = s + _twodigit(j);
                break;
            case 'x':
                // TBD: A note about %x and %X.  Python's time.strftime()
                // by default uses the "C" locale, which is changed by
                // using the setlocale() function.  In Java, the default
                // locale is set by user.language and user.region
                // properties and is "en_US" by default, at least around
                // here!  Locale "en_US" differs from locale "C" in the way
                // it represents dates and times.  Eventually we might want
                // to craft a "C" locale for Java and set JPython to use
                // this by default, but that's too much work right now.
                //
                // For now, we hard code %x and %X to return values
                // formatted in the "C" locale, i.e. the default way
                // CPython does it.  E.g.:
                //     %x == mm/dd/yy
                //     %X == HH:mm:SS
                //
                s = s + _twodigit(item(tup, 1) + 1) + "/" +
                    _twodigit(item(tup, 2)) + "/" +
                    _truncyear(item(tup, 0));
                break;
            case 'X':
                // See comment for %x above
                s = s + _twodigit(item(tup, 3)) + ":" +
                    _twodigit(item(tup, 4)) + ":" +
                    _twodigit(item(tup, 5));
                break;
            case 'Y':
                // year w/ century
                s = s + _padint(item(tup, 0), 4);
                break;
            case 'y':
                // year w/o century (00-99)
                s = s + _truncyear(item(tup, 0));
                break;
            case 'Z':
                // timezone name
                if (cal == null)
                    cal = _tupletocal(tup);
                {
                    boolean use_getid = true;
                    /* XXXAPI 1.2 START
                    try {
                        s = s + cal.getTimeZone().getDisplayName(
                            // in daylight savings time?  true if == 1 -1
                            // means the information was not available;
                            // treat this as if not in dst
                            item(tup, 8) > 0,
                            TimeZone.SHORT);
                        use_getid = false;
                    }
                    catch (NoSuchMethodError e) {}
                    XXXAPI 1.2 END */
                    if (use_getid)
                        // See note in classDictInit() above
                        s = s + cal.getTimeZone().getID();
                }
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
