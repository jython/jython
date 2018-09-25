package org.python.util;

/**
 * A somewhat general-purpose scanner for command options, based on CPython {@code getopt.c}.
 */
class OptionScanner {

    /** Valid options. ':' means expect an argument following. */
    private final String programOpts; // e.g. in Python "3bBc:dEhiJm:OQ:RsStuUvVW:xX?";
    /** Index in argv of the arg currently being processed (or about to be started). */
    private int argIndex = 0;
    /** Character index within the current element of argv (of the next option to process). */
    private int optIndex = 0;
    /** Option argument (where present for returned option). */
    private String optarg = null;
    /** Error message (after returning {@link #ERROR}. */
    private String message = "";
    /** Original argv passed at reset */
    private String[] args;
    /** Return to indicate argument processing is over. */
    static final char DONE = '\uffff';
    /** Return to indicate option was not recognised. */
    static final char ERROR = '\ufffe';
    /** Return to indicate the next argument is a free-standing argument. */
    static final char ARGUMENT = '\ufffd';

    /**
     * Class representing an argument of the long type, where the whole program argument represents
     * one option, e.g. "--help" or "-version". Such options must start with a '-'. The client
     * supplies an array of {@code LongSpec} objects to the constructor to define the valid cases.
     * Long options are recognised before single-letter options are looked for. Note that "-" itself
     * is treated as a long option (even though it is quite short), returning
     * {@link OptionScanner#ERROR} if not explicitly defined as a {@code LongSpec}.
     */
    static class LongSpec {

        final String key;
        final char returnValue;
        final boolean hasArgument;

        /**
         * Define that the long argument should return a given char value in calls to
         * {@link OptionScanner#getOption()}, and whether or not an option argument should appear
         * following it on the command line. This character value need not be the same as any
         * single-character option, and may be {@link OptionScanner#DONE} (typically for the key
         * {@code "--"}.
         *
         * @param key to match
         * @param returnValue to return when that matches
         * @param hasArgument an argument to the option is expected to follow
         */
        public LongSpec(String key, char returnValue, boolean hasArgument) {
            this.key = key;
            this.returnValue = returnValue;
            this.hasArgument = hasArgument;
        }

        /** The same as {@code LongSpec(key, returnValue, false)}. */
        public LongSpec(String key, char returnValue) {
            this(key, returnValue, false);
        }
    }

    private final LongSpec[] longSpec;

    /**
     * Create the scanner from command-line arguments, and information about the valid options.
     *
     * @param args command-line arguments (which must not change during scanning)
     * @param programOpts the one-letter options (with : indicating an option argument
     * @param longSpec table of long options (like --help)
     */
    OptionScanner(String[] args, String programOpts, LongSpec[] longSpec) {
        this.args = args;
        this.programOpts = programOpts;
        this.longSpec = longSpec;
    }

    /**
     * Get the next option (as a character), or return a code designating successful or erroneous
     * completion.
     *
     * @return next option from command line: the actual character or a code.
     */
    char getOption() {
        message = "";
        String arg;
        optarg = null;

        if (argIndex >= args.length) {
            // Option processing is complete
            return DONE;
        } else {
            // We are currently processing:
            arg = args[argIndex];
            if (optIndex == 0) {
                // And we're at the start of it.
                if (!arg.startsWith("-") || arg.length() <= 1) {
                    // Non-option program argument e.g. "-" or file name. Note no ++argIndex.
                    return ARGUMENT;
                } else if (longSpec != null) {
                    // Test for "whole arg" special cases
                    for (LongSpec spec : longSpec) {
                        if (spec.key.equals(arg)) {
                            if (spec.hasArgument) {
                                // Argument to option should be in next arg
                                if (++argIndex < args.length) {
                                    optarg = args[argIndex];
                                } else {
                                    // There wasn't a next arg.
                                    return error("Argument expected for the %s option", arg);
                                }
                            }
                            // And the next processing will be in the next arg
                            ++argIndex;
                            return spec.returnValue;
                        }
                    }
                    // No match: fall through.
                }
                // arg is one or more single character options. Continue after the '-'.
                optIndex = 1;
            }
        }

        // We are in arg=argv[argvIndex] at the character to examine is at optIndex.
        assert argIndex < args.length;
        assert optIndex > 0;
        assert optIndex < arg.length();

        char option = arg.charAt(optIndex++);
        if (optIndex >= arg.length()) {
            // The option was at the end of the arg, so the next action uses the next arg.
            ++argIndex;
            optIndex = 0;
        }

        // Look up the option character in the list of allowable ones
        int ptr;
        if ((ptr = programOpts.indexOf(option)) < 0 || option == ':') {
            if (arg.length() <= 2) {
                return error("Unknown option: -%c", option);
            } else {
                // Might be unrecognised long arg, or a one letter option in a group.
                return error("Unknown option: -%c or '%s'", option, arg);
            }
        }

        // Is the option marked as expecting an argument?
        if (++ptr < programOpts.length() && programOpts.charAt(ptr) == ':') {
            /*
             * The option's argument is the rest of the current argv[argvIndex][optIndex:]. If the
             * option is the last character of arg, argvIndex has already moved on and optIndex==0,
             * so this statement is still true, except that argvIndex may have moved beyond the end
             * of the array.
             */
            if (argIndex < args.length) {
                optarg = args[argIndex].substring(optIndex);
                // And the next processing will be in the next arg
                ++argIndex;
                optIndex = 0;
            } else {
                // We were looking for an argument but there wasn't one.
                return error("Argument expected for the -%c option", option);
            }
        }

        return option;
    }

    /** Get the argument of the previously returned option or {@code null} if none. */
    String getOptionArgument() {
        return optarg;
    }

    /**
     * Get a whole argument (not the argument of an option), for use after {@code ARGUMENT} was
     * returned. This advances the internal state to the next argument.
     */
    String getWholeArgument() {
        optIndex = 0;
        return args[argIndex++];
    }

    /**
     * Peek at a whole argument (not the argument of an option), for use after {@code ARGUMENT} was
     * returned. This <b>does not</b> advance the internal state to the next argument.
     */
    String peekWholeArgument() {
        return args[argIndex];
    }

    /** Number of arguments that remain unprocessed from the original array. */
    int countRemainingArguments() {
        return args.length - argIndex;
    }

    /** Get the error message (when we previously returned {@link #ERROR}. */
    String getMessage() {
        return message;
    }

    /** Set the error message as {@code String.format(message, args)} and return {@link #ERROR}. */
    char error(String message, Object... args) {
        this.message = String.format(message, args);
        return ERROR;
    }
}
