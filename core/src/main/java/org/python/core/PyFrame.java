// Copyright (c)2023 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;
import java.util.EnumSet;
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
    PyFrame<?> back;
    /** Code this frame is to execute. */
    final C code;
    /** Interpreter owning this frame. */
    protected final Interpreter interpreter;
    /** Built-in objects. */
    protected PyDict builtins;
    /** Global context (name space) of execution. */
    final PyDict globals;
    /** Local context (name space) of execution. (Assign if needed.) */
    Map<Object, Object> locals = null;

    /**
     * Foundation constructor on which subclass constructors rely.
     *
     *
     * In particular, the {@link #back} pointer is {@code null} in the
     * newly-created frame.
     *
     * @param interpreter providing the module context
     * @param code that this frame executes
     * @param globals global name space
     * @throws TypeError if {@code globals['__builtins__']} is invalid
     */
    /*
     * This provides a "loose" frame that is not yet part of any stack
     * until explicitly pushed (with {@link #push()}. A frame always
     * belongs to an {@link Interpreter}, but it does not necessarily
     * belong to a particular {@link ThreadState}.
     */
    protected PyFrame(Interpreter interpreter, C code, PyDict globals) throws TypeError {
        this.code = code;
        this.interpreter = interpreter;
        this.globals = globals;
    }

    /**
     * Foundation constructor on which subclass constructors rely.
     *
     * <ul>
     * <li>If the code has the trait {@link PyCode.Trait#NEWLOCALS} the
     * {@code locals} argument is ignored.</li>
     * <li>If the code has the trait {@link PyCode.Trait#NEWLOCALS} but
     * not {@link PyCode.Trait#OPTIMIZED}, a new empty ``dict`` will be
     * provided as locals.</li>
     * <li>If the code has the traits {@link PyCode.Trait#NEWLOCALS} and
     * {@link PyCode.Trait#OPTIMIZED}, {@code this.locals} will be
     * {@code null} until set by the sub-class.</li>
     * <li>Otherwise, if the argument {@link #locals} is not
     * {@code null} it specifies {@code this.locals}, and</li>
     * <li>if the argument {@link #locals} is {@code null}
     * {@code this.locals} will be the same as {@code globals}.</li>
     * </ul>
     *
     * @param code that this frame executes
     * @param interpreter providing the module context
     * @param globals global name space
     * @param locals local name space (or it may be {@code globals})
     */
    // Compare CPython _PyFrame_New_NoTrack in frameobject.c
    protected PyFrame(Interpreter interpreter, C code, PyDict globals, Object locals) {

        // Initialise the basics.
        this(interpreter, code, globals);

        // The need for a dictionary of locals depends on the code
        EnumSet<PyCode.Trait> traits = code.traits;
        if (traits.contains(PyCode.Trait.NEWLOCALS)) {
            // Ignore locals argument
            if (traits.contains(PyCode.Trait.OPTIMIZED)) {
                // We can create it later but probably won't need to
                this.locals = null;
            } else {
                this.locals = new PyDict();
            }
        } else if (locals == null) {
            // Default to same as globals.
            this.locals = globals;
        } else {
            /*
             * Use supplied locals. As it may not implement j.u.Map, we wrap any
             * Python object as a Map. Depending on the operations attempted,
             * this may break later.
             */
            this.locals = PyMapping.map(locals);
        }
        // Fix up the builtins module dictionary (simplified)
        this.builtins = interpreter.builtinsModule.getDict();
    }

    /**
     * Execute the code in this frame.
     *
     * @return return value of the frame
     */
    abstract Object eval();
}
