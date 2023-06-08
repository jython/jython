// Copyright (c)2023 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;
import java.util.Map;

/**
 * A {@code PyFrame} is the context for the execution of code.
 * Different concrete sub-classes of {@code PyFrame} exist to
 * execute different compiled representations of Python code. For
 * example, there is one for CPython 3.11 byte code and (we expect)
 * another for Java byte code. The type of code object supported is
 * the parameter {@code C} to the class.
 * <p>
 * In order that argument processing may be uinform irrespective of
 * concrete type, a {@code PyFrame} presents an abstraction that has
 * arguments laid out in an array. For example, the function
 * definition:<pre>
 * def func(a, b, c=3, d=4, /, e=5, f=6, *aa, g=7, h, i=9, **kk):
 *     v, w, x = b, c, d, e
 *     return u
 * </pre> the layout of the local variables in a frame would be as
 * below
 * <table class="framed-layout" style="border: none;">
 * <caption>A Python {@code frame}</caption>
 * <tr>
 * <td class="label">frame</td>
 * <td>a</td>
 * <td>b</td>
 * <td>c</td>
 * <td>d</td>
 * <td>e</td>
 * <td>f</td>
 * <td>g</td>
 * <td>h</td>
 * <td>i</td>
 * <td>aa</td>
 * <td>kk</td>
 * <td>u</td>
 * <td>v</td>
 * <td>w</td>
 * <td>x</td>
 * </tr>
 * <tr>
 * <td class="label" rowspan=2>code</td>
 * <td colspan=6>argcount</td>
 * <td colspan=3>kwonlyargcount</td>
 * <td>*</td>
 * <td>**</td>
 * <td colspan=4></td>
 * </tr>
 * <tr>
 * <td colspan=4>posonlyargcount</td>
 * <td colspan=13></td>
 * </tr>
 * <tr>
 * <td class="label">function</td>
 * <td colspan=2></td>
 * <td colspan=4>defaults</td>
 * <td colspan=3 style="border-style: dashed;">kwdefaults</td>
 * </tr>
 * </table>
 * <p>
 * In the last row of the table, the properties are supplied by the
 * function object during each call. {@code defaults} apply in the
 * position show, in order, while {@code kwdefaults} (in a map)
 * apply to keywords wherever the name matches. The names in the
 * frame are those in the {@link PyCode#varnames} field of the
 * associated code object
 * <p>
 * The frame presents an abstraction of an array of named local
 * variables, and two more of cell and free variables, while
 * concrete subclasses are free to implement these in whatever
 * manner they choose.
 *
 * @param <C> The type of code that this frame executes
 */
public abstract class PyFrame<C extends PyCode> {

    /** The Python type {@code frame}. */
    public static final PyType TYPE = PyType.fromSpec( //
            new PyType.Spec("frame", MethodHandles.lookup())
                    // Type admits no Python subclasses.
                    .flagNot(PyType.Flag.BASETYPE));

    /** Frames form a stack by chaining through the back pointer. */
    PyFrame<? extends PyCode> back;

    /** Function of which this is a frame. */
    final PyFunction<? extends C> func;

    /**
     * Code this frame is to execute, exposed as immutable
     * {@code f_code}. We have our own final copy because it is possible
     * to change the code object that defines {@link #func} but the
     * frame should continue to reference the code that created it.
     */
    final C code;

    /**
     * Local context (name space) of execution. (Assign if needed.) This
     * is allowed to be any type, but if it is ever actually used, the
     * interpreter will expect it to support the mapping protocol.
     */
    Object locals;

    /**
     * Foundation constructor on which subclass constructors rely. This
     * provides a "loose" frame that is not yet part of any stack until
     * explicitly pushed (with {@link ThreadState#push(PyFrame)}). In
     * particular, the {@link #back} pointer is {@code null} in the
     * newly-created frame.
     * <p>
     * A frame always belongs to an {@link Interpreter} via its
     * function, but it does not necessarily belong to a particular
     * {@code ThreadState}.
     *
     * @param func defining the code and globals
     */
    protected PyFrame(PyFunction<? extends C> func) {
        this.func = func;
        this.code = func.code;
    }

    /**
     * Get the interpreter that defines the import context when
     * executing code.
     *
     * @return Interpreter that defines the import context.
     */
    Interpreter getInterpreter() { return func.getInterpreter(); }

    // Java API ------------------------------------------------------

    @Override
    // Compare CPython frame_repr in frameobject.c
    public String toString() {
        int lineno = code.firstlineno;
        if (lineno == 0) { lineno = -1; }
        String file = code.filename, q = "'";
        if (file == null) {
            file = "";
            q = "";
        }
        return String.format("<frame at %#x, file %s%s%s, line %d, code %s>", Py.id(this), q, file,
                q, lineno, code.name);
    }

    /**
     * Provide {@link #locals} as a Java Map. This does not re-compute
     * {@code locals} as a dictionary in the way of
     * {@link #fastToLocals()}, but only dresses an existing value as a
     * Java {@code Map} (if it is not {@code null}).
     *
     * @return as a Java {@code Map}
     */
    protected Map<Object, Object> localsMapOrNull() {
        if (locals == null) {
            return null;
        } else {
            return PyMapping.map(locals);
        }
    }

    /**
     * Execute the code in this frame.
     *
     * @return return value of the frame
     */
    abstract Object eval();
}
