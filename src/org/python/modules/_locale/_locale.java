// (c) 2019 Jython Developers
// Licensed to PSF under a contributor agreement

package org.python.modules._locale;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyException;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.python.core.PySystemState;

import static org.python.core.RegistryKey.PYTHON_LOCALE_CONTROL;

/**
 * Java native implementation of underlying locale functions, fitting the interface defined or
 * implied by the Python locale module.
 *
 * Functional documentation can be found in the Python module docstring. This class depends on
 * registry key {@link org.python.core.RegistryKey#PYTHON_LOCALE_CONTROL}. Additional implementation
 * details for users of this class are captured below.
 * <p>
 * Unicode. In general, Java and its locale settings are quite unicode heavy, making rich use of
 * different symbols for e.g. currencies. This solution allows some of that to leak through by
 * encoding the Java locale value with the current character encoding. The Python locale module is
 * quite conservative and tends to keep things as solely strings. Python 2.x usually supports only
 * string / ascii by default in many cases, and only unicode when asked. This is a little less
 * conservative while only allowing values consistent with the current encoding.
 * <p>
 * An example of this is some Python implementations using EUR rather than the Euro symbol €
 * ({@code 'u20ac'}), probably because € is only available in Unicode. In the Java implementation,
 * if UTF-8 encoding is set, the resulting entry in {@code localeconv()['currency_code']} is
 * {@code '\xe2\x82\xac'}. This can be used by {@code print()} and related functions directly.
 * Encoding failures for {@code currency_symbol} fall back to {@code int_curr_symbol} (eg EUR).
 * Encoding failures for {@code negative_sign}, which are sometimes unicode, fall back to an ANSI
 * hyphen ('-'). Other encoding failures fallback to the 'C' locale values, where possible.
 * <p>
 * The C emulation locale uses only ANSI characters in non-unicode strings, in keeping with it being
 * the locale of last resort and maximum compatibility.
 * <p>
 * Positive sign position ({@code p_sign_posn}) is not a distinguished concept in Java and so this
 * is hardcoded to 3.
 * <p>
 * This class ensures read/write consistency, when changing locales via
 * {@link #setlocale(PyInteger,PyString)}, by declaring private variable {@code currentLocale} as
 * volatile, and only assigning it immutable objects. It does not lock to guarantee sequencing of
 * {@code setLocale()} calls from separate threads. In the rare cases where that would be needed, it
 * is the responsibility of the calling code.
 *
 * @since Jython 2.7.2
 */
public class _locale implements ClassDictInit {

    /**
     * The {@code locale} module exposes {@code _locale.Error} as {@code locale.Error}. Some Python
     * functions, including {@code strptime()}, depend on {@code locale.Error}, making it part of
     * the public API for native locale implementations.
     */
    public static PyObject Error;

    public static PyException LocaleException(String message) {
        return new PyException(Error, message);
    }

    // @formatter:off
    public static final PyString __doc__ = new PyString(
        "The _locale module exposes locale functions in the underlying "
        + "operating system or platform in a consistent way, for use by "
        + "the Python locale module. It is a native Java implementation, "
        + "paralleling libmodule.c in CPython and its dependence on C "
        + "libraries provided by the OS.\n"
        + "This module is currently in beta, and is enabled with the "
        + "registry property ``python.locale.control``. It is "
        + "disabled by default, resulting in jython 2.7.1 (and previous) "
        + "locale behaviour.\n"
        + "\n"
        + "+-------------------+-----------------------------------------+\n"
        + "+ Property Value    + Behaviour                               |\n"
        + "+===================+=========================================+\n"
        + "| jython2_legacy    | Mix of implicit Java locale and         |\n"
        + "|                   | emulated 'C'. Text and date formatting  |\n"
        + "|                   | mostly Java locale (eg ``%b``).         |\n"
        + "|                   | ``asctime()`` matches C emulation       |\n"
        + "|                   | ``setlocale()`` always errors by design.|\n"
        + "|                   | This matches version 2.7.1 and previous |\n"
        + "|                   | behaviour. Will be deprecated in future |\n"
        + "|                   | versions.                               |\n"
        + "+-------------------+-----------------------------------------+\n"
        + "| settable          | Java platform locale services made      |\n"
        + "|                   | available via locale module.            |\n"
        + "|                   | ``setlocale()`` works, using Java locale|\n"
        + "|                   | identifiers and LC_ALL (only). C locale |\n"
        + "|                   | available via ``setlocale()``. Initial  |\n"
        + "|                   | locale and encoding per Java defaults.  |\n"
        + "+-------------------+-----------------------------------------+\n"
        + "| '' or unset       | As per ``jython2_legacy``.              |\n"
        + "|                   | In future versions, per ``settable``.   |\n"
        + "+-------------------+-----------------------------------------+\n");
    // @formatter:on

    public static final String LOCALE_CONTROL_SETTABLE = "settable";
    public static final String LOCALE_CONTROL_JYTHON2_LEGACY = "jython2_legacy";

    // These values are the same as glibc locale/bits/locale.h
    // Specific integer constants are not mandated by the POSIX locale spec, so an existing
    // standard seemed as good an arbitrary set of values as any
    private static final int __LC_CTYPE = 0;
    private static final int __LC_NUMERIC = 1;
    private static final int __LC_TIME = 2;
    private static final int __LC_COLLATE = 3;
    private static final int __LC_MONETARY = 4;
    private static final int __LC_MESSAGES = 5;
    private static final int __LC_ALL = 6;
    @SuppressWarnings("unused")
    private static final int __LC_PAPER = 7;
    @SuppressWarnings("unused")
    private static final int __LC_NAME = 8;
    @SuppressWarnings("unused")
    private static final int __LC_ADDRESS = 9;
    @SuppressWarnings("unused")
    private static final int __LC_TELEPHONE = 10;
    @SuppressWarnings("unused")
    private static final int __LC_MEASUREMENT = 11;
    @SuppressWarnings("unused")
    private static final int __LC_IDENTIFICATION = 12;

    public static final PyInteger LC_CTYPE = new PyInteger(__LC_CTYPE);
    public static final PyInteger LC_NUMERIC = new PyInteger(__LC_NUMERIC);
    public static final PyInteger LC_TIME = new PyInteger(__LC_TIME);
    public static final PyInteger LC_COLLATE = new PyInteger(__LC_COLLATE);
    public static final PyInteger LC_MONETARY = new PyInteger(__LC_MONETARY);
    public static final PyInteger LC_MESSAGES = new PyInteger(__LC_MESSAGES);
    public static final PyInteger LC_ALL = new PyInteger(__LC_ALL);
    // Remaining constants not used by locale.py so are not exposed here

    // 127 chosen as consistent with hardcoded default in locale.py for C locale
    public static final int CHAR_MAX = 127;
    public static final PyInteger CHAR_MAX_PY_INT = new PyInteger(127);
    public static final PyString C_LOCALE_PY_STRING = new PyString("C");
    private static final Set<Locale> AVAILABLE_LOCALES;

    // Failsafe for date symbols only. Insufficient for full locale module behaviour.
    private static final DateSymbolLocale DEFAULT_LOCALE =
            new DateSymbolJyLocale(Locale.getDefault());

    // This has a subtle initialization dependency on the codecs module due to the initialization
    // in JyLocale(). Let the interpreter initialize via classDictInit().
    private static volatile PyLocale currentLocale = null;

    static {
        final Set<Locale> set = new HashSet<>(Arrays.asList(Locale.getAvailableLocales()));
        AVAILABLE_LOCALES = Collections.unmodifiableSet(set);
    }

    @SuppressWarnings("serial")
    public static void initClassExceptions(PyObject exceptions) {
        PyObject baseException = exceptions.__finditem__("BaseException");
        Error = Py.makeClass("locale.Error", baseException, new PyStringMap() {

            {
                __setitem__("__module__", Py.newString("locale"));
            }
        });
    }

    // This is used when the module is first initialized, or when simulating re-initialization from
    // tests
    public static void classDictInit(PyObject dict) {
        dict.__setitem__("Error", Error);
        try {
            changeCurrentLocaleToDefault();
        } catch (Throwable t) {
            throw Py.ImportError(
                    "Jython failed to load default Java locale, falling back. " + t.getMessage());
        }
        final String localeControl = PySystemState.registry.getProperty(PYTHON_LOCALE_CONTROL, "");
        if ("".equals(localeControl) || LOCALE_CONTROL_JYTHON2_LEGACY.equals(localeControl)) {
            throw Py.ImportError(
                    "Jython locale support not enabled with python.locale.control, falling back");
        }
        if (!LOCALE_CONTROL_SETTABLE.equals(localeControl)) {
            throw Py.ImportError(
                    "Jython locale support python.locale.control unrecognized value, falling back");
        }
        changeCurrentLocaleToC();
    }

    private static void changeCurrentLocaleToDefault() {
        currentLocale = new JyLocale(Locale.getDefault(), Charset.defaultCharset().name());
    }

    /**
     * Put {@code key} mapping to {@code value} into {@code result}, converting to {@code PyString}s
     * as needed
     */
    protected static void putConvEntry(PyDictionary result, String key, String value) {
        try {
            result.put(new PyString(key), new PyString(value));
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException(iae.getMessage() + " for key " + key);
        }
    }

    /**
     * Put {@code key} mapping to {@code value} into {@code result}, converting to {@code PyString}s
     * as needed
     */
    protected static void putConvEntry(PyDictionary result, String key, char value) {
        try {
            result.put(new PyString(key), new PyString(value));
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException(iae.getMessage() + " for key " + key);
        }
    }

    /**
     * Put {@code key} mapping to {@code value} into {@code result}, converting {@code key} to
     * {@code PyString}.
     */
    protected static void putConvEntry(PyDictionary result, String key, PyObject value) {
        try {
            result.put(new PyString(key), value);
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException(iae.getMessage() + " for key " + key);
        }
    }

    public static PyString __doc___localeconv =
            new PyString("Database of local conventions, mapped from Java.\n"
                    + "Note that Java locale information is relatively unicode-rich, "
                    + "particularly for currency symbols. Where that is not supported "
                    + "by the current encoding, the international currency symbol is "//
                    + "used.\n"//
                    + "C locale emulation matches CPython C locale emulation.");

    public static PyDictionary localeconv() {
        return currentLocale.localeconv();
    }

    public static PyString __doc___strcoll =
            new PyString("Compare Java (unicode) strings using the Java collator for the "
                    + "current locale, and the encoding normalization provided by strxfrm\n."
                    + "In the 'C' locale, this simply calls str.__cmp__() ");

    public static int strcoll(PyString str1, PyString str2) {
        return currentLocale.strcoll(str1, str2);
    }

    public static PyString __doc___strxfrm =
            new PyString("Normalize a string to unicode, for common comparison, using the "
                    + "locale encoding.\n"
                    + "In the 'C' locale, this is a no-op, returning the parameter.");

    public static PyString strxfrm(PyString str1) {
        return currentLocale.strxfrm(str1);
    }

    public static PyString __doc___setlocale =
            new PyString("Sets the locale given a category and normalized locale string.\n"
                    + "Only category LC_ALL is supported. Other categories such as "
                    + "LC_TIME or LC_NUMERIC result in a locale.Error." + "\n"//
                    + "Normalized format is per RFC 1766, but using underscores instead "
                    + "of dashes, eg zh_CH.UTF-8. Briefly, the format is "
                    + "[language]_[locality].[encoding], where [language] is a two "
                    + "character language identifer, locality is a regional indicator, "
                    + "and encoding is a character encoding. Other examples are "
                    + "en_AU.ISO8859-1 and de_DE.ISO8859-15.\n"
                    + "If this is an empty locale, the same language tag will be tried "
                    + "with dashes instead of underscores, ie [lanuage]-[locality].\n"
                    + "Normalization would usually be done by the enclosing locale module.\n"
                    + "This function treats a missing encoding as using UTF-8.\n");

    public static PyString setlocale(PyInteger category) {
        return currentLocale.getLocaleString();
    }

    /**
     * Java Locale loading behaviour is quite forgiving, or uninformative, depending on your
     * perspective. It will return a dummy locale for any language tag that fits the syntax, or make
     * various attempts to approximate a locale. This solution instead follows the stricter Python
     * behaviour of requiring a particular locale to be installed.
     */
    public static PyString setlocale(PyInteger category, PyString locale) {
        Py.writeDebug("_locale", "setlocale(category,locale==" + locale + ")");
        if (locale == null || locale.equals(Py.None)
                || locale.equals(currentLocale.getLocaleString())) {
            return setlocale(category);
        }
        if (!category.equals(LC_ALL)) {
            // Py.NotImplementedError not used as dependencies exist on
            // locale.Error
            throw LocaleException("Only LC_ALL is supported in this version of Jython");
        }
        if (locale.equals(C_LOCALE_PY_STRING)) {
            changeCurrentLocaleToC();
            return C_LOCALE_PY_STRING;
        }
        if (locale.equals(new PyString(""))) {
            changeCurrentLocaleToDefault();
        } else {
            changeLocaleFromLocaleString(locale);
        }
        return currentLocale.getLocaleString();
    }

    private static void changeLocaleFromLocaleString(PyString locale) {
        String localeStr = locale.toString();
        String[] localeParts = localeStr.split("\\.");
        String language;
        String encoding = "UTF-8";
        if (localeParts.length >= 3) {
            throw LocaleException("Does not conform to [language]_[locality].[encoding] format: "
                    + locale.toString());
        }
        if (localeParts.length == 1) {
            language = localeStr;
        } else {
            // == 2
            language = localeParts[0];
            encoding = localeParts[1];
        }
        Locale newLocale = loadLocale(language);
        if (!isAvailableLocale(newLocale)) {
            String underscoreAlias = language.replace('_', '-');
            // Not only are these often switched in Java, Locale.toLanguageTag() uses "-" and
            // Locale.toString() uses "_"
            newLocale = loadLocale(underscoreAlias);
            if (!isAvailableLocale(newLocale)) {
                throw LocaleException("unsupported locale setting: " + locale.toString());
            }
        }
        changeCurrentLocale(new JyLocale(newLocale, encoding));
    }

    private static void changeCurrentLocaleToC() {
        changeCurrentLocale(new CEmulationLocale());
    }

    private static boolean isAvailableLocale(Locale locale) {
        return locale != null && !locale.toString().isEmpty() && AVAILABLE_LOCALES.contains(locale);
    }

    private static Locale loadLocale(String language) {
        Locale newLocale = Locale.forLanguageTag(language);
        Py.writeDebug("_locale", "Current: " + currentLocale.getUnderlyingLocale());
        Py.writeDebug("_locale", "New: " + newLocale + " loaded with tag: " + language);
        return newLocale;
    }

    private static void changeCurrentLocale(PyLocale pyLocale) {
        Py.writeDebug("_locale", "Locale changed to: " + pyLocale.getLocaleString());
        currentLocale = pyLocale;
    }

    /**
     * Current {@code DateSymbolLocale} used by the Python interpreter. This object will no longer
     * reflect the current state after subsequent calls to {@code setlocale}.
     */
    public static DateSymbolLocale getDateSymbolLocale() {
        if (currentLocale == null) {
            return DEFAULT_LOCALE;
        }
        return currentLocale;
    }

}
