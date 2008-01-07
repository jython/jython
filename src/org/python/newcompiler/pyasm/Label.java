// (C) Copyright 2007 Tobias Ivarsson
package org.python.newcompiler.pyasm;

/**
 * Indicates a label in the code.
 * 
 * @author Tobias Ivarsson
 */
public final class Label {

    private static int idCount = 0;
    private int id;
    /**
     * A flag to keep track of whether this label has been visited or not.
     */
    public boolean visited = false;

    /**
     * Create a new label.
     */
    public Label() {
        this.id = idCount++;
    }

    public boolean equals(Object obj) {
        if (obj instanceof Label) {
            Label label = (Label) obj;
            return label.id == this.id;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return id;
    }

    public String toString() {
        return "L" + id;
    }

}
