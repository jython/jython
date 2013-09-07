// Copyright (c) 2013 Jython Developers
package org.python.core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A class named in configuration as the value of <code>python.console</code> must implement this
 * interface, and provide a constructor with a single <code>String</code> argument, to be acceptable
 * during initialization of the interpreter. The argument to the constructor names the encoding in
 * use on the console. Such a class may provide line editing and history recall to an interactive
 * console. A default implementation (that does not provide any such facilities) is available as
 * {@link PlainConsole}.
 */
public interface Console {

    /**
     * Complete initialization and (optionally) install a stream object with line-editing as the
     * replacement for <code>System.in</code>.
     *
     * @throws IOException in case of failure related to i/o
     */
    public void install() throws IOException;

    /**
     * Uninstall the Console (if possible). A Console that installs a replacement for
     * <code>System.in</code> should put back the original value.
     *
     * @throws UnsupportedOperationException if the Console cannot be uninstalled
     */
    public void uninstall() throws UnsupportedOperationException;

    /**
     * Write a prompt and read a line from standard input. The returned line does not include the
     * trailing newline. When the user enters the EOF key sequence, an EOFException should be
     * raised. The built-in function <code>raw_input</code> calls this method on the installed
     * console.
     *
     * @param prompt to output before reading a line
     * @return the line read in (encoded as bytes)
     * @throws IOException in case of failure related to i/o
     * @throws EOFException when the user enters the EOF key sequence
     */
    public ByteBuffer raw_input(CharSequence prompt) throws IOException, EOFException;

    /**
     * Write a prompt and read a line from standard input. The returned line does not include the
     * trailing newline. When the user enters the EOF key sequence, an EOFException should be
     * raised. The Py3k built-in function <code>input</code> calls this method on the installed
     * console.
     *
     * @param prompt to output before reading a line
     * @return the line read in
     * @throws IOException in case of failure related to i/o
     * @throws EOFException when the user enters the EOF key sequence
     */
    public CharSequence input(CharSequence prompt) throws IOException, EOFException;

}
