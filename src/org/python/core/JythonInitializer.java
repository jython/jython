package org.python.core;

import java.util.Properties;

import org.python.core.adapter.ExtensiblePyObjectAdapter;

/**
 * A service for initializing Jython without explicitly calling {@link PySystemState#initialize}. If
 * a file META-INF/services/org.python.core.JythonInitializer is on the classpath, Jython will
 * instantiate the class named in that file and use it in Jython's initialization. The given class
 * must be an implementation of this interface with a no-arg constructor.
 *
 * @see <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jar/jar.html#Service%20Provider">Java
       Service Providers</a>
 */
public interface JythonInitializer {

    /**
     * Called from {@link PySystemState#initialize} with the full set of initialization arguments.
     * Implementations may modify or replace the given arguments, and must call
     * {@link PySystemState#doInitialize}.
     *
     * @param argv
     *            - The command line arguments the jython interpreter was started with, or an empty
     *            array if jython wasn't started directly from the command line.
     * @param classLoader
     *            - The classloader to be used by sys, or null if no sys-specific classloader was
     *            specified
     */
    void initialize(Properties preProperties,
                    Properties postProperties,
                    String[] argv,
                    ClassLoader classLoader,
                    ExtensiblePyObjectAdapter adapter);
}
