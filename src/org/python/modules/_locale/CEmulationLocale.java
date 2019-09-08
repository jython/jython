// (c) 2019 Jython Developers
// Licensed to PSF under a contributor agreement

package org.python.modules._locale;

import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyList;
import org.python.core.PyString;

/**
 * Emulates the Python (ie POSIX) 'C' locale.
 *
 * The C emulation locale uses only ANSI characters in non-unicode strings, in keeping with it being
 * the locale of last resort and maximum compatibility.
 *
 * Used by the _locale module. Callers would not usually interact with this class directly unless
 * working with _locale internals.
 *
 * @since Jython 2.7.2
 */
public class CEmulationLocale implements PyLocale {
    /*
     * A number of these structures are repeated from Time.java and elsewhere They can be removed
     * from those other classes and pointed here once jython2_legacy behaviour is deprecated or
     * removed.
     */
    private static final String[] EN_SHORT_DAYS =
            new String[] {"", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    private static final String[] EN_DAYS = new String[] {"", "Sunday", "Monday", "Tuesday",
            "Wednesday", "Thursday", "Friday", "Saturday"};

    private static final String[] EN_SHORT_MONTHS = new String[] {"Jan", "Feb", "Mar", "Apr", "May",
            "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    private static final String[] EN_MONTHS = new String[] {"January", "February", "March", "April",
            "May", "June", "July", "August", "September", "October", "November", "December"};

    private static final String[] EN_AM_PM = new String[] {"am", "pm"};

    private static final PyDictionary EMULATION_CONV;

    private static void putEmpty(PyDictionary dict, String key) {
        dict.put(new PyString(key), Py.EmptyString);
    }

    private static void putCharMax(PyDictionary dict, String key) {
        dict.put(new PyString(key), _locale.CHAR_MAX_PY_INT);
    }

    static {
        EMULATION_CONV = new PyDictionary();
        /*
         * This is a repetition of the locale emulation in the locale module to allow resetting the
         * locale back to C emulation.
         */
        _locale.putConvEntry(EMULATION_CONV, "decimal_point", ".");
        putEmpty(EMULATION_CONV, "thousands_sep");
        putEmpty(EMULATION_CONV, "currency_symbol");
        putEmpty(EMULATION_CONV, "int_curr_symbol");
        putEmpty(EMULATION_CONV, "negative_sign");
        putEmpty(EMULATION_CONV, "positive_sign");
        putCharMax(EMULATION_CONV, "p_sign_posn");
        PyList groupingList = new PyList();
        _locale.putConvEntry(EMULATION_CONV, "grouping", groupingList);
        putEmpty(EMULATION_CONV, "mon_decimal_point");
        putEmpty(EMULATION_CONV, "mon_thousands_sep");
        putCharMax(EMULATION_CONV, "frac_digits");
        putCharMax(EMULATION_CONV, "int_frac_digits");
        _locale.putConvEntry(EMULATION_CONV, "mon_grouping", groupingList);
        putCharMax(EMULATION_CONV, "n_sign_posn");
        putCharMax(EMULATION_CONV, "p_cs_precedes");
        putCharMax(EMULATION_CONV, "n_cs_precedes");
        putCharMax(EMULATION_CONV, "p_sep_by_space");
        putCharMax(EMULATION_CONV, "n_sep_by_space");
    }

    @Override
    public PyDictionary localeconv() {
        return EMULATION_CONV;
    }

    @Override
    public PyString getLocaleString() {
        return _locale.C_LOCALE_PY_STRING;
    }

    @Override
    public PyString getUnderlyingLocale() {
        return _locale.C_LOCALE_PY_STRING;
    }

    @Override
    public int strcoll(PyString str1, PyString str2) {
        return str1.__cmp__(str2);
    }

    @Override
    public PyString strxfrm(PyString str1) {
        return str1;
    }

    @Override
    public String[] getShortWeekdays() {
        return EN_SHORT_DAYS;
    }

    @Override
    public String[] getWeekdays() {
        return EN_DAYS;
    }

    @Override
    public String[] getShortMonths() {
        return EN_SHORT_MONTHS;
    }

    @Override
    public String[] getMonths() {
        return EN_MONTHS;
    }

    @Override
    public String[] getAmPmStrings() {
        return EN_AM_PM;
    }

}