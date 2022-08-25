// Copyright (c)2022 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.nio.CharBuffer;

import org.python.core.stringlib.ByteArrayBuilder;

/**
 * Our equivalent to the Python code object ({@code PyCodeObject} in
 * CPython's C API).
 */
public class CPython38Code extends PyCode {

    /** Number of entries needed for evaluation stack. */
    final int stacksize;

    /** Instruction opcodes, not {@code null}. */
    final char[] wordcode;

    // -> CPythonCode
    /** Encodes the address to/from line number mapping */
    final PyBytes lnotab;

    /**
     * Full constructor based on CPython's
     * {@code PyCode_NewWithPosOnlyArgs}. The {@link #traits} of the
     * code are supplied here as CPython reports them: as a bit array in
     * an integer, but the constructor makes a conversion, and it is the
     * {@link #traits} which should be used at the Java level.
     *
     * @param argcount value of {@link PyCode#argcount}
     * @param posonlyargcount value of {@link PyCode#posonlyargcount}
     * @param kwonlyargcount value of {@link PyCode#kwonlyargcount}
     * @param nlocals value of {@link PyCode#nlocals}
     * @param stacksize value of {@link #stacksize}
     * @param flags value of {@link PyCode#flags} and
     *     {@link PyCode#traits}
     * @param code value of {@link #wordcode}
     * @param consts value of {@link PyCode#consts}
     * @param names value of {@link PyCode#names}
     * @param varnames value of {@link PyCode#varnames} must be
     *     {@code str}
     * @param freevars value of {@link PyCode#freevars} must be
     *     {@code str}
     * @param cellvars value of {@link PyCode#cellvars} must be
     *     {@code str}
     * @param filename value of {@link PyCode#filename} must be
     *     {@code str}
     * @param name value of {@link PyCode#name}
     * @param firstlineno value of {@link PyCode#firstlineno}
     * @param lnotab value of {@link #lnotab}
     */
    public CPython38Code( //
            int argcount,           // co_argcount
            int posonlyargcount,    // co_posonlyargcount
            int kwonlyargcount,     // co_kwonlyargcount
            int nlocals,            // co_nlocals
            int stacksize,          // co_stacksize
            int flags,              // co_flags

            PyBytes code,           // co_code

            PyTuple consts,         // co_consts

            PyTuple names,          // names ref'd in code
            PyTuple varnames,       // args and non-cell locals
            PyTuple freevars,       // ref'd here, def'd outer
            PyTuple cellvars,       // def'd here, ref'd nested

            String filename,     // loaded from
            String name,         // of function etc.
            int firstlineno,        // of source
            PyBytes lnotab          // map opcode address to source
    ) {
        super(argcount, posonlyargcount, kwonlyargcount, nlocals, flags, consts, names, varnames,
                freevars, cellvars, filename, name, firstlineno);
        // A few of these (just a few) are local to this class.
        this.wordcode = wordcode(code);
        this.stacksize = stacksize;
        this.lnotab = lnotab;

    }

    /**
     * Essentially equivalent to the (strongly-typed) constructor, but
     * accepting {@code Object} arguments, which are checked for type.
     * The {@link #traits} of the code are supplied here as CPython
     * reports them: as a bit array in an integer, but the constructor
     * makes a conversion, and it is the {@link #traits} which should be
     * used at the Java level.
     *
     * @param argcount value of {@link #argcount}
     * @param posonlyargcount value of {@link #posonlyargcount}
     * @param kwonlyargcount value of {@link #kwonlyargcount}
     * @param nlocals value of {@link #nlocals}
     * @param stacksize value of {@link #stacksize}
     * @param flags value of {@link #flags} and {@link #traits}
     * @param bytecode value of {@link #wordcode}
     * @param consts value of {@link #consts}
     * @param names value of {@link #names}
     * @param varnames value of {@link #varnames} must be {@code str}
     * @param freevars value of {@link #freevars} must be {@code str}
     * @param cellvars value of {@link #cellvars} must be {@code str}
     * @param filename value of {@link #filename} must be {@code str}
     * @param name value of {@link #name}
     * @param firstlineno value of {@link #firstlineno}
     * @param lnotab value of {@link #lnotab}
     * @return the frame ready for use
     */
    public static CPython38Code create(int argcount, int posonlyargcount, int kwonlyargcount,
            int nlocals, int stacksize, int flags, Object bytecode, Object consts, Object names,
            Object varnames, Object freevars, Object cellvars, Object filename, Object name,
            int firstlineno, Object lnotab) {

        PyBytes _bytecode = castBytes(bytecode, "bytecode");

        PyTuple _consts = names(consts, "consts");

        PyTuple _names = names(names, "names");
        PyTuple _varnames = names(varnames, "varnames");
        PyTuple _freevars = names(freevars, "freevars");
        PyTuple _cellvars = names(cellvars, "cellvars");

        String _filename = castString(filename, "filename");
        String _name = castString(name, "name");

        PyBytes _lnotab = castBytes(lnotab, "lnotab");

        return new CPython38Code(argcount, posonlyargcount, kwonlyargcount, nlocals, stacksize,
                flags, _bytecode, _consts, _names, _varnames, _freevars, _cellvars, _filename,
                _name, firstlineno, _lnotab);
    }

    // Attributes -----------------------------------------------------

    @Override
    // @Getter
    int co_stacksize() { return stacksize; }

    @Override
    // @Getter
    PyBytes co_code() {
        ByteArrayBuilder builder = new ByteArrayBuilder(2 * wordcode.length);
        for (char opword : wordcode) {
            // Opcode is high byte and first
            builder.append(opword >> 8).append(opword);
        }
        return new PyBytes(builder);
    }

    @Override
    // @Getter
    PyBytes co_lnotab() { return lnotab; }

    // Java API -------------------------------------------------------

    @Override
    CPython38Frame createFrame(Interpreter interpreter, PyDict globals, Object locals) {
        return new CPython38Frame(interpreter, this, globals, locals);
    }

    // Plumbing -------------------------------------------------------

    /**
     * Convert the contents of a Python {@code bytes} to 16-bit word
     * code as expected by the eval-loop in {@link CPython38Frame}.
     *
     * @param bytecode as compiled by Python as bytes
     * @return 16-bit word code
     */
    private static char[] wordcode(PyBytes bytecode) {
        CharBuffer wordbuf = bytecode.getNIOByteBuffer().asCharBuffer();
        final int len = wordbuf.remaining();
        char[] code = new char[len];
        wordbuf.get(code, 0, len);
        return code;
    }

    private static PyBytes castBytes(Object v, String arg) {
        try {
            return (PyBytes)v;
        } catch (ClassCastException cce) {
            throw Abstract.argumentTypeError("code", arg, "bytes", v);

        }
    }

    private static PyTuple names(Object v, String arg) {
        try {
            return (PyTuple)v;
        } catch (ClassCastException cce) {
            throw Abstract.argumentTypeError("code", arg, "tuple", v);

        }
    }

    private static String castString(Object v, String arg) {
        return PyUnicode.asString(v, () -> Abstract.argumentTypeError("code", arg, "str", v));
    }
}
