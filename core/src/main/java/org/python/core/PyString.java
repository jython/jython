// Copyright (c)2021 Jython Developers.
// Licensed to PSF under a contributor agreement.
// @formatter:off
package org.python.core;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.python.core.stringlib.FloatFormatter;
import org.python.core.stringlib.IntegerFormatter;
import org.python.core.stringlib.InternalFormat;
import org.python.core.stringlib.InternalFormat.Formatter;
import org.python.core.stringlib.InternalFormat.Spec;
import org.python.core.stringlib.TextFormatter;
import org.python.modules.ucnhashAPI;

/**
 * A builtin python string.
 */
/*
@ExposedType(name = "str", base = PyBaseString.class, doc = BuiltinDocs.str_doc)
*/
public class PyString extends PyBaseString /*implements BufferProtocol*/ {

    public static final PyType TYPE = PyType.fromClass(PyString.class);
    protected String string; // cannot make final because of Python intern support
    protected transient boolean interned = false;
    /** Supports the buffer API, see {@link #getBuffer(int)}. */
//    private Reference<BaseBuffer> export;

    public String getString() {
        return string;
    }

    // for PyJavaClass.init()
    public PyString() {
        this("", true);
    }

    protected PyString(PyType subType, String string, boolean isBytes) {
        super(subType);
        if (string == null) {
            throw new IllegalArgumentException("Cannot create PyString from null");
        } else if (!isBytes && !isBytes(string)) {
            throw new IllegalArgumentException("Cannot create PyString with non-byte value");
        }
        this.string = string;
    }

    /**
     * Fundamental constructor for <code>PyString</code> objects when the client provides a Java
     * <code>String</code>, necessitating that we range check the characters.
     *
     * @param subType the actual type being constructed
     * @param string a Java String to be wrapped
     */
    public PyString(PyType subType, String string) {
        this(subType, string, false);
    }

    public PyString(String string) {
        this(TYPE, string);
    }

    PyString(StringBuilder buffer) {
        this(TYPE, buffer.toString());
    }

    /**
     * Local-use constructor in which the client is allowed to guarantee that the
     * <code>String</code> argument contains only characters in the byte range. We do not then
     * range-check the characters.
     *
     * @param string a Java String to be wrapped (not null)
     * @param isBytes true if the client guarantees we are dealing with bytes
     */
    private PyString(String string, boolean isBytes) {
        super(TYPE);
        if (isBytes || isBytes(string)) {
            this.string = string;
        } else {
            throw new IllegalArgumentException("Cannot create PyString with non-byte value");
        }
    }

    // Added to satisfy references
    PyType getType() { return TYPE; }

    /**
     * Determine whether a string consists entirely of characters in the range 0 to 255. Only such
     * characters are allowed in the <code>PyString</code> (<code>str</code>) type, when it is not a
     * {@link PyUnicode}.
     *
     * @return true if and only if every character has a code less than 256
     */
    private static boolean isBytes(String s) {
        int k = s.length();
        if (k == 0) {
            return true;
        } else {
            // Bitwise-or the character codes together in order to test once.
            char c = 0;
            // Blocks of 8 to reduce loop tests
            while (k > 8) {
                c |= s.charAt(--k);
                c |= s.charAt(--k);
                c |= s.charAt(--k);
                c |= s.charAt(--k);
                c |= s.charAt(--k);
                c |= s.charAt(--k);
                c |= s.charAt(--k);
                c |= s.charAt(--k);
            }
            // Now the rest
            while (k > 0) {
                c |= s.charAt(--k);
            }
            // We require there to be no bits set from 0x100 upwards
            return c < 0x100;
        }
    }

    /**
     * Determine whether the string consists entirely of basic-plane characters. For a
     * {@link PyString}, of course, it is always <code>true</code>, but this is useful in cases
     * where either a <code>PyString</code> or a {@link PyUnicode} is acceptable.
     *
     * @return true
     */
    public boolean isBasicPlane() {
        return true;
    }

    // @formatter:off
    /*
    @ExposedNew
    static PyObject str_new(PyNewWrapper new_, boolean init, PyType subtype, PyObject[] args,
            String[] keywords) {
        ArgParser ap = new ArgParser("str", args, keywords, new String[] {"object"}, 0);
        PyObject S = ap.getPyObject(0, null);
        // Get the textual representation of the object into str/bytes form
        String str;
        if (S == null) {
            str = "";
        } else {
            // Let the object tell us its representation: this may be str or unicode.
            S = S.__str__();
            if (S instanceof PyUnicode) {
                // Encoding will raise UnicodeEncodeError if not 7-bit clean.
                str = codecs.encode((PyUnicode) S, null, null);
            } else {
                // Must be str/bytes, and should be 8-bit clean already.
                str = S.toString();
            }
        }
        if (new_.for_type == subtype) {
            return new PyString(str);
        } else {
            return new PyStringDerived(subtype, str);
        }
    }
    */

    public int[] toCodePoints() {
        int n = getString().length();
        int[] codePoints = new int[n];
        for (int i = 0; i < n; i++) {
            codePoints[i] = getString().charAt(i);
        }
        return codePoints;
    }

    /**
     * Return a substring of this object as a Java String.
     *
     * @param start the beginning index, inclusive.
     * @param end the ending index, exclusive.
     * @return the specified substring.
     */
    public String substring(int start, int end) {
        return getString().substring(start, end);
    }


    // Special methods -----------------------------------------------

    /*
    @ExposedMethod(doc = BuiltinDocs.str___str___doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str___len___doc)
    */


    @Override
    public String toString() {
        return getString();
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.str___repr___doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str___getitem___doc)
    */

    /*
    @ExposedMethod(defaults = "null")
    */

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___eq___doc)
    */

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___ne___doc)
    */

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___lt___doc)
    */

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___le___doc)
    */

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___gt___doc)
    */

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___ge___doc)
    */


    @Override
    public int hashCode() {
        return str___hash__();
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.str___hash___doc)
    */
    final int str___hash__() {
        return getString().hashCode();
    }

    // @formatter:off
    /*
    @Override
    public Object __tojava__(Class<?> c) {
        if (c.isAssignableFrom(String.class)) {
            /*
             * If c is a CharSequence we assume the caller is prepared to get maybe not an actual
             * String. In that case we avoid conversion so the caller can do special stuff with the
             * returned PyString or PyUnicode or whatever. (If c is Object.class, the caller usually
             * expects to get actually a String)
             * /
            return c == CharSequence.class ? this : getString();
        }

        if (c == Character.TYPE || c == Character.class) {
            if (getString().length() == 1) {
                return getString().charAt(0);
            }
        }

        if (c.isArray()) {
            if (c.getComponentType() == Byte.TYPE) {
                return toBytes();
            }
            if (c.getComponentType() == Character.TYPE) {
                return getString().toCharArray();
            }
        }

        if (c.isAssignableFrom(Collection.class)) {
            List<Object> list = new ArrayList();
            for (int i = 0; i < __len__(); i++) {
                list.add(pyget(i).__tojava__(String.class));
            }
            return list;
        }

        if (c.isInstance(this)) {
            return this;
        }

        return Py.NoConversion;
    }
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str___contains___doc)
    */

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___mul___doc)
    */

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___rmul___doc)
    */

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___add___doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str___getnewargs___doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str___mod___doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str_lower_doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str_upper_doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str_title_doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str_swapcase_doc)
    */

    /*
    @ExposedMethod(defaults = "null", doc = BuiltinDocs.str_strip_doc)
    */

    /*
    @ExposedMethod(defaults = "null", doc = BuiltinDocs.str_lstrip_doc)
    */

    /*
    @ExposedMethod(defaults = "null", doc = BuiltinDocs.str_rstrip_doc)
    */

    /*
    @ExposedMethod(defaults = {"null", "-1"}, doc = BuiltinDocs.str_split_doc)
    */

    /*
    @ExposedMethod(defaults = {"null", "-1"}, doc = BuiltinDocs.str_split_doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str_partition_doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str_rpartition_doc)
    */

    /*
    @ExposedMethod(defaults = "false", doc = BuiltinDocs.str_splitlines_doc)
    */

    /*
    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.str_index_doc)
    */

    /*
    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.str_rindex_doc)
    */

    /*
    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.str_count_doc)
    */

    /*
    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.str_find_doc)
    */

    /*
    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.str_rfind_doc)
    */

    /*
    @ExposedMethod(defaults = "null", doc = BuiltinDocs.str_ljust_doc)
    */

    /*
    @ExposedMethod(defaults = "null", doc = BuiltinDocs.str_rjust_doc)
    */

    /*
    @ExposedMethod(defaults = "null", doc = BuiltinDocs.str_center_doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str_zfill_doc)
    */

    /*
    @ExposedMethod(defaults = "8", doc = BuiltinDocs.str_expandtabs_doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str_capitalize_doc)
    */

    /*
    @ExposedMethod(defaults = "-1", doc = BuiltinDocs.str_replace_doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str_join_doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str_islower_doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str_isupper_doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str_isalpha_doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_isdecimal_doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str_isdigit_doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_isnumeric_doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str_istitle_doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str_isspace_doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str_encode_doc)
    */


    public PyObject decode() {
        // Stop-gap substitute to satisfy references
        return this;
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.str_decode_doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str__formatter_parser_doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str__formatter_field_name_split_doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str_format_doc)
    */

    /*
    @ExposedMethod(doc = BuiltinDocs.str___format___doc)
    */

    // Plumbing ------------------------------------------------------


/**
 * Interpreter for %-format strings. (Note visible across the core package.)
 */
final class StringFormatter {

    /** Index into {@link #format} being interpreted. */
    int index;
    /** Format being interpreted. */
    String format;
    /** Where the output is built. */
    StringBuilder buffer;
    /**
     * Index into args of argument currently being worked, or special values indicating -1: a single
     * item that has not yet been used, -2: a single item that has already been used, -3: a mapping.
     */
    int argIndex;
    /** Arguments supplied to {@link #format(PyObject)} method. */
    PyObject args;
    /** Indicate a <code>PyUnicode</code> result is expected. */
    boolean needUnicode;

    final char pop() {
        try {
            return format.charAt(index++);
        } catch (StringIndexOutOfBoundsException e) {
            throw new ValueError("incomplete format");
        }
    }

    final char peek() {
        return format.charAt(index);
    }

    final void push() {
        index--;
    }

    /**
     * Initialise the interpreter with the given format string, ready for {@link #format(PyObject)}.
     *
     * @param format string to interpret
     */
    public StringFormatter(String format) {
        this(format, false);
    }

    /**
     * Initialise the interpreter with the given format string, ready for {@link #format(PyObject)}.
     *
     * @param format string to interpret
     * @param unicodeCoercion to indicate a <code>PyUnicode</code> result is expected
     */
    public StringFormatter(String format, boolean unicodeCoercion) {
        index = 0;
        this.format = format;
        this.needUnicode = unicodeCoercion;
        buffer = new StringBuilder(format.length() + 100);
    }

    /**
     * Read the next object from the argument list, taking special values of <code>argIndex</code>
     * into account.
     */
    PyObject getarg() {
        PyObject ret = null;
        switch (argIndex) {
            case -3: // special index indicating a mapping
                return args;
            case -2: // special index indicating a single item that has already been used
                break;
            case -1: // special index indicating a single item that has not yet been used
                argIndex = -2;
                return args;
            default:
                ret = args.__finditem__(argIndex++);
                break;
        }
        if (ret == null) {
            throw new TypeError("not enough arguments for format string");
        }
        return ret;
    }

    /**
     * Parse a number from the format, except if the next thing is "*", read it from the argument
     * list.
     */
    int getNumber() {
        char c = pop();
        if (c == '*') {
            PyObject o = getarg();
            if (o instanceof PyInteger) {
                return ((PyInteger) o).getValue();
            }
            throw new TypeError("* wants int");
        } else {
            if (Character.isDigit(c)) {
                int numStart = index - 1;
                while (Character.isDigit(c = pop())) {}
                index -= 1;
                Integer i = Integer.valueOf(format.substring(numStart, index));
                return i.intValue();
            }
            index -= 1;
            return 0;
        }
    }

    /**
     * Return the argument as either a {@link PyInteger} or a {@link PyLong} according to its
     * <code>__int__</code> method, or its <code>__long__</code> method. If the argument has neither
     * method, or both raise an exception, we return the argument itself. The caller must check the
     * return type.
     *
     * @param arg to convert
     * @return PyInteger or PyLong if possible
     */
    private PyObject asNumber(PyObject arg) {
        if (arg instanceof PyInteger || arg instanceof PyLong) {
            // arg is already acceptable
            return arg;

        } else {
            // use __int__ or __long__to get an int (or long)
            if (arg.getClass() == PyFloat.class) {
                // A common case where it is safe to return arg.__int__()
                return arg.__int__();

            } else {
                /*
                 * In general, we can't simply call arg.__int__() because PyString implements it
                 * without exposing it to python (str has no __int__). This would make str
                 * acceptacle to integer format specifiers, which is forbidden by CPython tests
                 * (test_format.py). PyString implements __int__ perhaps only to help the int
                 * constructor. Maybe that was a bad idea?
                 */
                try {
                    // Result is the result of arg.__int__() if that works
                    return arg.__getattr__("__int__").__call__();
                } catch (PyException e) {
                    // Swallow the exception
                }

                // Try again with arg.__long__()
                try {
                    // Result is the result of arg.__long__() if that works
                    return arg.__getattr__("__long__").__call__();
                } catch (PyException e) {
                    // No __long__ defined (at Python level)
                    return arg;
                }
            }
        }
    }

    /**
     * Return the argument as a {@link PyFloat} according to its <code>__float__</code> method. If
     * the argument has no such method, or it raises an exception, we return the argument itself.
     * The caller must check the return type.
     *
     * @param arg to convert
     * @return PyFloat if possible
     */
    private PyObject asFloat(PyObject arg) {

        if (arg instanceof PyFloat) {
            // arg is already acceptable
            return arg;

        } else {
            // use __float__ to get a float.
            if (arg.getClass() == PyFloat.class) {
                // A common case where it is safe to return arg.__float__()
                return arg.__float__();

            } else {
                /*
                 * In general, we can't simply call arg.__float__() because PyString implements it
                 * without exposing it to python (str has no __float__). This would make str
                 * acceptacle to float format specifiers, which is forbidden by CPython tests
                 * (test_format.py). PyString implements __float__ perhaps only to help the float
                 * constructor. Maybe that was a bad idea?
                 */
                try {
                    // Result is the result of arg.__float__() if that works
                    return arg.__getattr__("__float__").__call__();
                } catch (PyException e) {
                    // No __float__ defined (at Python level)
                    return arg;
                }
            }
        }
    }

    /**
     * Return the argument as either a {@link PyString} or a {@link PyUnicode}, and set the
     * {@link #needUnicode} member accordingly. If we already know we are building a Unicode string
     * (<code>needUnicode==true</code>), then any argument that is not already a
     * <code>PyUnicode</code> will be converted by calling its <code>__unicode__</code> method.
     * Conversely, if we are not yet building a Unicode string (<code>needUnicode==false</code> ),
     * then a PyString will pass unchanged, a <code>PyUnicode</code> will switch us to Unicode mode
     * (<code>needUnicode=true</code>), and any other type will be converted by calling its
     * <code>__str__</code> method, which will return a <code>PyString</code>, or possibly a
     * <code>PyUnicode</code>, which will switch us to Unicode mode.
     *
     * @param arg to convert
     * @return PyString or PyUnicode equivalent
     */
    private PyString asText(PyObject arg) {

        if (arg instanceof PyUnicode) {
            // arg is already acceptable.
            needUnicode = true;
            return (PyUnicode) arg;

        } else if (needUnicode) {
            // The string being built is unicode, so we need that version of the arg.
            return arg.__unicode__();

        } else if (arg instanceof PyString) {
            // The string being built is not unicode, so arg is already acceptable.
            return (PyString) arg;

        } else {
            // The string being built is not unicode, so use __str__ to get a PyString.
            PyString s = arg.__str__();
            // But __str__ might return PyUnicode, and we have to notice that.
            if (s instanceof PyUnicode) {
                needUnicode = true;
            }
            return s;
        }
    }

    /**
     * Main service of this class: format one or more arguments with the format string supplied at
     * construction.
     *
     * @param args tuple or map containing objects, or a single object, to convert
     * @return result of formatting
     */
    @SuppressWarnings("fallthrough")
    public PyString format(PyObject args) {
        PyObject dict = null;
        this.args = args;

        if (args instanceof PyTuple) {
            // We will simply work through the tuple elements
            argIndex = 0;
        } else {
            // Not a tuple, but possibly still some kind of container: use special argIndex values.
            argIndex = -1;
            if (args instanceof AbstractDict || (!(args instanceof PySequence) &&
            // See issue 2511: __getitem__ should be looked up directly in the dict, rather
            // than going through another __getattr__ call. We achieve this by using
            // object___findattr__ instead of generic __findattr__.
                    args.object___findattr__("__getitem__".intern()) != null)) {
                dict = args;
                argIndex = -3;
            }
        }

        while (index < format.length()) {

            // Read one character from the format string
            char c = pop();
            if (c != '%') {
                buffer.append(c);
                continue;
            }

            // It's a %, so the beginning of a conversion specifier. Parse it.

            // Attributes to be parsed from the next format specifier
            boolean altFlag = false;
            char sign = Spec.NONE;
            char fill = ' ';
            char align = '>';
            int width = Spec.UNSPECIFIED;
            int precision = Spec.UNSPECIFIED;

            // A conversion specifier contains the following components, in this order:
            // + The '%' character, which marks the start of the specifier.
            // + Mapping key (optional), consisting of a parenthesised sequence of characters.
            // + Conversion flags (optional), which affect the result of some conversion types.
            // + Minimum field width (optional), or an '*' (asterisk).
            // + Precision (optional), given as a '.' (dot) followed by the precision or '*'.
            // + Length modifier (optional).
            // + Conversion type.

            c = pop();
            if (c == '(') {
                // Mapping key, consisting of a parenthesised sequence of characters.
                if (dict == null) {
                    throw new TypeError("format requires a mapping");
                }
                // Scan along until a matching close parenthesis is found
                int parens = 1;
                int keyStart = index;
                while (parens > 0) {
                    c = pop();
                    if (c == ')') {
                        parens--;
                    } else if (c == '(') {
                        parens++;
                    }
                }
                // Last c=pop() is the closing ')' while indexKey is just after the opening '('
                String tmp = format.substring(keyStart, index - 1);
                // Look it up using this extent as the (right type of) key.
                this.args = dict.__getitem__(needUnicode ? new PyUnicode(tmp) : new PyString(tmp));
            } else {
                // Not a mapping key: next clause will re-read c.
                push();
            }

            // Conversion flags (optional) that affect the result of some conversion types.
            while (true) {
                switch (c = pop()) {
                    case '-':
                        align = '<';
                        continue;
                    case '+':
                        sign = '+';
                        continue;
                    case ' ':
                        if (!Spec.specified(sign)) {
                            // Blank sign only wins if '+' not specified.
                            sign = ' ';
                        }
                        continue;
                    case '#':
                        altFlag = true;
                        continue;
                    case '0':
                        fill = '0';
                        continue;
                }
                break;
            }
            // Push back c as next clause will re-read c.
            push();

            /*
             * Minimum field width (optional). If specified as an '*' (asterisk), the actual width
             * is read from the next element of the tuple in values, and the object to convert comes
             * after the minimum field width and optional precision. A custom getNumber() takes care
             * of the '*' case.
             */
            width = getNumber();
            if (width < 0) {
                width = -width;
                align = '<';
            }

            /*
             * Precision (optional), given as a '.' (dot) followed by the precision. If specified as
             * '*' (an asterisk), the actual precision is read from the next element of the tuple in
             * values, and the value to convert comes after the precision. A custom getNumber()
             * takes care of the '*' case.
             */
            c = pop();
            if (c == '.') {
                precision = getNumber();
                if (precision < -1) {
                    precision = 0;
                }
                c = pop();
            }

            // Length modifier (optional). (Compatibility feature?) It has no effect.
            if (c == 'h' || c == 'l' || c == 'L') {
                c = pop();
            }

            /*
             * As a function of the conversion type (currently in c) override some of the formatting
             * flags we read from the format specification.
             */
            switch (c) {
                case 's':
                case 'r':
                case 'c':
                case '%':
                    // These have string-like results: fill, if needed, is always blank.
                    fill = ' ';
                    break;

                default:
                    if (fill == '0' && align == '>') {
                        // Zero-fill comes after the sign in right-justification.
                        align = '=';
                    } else {
                        // If left-justifying, the fill is always blank.
                        fill = ' ';
                    }
            }

            /*
             * Encode as an InternalFormat.Spec. The values in the constructor always have specified
             * values, except for sign, width and precision.
             */
            Spec spec = new Spec(fill, align, sign, altFlag, width, false, precision, c);

            /*
             * Process argument according to format specification decoded from the string. It is
             * important we don't read the argument from the list until this point because of the
             * possibility that width and precision were specified via the argument list.
             */

            // Depending on the type of conversion, we use one of these formatters:
            FloatFormatter ff;
            IntegerFormatter fi;
            TextFormatter ft;
            Formatter f; // = ff, fi or ft, whichever we actually use.

            switch (spec.type) {

                case 's': // String: converts any object using __str__(), __unicode__() ...
                case 'r': // ... or repr().
                    PyObject arg = getarg();

                    // Get hold of the actual object to display (may set needUnicode)
                    PyString argAsString = asText(spec.type == 's' ? arg : arg.__repr__());
                    // Format the str/unicode form of the argument using this Spec.
                    f = ft = new TextFormatter(buffer, spec);
                    ft.setBytes(!needUnicode);
                    ft.format(argAsString.getString());
                    break;

                case 'd': // All integer formats (+case for X).
                case 'o':
                case 'x':
                case 'X':
                case 'c': // Single character (accepts integer or single character string).
                case 'u': // Obsolete type identical to 'd'.
                case 'i': // Compatibility with scanf().

                    // Format the argument using this Spec.
                    f = fi = new IntegerFormatter.Traditional(buffer, spec);
                    // If not producing PyUnicode, disallow codes >255.
                    fi.setBytes(!needUnicode);

                    arg = getarg();

                    if (arg instanceof PyString && spec.type == 'c') {
                        if (arg.__len__() != 1) {
                            throw new TypeError("%c requires int or char");
                        } else {
                            if (!needUnicode && arg instanceof PyUnicode) {
                                // Change of mind forced by encountering unicode object.
                                needUnicode = true;
                                fi.setBytes(false);
                            }
                            fi.format(((PyString) arg).getString().codePointAt(0));
                        }

                    } else {
                        // Note various types accepted here as long as they have an __int__ method.
                        PyObject argAsNumber = asNumber(arg);

                        // We have to check what we got back.
                        if (argAsNumber instanceof PyInteger) {
                            fi.format(((PyInteger) argAsNumber).getValue());
                        } else if (argAsNumber instanceof PyLong) {
                            fi.format(((PyLong) argAsNumber).getValue());
                        } else {
                            // It couldn't be converted, raise the error here
                            throw new TypeError(
                                    "%" + spec.type + " format: a number is required, not "
                                            + arg.getType().fastGetName());
                        }
                    }

                    break;

                case 'e': // All floating point formats (+case).
                case 'E':
                case 'f':
                case 'F':
                case 'g':
                case 'G':

                    // Format using this Spec the double form of the argument.
                    f = ff = new FloatFormatter(buffer, spec);
                    ff.setBytes(!needUnicode);

                    // Note various types accepted here as long as they have a __float__ method.
                    arg = getarg();
                    PyObject argAsFloat = asFloat(arg);

                    // We have to check what we got back..
                    if (argAsFloat instanceof PyFloat) {
                        ff.format(((PyFloat) argAsFloat).getValue());
                    } else {
                        // It couldn't be converted, raise the error here
                        throw new TypeError(
                                "float argument required, not " + arg.getType().fastGetName());
                    }

                    break;

                case '%': // Percent symbol, but surprisingly, padded.

                    // We use an integer formatter.
                    f = fi = new IntegerFormatter.Traditional(buffer, spec);
                    fi.setBytes(!needUnicode);
                    fi.format('%');
                    break;

                default:
                    throw new ValueError("unsupported format character '"
                            + codecs.encode(Py.newUnicode(spec.type), null, "replace") + "' (0x"
                            + Integer.toHexString(spec.type) + ") at index " + (index - 1));
            }

            // Pad the result as specified (in-place, in the buffer).
            f.pad();
        }

        /*
         * All fields in the format string have been used to convert arguments (or used the argument
         * as a width, etc.). This had better not leave any arguments unused. Note argIndex is an
         * index into args or has a special value. If args is a 'proper' index, It should now be out
         * of range; if a special value, it would be wrong if it were -1, indicating a single item
         * that has not yet been used.
         */
        if (argIndex == -1 || (argIndex >= 0 && args.__finditem__(argIndex) != null)) {
            throw new TypeError("not all arguments converted during string formatting");
        }

        // Return the final buffer contents as a str or unicode as appropriate.
        return needUnicode ? new PyUnicode(buffer) : new PyString(buffer);
    }

}
