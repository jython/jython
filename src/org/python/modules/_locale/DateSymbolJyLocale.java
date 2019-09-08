package org.python.modules._locale;

import java.text.DateFormatSymbols;
import java.util.Locale;

/**
 * Separating these largely constant values from Python encoding conversions allows for safer
 * initialization even if modules are loaded in different orders. The Python {@code locale} and
 * {@code codecs} modules have interdependencies, as well as {@link org.python.modules.time.Time}
 * and {@link org.python.modules._locale._locale}. The latter in particular depends on
 * {@code java.util.Locale} and this class, but only uses the date symbol aspect.
 *
 * @since Jython 2.7.2
 */
public class DateSymbolJyLocale implements DateSymbolLocale {

    protected final Locale locale;
    protected final DateFormatSymbols dfSymbols;

    public DateSymbolJyLocale(Locale locale) {
        super();
        this.locale = locale;
        this.dfSymbols = DateFormatSymbols.getInstance(locale);
    }

    @Override
    public String[] getShortWeekdays() {
        return dfSymbols.getShortWeekdays();
    }

    @Override
    public String[] getWeekdays() {
        return dfSymbols.getWeekdays();
    }

    @Override
    public String[] getShortMonths() {
        return dfSymbols.getShortMonths();
    }

    @Override
    public String[] getMonths() {
        return dfSymbols.getMonths();
    }

    @Override
    public String[] getAmPmStrings() {
        return dfSymbols.getAmPmStrings();
    }

}