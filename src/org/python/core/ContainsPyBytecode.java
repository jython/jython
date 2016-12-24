package org.python.core;

/**
 * Jython stores Python-Bytecode of methods and functions that exceed
 * JVM method-size restrictions in String literals.
 * While Java supports rather long strings, constrained only by
 * int-addressing of arrays, it supports only up to 65535 characters
 * in literals (not sure how escape-sequences are counted).
 * To circumvent this limitation, the code is automatically splitted
 * into several literals with the following naming-scheme.
 *
 * - The marker-interface 'ContainsPyBytecode' indicates that a class
 *   contains (static final) literals of the following scheme:
 * - a prefix of '___' indicates a bytecode-containing string literal
 * - a number indicating the number of parts follows
 * - '0_' indicates that no splitting occurred
 * - otherwise another number follows, naming the index of the literal
 * - indexing starts at 0
 *
 * Examples:
 * ___0_method1   contains bytecode for method1
 * ___2_0_method2 contains first part of method2's bytecode
 * ___2_1_method2 contains second part of method2's bytecode
 *
 * Note that this approach is provisional. In future, Jython might contain
 * the bytecode directly as bytecode-objects. The current approach was
 * feasible with much less complicated JVM bytecode-manipulation, but needs
 * special treatment after class-loading.
 *
 * In a future approach this interface might be removed.
 */
public interface ContainsPyBytecode {
    // For now this is a pure marker-interface.
}
