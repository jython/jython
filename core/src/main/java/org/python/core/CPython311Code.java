// Copyright (c)2023 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.python.core.stringlib.ByteArrayBuilder;

/**
 * Our equivalent to the Python code object ({@code PyCodeObject} in
 * CPython's C API).
 */
public class CPython311Code extends PyCode {

    /**
     * Describe the layout of the frame local variables (including
     * arguments), cell and free variables allowing implementation-level
     * access to CPython-specific features.
     */
    final CPythonLayout layout;

    /**
     * Instruction opcodes, not {@code null}. Treat these as unsigned
     * 16-bit patterns in which the low 8 bits is the argument and the
     * upper 8 bits is the opcode itself.
     */
    final short[] wordcode;

    /**
     * Table of byte code address ranges mapped to source lines,
     * presentable as defined in PEP 626.
     */
    // See CPython lnotab_notes.txt
    final byte[] linetable;

    /** Number of entries needed for evaluation stack. */
    final int stacksize;

    /**
     * Table of byte code address ranges mapped to handler addresses in
     * a compact byte encoding (defined by CPython and appearing in the
     * serialised form of a {@code code} object).
     */
    final byte[] exceptiontable;

    /**
     * Full constructor based on CPython's
     * {@code PyCode_NewWithPosOnlyArgs}. The {@link #traits} of the
     * code are supplied here as CPython reports them: as a bit array in
     * an integer, but the constructor makes a conversion, and it is the
     * {@link #traits} which should be used at the Java level.
     * <p>
     * Where the parameters map directly to an attribute of the code
     * object, that is the best way to explain them. Note that this
     * factory method is tuned to the needs of {@code marshal.read}
     * where the serialised form makes no secret of the version-specific
     * implementation details.
     *
     * @param filename {@code co_filename}
     * @param name {@code co_name}
     * @param qualname {@code co_qualname}
     * @param flags {@code co_flags} a bitmap of traits
     *
     * @param wordcode {@code co_code} as unsigned 16-bit words
     * @param firstlineno first source line of this code
     * @param linetable mapping byte code ranges to source lines
     *
     * @param consts {@code co_consts}
     * @param names {@code co_names}
     *
     * @param layout variable names and properties, in the order
     *     {@code co_varnames + co_cellvars + co_freevars} but without
     *     repetition.
     *
     * @param argcount {@code co_argcount} the number of positional
     *     parameters (including positional-only arguments and arguments
     *     with default values)
     * @param posonlyargcount {@code co_posonlyargcount} the number of
     *     positional-only arguments (including arguments with default
     *     values)
     * @param kwonlyargcount {@code co_kwonlyargcount} the number of
     *     keyword-only arguments (including arguments with default
     *     values)
     *
     * @param stacksize {@code co_stacksize}
     * @param exceptiontable supports exception processing
     */
    public CPython311Code( //
            // Grouped as _PyCodeConstructor in pycore_code.h
            // Metadata
            String filename, String name, String qualname, //
            int flags,
            // The code
            short[] wordcode, int firstlineno, byte[] linetable,
            // Used by the code
            Object[] consts, String[] names,
            // Mapping frame offsets to information
            CPythonLayout layout,
            // Parameter navigation with varnames
            int argcount, int posonlyargcount, int kwonlyargcount,
            // Needed to support execution
            int stacksize, byte[] exceptiontable) {

        // Most of the arguments are applicable to any PyCode
        super(filename, name, qualname, flags, //
                firstlineno, //
                consts, names, //
                argcount, posonlyargcount, kwonlyargcount);

        // A few are CPython-specific (tentatively these).
        this.layout = layout;
        this.wordcode = wordcode;
        this.linetable = linetable;
        this.stacksize = stacksize;
        this.exceptiontable = exceptiontable;
    }

    /**
     * Essentially equivalent to the (strongly-typed) constructor, but
     * accepting {@code Object} arguments, which are checked for type
     * here. This is primarily designed for use by the {@code marshal}
     * module.
     * <p>
     * The {@link #traits} of the code are supplied here as CPython
     * reports them: as a bitmap in an integer, but the constructor
     * makes a conversion, and it is the {@link #traits} which should be
     * used at the Java level.
     * <p>
     * Where the parameters map directly to an attribute of the code
     * object, that is the best way to explain them. Note that this
     * factory method is tuned to the needs of {@code marshal.read}
     * where the serialised form makes no secret of the version-specific
     * implementation details.
     *
     * @param filename ({@code str}) = {@code co_filename}
     * @param name ({@code str}) = {@code co_name}
     * @param qualname ({@code str}) = {@code co_qualname}
     * @param flags ({@code int}) = @code co_flags} a bitmap of traits
     *
     * @param bytecode ({@code bytes}) = {@code co_code}
     * @param firstlineno ({@code int}) = {@code co_firstlineno}
     * @param linetable ({@code bytes}) = {@code co_linetable}
     *
     * @param consts ({@code tuple}) = {@code co_consts}
     * @param names ({@code tuple[str]}) = {@code co_names}
     *
     * @param localsplusnames ({@code tuple[str]}) variable names
     * @param localspluskinds ({@code bytes}) variable kinds
     * @param argcount ({@code int}) = {@code co_argcount}
     * @param posonlyargcount ({@code int}) = {@code co_posonlyargcount}
     * @param kwonlyargcount ({@code int}) = {@code co_kwonlyargcount}
     * @param stacksize ({@code int}) = {@code co_stacksize}
     * @param exceptiontable ({@code tuple}) supports exception
     *     processing
     * @return a new code object
     */
    // Compare CPython _PyCode_New in codeobject.c
    public static CPython311Code create( //
            // Grouped as _PyCodeConstructor in pycore_code.h
            // Metadata
            Object filename, Object name, Object qualname, int flags,
            // The code
            Object bytecode, int firstlineno, Object linetable,
            // Used by the code
            Object consts, Object names,
            // Mapping frame offsets to information
            Object localsplusnames, Object localspluskinds,
            // For navigation within localsplus
            int argcount, int posonlyargcount, int kwonlyargcount,
            // Needed to support execution
            int stacksize, Object exceptiontable) {

        // Order of checks and casts based on _PyCode_Validate FWIW
        if (argcount < posonlyargcount || posonlyargcount < 0 || kwonlyargcount < 0) {
            throw new ValueError("code: argument counts inconsistent");
        }
        if (stacksize < 0) { throw new ValueError("code: bad stacksize"); }
        if (flags < 0) { throw new ValueError("code: bad flags argument"); }

        PyBytes _bytecode = castBytes(bytecode, "bytecode");
        PyTuple _consts = castTuple(consts, "consts");
        String[] _names = names(names, "names");

        // Compute a layout from localsplus* arrays
        CPythonLayout _layout =
                new CPythonLayout(localsplusnames, localspluskinds, totalargs(argcount, flags));

        String _name = castString(name, "name");
        String _qualname = castString(qualname, "qualname");
        String _filename = castString(filename, "filename");

        PyBytes _linetable = castBytes(linetable, "linetable");
        PyBytes _exceptiontable = castBytes(exceptiontable, "exceptiontable");

        // Everything is the right type and size
        return new CPython311Code(//
                _filename, _name, _qualname, flags, //
                wordcode(_bytecode), firstlineno, _linetable.asByteArray(), //
                _consts.toArray(), _names, //
                _layout, //
                argcount, posonlyargcount, kwonlyargcount, //
                stacksize, _exceptiontable.asByteArray());
    }

    // Attributes -----------------------------------------------------

    @Override
    int co_stacksize() { return stacksize; }

    @Override
    PyBytes co_code() {
        ByteArrayBuilder builder = new ByteArrayBuilder(2 * wordcode.length);
        for (short opword : wordcode) {
            // Opcode is high byte and goes first in byte code
            builder.append(opword >> 8).append(opword);
        }
        return new PyBytes(builder);
    }

    // Java API -------------------------------------------------------

    /**
     * Create a {@code PyFunction} that will execute this {@code PyCode}
     * (adequate for module-level code).
     *
     * @param interpreter providing the module context
     * @param globals name space to treat as global variables
     * @return the function
     */
    // Compare CPython PyFunction_NewWithQualName in funcobject.c
    // ... with the interpreter required by architecture
    @Override
    CPython311Function createFunction(Interpreter interpreter, PyDict globals) {
        return new CPython311Function(interpreter, this, globals);
    }

    @Override
    CPython311Function createFunction(Interpreter interpreter, PyDict globals, Object[] defaults,
            PyDict kwdefaults, Object annotations, PyCell[] closure) {
        return new CPython311Function(interpreter, this, globals, defaults, kwdefaults, annotations,
                closure);
    }

    /**
     * Build an {@link ArgParser} to match the code object and given
     * defaults. This is a call-back when constructing a
     * {@code CPython311Function} from this {@code code} object and also
     * when the code object of a function is replaced. The method
     * ensures the parser reflects the variable names and the frame
     * layout implied by the code object. The caller (the function
     * definition) supplies the default values of arguments on return.
     *
     * @return parser reflecting the frame layout of this code object
     */
    ArgParser buildParser() {
        int regargcount = argcount + kwonlyargcount;
        return new ArgParser(name, layout.localnames, regargcount, posonlyargcount, kwonlyargcount,
                traits.contains(PyCode.Trait.VARARGS), traits.contains(PyCode.Trait.VARKEYWORDS));
    }

    @Override
    CPythonLayout layout() { return layout; }

    /**
     * Store information about the variables required by a
     * {@link CPython311Code} object and where they will be stored in
     * the frame it creates.
     */
    final static class CPythonLayout implements Layout {
        /** Count of {@code co_varnames} */
        final int nvarnames;
        /** Count of {@code co_cellvars} */
        final int ncellvars;
        /** Count of {@code co_freevars} */
        final int nfreevars;
        /**
         * Index of first cell (which may be a parameter). Cell variables do
         * not in general form a contiguous block in the frame.
         */
        private final int cell0;
        /**
         * Index of first free variable. Free variables form a contiguous
         * block in the frame from this index.
         */
        final int free0;
        /** Names of all the variables in frame order. */
        private final String[] localnames;
        /** Kinds of all the variables in frame order. */
        private final byte[] kinds;

        /**
         * Construct a {@code Layout} based on a representation used
         * internally by CPython that appears in the stream {@code marshal}
         * writes, e.g. in a {@code .pyc} file.
         *
         * @param localsplusnames tuple of all the names
         * @param localspluskinds bytes of kinds of variables
         * @param nargs the number (leading) that are arguments
         */
        CPythonLayout(
                // Mapping frame offsets to information
                Object localsplusnames, Object localspluskinds,
                // For navigation within localsplus
                int nargs) {

            PyTuple nameTuple = castTuple(localsplusnames, "localsplusnames");
            PyBytes kindBytes = castBytes(localspluskinds, "localspluskinds");

            int n = nameTuple.size();
            this.localnames = new String[n];
            this.kinds = new byte[n];

            if (kindBytes.size() != n) {
                throw new ValueError(LENGTHS_UNEQUAL, kindBytes.size(), n);
            }

            // Compute indexes into name arrays as we go
            int nloc = 0, nfree = 0, ncell = 0, icell0 = -1;

            /*
             * Step through the localsplus* variables saving the name and kind
             * of each, and counting the different kinds.
             */
            for (int i = 0; i < n; i++) {

                String s = PyUnicode.asString(nameTuple.get(i),
                        o -> Abstract.typeError(NAME_TUPLES_STRING, o, "localsplusnames"));
                byte kindByte = kindBytes.get(i).byteValue();

                if ((kindByte & CO_FAST_LOCAL) != 0) {
                    if ((kindByte & CO_FAST_CELL) != 0) {
                        // Argument referenced by nested scope.
                        ncell += 1;
                        // Remember where this happens first.
                        if (icell0 < 0) { icell0 = i; }
                    }
                    nloc += 1;
                } else if ((kindByte & CO_FAST_CELL) != 0) {
                    // Locally defined but referenced in nested scope.
                    ncell += 1;
                } else if ((kindByte & CO_FAST_FREE) != 0) {
                    // Supplied from a containing scope.
                    nfree += 1;
                }
                localnames[i] = s;
                kinds[i] = kindByte;
            }

            // Cache the counts and cardinal points.
            this.nvarnames = nloc;
            this.ncellvars = ncell;
            this.nfreevars = nfree;
            // If icell0>=0 cell parameter seen, else first cell.
            this.cell0 = icell0 >= 0 ? icell0 : n - nfree - ncell;
            this.free0 = localnames.length - nfree;
        }

        @Override
        public int size() { return localnames.length; }

        @Override
        public String name(int index) { return localnames[index]; }

        @Override
        public EnumSet<VariableTrait> traits(int index) {
            byte kindByte = kinds[index];

            if ((kindByte & CO_FAST_LOCAL) != 0) {
                if ((kindByte & CO_FAST_CELL) != 0)
                    // Argument referenced by nested scope
                    return EnumSet.of(VariableTrait.PLAIN, VariableTrait.CELL);
                else
                    return EnumSet.of(VariableTrait.PLAIN);
            } else if ((kindByte & CO_FAST_CELL) != 0) {
                // Locally defined but referenced in nested scope
                return EnumSet.of(VariableTrait.CELL);
            } else {
                // Supplied from a containing scope
                assert (kindByte & CO_FAST_FREE) != 0;
                return EnumSet.of(VariableTrait.FREE);
            }
        }

        @Override
        public Stream<String> localnames() { return Arrays.stream(localnames); }

        @Override
        public Stream<String> varnames() {
            Spliterator<String> s = spliterator(CO_FAST_LOCAL, nvarnames, 0);
            return StreamSupport.stream(s, false);
        }

        @Override
        public Stream<String> cellvars() {
            Spliterator<String> s = spliterator(CO_FAST_CELL, ncellvars, cell0);
            return StreamSupport.stream(s, false);
        }

        @Override
        public Stream<String> freevars() {
            Spliterator<String> s =
                    spliterator(CO_FAST_FREE, nfreevars, localnames.length - nfreevars);
            return StreamSupport.stream(s, false);
        }

        @Override
        public int nvarnames() { return nvarnames; }

        /** @return the length of {@code co_cellvars} */
        @Override
        public int ncellvars() { return ncellvars; }

        /** @return the length of {@code co_freevars} */
        @Override
        public int nfreevars() { return nfreevars; }

        /**
         * A {@code Spliterator} of local variable names of the kind
         * indicated in the mask. The caller must specify where to start
         * looking in the list and how many names there ought to be.
         *
         * @param mask single bit kind
         * @param count how many of that kind
         * @param start to start looking
         * @return a spliterator of the names
         */
        private Spliterator<String> spliterator(final int mask, final int count, int start) {
            return new Spliterator<String>() {
                private int i = start, remaining = count;

                @Override
                public boolean tryAdvance(Consumer<? super String> action) {
                    if (remaining > 0) {
                        while ((kinds[i++] & mask) == 0) {} // nothing
                        action.accept(localnames[i - 1]);
                        remaining -= 1;
                        return true;
                    } else
                        return false;
                }

                @Override
                public Spliterator<String> trySplit() { return null; }

                @Override
                public long estimateSize() { return count; }

                @Override
                public int characteristics() { return ORDERED | SIZED | IMMUTABLE; }
            };
        }
    }

    // Plumbing -------------------------------------------------------

    private static final String NAME_TUPLES_STRING =
            "name tuple must contain only strings, not '%s' (in %s)";
    private static final String LENGTHS_UNEQUAL =
            "lengths unequal localspluskinds(%d) _localsplusnames(%d)";

    private static final int CO_FAST_LOCAL = 0x20, CO_FAST_CELL = 0x40, CO_FAST_FREE = 0x80;

    /**
     * Convert the contents of a Python {@code bytes} to 16-bit word
     * code as expected by the eval-loop in {@link CPython311Frame}.
     *
     * @param bytecode as compiled by Python as bytes
     * @return 16-bit word code
     */
    private static short[] wordcode(PyBytes bytecode) {
        ShortBuffer wordbuf = bytecode.getNIOByteBuffer().asShortBuffer();
        final int len = wordbuf.remaining();
        short[] code = new short[len];
        wordbuf.get(code, 0, len);
        return code;
    }
}
