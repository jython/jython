package org.python.util.install;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

public class UnicodeSequencesTest extends TestCase {

    private static Set _latin1Encodings;

    public void testUmlaute() {
        String fileEncoding = System.getProperty("file.encoding", "unknown");
        if (getLatin1Encodings().contains(fileEncoding)) {
            assertEquals("ä", UnicodeSequences.a2);
            assertEquals("Ä", UnicodeSequences.A2);
            assertEquals("ö", UnicodeSequences.o2);
            assertEquals("Ö", UnicodeSequences.O2);
            assertEquals("ü", UnicodeSequences.u2);
            assertEquals("Ü", UnicodeSequences.U2);
        }
    }

    private static Set getLatin1Encodings() {
        if (_latin1Encodings == null) {
            _latin1Encodings = new HashSet(3);
            _latin1Encodings.add("ISO-LATIN-1");
            _latin1Encodings.add("ISO-8859-1");
            _latin1Encodings.add("Cp1252");
        }
        return _latin1Encodings;
    }

}
