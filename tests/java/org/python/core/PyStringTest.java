package org.python.core;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/** Odd tests of the Python {@code str} implementation that are best done from Java. */
public class PyStringTest {

    /**
     * Construction of a {@code PyString} from a {@code String} with characters > {@code 0xff}
     * results in an {@code IllegalArgumentException} that quotes a safely-escaped version of the
     * string.
     */
    @Test
    public void preventNonBytePyString() {
        try {
            // Quercus cerris (turkey oak) in Turkish. https://tr.wikipedia.org
            PyString s = Py.newString("Saçlı meşe");
        } catch (IllegalArgumentException iae) {
            String m = iae.getMessage();
            assertTrue("Quotes message", m.contains("Sa\\xe7l\\u0131 me\\u015fe"));
            return;
        }
        fail("Expected exception not thrown");
    }
}
