// Copyright © Corporation for National Research Initiatives
package org.python.modules;
import org.python.core.*;

class TimeFunctions extends PyBuiltinFunctionSet
{
    public PyObject __call__() {
        switch(index) {
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

    public static double mktime(PyTuple tup) {
        java.util.GregorianCalendar cal = new java.util.GregorianCalendar(
            tup.__getitem__(0).__int__().getValue(),
            tup.__getitem__(1).__int__().getValue()-1,
            tup.__getitem__(2).__int__().getValue(),
            tup.__getitem__(3).__int__().getValue(),
            tup.__getitem__(4).__int__().getValue(),
            tup.__getitem__(5).__int__().getValue());
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

    protected static String[] _daynames = {
        "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    protected static String[] _monthnames = {
        null, "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep",
        "Oct", "Nov", "Dec"};

    public static String asctime(PyTuple tup) {
        String s = _daynames[tup.__getitem__(6).__int__().getValue()];
        s = s + " " + _monthnames[tup.__getitem__(1).__int__().getValue()];
        s = s + " " + _twodigit(tup.__getitem__(2).__int__());
        s = s + " " + _twodigit(tup.__getitem__(3).__int__());
        s = s + ":" + _twodigit(tup.__getitem__(4).__int__());
        s = s + ":" + _twodigit(tup.__getitem__(5).__int__());
        s = s + " " + tup.__getitem__(0).__str__().toString();
        return s;
    }

    private static String _twodigit(PyInteger i) {
        String s = i.toString();
        if (s.length() == 1)
	    s = "0" + s;
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

    // strftime()
}
