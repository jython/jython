/* Provides the interface expected by python.jjt.  I don't understand why
 * javacc 0.7.1 is generating a different TokenMgrError file now!
 */

package org.python.parser;

public class PythonTokenError extends TokenMgrError
{
    public int errorLine;
    public int errorColumn;
    public boolean EOFSeen;

    public PythonTokenError(String message, int errorLine, int errorColumn) {
	super(message, LEXICAL_ERROR);
	this.EOFSeen = false;
	this.errorLine = errorLine;
	this.errorColumn = errorColumn;
    }
}

	
