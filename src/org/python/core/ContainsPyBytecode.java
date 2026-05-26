package org.python.core;

/**
 * Jython stores Python bytecode of methods and functions that exceed
 * JVM method size restrictions in string literals.
 * While Java supports rather long strings, constrained only by
 * {@code int}-addressing of arrays, it supports only up to 65535 characters
 * in literals (not sure how escape-sequences are counted).
 * To circumvent this limitation, the code is automatically split
 * into several literals with the following naming-scheme.
 * <p>
 * The marker-interface {@link ContainsPyBytecode} indicates that a class
 * contains ({@code static final}) literals of the following scheme:
 * <ul>
 * <li>a prefix of '{@code ___}' indicates a bytecode-containing string literal
 * <li>a number indicating the number of parts follows
 * <li>'{@code 0_}' indicates that no splitting occurred
 * <li>otherwise another number follows, naming the index of the literal
 * <li>indexing starts at {@code 0}
 * </ul>
 * <p>
 * Examples:
 * <ul>
 * <li>{@code ___0_method1}   contains bytecode for {@code method1}
 * <li>{@code ___2_0_method2} contains first part of {@code method2}'s bytecode
 * <li>{@code ___2_1_method2} contains second part of {@code method2}'s bytecode
 * </ul>
 * <p>
 * Note that this approach is provisional. In future, Jython might contain
 * the bytecode directly as bytecode objects. The current approach was
 * feasible with much less complicated JVM bytecode manipulation, but needs
 * special treatment after class loading.
 * <p>
 * In a future approach this interface might be removed.
 *
 * @see BytecodeLoader#fixPyBytecode(Class)
 */
public interface ContainsPyBytecode {
    // For now this is a pure marker-interface.
}
