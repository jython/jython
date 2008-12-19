// Copyright (c) Corporation for National Research Initiatives

// An implementation of the Python standard time module.  Currently
// unimplemented:
//
// accept2dyear
//
// There may also be some incompatibilities in strftime(), because the Java
// tools for creating those formats don't always map to C's strftime()
// function.
//
// see org/python/modules/time.java for previous history.
package org.python.modules.time;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyBuiltinFunctionSet;
import org.python.core.PyException;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.__builtin__;

class TimeFunctions extends PyBuiltinFunctionSet
{
    public TimeFunctions(String name, int index, int argcount) {
        super(name, index, argcount);
    }

    public PyObject __call__() {
        switch (index) {
        case 0:
            return Py.newFloat(Time.time());
        case 1:
            return Py.newFloat(Time.clock());
        default:
            throw info.unexpectedCall(0, false);
        }
    }
}

public class Time implements ClassDictInit
{
    public static PyString __doc__ = new PyString(
        "This module provides various functions to manipulate time values.\n"+
        "\n"+
        "There are two standard representations of time.  One is the "+
                "number\n"+
        "of seconds since the Epoch, in UTC (a.k.a. GMT).  It may be an "+
                "integer\n"+
        "or a floating point number (to represent fractions of seconds).\n"+
        "The Epoch is system-defined; on Unix, it is generally "+
                "January 1st, 1970.\n"+
        "The actual value can be retrieved by calling gmtime(0).\n"+
        "\n"+
        "The other representation is a tuple of 9 integers giving "+
                "local time.\n"+
        "The tuple items are:\n"+
        "  year (four digits, e.g. 1998)\n"+
        "  month (1-12)\n"+
        "  day (1-31)\n"+
        "  hours (0-23)\n"+
        "  minutes (0-59)\n"+
        "  seconds (0-59)\n"+
        "  weekday (0-6, Monday is 0)\n"+
        "  Julian day (day in the year, 1-366)\n"+
        "  DST (Daylight Savings Time) flag (-1, 0 or 1)\n"+
        "If the DST flag is 0, the time is given in the regular time zone;\n"+
        "if it is 1, the time is given in the DST time zone;\n"+
        "if it is -1, mktime() should guess based on the date and time.\n"+
        "\n"+
        "Variables:\n"+
        "\n"+
        "timezone -- difference in seconds between UTC and local "+
                "standard time\n"+
        "altzone -- difference in  seconds between UTC and local DST time\n"+
        "daylight -- whether local time should reflect DST\n"+
        "tzname -- tuple of (standard time zone name, DST time zone name)\n"+
        "\n"+
        "Functions:\n"+
        "\n"+
        "time() -- return current time in seconds since the Epoch "+
                "as a float\n"+
        "clock() -- return CPU time since process start as a float\n"+
        "sleep() -- delay for a number of seconds given as a float\n"+
        "gmtime() -- convert seconds since Epoch to UTC tuple\n"+
        "localtime() -- convert seconds since Epoch to local time tuple\n"+
        "asctime() -- convert time tuple to string\n"+
        "ctime() -- convert time in seconds to string\n"+
        "mktime() -- convert local time tuple to seconds since Epoch\n"+
        "strftime() -- convert time tuple to string according to "+
                "format specification\n"+
        "strptime() -- parse string to time tuple according to "+
                "format specification\n"
    );

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("time", new TimeFunctions("time", 0, 0));
        dict.__setitem__("clock", new TimeFunctions("clock", 1, 0));
        dict.__setitem__("struct_time", PyTimeTuple.TYPE);
        dict.__setitem__("__name__", Py.newString("time"));

        // calculate the static variables tzname, timezone, altzone, daylight
        TimeZone tz = TimeZone.getDefault();

        tzname = new PyTuple(new PyString(tz.getDisplayName(false, 0)),
                             new PyString(tz.getDisplayName(true, 0)));

        daylight = tz.useDaylightTime() ? 1 : 0;
        timezone = -tz.getRawOffset() / 1000;
        altzone = timezone - tz.getDSTSavings() / 1000;
    }

    public static double time() {
        return System.currentTimeMillis()/1000.0;
    }

    /**
     * @return - the seconds elapsed since the first call to this function
     */
    public static double clock() {
        // Check against an explicit initialization variable, clockInitialized,
        // rather than a value of initialClock since the initial call to
        // System.nanoTime can yield anything and that could lead to initialTime
        // being set twice.
        if(!clockInitialized) {
            initialClock = System.nanoTime();
            clockInitialized = true;
            return 0;
        }
        return (System.nanoTime() - initialClock) / NANOS_PER_SECOND;
    }
    private static final double NANOS_PER_SECOND = 1000000000.0;
    private static long initialClock;
    private static volatile boolean clockInitialized;

    private static void throwValueError(String msg) {
        throw new PyException(Py.ValueError, new PyString(msg));
    }

    private static int item(PyTuple tup, int i) {
        // knows about and asserts format on tuple items.  See
        // documentation for Python's time module for details.
        int val = tup.__getitem__(i).asInt();
        boolean valid = true;
        switch (i) {
        case 0: break;                                  // year
        case 1: valid = (0 <= val && val <= 12); break; // month 1-12 (or 0)
        case 2: valid = (0 <= val && val <= 31); break; // day 1 - 31 (or 0)
        case 3: valid = (0 <= val && val <= 23); break; // hour 0 - 23
        case 4: valid = (0 <= val && val <= 59); break; // minute 0 - 59
        case 5: valid = (0 <= val && val <= 61); break; // second 0 - 59 (plus 2 leap seconds)
        case 6: valid = (0 <= val && val <= 6);  break; // weekday 0 - 6
        case 7: valid = (0 <= val && val < 367); break; // julian day 1 - 366 (or 0)
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
        switch (i) {
        case 1:
            if (val > 0) {
                // Java's months are usually 0-11
                val--;
            }
            break;
        case 2:
        case 7:
            // 'or 0'
            if (val == 0) {
                val = 1;
            }
            break;
        }
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
        int dst = item(tup, 8);
        if(dst == 0 || dst == 1) {
            cal.set(Calendar.DST_OFFSET, dst * cal.getTimeZone().getDSTSavings());
        }
        return cal.getTime().getTime()/1000.0;
    }

    protected static PyTimeTuple _timefields(double secs, TimeZone tz) {
        GregorianCalendar cal = new GregorianCalendar(tz);
        cal.clear();
        secs = secs * 1000;
        if (secs < Long.MIN_VALUE || secs > Long.MAX_VALUE) {
            throw Py.ValueError("timestamp out of range for platform time_t");
        }
        cal.setTime(new Date((long)secs));
        // This call used to be needed to work around JVM bugs.
        // It appears to break jdk1.2, so it's not removed.
        // cal.clear();
        int dow = cal.get(Calendar.DAY_OF_WEEK)-2;
        if (dow<0)
            dow = dow+7;
        // TBD: is this date dst?
        boolean isdst = tz.inDaylightTime(cal.getTime());
        return new PyTimeTuple(new PyInteger(cal.get(Calendar.YEAR)),
                               new PyInteger(cal.get(Calendar.MONTH)+1),
                               new PyInteger(cal.get(Calendar.DAY_OF_MONTH)),
                               new PyInteger(cal.get(Calendar.HOUR) +
                                             12*cal.get(Calendar.AM_PM)),
                               new PyInteger(cal.get(Calendar.MINUTE)),
                               new PyInteger(cal.get(Calendar.SECOND)),
                               new PyInteger(dow),
                               new PyInteger(cal.get(Calendar.DAY_OF_YEAR)),
                               new PyInteger(isdst ? 1 : 0));
    }

    /**
     * Convert a time argument that may be an optional float or None value to a
     * double. Throws a TypeError on failure.
     *
     * @param arg a PyObject number of None
     * @return a double value
     */
    public static double parseTimeDoubleArg(PyObject arg) {
        if (arg == Py.None) {
            return time();
        }
        Object result = arg.__tojava__(Double.class);
        if (result == Py.NoConversion) {
            throw Py.TypeError("a float is required");
        }
        return (Double)result;
    }

    public static PyTuple localtime() {
        return localtime(Py.None);
    }

    public static PyTuple localtime(PyObject secs) {
        return _timefields(parseTimeDoubleArg(secs), TimeZone.getDefault());
    }

    public static PyTuple gmtime() {
        return gmtime(Py.None);
    }

    public static PyTuple gmtime(PyObject arg) {
        return _timefields(parseTimeDoubleArg(arg), TimeZone.getTimeZone("GMT"));
    }

    public static PyString ctime() {
        return ctime(Py.None);
    }

    public static PyString ctime(PyObject secs) {
        return asctime(localtime(secs));
    }

    // Python's time module specifies use of current locale
    protected static Locale currentLocale = null;
    protected static DateFormatSymbols datesyms = new DateFormatSymbols();
    protected static String[] shortdays = null;
    protected static String[] shortmonths = null;

    private static String[] enshortdays = new String[] {"Mon",
                                                        "Tue",
                                                        "Wed",
                                                        "Thu",
                                                        "Fri",
                                                        "Sat",
                                                        "Sun"};

    private static String[] enshortmonths = new String[] {"Jan",
                                                          "Feb",
                                                          "Mar",
                                                          "Apr",
                                                          "May",
                                                          "Jun",
                                                          "Jul",
                                                          "Aug",
                                                          "Sep",
                                                          "Oct",
                                                          "Nov",
                                                          "Dec"};

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
            Arrays.fill(c, '0');
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

    public static PyString asctime() {
        return asctime(localtime());
    }

    public static PyString asctime(PyTuple tup) {
        StringBuffer buf = new StringBuffer(25);
        buf.append(enshortdays[item(tup, 6)]).append(' ');
        buf.append(enshortmonths[item(tup, 1)]).append(' ');
        int dayOfMonth = item(tup, 2);
        if(dayOfMonth < 10){
            buf.append(' ');
        }
        buf.append(dayOfMonth).append(' ');
        buf.append(_twodigit(item(tup, 3))).append(':');
        buf.append(_twodigit(item(tup, 4))).append(':');
        buf.append(_twodigit(item(tup, 5))).append(' ');
        return new PyString(buf.append(item(tup, 0)).toString());
    }

    public static String locale_asctime(PyTuple tup) {
        checkLocale();
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

    public static PyString strftime(String format) {
        return strftime(format, localtime());
    }

    public static PyString strftime(String format, PyTuple tup) {
        checkLocale();

        // Immediately validate the tuple
        int[] items = new int[9];
        for (int i = 0; i < 9; i ++) {
            items[i] = item(tup, i);
        }

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
                j = items[6];
                s = s + _shortday(j);
                break;
            case 'A':
                // full weekday
                // see _shortday()
                syms = datesyms.getWeekdays();
                j = items[6];
                if (0 <= j && j < 6)
                    s = s + syms[j+2];
                else if (j== 6)
                    s = s + syms[1];
                else
                    throwValueError("day of week out of range (0 - 6)");
                break;
            case 'b':
                // abbrev month
                j = items[1];
                s = s + _shortmonth(j);
                break;
            case 'B':
                // full month
                syms = datesyms.getMonths();
                j = items[1];
                s = s + syms[j];
                break;
            case 'c':
                s = s + locale_asctime(tup);
                break;
            case 'd':
                // day of month (01-31)
                s = s + _twodigit(items[2]);
                break;
            case 'H':
                // hour (00-23)
                s = s + _twodigit(items[3]);
                break;
            case 'I':
                // hour (01-12)
                j = items[3] % 12;
                if (j == 0)
                    j = 12;                  // midnight or noon
                s = s + _twodigit(j);
                break;
            case 'j':
                // day of year (001-366)
                s = s + _padint(items[7], 3);
                break;
            case 'm':
                // month (01-12)
                s = s + _twodigit(items[1] + 1);
                break;
            case 'M':
                // minute (00-59)
                s = s + _twodigit(items[4]);
                break;
            case 'p':
                // AM/PM
                j = items[3];
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
                s = s + _twodigit(items[5]);
                break;
            case 'U':
                // week of year (sunday is first day) (00-53).  all days in
                // new year preceding first sunday are considered to be in
                // week 0
                if (cal == null)
                    cal = _tupletocal(tup);
                cal.setFirstDayOfWeek(Calendar.SUNDAY);
                cal.setMinimalDaysInFirstWeek(7);
                j = cal.get(Calendar.WEEK_OF_YEAR);
                if (cal.get(Calendar.MONTH) == Calendar.JANUARY && j >= 52)
                    j = 0;
                s = s + _twodigit(j);
                break;
            case 'w':
                // weekday as decimal (0=Sunday-6)
                // tuple format has monday=0
                j = (items[6] + 1) % 7;
                s = s + j;
                break;
            case 'W':
                // week of year (monday is first day) (00-53).  all days in
                // new year preceding first sunday are considered to be in
                // week 0
                if (cal == null)
                    cal = _tupletocal(tup);
                cal.setFirstDayOfWeek(Calendar.MONDAY);
                cal.setMinimalDaysInFirstWeek(7);
                j = cal.get(Calendar.WEEK_OF_YEAR);

                if (cal.get(Calendar.MONTH) == Calendar.JANUARY && j >= 52)
                    j = 0;
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
                // to craft a "C" locale for Java and set Jython to use
                // this by default, but that's too much work right now.
                //
                // For now, we hard code %x and %X to return values
                // formatted in the "C" locale, i.e. the default way
                // CPython does it.  E.g.:
                //     %x == mm/dd/yy
                //     %X == HH:mm:SS
                //
                s = s + _twodigit(items[1] + 1) + "/" +
                    _twodigit(items[2]) + "/" +
                    _truncyear(items[0]);
                break;
            case 'X':
                // See comment for %x above
                s = s + _twodigit(items[3]) + ":" +
                    _twodigit(items[4]) + ":" +
                    _twodigit(items[5]);
                break;
            case 'Y':
                // year w/ century
                s = s + _padint(items[0], 4);
                break;
            case 'y':
                // year w/o century (00-99)
                s = s + _truncyear(items[0]);
                break;
            case 'Z':
                // timezone name
                if (cal == null)
                    cal = _tupletocal(tup);
                // If items[8] == 1, we're in daylight savings time.
                // -1 means the information was not available; treat this as if not in dst.
                s = s + cal.getTimeZone().getDisplayName(items[8] > 0, 0);
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
        // FIXME: This have problems with localized data:
        //        $ LANG=es_ES.UTF-8 jythont -c "import time; print time.strftime('%A')"
        //        s?bado
        //
        //        On the other hand, returning unicode would break some doctests
        //        and will give problems when the return value of strftime is
        //        mixed with other strings and then the final result is stored
        //        on places which only support str and not unicode (such as
        //        os.environ)
        //
        // TODO:  Check how CPython deals with this problem.
        return new PyString(s);
    }


    private static void checkLocale() {
        if (!Locale.getDefault().equals(currentLocale)) {
            currentLocale = Locale.getDefault();
            datesyms = new DateFormatSymbols(currentLocale);
            shortdays = null;
            shortmonths = null;
        }
    }

    public static PyTuple strptime(String data_string) {
        return strptime(data_string, DEFAULT_FORMAT_PY);
    }

    /**
     * Calls _strptime.strptime(), for cases that our SimpleDateFormat backed
     * strptime can't handle.
     */
    private static PyTuple pystrptime(String data_string, String format) {
        return (PyTuple) __builtin__.__import__("_strptime")
                                    .__getattr__("strptime")
                                    .__call__(Py.newUnicode(data_string),
                                              Py.newUnicode(format));
    }

    public static PyTuple strptime(String data_string, String format) {
        if (format == null || data_string == null) {
            // this is not a very interesting error message, but it's the same
            // as what CPython outputs
            throw Py.TypeError("expected string of buffer");
        }
        String jformat = py2java_format(format);
        if (jformat == null) {
            // Format not translatable to java, fallback to _strptime
            return pystrptime(data_string, format);
        }
        SimpleDateFormat d = new SimpleDateFormat(jformat);
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(d.parse(data_string));
        } catch (ParseException e) {
            throwValueError("time data did not match format:  data=" + data_string + "  fmt=" + format);
        }
        int isdst = -1;
        if (jformat.contains("zzz")) {
            isdst = cal.getTimeZone().inDaylightTime(cal.getTime()) ? 1 : 0;
        }
        return  new PyTuple(new PyObject[] {
            new PyInteger(cal.get(Calendar.YEAR)),
            new PyInteger(cal.get(Calendar.MONTH)+1),
            new PyInteger(cal.get(Calendar.DAY_OF_MONTH)),
            new PyInteger(cal.get(Calendar.HOUR) + 12*cal.get(Calendar.AM_PM)),
            new PyInteger(cal.get(Calendar.MINUTE)),
            new PyInteger(cal.get(Calendar.SECOND)),
            new PyInteger((cal.get(Calendar.DAY_OF_WEEK)-2) % 7), // refactored for mon(0)-sun(6)
            new PyInteger(cal.get(Calendar.DAY_OF_YEAR)),
            new PyInteger(isdst) }
        );
    }

    private static final String DEFAULT_FORMAT_PY = "%a %b %d %H:%M:%S %Y";
    private static final HashMap<Character, String> py2java = new HashMap<Character, String>() {{
        put('a', "EEE");
        put('A', "EEEE");
        put('b', "MMM");
        put('B', "MMMM");
        put('c', "EEE MMM dd HH:mm:ss yyyy");
        put('d', "dd");
        put('H', "HH");
        put('I', "hh");
        put('j', "DDD");
        put('m', "MM");
        put('M', "mm");
        put('p', "a");
        put('S', "ss");
        put('U', "ww");
        put('W', "ww"); // same as U ??
        put('x', "MM/dd/yy");
        put('X', "HH:mm:ss");
        put('y', "yy");
        put('Y', "yyyy");
        put('Z', "zzz");
        put('%', "%");
    }};

    // strptime formats not supported by SimpleDateFormat:
    private static final List<Character> notSupported = new ArrayList<Character>() {{
        add('w');
    }};


    /**
     * @return the {@link SimpleDateFormat} format string equivalent to the
     * strptime format string supplied as parameter, or <b>null</b> if there is
     * no equivalent for SimpleDateFormat.
     */
    private static String py2java_format(String format) {
        StringBuilder builder = new StringBuilder();
        boolean directive = false;
        boolean inQuote = false;

        for (int i = 0; i < format.length(); i++) {
            char charAt = format.charAt(i);

            if (charAt == '%' && !directive) {
                directive = true;
                continue;
            }

            if (!directive) {
                // ascii letters are considered SimpleDateFormat directive patterns unless
                // escaped
                boolean needsQuote = (charAt >= 'A' && charAt <= 'Z') ||
                        (charAt >= 'a' && charAt <= 'z');
                if (needsQuote && !inQuote || !needsQuote && inQuote) {
                    builder.append("'");
                    inQuote = needsQuote;
                }
                builder.append(charAt);
                continue;
            } else if (inQuote) {
                builder.append("'");
                inQuote = false;
            }

            String translated = py2java.get(charAt);
            if (translated == null && notSupported.contains(charAt)) {
                return null;
            }
            builder.append(translated != null ? translated : charAt);
            directive = false;
        }
        return builder.toString();
    }
}
