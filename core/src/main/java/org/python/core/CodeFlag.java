package org.python.core;

/**
 * Characteristics of a {@code PyCode} (as CPython co_flags). These
 * are not all relevant to all code types.
 */
// XXX Consider not having this, only flags.
enum CodeFlag {
    OPTIMIZED, NEWLOCALS, VARARGS, VARKEYWORDS, NESTED, GENERATOR, COROUTINE,
    ITERABLE_COROUTINE, ASYNC_GENERATOR
}
