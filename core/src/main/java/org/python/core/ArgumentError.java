package org.python.core;

/**
 * The type of (non-Python) exception thrown by invoking a slot or
 * method with the wrong pattern of arguments. An {@code ArgumentError}
 * encapsulates what a particular method or slot expected by way of the
 * number of positional arguments and the presence or otherwise of
 * keyword arguments.
 * <p>
 * Having a distinct exception solves the problem that not all the
 * context for an informative message may be in scope at discovery.
 * {@code ArgumentError} should be caught as soon as the necessary
 * context is available and converted to a Python exception.
 */
class ArgumentError extends Exception {
    private static final long serialVersionUID = 1L;

    enum Mode {
        NOARGS, NUMARGS, MINMAXARGS, NOKWARGS
    }

    final ArgumentError.Mode mode;
    final short minArgs, maxArgs;

    private ArgumentError(Mode mode, int minArgs, int maxArgs) {
        this.mode = mode;
        this.minArgs = (short) minArgs;
        this.maxArgs = (short) maxArgs;
    }

    /**
     * The mode is {@link Mode#NOARGS} or {@link Mode#NOKWARGS}. In the
     * latter case, {@link #minArgs} and {@link #maxArgs} should be
     * ignored.
     *
     * @param mode qualifies the sub-type of the problem
     */
    ArgumentError(Mode mode) { this(mode, 0, 0); }

    /**
     * The mode is {@link Mode#NUMARGS}.
     *
     * @param numArgs expected number of arguments
     */
    ArgumentError(int numArgs) {
        this(Mode.NUMARGS, numArgs, numArgs);
    }

    /**
     * The mode is {@link Mode#MINMAXARGS}.
     *
     * @param minArgs minimum expected number of arguments
     * @param maxArgs maximum expected number of arguments
     */
    ArgumentError(int minArgs, int maxArgs) {
        this(Mode.MINMAXARGS, minArgs, maxArgs);
    }

    @Override
    public String toString() {
        switch (mode) {
        case NOARGS:
            return "takes no arguments";
        case NUMARGS:
            return String.format("takes %d arguments", minArgs);
        case MINMAXARGS:
            return String.format("takes from %d to %d arguments",
                    minArgs, maxArgs);
        case NOKWARGS:
            return "takes no keyword arguments";
        default:
            return mode.toString();
        }
    }
}
