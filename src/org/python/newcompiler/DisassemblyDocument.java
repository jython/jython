// (C) Copyright 2007 Tobias Ivarsson
package org.python.newcompiler;

/**
 * A class representing (the generation of) a document containing the
 * disassembly of some bytecode.
 * 
 * @author Tobias Ivarsson
 */
public interface DisassemblyDocument {

    /**
     * Create a new document that is sub ordinated to this document.
     * 
     * @return the sub-document.
     */
    public DisassemblyDocument newSubSection();

    /**
     * Create a new document that represents the lines before the this document.
     * 
     * @return the pre-document.
     */
    public DisassemblyDocument newPreTitle();

    /**
     * Writes a title for this document.
     * 
     * @param string The title to write.
     */
    public void putTitle(String string);

    /**
     * Writes a title annotated with a bytecode offset to this document. 
     * 
     * @param offset The offset
     * @param string The title to write.
     */
    public void putTitle(long offset, String string);

    /**
     * Writes a line to this document.
     * 
     * @param string The line to write.
     */
    public void put(String string);

    /**
     * Writes a line annotated with a bytecode offset to this document.
     * 
     * @param offset The offset
     * @param string The line to write.
     */
    public void put(long offset, String string);

    /**
     * Write a labeled address to this document.
     * 
     * @param string The label to write.
     */
    public void putLabel(String string);

}
