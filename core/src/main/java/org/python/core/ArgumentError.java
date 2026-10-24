// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

/**
 * The type of (non-Python) exception thrown by invoking a method with
 * the wrong pattern of arguments. An {@code ArgumentError} encapsulates
 * what a particular method expected by way of the number of positional
 * arguments and the presence or otherwise of keyword arguments.
 * <p>
 * Having a distinct exception solves the problem that not all the
 * context for an informative message may be in scope at discovery.
 * {@code ArgumentError} should be caught as soon as the necessary
 * context is available and converted to a Python exception.
 */
// TODO Extend ArgumentError to type errors when invokeExact fails
// Necessary context is the arguments, handle and ArgParser.
public class ArgumentError extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * The types of problem {@code ArgumentError} can express in an error
     * message. These will emerge in {@link #toString()}.
     */
    public enum Mode {
        /** Method takes no arguments */
        NOARGS,
        /** Method takes [numArgs] argument(s) */
        NUMARGS,
        /** Method takes from [minArgs] to [maxArgs] arguments */
        MINMAXARGS,
        /** Method requires at least a 'self' argument */
        SELF,
        /** Method takes no keyword arguments */
        NOKWARGS;

        /**
         * Choose a mode from {@code NOARGS} to {@code MINMAXARGS} based on the
         * min and max argument numbers
         *
         * @param minArgs minimum expected number of arguments
         * @param maxArgs maximum expected number of arguments
         * @return a mode
         */
        static Mode choose(int minArgs, int maxArgs) {
            if (minArgs != maxArgs)
                return MINMAXARGS;
            else if (minArgs != 0)
                return NUMARGS;
            else
                return NOARGS;
        }
    }

    /** The type of error. */
    public final ArgumentError.Mode mode;
    /** Minimum number of arguments. */
    public final short minArgs;
    /** Maximum number of arguments. */
    public final short maxArgs;

    private ArgumentError(Mode mode, int minArgs, int maxArgs) {
        this.mode = mode;
        this.minArgs = (short)minArgs;
        this.maxArgs = (short)maxArgs;
    }

    /**
     * Create an {@code ArgumentError} specifying the mode is
     * {@link Mode#NOARGS} or {@link Mode#NOKWARGS}. In the latter case,
     * {@link #minArgs} and {@link #maxArgs} should be ignored.
     *
     * @param mode qualifies the sub-type of the problem
     */
    public ArgumentError(Mode mode) { this(mode, 0, 0); }

    /**
     * Create an {@code ArgumentError} with mode is {@link Mode#NUMARGS} or
     * {@link Mode#NOARGS} according to the argument.
     *
     * @param numArgs expected number of arguments
     */
    public ArgumentError(int numArgs) { this(numArgs, numArgs); }

    /**
     * Create an {@code ArgumentError} with mode {@link Mode#MINMAXARGS},
     * {@link Mode#NUMARGS} or {@link Mode#NOARGS} according to the
     * arguments.
     *
     * @param minArgs minimum expected number of arguments
     * @param maxArgs maximum expected number of arguments
     */
    public ArgumentError(int minArgs, int maxArgs) {
        this(Mode.choose(minArgs, maxArgs), minArgs, maxArgs);
    }

    /**
     * In cases where a call was made with an extra positional argument the
     * user would not recognise, the message produced by {@link #toString()}
     * will be one-off in its statement of the expected number (or allowable
     * range) of arguments in the signature. Only some modes of
     * {@code ArgumentError} need this adjustment (not {@link Mode#NOKWARGS}
     * for example), and where no change is needed the return is just
     * {@code this}.
     *
     * @return adjusted {@code ArgumentError}.
     */
    public ArgumentError dropPrefix() {
        if (maxArgs < 1) {
            return this;
        } else {
            return new ArgumentError(Math.max(minArgs - 1, 0), maxArgs - 1);
        }
    }

    @Override
    public String toString() {
        switch (mode) {
            case NOARGS:
                return "takes no arguments";
            case NUMARGS:
                String s = minArgs == 1 ? "" : "s";
                return String.format("takes %d argument%s", minArgs, s);
            case MINMAXARGS:
                return String.format("takes from %d to %d arguments", minArgs, maxArgs);
            case SELF:
                return "'self' required";
            case NOKWARGS:
                return "takes no keyword arguments";
            default:
                return mode.toString();
        }
    }
}
