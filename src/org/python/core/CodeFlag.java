package org.python.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

/**
 * Represents flags that can be set on code objects.
 * 
 * @author Tobias Ivarsson
 */
public enum CodeFlag {
    /**
     * Denotes that the code block uses fast locals.
     */
    CO_OPTIMIZED(0x0001),
    /**
     * Denotes that a new dictionary should be created for the code block.
     */
    CO_NEWLOCALS(0x0002),
    /**
     * The compiled code block has a varargs argument.
     */
    CO_VARARGS(0x0004),
    /**
     * The compiled code block has a varkeyword argument.
     */
    CO_VARKEYWORDS(0x0008),
    /**
     * The compiled code block is a generator code block.
     */
    CO_GENERATOR(0x0020),
    /**
     * Denotes that nested scopes are enabled in the code block.
     */
    CO_NESTED(0x0010),
    /**
     * Denotes that generators are enabled in the code block.
     */
    CO_GENERATOR_ALLOWED(0x1000),
    /**
     * Standard division of integers returns float, truncating division needs to
     * be enforced.
     */
    CO_FUTURE_DIVISION(0x2000),
    /**
     * Absolute import.
     */
    CO_FUTURE_ABSOLUTE_IMPORT(0x4000),
    /**
     * With statement.
     */
    CO_FUTURE_WITH_STATEMENT(0x8000);

    public final int flag;
    private static Iterable<CodeFlag> allFlags = Collections.unmodifiableList(Arrays.asList(values()));

    private CodeFlag(int flag) {
        this.flag = flag;
    }

    public boolean isFlagBitSetIn(int flags) {
        return (flags & flag) != 0;
    }

    static Iterable<CodeFlag> parse(final int flags) {
        return new Iterable<CodeFlag>() {

            public Iterator<CodeFlag> iterator() {
                return new Iterator<CodeFlag>() {
                    Iterator<CodeFlag> all = allFlags.iterator();
                    CodeFlag next = null;

                    public boolean hasNext() {
                        if (next != null) {
                            return true;
                        }
                        while (all.hasNext()) {
                            CodeFlag flag = all.next();
                            if (flag.isFlagBitSetIn(flags)) {
                                next = flag;
                                return true;
                            }
                        }
                        return false;
                    }

                    public CodeFlag next() {
                        if (hasNext()) try {
                            return next;
                        } finally {
                            next = null;
                        }
                        throw new IllegalStateException();
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }

                };
            }

        };
    }
}
