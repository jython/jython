package org.python.modules._locale;

/**
 * Date related string values.
 *
 * @since Jython 2.7.2
 */
public interface DateSymbolLocale {

    public String[] getShortWeekdays();

    public String[] getShortMonths();

    public String[] getMonths();

    public String[] getAmPmStrings();

    public String[] getWeekdays();

}