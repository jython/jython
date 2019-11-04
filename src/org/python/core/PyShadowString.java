package org.python.core;

import java.util.regex.Pattern;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

/**
 * This class provides a string that sometimes seems to change value, as far as equality tests and
 * <code>startswith</code> are concerned. This solves a problem that Jython users sometimes
 * experience with libraries that are sensitive to platform.
 * <p>
 * A library may test for a particular platform in order to adjust to local file name conventions or
 * to decide which operating system commands are available. In Jython, <code>os.name</code> and
 * <code>sys.platform</code> indicate that Java is the platform, which is necessary information in
 * some parts of the standard library, but other libraries assuming CPython then draw incorrect
 * conclusions.
 * <p>
 * With hindsight, a better choice could be made, where <code>sys.platform</code> etc. indicated
 * Windows or Posix, and something else indicates the implementation. A change in Jython 2 would
 * cause more problems than it solved, but we expect Jython 3 to work that way. In the Python
 * Standard Library, the Jython project can make all necessary changes. It can't do anything about
 * third-party libraries. But it would be a big help if users could cause <code>sys.platform</code>
 * or <code>os.name</code> to appear to have the OS-dependent value as far as those troublesome
 * libraries were concerned.
 * <p>
 * This is what this class achieves. <code>os.name</code> and <code>sys.platform</code> regular
 * strings for most purposes, but each has a "shadow" value that is used in contexts the user may
 * specify.
 */
@Untraversable
@ExposedType(name = "shadowstr", base = PyString.class, isBaseType = true)
public class PyShadowString extends PyString {

    public static final PyType TYPE = PyType.fromClass(PyShadowString.class);

    /**
     * Contexts (expressed as a {@link PyTuple} of class name and method name) where {@link #shadow}
     * is allowed to match as well as the primary value.
     */
    protected PyList targets;

    /**
     * The shadow string is additionally used for some comparisons, especially for __eq__. __eq__
     * will evaluate positive if the other string equals the primary value *or* the shadow. The
     * shadow persists slicing (is sliced accordingly) and is taken into account by startswith.
     */
    protected PyString shadow;

    /** Empty string (not very useful but needed for technical reasons). */
    public PyShadowString() {
        this(Py.EmptyString, Py.EmptyString);
    }

    /** Construct an instance specifying primary and shadow values. */
    public PyShadowString(String primary, String shadow) {
        this(TYPE, Py.newString(primary), Py.newString(shadow), new PyList());
    }

    /**
     * Construct an instance specifying primary and shadow values
     * (bytes object expected for primary).
     * This somewhat uncanonical constructor was removed in Jython 2.7.2.
     * The deprecated version is kept for compatibility with JyNI 2.7-alpha5.
     *
     * @deprecated use the constructor with strings instead.
     */
    @Deprecated
    public PyShadowString(PyObject primary, String shadow) {
        this(TYPE, primary, Py.newString(shadow), new PyList());
    }

    /** Construct an instance specifying primary and shadow values (bytes objects expected). */
    private PyShadowString(PyObject primary, PyObject shadow) {
        this(TYPE, primary, shadow, new PyList());
    }

    private PyShadowString(PyObject primary, PyObject shadow, PyList targets) {
        this(TYPE, primary, shadow, targets);
    }

    public PyShadowString(PyType subtype, PyObject primary, PyObject shadow) {
        this(subtype, primary, shadow, new PyList());
    }

    private PyShadowString(PyType subtype, PyObject primary, PyObject shadow, PyList targets) {
        super(subtype, primary.__str__().getString());
        this.shadow = shadow.__str__();
        this.targets = targets;
    }

    @ExposedNew
    static PyObject shadowstr_new(PyNewWrapper new_, boolean init, PyType subtype, PyObject[] args,
            String[] keywords) {
        ArgParser ap =
                new ArgParser("shadowstr", args, keywords, new String[] {"primary", "shadow"}, 0);

        PyObject valueObj = ap.getPyObject(0, Py.EmptyString);
        PyObject shadowObj = ap.getPyObject(1, Py.EmptyString);

        if (valueObj instanceof PyString && shadowObj instanceof PyString) {
            return new PyShadowString(valueObj, shadowObj);
        } else {
            String message = String.format("arguments must be strings not (%.200s, %.200s)",
                    valueObj.getType(), shadowObj.getType());
            throw Py.TypeError(message);
        }
    }

    /** Convert a PyObject (specifying a regex) to a compiled pattern or null. */
    private static Pattern getPattern(PyObject o) {
        if (o instanceof PyString) {
            return Pattern.compile(o.toString());
        } else {
            return null;
        }
    }

    /**
     * Test whether the current code is executing in of one of the target contexts, by searching up
     * the stack for a class and method pait that match.
     *
     * @return true iff in one of the named contexts
     */
    private boolean isTarget() {
        // Get a stack trace by constructing an exception here
        Exception exc = new Exception();

        for (PyObject obj : targets.getList()) {
            // Only process proper tuple entries
            if (obj instanceof PyTuple && ((PyTuple) obj).__len__() >= 2) {

                // Compile the target specification
                PyTuple target = (PyTuple) obj;
                Pattern clazz = getPattern(target.__finditem__(0));
                Pattern method = getPattern(target.__finditem__(1));

                // Now scan the stack using this pair of patterns
                for (StackTraceElement ste : exc.getStackTrace()) {
                    if (clazz == null || clazz.matcher(ste.getClassName()).matches()) {
                        // Either we don't care about the class it matches, and ...
                        if ((method == null || method.matcher(ste.getMethodName()).matches())) {
                            // Either we don't care about the method name or it matches
                            return true;
                        }
                    }
                }
            }
        }

        // Nothing matched
        return false;
    }

    /** Get the shadow value. */
    public PyString getshadow() {
        return shadowstr_getshadow();
    }

    @ExposedMethod
    public final PyString shadowstr_getshadow() {
        return shadow;
    }

    /**
     * Specify a context (class, method) in which the shadow string is allowed to match.
     *
     * @param className class name to match or null to match anything.
     * @param methodName method name to match or null to match anything.
     */
    public void addTarget(String className, String methodName) {
        shadowstr_addtarget( //
                className == null ? Py.None : Py.newUnicode(className),
                methodName == null ? Py.None : Py.newUnicode(methodName));
    }

    @ExposedMethod(defaults = {"null"})
    public final void shadowstr_addtarget(PyObject classname, PyObject methodname) {
        // In principle these could be unicode strings
        PyTuple entry = new PyTuple(asUnicode(classname), asUnicode(methodname));
        targets.add(entry);
    }

    /** Prepare argument for addtarget, allowing string-like values or None. */
    private static PyObject asUnicode(PyObject o) {
        if (o == null || o == Py.None) {
            return Py.None;
        } else if (o instanceof PyString) {
            return o.__unicode__();
        }
        throw Py.TypeError(String.format("string or None required, not %.200s", o.getType()));
    }

    /**
     * Return a list of the tuples specifying the contexts in which the shadow value will be
     * consulted during matching.
     */
    public PyList getTargets() {
        return (PyList) shadowstr_gettargets();
    }

    @ExposedMethod
    public final PyObject shadowstr_gettargets() {
        return targets;
    }

    /**
     * Compare this <code>PyShadowString</code> with another <code>PyObject</code> for equality. A
     * <code>PyShadowString</code> is equal to the other object if its primary value is equal to it,
     * or if its shadow value is equal to the other object and the test is made in one of its target
     * contexts. (Two <code>PyShadowString</code> are equal if the primary values are equal, the
     * primary of one matches the shadow of the other in the shadow's context, or their shadows
     * match and both are in context.
     *
     * @param other to compare
     * @return <code>PyBoolean</code> result (or <code>null</code> if not implemented)
     */
    @Override
    public PyObject __eq__(PyObject other) {
        return shadowstr___eq__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject shadowstr___eq__(PyObject other) {
        // Re-wrap the primary value as a PyString to invoke the right kind of equality test.
        PyObject result = testEqual(new PyString(getString()), other);
        if (result != Py.False) {
            // True, or null if str does not know how to compare with other (so we don't either).
            return result;

        } else if (targets.isEmpty()) {
            // We aren't going to be using our shadow string
            return Py.False;

        } else {
            // Since we have targets, compare the shadow string with the other object.
            result = testEqual(shadow, other);
            if (result == Py.True) {
                // It matches, so the result is true iff we are in a target context
                return Py.newBoolean(isTarget());
            } else {
                return result;
            }
        }
    }

    /**
     * Test for equality, used as a helper to <code>shadowstr___eq__</code>, dealing with the
     * possibility that <code>other</code> is another <code>PyShadowString</code>.
     */
    private static final PyObject testEqual(PyString string, PyObject other) {
        if (other instanceof PyShadowString) {
            return ((PyShadowString) other).shadowstr___eq__(string);
        } else {
            return string.__eq__(other);
        }
    }

    @Override
    public PyObject __getslice__(PyObject start, PyObject stop, PyObject step) {
        PyObject primary = super.__getslice__(start, stop, step);
        PyObject shadow = this.shadow.__getslice__(start, stop, step);

        return new PyShadowString(primary, shadow, targets);
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
        return super.startswith(prefix, startObj, endObj) //
                || (!targets.isEmpty() && shadow.startswith(prefix, startObj, endObj)
                        && isTarget());
    }

    @Override
    public PyString __repr__() {
        return shadowstr___repr__();
    }

    @ExposedMethod
    final PyString shadowstr___repr__() {
        // What you'd type to get this instance (without targets).
        String fmt = "PyShadowString(%.200s, %.200s)";
        return Py.newString(String.format(fmt, super.__repr__(), shadow.__repr__()));
    }
}
