package org.python.modules;
import java.text.Normalizer;
import java.text.Normalizer.Form;

import org.python.core.Py;
import org.python.core.PyUnicode;

/**
 * Incomplete unicodedata module.
 * 
 * This should be replaced by a unicodedata module compiled in the same way
 * as CPython's unicodedata is generated. In the meantime, this implements some
 * commonly used functions which allows Jython run some popular software (such 
 * as Django).
 */
public class unicodedata {

    /**
     * Return the normal form 'form' for the Unicode string unistr.  Valid
     * values for form are 'NFC', 'NFKC', 'NFD', and 'NFKD'. 
     */
    public static PyUnicode normalize(String form, String unistr) {
        return Py.newUnicode(Normalizer.normalize(unistr, Form.valueOf(form)));
    }
}
