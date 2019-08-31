// (c) 2019 Jython Developers
// Licensed to PSF under a contributor agreement

package org.python.modules._locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.python.core.RegistryKey.PYTHON_LOCALE_CONTROL;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.python.core.Py;
import org.python.core.PyCode;
import org.python.core.PyDictionary;
import org.python.core.PyException;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PySet;
import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.python.core.PySystemState;
import org.python.core.PyUnicode;
import org.python.util.PythonInterpreter;

public class _localeTest {

    private PySystemState systemState;
    private PythonInterpreter interp;

    @Before
    public void setUp() throws Exception {
        // Initialise a Jython interpreter
        systemState = Py.getSystemState();
        interp = new PythonInterpreter(new PyStringMap(), systemState);
    }

    @SuppressWarnings("static-access")
    private void setLocaleProperty(String value) {
        systemState.registry.setProperty(PYTHON_LOCALE_CONTROL, value);
    }

    private void defaultState() {
        setLocaleProperty("");
    }

    private void jython2LegacyState() {
        setLocaleProperty("jython2_legacy");
    }

    private void settableState() {
        setLocaleProperty("settable");
    }

    private void importAndInit() {
        interp.exec("import _locale");
        /*
         * Following line is a hacky workaround, because interpreter and PySystemState can't be
         * fully reinitialized. classDictInit() is called implicitly on first module import only.
         */
        interp.exec("_locale.classDictInit({})");
    }

    private void settableInit() {
        settableState();
        importAndInit();
    }

    private void assertPyTrue(String variableName) {
        PyObject result = interp.get(variableName);
        assertEquals(Py.True, result);
    }

    private void assertPyEquals(int expected, String valueName) {
        PyObject result = interp.get(valueName);
        assertEquals(new PyInteger(expected), result);
    }

    private void assertPyEquals(String expected, String valueName) {
        PyObject result = interp.get(valueName);
        assertEquals(new PyString(expected), result);
    }

    private void assertInterpEquals(String expected, String value) {
        PyObject result1 = interp.get(expected);
        PyObject result2 = interp.get(value);
        assertEquals(result1, result2);
    }

    @Test
    public void moduleImportSettable() {
        settableInit();
        PyObject _localeModule = interp.get("_locale");
        assertNotNull(_localeModule);
    }

    @Test(expected = PyException.class)
    public void moduleImportLegacy() {
        jython2LegacyState();
        importAndInit();
    }

    @Test(expected = PyException.class)
    public void moduleImportDefault() {
        defaultState();
        importAndInit();
    }

    private static int javaMajorVersion() {
        String versionString = System.getProperty("java.runtime.version");
        String[] javaVersionElements = versionString.split("\\.|_|-b");
        int major = Integer.valueOf(javaVersionElements[0]);
        int minor = Integer.valueOf(javaVersionElements[1]);
        if (major == 1) {
            return minor;
        }
        return major;
    }

    public void setLocaleEN_US(String tag) {
        // Anchor test to a popular locale to prove alignment
        settableInit();
        /*
         * Java 8 and Python use en-US negative currency formatting ($123). This changes to -$123
         * from 9 onwards.
         */
        int nSignPosition = 0;
        if (javaMajorVersion() >= 9) {
            nSignPosition = 1;
        }
        // @formatter:off
        String script =
                  "from _locale import setlocale, localeconv\n"
                + "setlocale(_locale.LC_ALL, '" + tag + "' ) \n"
                + "expected = "
                + "{'mon_decimal_point': '.', 'int_frac_digits': 2, "
                + " 'p_sep_by_space': 0, 'frac_digits': 2, "
                + " 'thousands_sep': ',', "
                + " 'n_sign_posn': " + String.valueOf(nSignPosition) + " , "
                + "'decimal_point': '.', 'int_curr_symbol': 'USD', "
                + "'n_cs_precedes': 1, 'p_sign_posn': 3, "
                + "'mon_thousands_sep': ',', 'negative_sign': '-', "
                + "'currency_symbol': '$', 'n_sep_by_space': 0 , "
                + "'positive_sign': '', 'p_cs_precedes': 1  } \n"
                + "actual = localeconv() \n"
                + "grouping = actual.pop('grouping',None) \n"
                + "mon_grouping = actual.pop('mon_grouping',None) \n"
                + "result = set( actual.items() ) ^ set(expected.items()) \n"
                + "resultGrouping = (grouping == [3,0] )  \n"
                + "resultMonGrouping = (mon_grouping == [3,0]  ) \n";
        // @formatter:on
        interp.exec(script);
        PyObject result = interp.get("result");
        assertEquals(new PySet(), result);
        assertPyTrue("resultGrouping");
        assertPyTrue("resultMonGrouping");
    }

    @Test
    public void setLocaleEN_US_Underscore() {
        /*
         * Locales are aliased to underscore equivalents, due to that being a common convention in
         * Python and Java itself.
         */
        setLocaleEN_US("en_US.ISO8859-1");
    }

    @Test
    public void setLocaleEN_US_Dash() {
        setLocaleEN_US("en-US");
    }

    @Test
    public void setLocaleEN_US_Tuple() {
        settableInit();
        // @formatter:off
        String script =
                  "from locale import setlocale, getlocale, LC_ALL \n"
                + "setlocale(LC_ALL,('en-US','ISO8859-1') ) \n"
                + "actual = getlocale() \n"
                + "expected = ('en_US', 'ISO8859-1') \n";
        // @formatter:on
        PyCode code = interp.compile(script);
        interp.exec(code);
        assertInterpEquals("expected", "actual");
    }

    @Test
    public void setLocaleNotLCALL() {
        settableInit();
        String script = "import locale \n" + "caught = False \n" + "try: \n"
                + "    locale.setlocale(locale.LC_MONETARY,'zh-CN') \n"
                + "except locale.Error as e: \n" + "    caught = True \n";
        // @formatter:on
        interp.exec(script);
        assertPyTrue("caught");
    }

    @Test
    public void setLocaleEmpty() {
        settableInit();
        // @formatter:off
        String script =
                "from _locale import setlocale \n"
              + "setlocale(_locale.LC_ALL,'zh_CN.UTF-8') \n"
              + "result1 = setlocale(_locale.LC_ALL) \n"
              + "result2 = setlocale(_locale.LC_MONETARY,None) \n"
              + "result3 = setlocale(_locale.LC_MONETARY,'zh_CN.UTF-8') \n";
        interp.exec(script);
        assertPyEquals("zh_CN.UTF-8", "result1");
        assertPyEquals("zh_CN.UTF-8", "result2");
        assertPyEquals("zh_CN.UTF-8", "result3");
    }

    @Test
    public void setLocaleDefault() {
        settableInit();
        // @formatter:off
        String script =
                "from locale import setlocale \n"
              + "result1 = setlocale(_locale.LC_ALL,'zh_CN.UTF-8') \n"
              + "result2 = setlocale(_locale.LC_ALL,'') ";
        // @formatter:on
        interp.exec(script);
        assertPyEquals("zh_CN.UTF-8", "result1");
        assertPyEquals(Locale.getDefault().toString() + "." + Charset.defaultCharset().name(),
                "result2");
    }

    @Test(expected = PyException.class)
    public void setLocaleInvalid() {
        settableInit();
        // @formatter:off
        String script =
            "from _locale import setlocale \n"
          + "result = setlocale(_locale.LC_ALL,'green_midget_cafe_nosuch') \n";
        // @formatter:on
        interp.exec(script);
    }

    @Test
    public void setLocaleGerman() {
        settableInit();
        // @formatter:off
        String script =
                  "from _locale import setlocale, localeconv\n"
                + "setlocale(_locale.LC_ALL,'de_DE.ISO8859-15') \n"
                + "expected = "
                + "{'mon_decimal_point': ',', 'int_frac_digits': 2, "
                + " 'p_sep_by_space': 1, 'frac_digits': 2, "
                + "'thousands_sep': '.', 'n_sign_posn': 1, "
                + "'decimal_point': ',', 'int_curr_symbol': 'EUR', "
                + "'n_cs_precedes': 0, 'p_sign_posn': 3, "
                + "'mon_thousands_sep': '.', 'negative_sign': '-', "
                + "'currency_symbol':  '\\xa4' , 'n_sep_by_space': 1, "
                + "'p_cs_precedes': 0, 'positive_sign': ''} \n"
                + "actual = localeconv() \n"
                + "grouping = actual.pop('grouping',None) \n"
                + "mon_grouping = actual.pop('mon_grouping',None) \n"
                + "result = set( actual.items() ) ^ set(expected.items()) \n"
                + "resultGrouping = (grouping == [3,0] )  \n"
                + "resultMonGrouping = (mon_grouping == [3,0]  ) \n";
        // @formatter:on
        PyCode code = interp.compile(script);
        interp.exec(code);
        PyObject result = interp.get("result");
        assertEquals(new PySet(), result);
        assertPyTrue("resultGrouping");
        assertPyTrue("resultMonGrouping");
    }

    @Test
    public void setLocaleChinaMainland() {
        settableInit();
        /*
         * Java has ¥ \uffe5 rather than 元 \u5143 for zh-CN Java has negative sign preceding, Python
         * following.
         */
        // @formatter:off
        String script =
                  "from _locale import setlocale, localeconv\n"
                + "setlocale(_locale.LC_ALL,'zh_CN.UTF-8') \n"
                + "expected = "
                + "{'mon_decimal_point': '.', 'int_frac_digits': 2, "
                + " 'p_sep_by_space': 0, 'frac_digits': 2, "
                + "'thousands_sep': ',', 'n_sign_posn': 1, "
                + "'decimal_point': '.', 'int_curr_symbol': 'CNY', "
                + "'n_cs_precedes': 1, 'p_sign_posn': 3, "
                + "'mon_thousands_sep': ',', 'negative_sign': '-', "
                + "'currency_symbol':  '\\xef\\xbf\\xa5' , "
                + "'n_sep_by_space': 0, "
                + "'p_cs_precedes': 1, 'positive_sign': ''} \n"
                + "actual = localeconv() \n"
                + "grouping = actual.pop('grouping',None) \n"
                + "mon_grouping = actual.pop('mon_grouping',None) \n"
                + "result = set( actual.items() ) ^ set(expected.items()) \n"
                + "resultGrouping = (grouping == [3,0] )  \n"
                + "resultMonGrouping = (mon_grouping == [3,0]  ) \n";
        // @formatter:on
        PyCode code = interp.compile(script);
        interp.exec(code);
        PyObject result = interp.get("result");
        assertEquals(new PySet(), result);
        assertPyTrue("resultGrouping");
        assertPyTrue("resultMonGrouping");
    }

    @Test
    public void setlocaleJPEncodingCurrencyFallback() {
        settableInit();
        /*
         * Deliberately set a Japanese locale with an ANSI US codepage lacking the ¥ \uffe5
         * character. Which is a bit weird, but shows fallback.
         */
        // @formatter:off
        String script =
                  "from _locale import setlocale, localeconv\n"
                + "setlocale(_locale.LC_ALL,'ja_JP.ISO8859-1') \n"
                + "result = localeconv()['currency_symbol']  \n";
        // @formatter:on
        interp.exec(script);
        PyObject result = interp.get("result");
        assertEquals(new PyString("JPY"), result);
    }

    /**
     * We use the unconventional "~h" as a byte marker, rather than say \x, to show the conversion
     * happens in this unit test, when troubleshooting.
     */
    private String byteStr(byte[] bytes, boolean lePadding) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            sb.append("~h");
            sb.append(Integer.toHexString(bytes[i]));
            if (lePadding) {
                sb.append("~h0");
            }
        }
        return sb.toString();
    }

    private String utf16LEByteStr(String in, boolean lePadding) {
        return byteStr(in.getBytes(StandardCharsets.UTF_16LE), lePadding);
    }

    private void assertDictValueEquals(PyDictionary dict, String expectedValue, String key) {
        assertEquals(new PyUnicode(expectedValue), dict.__getitem__(new PyString(key)));
    }

    /**
     * Assert byte transcription equality for the PyString found in {@code}dict{code} using
     * {@code}key{code} under encoding utf-16le
     */
    private void assertDictUTF16LEEquals(PyDictionary dict, String expectedStr, String key) {
        PyString value = (PyString) dict.__getitem__(new PyString(key));
        // PyString applies zero padding, we need to add it to Java String to compare
        assertEquals(utf16LEByteStr(expectedStr, true), utf16LEByteStr(value.getString(), false));
    }

    @Test
    public void utf16EncodingAndFallback() {
        /*
         * Chinese Finland is not usually found in locale databases, so this lets us create our own
         * locale with unusual symbols.
         */
        Locale unicodeMockLocale = new Locale.Builder().setLanguage("zh").setRegion("FI").build();
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(unicodeMockLocale);
        DecimalFormatSymbols dfs = df.getDecimalFormatSymbols();
        // no locale uses these, it is to trigger fallbacks
        dfs.setMinusSign('\u2212');
        dfs.setGroupingSeparator('\u65E0');
        dfs.setDecimalSeparator('\u70B9');
        df.setDecimalFormatSymbols(dfs);
        // Fallback case
        PyDictionary dict = JyLocale.localeConvForFormat(df, df, "ISO8859-1");
        assertDictValueEquals(dict, "-", "negative_sign");
        assertDictValueEquals(dict, ".", "decimal_point");
        assertDictValueEquals(dict, "", "thousands_sep");
        assertDictValueEquals(dict, "", "mon_thousands_sep");
        // Check happy path where values are compatible with the encoding
        dict = JyLocale.localeConvForFormat(df, df, "utf-16le");
        assertDictUTF16LEEquals(dict, "\u2212", "negative_sign");
        assertDictUTF16LEEquals(dict, "\u70B9", "decimal_point");
        assertDictUTF16LEEquals(dict, "\u65E0", "thousands_sep");
        assertDictUTF16LEEquals(dict, "\u65E0", "mon_thousands_sep");
    }

    @Test
    public void negativeSignPosition() {
        // per documentation for Python locale module
        Locale locale = Locale.forLanguageTag("en-US");
        DecimalFormat df = (DecimalFormat) NumberFormat.getCurrencyInstance(locale);
        df.setNegativePrefix("($"); // ($123) as in en-US Java 8
        df.setNegativeSuffix(")");
        assertEquals(0, JyLocale.negativeSignPosition(df));
        df.setNegativePrefix("-"); // -123 EUR as in es_ES Spanish
        df.setNegativeSuffix("EUR");
        assertEquals(1, JyLocale.negativeSignPosition(df));
        df.setNegativePrefix("");      // No clear example of this
        df.setNegativeSuffix("SPAM-"); // that is distinguishable from 4
        // fy Western Frisian doesn't qualify (see below) so this is a synthetic one.
        assertEquals(2, JyLocale.negativeSignPosition(df));
        df.setNegativePrefix("FCFA-"); // FCFA-123 as in sg-CF Sango (CAR)
        df.setNegativeSuffix("");
        assertEquals(3, JyLocale.negativeSignPosition(df));
        df.setNegativePrefix("CHF"); // CHF123- as in German Swiss Francs
        df.setNegativeSuffix("-");   // Python de_CH.ISO8859-1
        assertEquals(4, JyLocale.negativeSignPosition(df));
        df.setNegativePrefix("¤ "); // U0164 fy Western Frisian is the only Python
        df.setNegativeSuffix("-");  // example of 2, and appears identical to 4
        assertEquals(4, JyLocale.negativeSignPosition(df));
        df.setNegativePrefix(""); // 123 as in empty locales
        df.setNegativeSuffix("");
        assertEquals(_locale.CHAR_MAX, JyLocale.negativeSignPosition(df));
    }

    @Test
    public void positiveCurrencyPrecedesValue() {
        Locale locale = Locale.forLanguageTag("en-US");
        DecimalFormat df = (DecimalFormat) NumberFormat.getCurrencyInstance(locale);
        df.setPositivePrefix("$"); // $123 as in en-US Java 8
        df.setPositiveSuffix("");
        assertEquals(1, JyLocale.positiveCurrencyPrecedesValue(df));
        df.setPositivePrefix(""); // 123$ (similar to 123 CHF)
        df.setPositiveSuffix(" $");
        assertEquals(0, JyLocale.positiveCurrencyPrecedesValue(df));
    }

    @Test
    public void negativeCurrencyPrecedesValue() {
        Locale locale = Locale.forLanguageTag("en-US");
        DecimalFormat df = (DecimalFormat) NumberFormat.getCurrencyInstance(locale);
        df.setNegativePrefix("($"); // ($123) as in en-US Java 8
        df.setNegativeSuffix(")");
        assertEquals(1, JyLocale.negativeCurrencyPrecedesValue(df));
        df.setNegativePrefix(""); // 123$ (similar to 123 CHF)
        df.setNegativeSuffix(" $");
        assertEquals(0, JyLocale.negativeCurrencyPrecedesValue(df));
    }

    @Test
    public void positiveSeparatedBySpace() {
        Locale locale = Locale.forLanguageTag("en-US");
        DecimalFormat df = (DecimalFormat) NumberFormat.getCurrencyInstance(locale);
        df.setPositivePrefix("$"); // $123 as in en-US Java 8
        df.setPositiveSuffix("");
        assertEquals(0, JyLocale.positiveSeparatedBySpace(df));
        df.setPositivePrefix(""); // 123 CHF
        df.setPositiveSuffix(" CHF");
        assertEquals(1, JyLocale.positiveSeparatedBySpace(df));
    }

    @Test
    public void negativeSeparatedBySpace() {
        Locale locale = Locale.forLanguageTag("de-DE");
        DecimalFormat df = (DecimalFormat) NumberFormat.getCurrencyInstance(locale);
        // Many Java locales use U+00A0, non-breaking space, for spacing, rather than " ".
        assertEquals(1, JyLocale.negativeSeparatedBySpace(df));
        df.setNegativePrefix(""); // -123 €
        df.setNegativeSuffix("\u00A0" + "€");
        assertEquals(1, JyLocale.negativeSeparatedBySpace(df));
        df.setNegativePrefix(""); // 123 EUR/
        df.setNegativeSuffix(" EUR");
        assertEquals(1, JyLocale.negativeSeparatedBySpace(df));
        // Many Java locales use
        df.setNegativePrefix("-$"); // -$123
        df.setNegativeSuffix("");
        assertEquals(0, JyLocale.negativeSeparatedBySpace(df));
    }

    @Test
    public void extendedWhitespace() {
        assertTrue(JyLocale.isExtendedWhitespace(' '));
        assertFalse(JyLocale.isExtendedWhitespace('a'));
        assertTrue(JyLocale.isExtendedWhitespace('\u00A0'));
        assertFalse(JyLocale.isExtendedWhitespace('€'));
    }

    @Test
    public void getlocale() {
        settableInit();
        // @formatter:off
        String script =
                  "from locale import normalize, setlocale, getlocale, LC_ALL, LC_NUMERIC\n"
                + "norm = normalize('zh_CN') \n"
                + "setlocale(LC_ALL,('zh_CN',None)) \n"
                + "actual = getlocale() \n" + "expected = ('zh_CN', 'gb2312') \n";
        // @formatter:on
        PyCode code = interp.compile(script);
        interp.exec(code);
        PyObject norm = interp.get("norm");
        assertEquals(new PyString("zh_CN.gb2312"), norm);
        PyObject actual = interp.get("actual");
        PyObject expected = interp.get("expected");
        assertEquals(expected, actual);
        script = "actual = getlocale(LC_NUMERIC) \n";
        interp.exec(script);
        actual = interp.get("actual");
        assertEquals(expected, actual);
    }

    @Test
    public void setlocaleC() {
        settableInit();
        // @formatter:off
        String script =
                  "from locale import setlocale, getlocale, localeconv, LC_ALL\n"
                + "setlocale(LC_ALL,'C') \n"
                + "actualLocale = getlocale() \n"
                + "actual = localeconv() \n"
                + "expected = { "
                + "  'mon_decimal_point': '', 'int_frac_digits': 127, "
                + "  'p_sep_by_space': 127, 'frac_digits': 127, "
                + "  'thousands_sep': '', 'n_sign_posn': 127, "
                + "  'decimal_point': '.', 'int_curr_symbol': '', "
                + "  'n_cs_precedes': 127, 'p_sign_posn': 127, "
                + "  'mon_thousands_sep': '', 'negative_sign': '', "
                + "  'currency_symbol': '', 'n_sep_by_space': 127, "
                + "  'p_cs_precedes': 127, 'positive_sign': '' } \n"
                + "expectedLocale = (None,None) \n"
                + "grouping = actual.pop('grouping',None) \n"
                + "mon_grouping = actual.pop('mon_grouping',None) \n"
                + "result = set( actual.items() ) ^ set(expected.items()) \n"
                + "resultGrouping = (grouping == [] )  \n"
                + "resultMonGrouping = (mon_grouping == []  ) \n";
        // @formatter:on
        interp.exec(script);
        assertInterpEquals("expectedLocale", "actualLocale");
        PyObject result = interp.get("result");
        assertEquals(new PySet(), result);
        assertPyTrue("resultGrouping");
        assertPyTrue("resultMonGrouping");
    }

    @Test
    public void strCompareUS() {
        settableInit();
        _locale.setlocale(_locale.LC_ALL, new PyString("en_US"));
        assertEquals(-1, _locale.strcoll(new PyString("aaa"), new PyString("baa")));
        assertEquals(1, _locale.strcoll(new PyString("baa"), new PyString("aaa")));
        assertEquals(0, _locale.strcoll(new PyString("knight"), new PyString("knight")));
    }

    @Test
    public void strCompareChineseMainlandInterp() {
        Py.writeDebug("_localeTest", "strCompareCMI()");
        // also test exposure through locale itself here
        settableInit();
        // @formatter:off
        String script = "from _locale import setlocale, LC_ALL, strcoll \n"
                + "setlocale(LC_ALL, 'zh_CN.UTF-8'  ) \n"
                + "resultSame = strcoll( u'\\u4e00', u'\\u4e00' ) \n" // yi / yi
                + "resultLvsU = strcoll( '\\xe4\\xb8\\x80', u'\\u4e00' ) \n"
                + "resultAscii = strcoll( 'b', 'a' ) \n"
                + "resultUltU1 = strcoll( u'\\u4e00', u'\\u5f00' ) \n" // yi / kai
                + "resultUltU2 = strcoll( u'\\u597d', u'\\u4e00' ) \n"; // hao / yi
        // @formatter:on
        interp.exec(script);
        assertPyEquals(0, "resultSame");
        assertPyEquals(0, "resultLvsU");
        assertPyEquals(1, "resultAscii");
        assertPyEquals(1, "resultUltU1");
        assertPyEquals(-1, "resultUltU2");
    }

    @Test
    public void strCompareC() {
        Py.writeDebug("_localeTest", "strCompareC()");
        // also test exposure through locale itself here
        settableInit();
        // @formatter:off
        String script =
                  "from locale import setlocale, LC_ALL, strcoll \n"
                + "setlocale(LC_ALL, 'C' ) \n"
                + "result1 = strcoll( 'a', 'b' ) \n"
                + "result2 = strcoll( 'b', 'a' ) \n";
        // @formatter:on
        interp.exec(script);
        assertPyEquals(-1, "result1");
        assertPyEquals(1, "result2");
    }

    @Test
    public void strxfrm() {
        Py.writeDebug("_localeTest", "strxfrm()");
        settableInit();
        // @formatter:off
        String script =
                  "from _locale import setlocale, LC_ALL, strxfrm \n"
                + "setlocale(LC_ALL, 'zh_CN.UTF-8'  ) \n"
                + "resultU = strxfrm( u'\\u4e00') \n"
                + "resultL = strxfrm( '\\xe4\\xb8\\x80' ) \n"
                + "result1 = (resultU == u'\\u4e00')  \n"
                + "result2 = (resultU == resultL)  \n" ;
        // @formatter:on
        interp.exec(script);
        assertPyTrue("result1");
        assertPyTrue("result2");
    }

    @Test
    public void dateSymbols() {
        settableInit();
        // @formatter:off
        String script =
                "from locale import setlocale, LC_ALL \n" +
                "from datetime import datetime \n" +
                "setlocale(LC_ALL, 'C' ) \n" +
                "resultC = datetime(2019,04,15,15, 56, 44).strftime('%c') \n" +
                "setlocale(LC_ALL, 'de_DE' ) \n" +
                "resultD = datetime(1919,05,16,15, 56, 44).strftime('%A %Y') \n" ;
        // @formatter:on
        interp.exec(script);
        assertPyEquals("Mon Apr 15 15:56:44 2019", "resultC");
        assertPyEquals("Freitag 1919", "resultD");
    }

}
