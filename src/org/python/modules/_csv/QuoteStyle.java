/* Copyright (c) Jython Developers */
package org.python.modules._csv;

/**
 * CSV quoting styles.
 */
public enum QuoteStyle {
    QUOTE_MINIMAL, QUOTE_ALL, QUOTE_NONNUMERIC, QUOTE_NONE;

    /**
     * Return a QuoteStyle instance from an integer value.
     *
     * @param ordinal an int value
     * @return a QuoteStyle instance of null if invalid ordinal
     */
    public static QuoteStyle fromOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= values().length) {
            return null;
        }
        return values()[ordinal];
    }
}
