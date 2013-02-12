package org.python.modules._io;

import org.python.core.Py;
import org.python.core.PyException;

/**
 * An object able to check a file access mode provided as a String and represent it as boolean
 * attributes and in a normalised form. Such a string is the the mode argument of the several open()
 * functions available in Python and certain constructors for streams-like objects.
 */
public class OpenMode {

    /** Original string supplied as the mode */
    public final String originalModeString;

    /** Whether this file is opened for reading ('r') */
    public boolean reading;

    /** Whether this file is opened for writing ('w') */
    public boolean writing;

    /** Whether this file is opened in appending mode ('a') */
    public boolean appending;

    /** Whether this file is opened for updating ('+') */
    public boolean updating;

    /** Whether this file is opened in binary mode ('b') */
    public boolean binary;

    /** Whether this file is opened in text mode ('t') */
    public boolean text;

    /** Whether this file is opened in universal newlines mode ('U') */
    public boolean universal;

    /** Whether the mode contained some other symbol from the allowed ones */
    public boolean other;

    /** Set true when any invalid symbol or combination is discovered */
    public boolean invalid;

    /**
     * Error message describing the way in which the mode is invalid, or null if no problem has been
     * found. This field may be set by the constructor (in the case of duplicate or unrecognised
     * mode letters), by the {@link #validate()} method, or by client code.
     */
    public String message;

    /**
     * Decode the given string to an OpenMode object, checking for duplicate or unrecognised mode
     * letters. Valid letters are those in "rwa+btU". Errors in the mode string do not raise an
     * exception, they simply generate an appropriate error message in {@link #message}. After
     * construction, a client should always call {@link #validate()} to complete validity checks.
     *
     * @param mode
     */
    public OpenMode(String mode) {

        originalModeString = mode;
        int n = mode.length();
        boolean duplicate = false;

        for (int i = 0; i < n; i++) {
            char c = mode.charAt(i);

            switch (c) {
                case 'r':
                    duplicate = reading;
                    reading = true;
                    break;
                case 'w':
                    duplicate = writing;
                    writing = true;
                    break;
                case 'a':
                    duplicate = appending;
                    appending = true;
                    break;
                case '+':
                    duplicate = updating;
                    updating = true;
                    break;
                case 't':
                    duplicate = text;
                    text = true;
                    break;
                case 'b':
                    duplicate = binary;
                    binary = true;
                    break;
                case 'U':
                    duplicate = universal;
                    universal = true;
                    break;
                default:
                    other = true;
            }

            // duplicate is set iff c was encountered previously */
            if (duplicate) {
                invalid = true;
                break;
            }
        }

    }

    /**
     * Adjust and validate the flags decoded from the mode string. The method affects the flags
     * where the presence of one flag implies another, then if the {@link #invalid} flag is not
     * already <code>true</code>, it checks the validity of the flags against combinations allowed
     * by the Python <code>io.open()</code> function. In the case of a violation, it sets the
     * <code>invalid</code> flag, and sets {@link #message} to a descriptive message. The point of
     * the qualification "if the <code>invalid</code> flag is not already <code>true</code>" is that
     * the message should always describe the first problem discovered. If left blank, as in fact
     * the constructor does, it will be filled by the generic message when {@link #checkValid()} is
     * finally called. Clients may override this method (by sub-classing) to express the validation
     * correct in their context.
     * <p>
     * The invalid combinations enforced here are those for the "raw" (ie non-text) file types:
     * <ul>
     * <li>universal & (writing | appending)),</li>
     * <li>text & binary</li>,
     * <li>reading & writing,</li>
     * <li>appending & (reading | writing)</li>
     * </ul>
     * See also {@link #validate(String, String, String)} for additional checks relevant to text
     * files.
     */
    public void validate() {

        // Implications
        reading |= universal;

        // Standard tests
        if (!invalid) {
            if (universal && (writing || appending)) {
                message = "can't use U and writing mode at once";
            } else if (text && binary) {
                message = "can't have text and binary mode at once";
            } else {
                // How many of r/U, w and a were given?
                int rwa = 0;
                if (reading) {
                    rwa += 1;
                }
                if (writing) {
                    rwa += 1;
                }
                if (appending) {
                    rwa += 1;
                }
                if (rwa != 1) {
                    message = "must have exactly one of read/write/append mode";
                }
            }
            invalid |= (message != null);
        }
    }

    /**
     * Perform additional validation of the flags relevant to text files. If {@link #invalid} is not
     * already <code>true</code>, and the mode includes {@link #binary}, then all the arguments to
     * this call must be <code>null</code>. If the criterion is not met, then on return from the
     * method, <code>invalid==true</code> and {@link #message} is set to a standard error message.
     * This is the standard additional validation applicable to text files. (By "standard" we mean
     * the test and messages that CPython <code>io.open</code> uses.)
     *
     * @param encoding argument to <code>open()</code>
     * @param errors argument to <code>open()</code>
     * @param newline argument to <code>open()</code>
     */
    public void validate(String encoding, String errors, String newline) {

        // If the basic tests passed and binary mode is set one check text arguments null
        if (!invalid && binary) {
            if (encoding != null) {
                message = "binary mode doesn't take an encoding argument";
            } else if (errors != null) {
                message = "binary mode doesn't take an errors argument";
            } else if (newline != null) {
                message = "binary mode doesn't take a newline argument";
            }
            invalid = (message != null);
        }
    }

    /**
     * Call {@link #validate()} and raise an exception if the mode string is not valid, as signalled
     * by either {@link #invalid} or {@link #other} being <code>true</code> after that call. If no
     * more specific message has been assigned in {@link #message}, report the original mode string.
     *
     * @throws PyException (ValueError) if the mode string was invalid.
     */
    public void checkValid() throws PyException {

        // Actually perform the check
        validate();

        // The 'other' flag reports alien symbols in the original mode string
        invalid |= other;

        // Finally, if invalid, report this as an error
        if (invalid) {
            if (message == null) {
                // Duplicates discovered in the constructor or invalid symbols
                message = String.format("invalid mode: '%.20s'", originalModeString);
            }
            throw Py.ValueError(message);
        }
    }

    /**
     * The mode string we need when constructing a <code>FileIO</code> initialised with the present
     * mode. Note that this is not the same as the full open mode because it omits the text-based
     * attributes.
     *
     * @return "r", "w", or "a" with optional "+".
     */
    public String forFileIO() {
        StringBuilder m = new StringBuilder(2);
        if (appending) {
            m.append('a');
        } else if (writing) {
            m.append('w');
        } else {
            m.append('r');
        }
        if (updating) {
            m.append('+');
        }
        return m.toString();
    }

    /**
     * The mode string that a text file should claim to have, when initialised with the present
     * mode. Note that this only contains text-based attributes. Since mode 't' has no effect,
     * except to produce an error if specified with 'b', we don't reproduce it.
     *
     * @return "", or "U".
     */
    public String text() {
        return universal ? "U" : "";
    }

    @Override
    public String toString() {
        StringBuilder m = new StringBuilder(4);
        if (appending) {
            m.append('a');
        } else if (writing) {
            m.append('w');
        } else {
            m.append('r');
        }
        if (updating) {
            m.append('+');
        }
        if (text) {
            m.append('t');
        } else if (binary) {
            m.append('b');
        }
        if (universal) {
            m.append('U');
        }
        return m.toString();
    }

}
