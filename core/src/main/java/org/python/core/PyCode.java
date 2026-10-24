// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;
import java.util.EnumSet;
import java.util.stream.Stream;

import org.python.core.CodeFlag.PyCF;
import org.python.internal.Util;
import org.python.types.Exposed.Getter;
import org.python.types.Exposed.Member;
import org.python.types.TypeSpec;
import org.python.types.WithClass;

/**
 * The Python {@code code} object. A {@code code} object describes the
 * layout of a {@link PyFrame}, and is a factory for frames of matching
 * type.
 * <p>
 * In this implementation, while there is only one Python type
 * {@code code}, we allow alternative implementations of it. In
 * particular, we provide for a code object that is the result of
 * compiling to JVM byte code, in addition to the expected support for
 * Python byte code. While we represent compiled code of any kind as a
 * Python {@code code} object, not all attributes documented in the
 * Python data model will be meaningful in every implementation of
 * {@code PyCode}.
 * <p>
 * The abstract base {@code PyCode} has a need to store fewer attributes
 * than the concrete CPython {@code code} object, where the only
 * realisation holds a block of byte code with broadly similar needs
 * from one version to the next. We provide get-methods matching all
 * those of CPython, and each concrete class can override them where
 * meaningful.
 */
// Compare CPython PyCodeObject in codeobject.c
public abstract class PyCode implements WithClass {

    /** The Python type {@code code}. */
    public static final PyType TYPE = PyType.fromSpec( //
            new TypeSpec("code", MethodHandles.lookup()));
    /*
     * It is not easy to say, while there is only one concrete sub-class to
     * learn from, which attributes may safely be be in the base, and which
     * implemented in the sub-class to suit the local needs of a definition
     * in CPython or Java byte code.
     */

    /** Characteristics of this {@code PyCode}. */
    // Compare CPython co_flags in code.h
    final EnumSet<CodeFlag> flags;

    /** Source file from which compiled. */
    @Member("co_filename")
    final String filename;
    /** Name of function etc. */
    @Member("co_name")
    final String name;
    /** Fully qualified name of function etc. */
    @Member("co_qualname")
    final String qualname;

    /** Number of positional parameters (not counting {@code *args}). */
    @Member("co_argcount")
    final int argcount;
    /** Number of positional-only parameters. */
    @Member("co_posonlyargcount")
    final int posonlyargcount;
    /** Number of keyword-only parameters. */
    @Member("co_kwonlyargcount")
    final int kwonlyargcount;

    /** First source line number of this code. */
    final int firstlineno;

    /** Constant objects needed by the code. Not {@code null}. */
    final Object[] consts;

    /** Names referenced in the code. Not {@code null}. */
    final String[] names;

    // Construct with arrays not tuples.
    /**
     * Full constructor. The {@link #flags} of the code are supplied here as
     * CPython reports them: as a bit array in an integer, but the
     * constructor makes a conversion, and it is the {@link #flags} which
     * should be used at the Java level.
     * <p>
     * Where the parameters map directly to an attribute of the code object,
     * that is the best way to explain them. Note that this factory method
     * is tuned to the needs of {@code marshal.read} where the serialised
     * form makes no secret of the version-specific implementation details.
     *
     * @param filename {@code co_filename}
     * @param name {@code co_name}
     * @param qualname {@code co_qualname}
     * @param flags {@code co_flags} a bitmap of code flags
     *
     * @param firstlineno {@code co_firstlineno}
     *
     * @param consts {@code co_consts}
     * @param names {@code co_names}
     *
     * @param argcount {@code co_argcount} the number of positional
     *     parameters (including positional-only arguments and arguments
     *     with default values)
     * @param posonlyargcount {@code co_posonlyargcount} the number of
     *     positional-only arguments (including arguments with default
     *     values)
     * @param kwonlyargcount {@code co_kwonlyargcount} the number of
     *     keyword-only arguments (including arguments with default values)
     */
    public PyCode( //
            // Grouped as _PyCodeConstructor in pycore_code.h
            // Metadata
            String filename, String name, String qualname, //
            EnumSet<CodeFlag> flags,
            // The code (not seeing actual byte code in abstract base)
            int firstlineno, // ??? sensible given filename
            // Used by the code
            Object[] consts, String[] names, //
            // Parameter navigation with varnames
            int argcount, int posonlyargcount, int kwonlyargcount) {
        this.argcount = argcount;
        this.posonlyargcount = posonlyargcount;
        this.kwonlyargcount = kwonlyargcount;

        this.flags = flags;
        this.consts = consts;

        this.names = names;

        this.filename = filename;
        this.name = name;
        this.qualname = qualname;
        this.firstlineno = firstlineno;
    }

    @Override
    public PyType getType() { return TYPE; }

    /**
     * Traits characterising local variables of the frame this code object
     * will produce.
     */
    enum VariableTrait {
        /**
         * Belongs in {@code co_varnames}. For legacy reasons this means
         * parameters to the function (even if they are also cell variables),
         * and other local variables that are not cells (or free).
         */
        PLAIN,
        /**
         * Belongs in {@code co_cellvars}. This means the non-free cell
         * variables, even if they are also parameters to the function, in which
         * case they have the {@link #PLAIN} trait too.
         */
        CELL,
        /**
         * Belongs in {@code co_freevars}. These are just the free cell
         * variables. (They cannot also be parameters.)
         */
        FREE
    }

    /**
     * Interface on a store of information about the variables required by a
     * code object and where they will be stored in the frame it creates.
     * This interface abstracts the the storage layout of any concrete
     * implementation of {@link PyCode} or {@link PyFrame} and the
     * description the latter must be able to make of its local variables in
     * the Python API of a {@code code} object. This allows us to treat code
     * objects the same way, whether they contain Python 3.11 byte code or
     * Java byte code.
     * <p>
     * It is used to, for example, initialise the {@code frame} of a
     * function call, and to compute the name tuples of a {@link PyCode} and
     * the argument parser is uses.
     */
    interface Layout {

        /** @return total number of local variables. */
        default int size() {
            // This can't overflow since it is the size of an array.
            return (int)localnames().count();
        }

        /**
         * Return name of one local frame variable.
         *
         * @param index of variable
         * @return name of one variable.
         */
        String name(int index);

        /**
         * Return the {@link VariableTrait}s of the variable at a given index .
         *
         * @param index of variable
         * @return traits of the local variable
         */
        EnumSet<VariableTrait> traits(int index);

        /**
         * Return a stream of the names of all the local variables These are the
         * parameters and then the other plain, cell and free variables, but
         * occurring only once each (whereas {@code co_cellvars} will repeat
         * names from {@code co_varnames} if they are parameters.
         *
         * @return names of all local variables.
         */
        Stream<String> localnames();

        /**
         * Return a stream of the names of variables to include in
         * {@code co_varnames}. These are the parameters and then the plain
         * (non-cell, non-free) variables. Note that some of the arguments may
         * be cell variables.
         *
         * @return names of non-cell and parameters variables.
         */
        Stream<String> varnames();

        /**
         * Return a stream of the names of variables to include in
         * {@code co_cellvars}. These are the variables defined by this
         * {@code code} object and stored as cells. Note that some of the
         * parameters may be cell variables.
         *
         * @return names of cell variables (may be parameters).
         */
        Stream<String> cellvars();

        /**
         * Return a stream of the names of variables to include in
         * {@code co_freevars}. These are the variables stored as cells but
         * defined in another {@code code} object.
         *
         * @return names of free variables.
         */
        Stream<String> freevars();

        /** @return the length of {@code co_varnames} */
        default int nvarnames() {
            // This can't overflow since it is the size of an array.
            return (int)varnames().count();
        }

        /** @return the length of {@code co_cellvars} */
        default int ncellvars() {
            // This can't overflow since it is the size of an array.
            return (int)cellvars().count();
        }

        /** @return the length of {@code co_freevars} */
        default int nfreevars() {
            // This can't overflow since it is the size of an array.
            return (int)freevars().count();
        }
    }

    /**
     * Describe the layout of the frame local variables (at least the
     * arguments), cell and free variables. {@link #co_varnames},
     * {@link #co_cellvars} and {@link #co_freevars} are derived from this,
     * and the signature of the code as a function.
     *
     * @return a {@link Layout} object describing the variables
     */
    // CPython specific at first glance but not after some thought.
    // Compare CPython 3.11 localsplusnames and localspluskinds
    abstract Layout layout();

    // Attributes ----------------------------------------------------

    /**
     * Get required stack size of the code object for CPython bytecode. This
     * attribute is only meaningful for CPython code.
     *
     * @return line-number lookup as a {@code bytes}
     */
    @SuppressWarnings("static-method")
    @Getter
    int co_stacksize() { return 0; }

    /**
     * Return a {@code bytes} object representing the sequence of CPython
     * bytecode instructions in the code. This attribute is only meaningful
     * for CPython code.
     *
     * @return the bytecode
     */
    @SuppressWarnings("static-method")
    @Getter
    PyBytes co_code() { return PyBytes.EMPTY; }

    /**
     * Get the string encoding the mapping from CPython bytecode offsets to
     * line numbers in the source in a manner documented in the CPython
     * source code :/. This attribute is only meaningful for CPython code.
     *
     * @return line-number lookup as a {@code bytes}
     */
    @SuppressWarnings("static-method")
    @Getter
    PyBytes co_lnotab() { return PyBytes.EMPTY; }

    /**
     * Get {@link #consts} as a {@code tuple}.
     *
     * @return {@link #consts} as a {@code tuple}
     */
    @Getter
    PyTuple co_consts() { return PyTuple.from(consts); }

    /**
     * Get {@link #names} as a {@code tuple}.
     *
     * @return {@link #names} as a {@code tuple}
     */
    @Getter
    PyTuple co_names() { return PyTuple.from(names); }

    /**
     * Get {@code co_varnames} as a {@code tuple}.
     *
     * @return {@code co_varnames} as a {@code tuple}
     */
    @Getter
    PyTuple co_varnames() { return PyTuple.from(layout().varnames()); }

    /**
     * Get {@code co_cellvars} as a {@code tuple}.
     *
     * @return {@code co_cellvars} as a {@code tuple}
     */
    @Getter
    PyTuple co_cellvars() { return PyTuple.from(layout().cellvars()); }

    /**
     * Get {@code co_freevars} as a {@code tuple}.
     *
     * @return {@code co_freevars} as a {@code tuple}
     */
    @Getter
    PyTuple co_freevars() { return PyTuple.from(layout().freevars()); }

    /**
     * Get {@code co_flags} as an {@code int}.
     *
     * @return {@code co_flags} as a {@code int}
     */
    @Getter
    int co_flags() { return PyCF.from(flags); }

    // Special methods -----------------------------------------------

    // Compare CPython code_repr in codeobject.c
    @SuppressWarnings("unused")
    private Object __repr__() {
        int lineno = firstlineno != 0 ? firstlineno : -1;
        String file = filename, q = "\"";
        if (file == null) { file = "???"; q = ""; }
        return String.format("<code object %s at %#x, file %s%s%s, line %d>", name, Py.id(this), q,
                file, q, lineno);
    }

    // Java API ------------------------------------------------------

    @Override
    public String toString() { return PyUtil.defaultToString(this); }

    /**
     * Create a {@code PyFunction} that will execute this {@code PyCode}.
     * The strongly-typed {@code defaults}, {@code kwdefaults} ,
     * {@code closure} and {@code annotations} may be {@code null} if they
     * would otherwise be empty. {@code annotations} is always exposed as a
     * {@code dict}, but may be presented to the constructor as a
     * {@code dict} or {@code tuple} of keys and values (or {@code null}).
     *
     * @param interpreter providing the module context
     * @param globals name space to treat as global variables
     * @param defaults default positional argument values or {@code null}
     * @param kwdefaults default keyword argument values or {@code null}
     * @param annotations type annotations ({@code dict}, {@code null} or
     *     maybe {@code tuple})
     * @param closure variables referenced but not defined here, must be
     *     size expected by code or {@code null} if empty.
     * @return the function from this code
     */
    abstract PyFunction<? extends PyCode> createFunction(Interpreter interpreter, PyDict globals,
            Object[] defaults, PyDict kwdefaults, Object annotations, PyCell[] closure);

    /**
     * Create a {@code PyFunction} that will execute this {@code PyCode}
     * (adequate for module-level code).
     *
     * @param interpreter providing the module context
     * @param globals name space to treat as global variables
     * @return the function
     */
    // Compare CPython PyFunction_New in funcobject.c
    // ... with the interpreter required by architecture
    PyFunction<? extends PyCode> createFunction(Interpreter interpreter, PyDict globals) {
        return createFunction(interpreter, globals, Util.EMPTY_ARRAY, Py.dict(), Py.dict(),
                PyCell.EMPTY_ARRAY);
    }

    /**
     * Return the total space in a frame of a code object, that must be
     * reserved for arguments. This is also the size of the layout array
     * appearing as an argument to constructors.
     *
     * @return total space in frame for arguments
     */
    int totalargs() { return totalargs(argcount, flags); }

    /**
     * From the values of {@code co_argcount} and {@code co_flags} (in
     * practice, as they are de-marshalled), compute the total space in a
     * frame of a code object, that must be reserved for arguments. This is
     * also the size of the layout array appearing as an argument to certain
     * constructors.
     *
     * @param argcount argument count excluding collector parameters.
     * @param flags characteristics of the code object
     * @return total space in frame for arguments
     */
    static int totalargs(int argcount, EnumSet<CodeFlag> flags) {
        if (flags.contains(CodeFlag.VARARGS)) { argcount++; }
        if (flags.contains(CodeFlag.VARKEYWORDS)) { argcount++; }
        return argcount;
    }

    // Plumbing ------------------------------------------------------

    /** Empty (zero-length) array of {@code String}. */
    protected static final String[] EMPTY_STRING_ARRAY = Util.EMPTY_STRING_ARRAY;

    private static final String NAME_TUPLES_STRING =
            "name tuple must contain only strings, not '%s' (in %s)";

    /**
     * Check that all the argument is a tuple and that all objects in it are
     * {@code str}, and return them as an array of {@code String}.
     *
     * @param v of names
     * @param tupleName the name of the argument (for error production)
     * @return the names as {@code String[]}
     */
    protected static String[] names(Object v, String tupleName) {
        PyTuple tuple = castTuple(v, tupleName);
        String[] s = new String[tuple.size()];
        int i = 0;
        for (Object name : tuple) {
            s[i++] = PyUnicode.asString(name,
                    o -> Abstract.typeError(NAME_TUPLES_STRING, o, tupleName));
        }
        return s;
    }

    /**
     * @param v to check is a Python {@code bytes}
     * @param arg name of argument (for message only)
     * @return {@code v}
     * @throws PyBaseException (TypeError) if {@code v} cannot be cast to
     *     {@code bytes}
     */
    protected static PyBytes castBytes(Object v, String arg) throws PyBaseException {
        if (v instanceof PyBytes b)
            return b;
        else
            throw Abstract.argumentTypeError("code", arg, "bytes", v);
    }

    /**
     * @param v to check is a Python {@code tuple}
     * @param arg name of argument (for message only)
     * @return {@code v}
     * @throws PyBaseException (TypeError) if {@code v} cannot be cast to
     *     {@code tuple}
     */
    protected static PyTuple castTuple(Object v, String arg) {
        if (v instanceof PyTuple t)
            return t;
        else
            throw Abstract.argumentTypeError("code", arg, "tuple", v);
    }

    /**
     * Cast a Python {@code str} to a Java String or raise a
     * {@code TypeError} mentioning an argument name.
     *
     * @param v to check and cast/convert
     * @param argName the name of the argument (for error production)
     * @return {@code v}
     */
    protected static String castString(Object v, String argName) {
        return PyUnicode.asString(v, o -> Abstract.argumentTypeError("code", argName, "str", o));
    }
}
