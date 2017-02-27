package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

@Untraversable
@ExposedType(name = "shadowstr", base = PyString.class, isBaseType = false)
public class PyShadowString extends PyString {
    public static final PyType TYPE = PyType.fromClass(PyShadowString.class);

    protected PyList targets;

    /**
     * The shadow string is additionally used for some comparisons, especially for __eq__.
     * __eq__ will evaluate positive if the other string equals the actual value
     * *or* the shadow. The shadow persists slicing (is sliced accordingly)
     * and is taken into account by startswith.
     */
    protected String shadow;
    
    // for PyJavaClass.init()
    public PyShadowString() {
        this(TYPE, "", "");
        targets = new PyList();
    }

    public PyShadowString(String str, String shadow) {
        super(TYPE, str);
        this.shadow = shadow;
        targets = new PyList();
    }

    public PyShadowString(String str, String shadow, boolean isBytes) {
        super(TYPE, str, isBytes);
        this.shadow = shadow;
        targets = new PyList();
    }
    
    public PyShadowString(String str, String shadow, boolean isBytes, PyList targets) {
        super(TYPE, str, isBytes);
        this.shadow = shadow;
        this.targets = targets;
    }

    public PyShadowString(PyObject str, String shadow) {
        super(str.toString());
        this.shadow = shadow;
        targets = new PyList();
    }

    public PyShadowString(PyType subtype, String str, String shadow) {
        super(subtype, str);
        this.shadow = shadow;
        targets = new PyList();
    }

    public PyShadowString(PyType subtype, PyObject str, String shadow) {
        super(subtype, str.toString());
        this.shadow = shadow;
        targets = new PyList();
    }

    @ExposedNew
    static PyObject shadowstr_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("shadowstr", args, keywords,
                new String[] {"string", "shadow"}, 0);
        PyObject S = ap.getPyObject(0, null);
        PyObject Sh = ap.getPyObject(1, null);
        // Get the textual representation of the object into str/bytes form
        String str, shd;
        if (S == null) {
            str = "";
        } else {
            // Let the object tell us its representation: this may be str or unicode.
            S = S.__str__();
            if (S instanceof PyUnicode) {
                // Encoding will raise UnicodeEncodeError if not 7-bit clean.
                str = codecs.encode((PyUnicode)S, null, null);
            } else {
                // Must be str/bytes, and should be 8-bit clean already.
                str = S.toString();
            }
        }
        if (Sh == null) {
            shd = "";
        } else {
            // Let the object tell us its representation: this may be str or unicode.
            Sh = Sh.__str__();
            if (Sh instanceof PyUnicode) {
                // Encoding will raise UnicodeEncodeError if not 7-bit clean.
                shd = codecs.encode((PyUnicode)Sh, null, null);
            } else {
                // Must be str/bytes, and should be 8-bit clean already.
                shd = Sh.toString();
            }
        }
        return new PyShadowString(str, shd);
    }

    private boolean isTarget() {
        Exception exc = new Exception();
        PyObject obj;
        boolean result;
        for (StackTraceElement ste: exc.getStackTrace()) {
            for (PyObject itm: targets.getList()) {
                result = true;
                obj = ((PyTuple) itm).__finditem__(0);
                if (obj != null && obj != Py.None) {
                    if (!ste.getClassName().matches(obj.toString())) {
                        result = false;
                    }
                }
                if (result) {
                    obj = ((PyTuple) itm).__finditem__(1);
                    if (obj != null && obj != Py.None) {
                        if (!ste.getMethodName().matches(obj.toString())) {
                            result = false;
                        }
                    }
                }
                if (result) {
                    // we have a match
                    return true;
                }
            }
        }
        return false;
    }

    public String getShadow() {
        return shadow;
    }

    public PyString getshadow() {
        return (PyString) shadowstr_getshadow();
    }

    @ExposedMethod
    public final PyObject shadowstr_getshadow() {
        return Py.newString(shadow);
    }

    public void addTarget(String className, String methodName) {
        PyString classname = className == null ? null : Py.newString(className);
        PyString methodname = methodName == null ? null : Py.newString(methodName);
        shadowstr_addtarget(classname, methodname);
    }

    @ExposedMethod(defaults = {"null"})
    public final void shadowstr_addtarget(PyObject classname, PyObject methodname) {
        targets.add(methodname != null ?
                new PyTuple(classname == null ? Py.None : classname, methodname) :
                new PyTuple(classname == null ? Py.None : classname));
    }

    public PyList getTargets() {
        return (PyList) shadowstr_gettargets();
    }

    @ExposedMethod
    public final PyObject shadowstr_gettargets() {
        return targets;
    }

    @Override
    public PyObject __eq__(PyObject other) {
        if (!isTarget()) {
            return str___eq__(other);
        }
        return shadowstr___eq__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject shadowstr___eq__(PyObject other) {
        String s = other.toString();
        if (s != null && s.equals(shadow)) return Py.True;
        return str___eq__(other);
    }

    @Override
    protected PyShadowString fromSubstring(int begin, int end) {
        // Method is overridden in PyUnicode, so definitely a PyString
        int shadowBegin = begin, shadowEnd = end;
        if (begin > shadow.length()) {
            shadowBegin = shadow.length();
        }
        if (end > shadow.length()) {
            shadowEnd = shadow.length();
        }
        return new PyShadowString(getString().substring(begin, end),
                shadow.substring(shadowBegin, shadowEnd), true, targets);
    }

    @Override
    protected PyObject getslice(int start, int stop, int step) {
        if (step > 0 && stop < start) {
            stop = start;
        }
        if (step == 1) {
            return fromSubstring(start, stop);
        } else {
            int n = sliceLength(start, stop, step);
            char new_chars[] = new char[n];
            int j = 0;
            for (int i = start; j < n; i += step) {
                new_chars[j++] = getString().charAt(i);
            }
            char new_shadow_chars[] = new char[n];
            j = 0;
            try {
                for (int i = start; j < n; i += step) {
                    new_chars[j] = shadow.charAt(i);
                    j++; // separate line, so in exception case j is clearly before increment
                }
            } catch (IndexOutOfBoundsException ioobe)
            {
                return new PyShadowString(new String(new_chars),
                        new String(new_shadow_chars, 0, j), true, targets);
            }
            return new PyShadowString(new String(new_chars),
                    new String(new_shadow_chars), true, targets);
        }
    }

    @Override
    public boolean startswith(PyObject prefix) {
        return shadowstr_startswith(prefix, null, null);
    }

    @Override
    public boolean startswith(PyObject prefix, PyObject start) {
        return shadowstr_startswith(prefix, start, null);
    }

    @Override
    public boolean startswith(PyObject prefix, PyObject start, PyObject end) {
        return shadowstr_startswith(prefix, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"})
    final boolean shadowstr_startswith(PyObject prefix, PyObject startObj, PyObject endObj) {
        if (!isTarget()) {
            return str_startswith(prefix, startObj, endObj);
        }
        int[] indices = translateIndices(startObj, endObj);
        int start = indices[0];
        int sliceLen = indices[1] - start;

        if (!(prefix instanceof PyTuple)) {
            // It ought to be PyUnicode or some kind of bytes with the buffer API.
            String s = asUTF16StringOrError(prefix);
            // If s is non-BMP, and this is a PyString (bytes), result will correctly be false.
            return sliceLen >= s.length() &&
                    (getString().startsWith(s, start) || shadow.startsWith(s, start));
        } else {
            // Loop will return true if this slice starts with any prefix in the tuple
            for (PyObject prefixObj : ((PyTuple)prefix).getArray()) {
                // It ought to be PyUnicode or some kind of bytes with the buffer API.
                String s = asUTF16StringOrError(prefixObj);
                // If s is non-BMP, and this is a PyString (bytes), result will correctly be false.
                if (sliceLen >= s.length() &&
                        (getString().startsWith(s, start) || shadow.startsWith(s, start))) {
                    return true;
                }
            }
            // None matched
            return false;
        }
    }

    @Override
    public PyString __repr__() {
        return shadowstr___repr__();
    }

    @ExposedMethod
    final PyString shadowstr___repr__() {
        return new PyString(encode_UnicodeEscape(getString()+" ( =="+shadow+" for targets )", true));
    }
}
