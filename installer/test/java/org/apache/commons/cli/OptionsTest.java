package org.apache.commons.cli;

import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

public class OptionsTest extends TestCase {

    private Options _options;

    public void testSortAsAdded() {
        _options = new Options();
        _options.setSortAsAdded(true);
        _options.addOption("f", "first", false, "first");
        _options.addOption("s", "second", false, "second");
        _options.addOption("t", "third", false, "third");
        Collection optionCollection = _options.getOptions();
        Iterator optionIterator = optionCollection.iterator();
        assertTrue(optionIterator.hasNext());
        assertEquals("first", ((Option) optionIterator.next()).getLongOpt());
        assertTrue(optionIterator.hasNext());
        assertEquals("second", ((Option) optionIterator.next()).getLongOpt());
        assertTrue(optionIterator.hasNext());
        assertEquals("third", ((Option) optionIterator.next()).getLongOpt());
        assertFalse(optionIterator.hasNext());
    }
}
