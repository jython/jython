// Copyright (c) 2013 Jython Developers
package org.python.core;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

/**
 * A base class for classes that can install a console wrapper for a specific console-handling
 * library. The Jython command-line application, when it detects that the console is an interactive
 * session, chooses and installs a class named in registry item <code>python.console</code>, and
 * implementing interface {@link Console}. <code>PlainConsole</code> may be selected by the user
 * through that registry, and is the default console when the selected one fails to load. It will
 * also be installed by the Jython command-line application when in non-interactive mode.
 * <p>
 * Unlike some consoles, <code>PlainConsole</code> does not install a replacement for
 * <code>System.in</code> or <code>System.out</code>, or use a native library. It prompts on
 * <code>System.out</code> and reads from <code>System.in</code> (wrapped with the console
 * encoding).
 *
 * @see org.python.core.RegistryKey#PYTHON_CONSOLE
 */
public class PlainConsole implements Console {

    /** Encoding to use for line input. */
    public final String encoding;

    /** Encoding to use for line input as a <code>Charset</code>. */
    public final Charset encodingCharset;

    /**
     * Construct an instance of the console class specifying the character encoding. This encoding
     * must be one supported by the JVM. The PlainConsole does not replace <code>System.in</code> or
     * <code>System.out</code>, and does not add any line-editing capability to what is standard for
     * your OS console.
     *
     * @param encoding name of a supported encoding or <code>null</code> for
     *            <code>Charset.defaultCharset()</code>
     */
    public PlainConsole(String encoding) throws IllegalCharsetNameException,
            UnsupportedCharsetException {
        if (encoding == null) {
            encoding = Charset.defaultCharset().name();
        }
        this.encoding = encoding;
        encodingCharset = Charset.forName(encoding);
    }

    @Override
    public void install() {
        // Nothing to do!
    }

    /**
     * A <code>PlainConsole</code> may be uninstalled. This method assumes any sub-class may not be
     * uninstalled. Sub-classes that permit themselves to be uninstalled <b>must</b> override (and
     * not call) this method.
     *
     * @throws UnsupportedOperationException unless this class is exactly <code>PlainConsole</code>
     */
    @Override
    public void uninstall() throws UnsupportedOperationException {
        Class<? extends Console> myClass = this.getClass();
        if (myClass != PlainConsole.class) {
            throw new UnsupportedOperationException(myClass.getSimpleName()
                    + " console may not be uninstalled.");
        }
    }

    @Override
    public String getEncoding() {
        return encoding;
    }

    @Override
    public Charset getEncodingCharset() {
        return encodingCharset;
    }

}
