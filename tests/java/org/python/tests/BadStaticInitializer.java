package org.python.tests;


public class BadStaticInitializer {
    static {
        // javac doesn't like a naked throw in a static block, so surround it in a dummy if
        if (true) {
            throw new RuntimeException();
        }
    }
}
