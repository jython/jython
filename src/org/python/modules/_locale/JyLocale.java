// (c) 2019 Jython Developers
// Licensed to PSF under a contributor agreement

package org.python.modules._locale;

import java.text.Collator;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyException;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyString;
import org.python.core.PyUnicode;

/**
 * Sources locale information from standard Java API functions, such as that in
 * {@link java.util.Locale} and {@link java.text.DecimalFormat}.
 *
 * Used by the _locale module. Callers would not usually interact with this class directly unless
 * working with _locale internals.
 *
 * @since Jython 2.7.2
 */
public class JyLocale extends DateSymbolJyLocale implements PyLocale {

    private final String encoding;
    private final PyDictionary conv;
    private final Collator collator;

    public JyLocale(Locale locale, String encoding) {
        super(locale);
        this.encoding = encoding;
        this.conv = initLocaleConv();
        this.collator = Collator.getInstance(locale);
    }

    @Override
    public PyDictionary localeconv() {
        return conv;
    }

    private PyDictionary initLocaleConv() {
        DecimalFormat df = (DecimalFormat) NumberFormat.getInstance(locale);
        DecimalFormat cf = (DecimalFormat) NumberFormat.getCurrencyInstance(locale);
        return localeConvForFormat(df, cf, getEncoding());
    }

    /**
     * Fallbacks provided for charset conversion failures in this method match either the C locale,
     * or plausible lower-risk substitutes, like ISO currency code for currency symbol.
     */
    static PyDictionary localeConvForFormat(DecimalFormat decimalFormat,
            DecimalFormat currencyFormat, String encoding) {
        PyDictionary result = new PyDictionary();
        DecimalFormatSymbols dfs = decimalFormat.getDecimalFormatSymbols();
        DecimalFormatSymbols cfs = currencyFormat.getDecimalFormatSymbols();
        putConvUnicodeEntry(result, "decimal_point", dfs.getDecimalSeparator(), ".", encoding);
        putConvUnicodeEntry(result, "thousands_sep", dfs.getGroupingSeparator(), "", encoding);
        putConvUnicodeEntry(result, "currency_symbol", cfs.getCurrencySymbol(),
                dfs.getInternationalCurrencySymbol(), encoding);
        _locale.putConvEntry(result, "int_curr_symbol", cfs.getInternationalCurrencySymbol());
        putConvUnicodeEntry(result, "negative_sign", cfs.getMinusSign(), "-", encoding);
        // No positive sign concept in Java locale
        _locale.putConvEntry(result, "positive_sign", new PyString(""));
        _locale.putConvEntry(result, "p_sign_posn", new PyInteger(3));
        PyList groupingList = new PyList();
        groupingList.add(new PyInteger(decimalFormat.getGroupingSize()));
        groupingList.add(new PyInteger(0));
        _locale.putConvEntry(result, "grouping", groupingList);
        _locale.putConvEntry(result, "mon_decimal_point", cfs.getMonetaryDecimalSeparator());
        putConvUnicodeEntry(result, "mon_thousands_sep", cfs.getGroupingSeparator(), "", encoding);
        _locale.putConvEntry(result, "frac_digits",
                new PyInteger(currencyFormat.getMaximumFractionDigits()));
        _locale.putConvEntry(result, "int_frac_digits",
                new PyInteger(currencyFormat.getMaximumFractionDigits()));
        PyList monGroupingList = new PyList();
        monGroupingList.add(new PyInteger(currencyFormat.getGroupingSize()));
        monGroupingList.add(new PyInteger(0));
        _locale.putConvEntry(result, "mon_grouping", monGroupingList);
        _locale.putConvEntry(result, "n_sign_posn",
                new PyInteger(negativeSignPosition(currencyFormat)));
        _locale.putConvEntry(result, "p_cs_precedes",
                new PyInteger(positiveCurrencyPrecedesValue(currencyFormat)));
        _locale.putConvEntry(result, "n_cs_precedes",
                new PyInteger(negativeCurrencyPrecedesValue(currencyFormat)));
        _locale.putConvEntry(result, "p_sep_by_space",
                new PyInteger(positiveSeparatedBySpace(currencyFormat)));
        _locale.putConvEntry(result, "n_sep_by_space",
                new PyInteger(negativeSeparatedBySpace(currencyFormat)));
        return result;
    }

    private static void putConvUnicodeEntry(PyDictionary result, String key, String value,
            String fallback, String encoding) {
        try {
            result.put(new PyString(key), new PyString(new PyUnicode(value).encode(encoding)));
        } catch (PyException pye) {
            encodingFallback(result, key, fallback, pye);
        }
    }

    private static void putConvUnicodeEntry(PyDictionary result, String key, char value,
            String fallback, String encoding) {
        try {
            result.put(new PyString(key), new PyString(new PyUnicode(value).encode(encoding)));
        } catch (PyException pye) {
            encodingFallback(result, key, fallback, pye);
        }
    }

    private static void encodingFallback(PyDictionary result, String key, String fallback,
            PyException pye) {
        Py.writeComment("_locale",
                "Could not encode value for key " + key + " " + pye.getMessage());
        result.put(new PyString(key), new PyString(fallback));
    }

    @Override
    public PyString getLocaleString() {
        return new PyString(locale.toString() + "." + encoding);
    }

    @Override
    public PyString getUnderlyingLocale() {
        return new PyString(locale.toString());
    }

    @Override
    public int strcoll(PyString str1, PyString str2) {
        return collator.compare(unicoder(str1), unicoder(str2));
    }

    private String unicoder(PyString str) {
        return strxfrm(str).toString();
    }

    @Override
    public PyString strxfrm(PyString str) {
        if (str instanceof PyUnicode) {
            return str;
        }
        return (PyString) str.decode(encoding);
    }

    public String getEncoding() {
        return encoding;
    }

    static int negativeSignPosition(DecimalFormat df) {
        String prefix = df.getNegativePrefix();
        String suffix = df.getNegativeSuffix();
        if ("".equals(suffix)) {
            if ("".equals(prefix)) {
                // Nothing is specified in this locale.
                return _locale.CHAR_MAX;
            }
        }
        if (prefix.startsWith("(") && suffix.endsWith(")")) {
            // Currency and value are surrounded by parentheses.
            return 0;
        }
        final String MINUS = String.valueOf(df.getDecimalFormatSymbols().getMinusSign());
        if (prefix.startsWith(MINUS)) {
            // The sign should precede the value and currency symbol.
            return 1;
        } else if (prefix.endsWith(MINUS)) {
            // The sign should immediately precede the value.
            return 3;
        } else if (suffix.startsWith(MINUS)) {
            // The sign should immediately follow the value.
            return 4;
        }
        // The sign should follow the value and currency symbol.
        return 2;
    }

    static int positiveCurrencyPrecedesValue(DecimalFormat df) {
        return currencyPrecedesValue(df, df.getPositivePrefix());
    }

    static int negativeCurrencyPrecedesValue(DecimalFormat df) {
        return currencyPrecedesValue(df, df.getNegativePrefix());
    }

    private static int currencyPrecedesValue(DecimalFormat df, String prefix) {
        if (prefix.contains(df.getDecimalFormatSymbols().getCurrencySymbol())) {
            return 1;
        }
        return 0;
    }

    static int positiveSeparatedBySpace(DecimalFormat df) {
        return separatedBySpace(df.getPositivePrefix(), df.getPositiveSuffix());
    }

    static int negativeSeparatedBySpace(DecimalFormat df) {
        return separatedBySpace(df.getNegativePrefix(), df.getNegativeSuffix());
    }

    private static int separatedBySpace(String prefix, String suffix) {
        if ((!prefix.isEmpty() && isExtendedWhitespace(prefix.charAt(prefix.length() - 1)))) {
            return 1;
        } else if ((!suffix.isEmpty() && isExtendedWhitespace(suffix.charAt(0)))) {
            return 1;
        }
        return 0;
    }

    /**
     * Includes non-breaking space, but not extended codepoints
     */
    public static boolean isExtendedWhitespace(char c) {
        return Character.isWhitespace(c) || '\u00A0' == c;
    }

}