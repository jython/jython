/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer;

import org.python.indexer.ast.NAttribute;
import org.python.indexer.ast.NName;
import org.python.indexer.ast.NNode;
import org.python.indexer.ast.NStr;

/**
 * Encapsulates information about a binding reference.
 */
public class Ref {

    private static final int ATTRIBUTE = 0x1;
    private static final int CALL = 0x2;  // function/method call
    private static final int NEW = 0x4;  // instantiation
    private static final int STRING = 0x8; // source node is a String

    private int start;
    private String file;
    private String name;
    private int flags;

    public Ref(NNode node) {
        if (node == null) {
            throw new IllegalArgumentException("null node");
        }
        file = node.getFile();
        start = node.start();

        if (node instanceof NName) {
            NName nn = ((NName)node);
            name = nn.id;
            if (nn.isCall()) {
                // We don't always have enough information at this point to know
                // if it's a constructor call or a regular function/method call,
                // so we just determine if it looks like a call or not, and the
                // indexer will convert constructor-calls to NEW in a later pass.
                markAsCall();
            }
        } else if (node instanceof NStr) {
            markAsString();
            name = ((NStr)node).n.toString();
        } else {
            throw new IllegalArgumentException("I don't know what " + node + " is.");
        }

        NNode parent = node.getParent();
        if ((parent instanceof NAttribute)
            && node == ((NAttribute)parent).attr) {
            markAsAttribute();
        }
    }

    /**
     * Constructor that provides a way for clients to add additional references
     * not associated with an AST node (e.g. inside a comment or doc string).
     * @param path absolute path to the file containing the reference
     * @param offset the 0-indexed file offset of the reference
     * @param text the text of the reference
     */
    public Ref(String path, int offset, String text) {
        if (path == null) {
            throw new IllegalArgumentException("'path' cannot be null");
        }
        if (text == null) {
            throw new IllegalArgumentException("'text' cannot be null");
        }
        file = path;
        start = offset;
        name = text;
    }

    /**
     * Returns the file containing the reference.
     */
    public String getFile() {
        return file;
    }

    /**
     * Returns the text of the reference.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the starting file offset of the reference.
     */
    public int start() {
        return start;
    }

    /**
     * Returns the ending file offset of the reference.
     */
    public int end() {
        return start + length();
    }

    /**
     * Returns the length of the reference text.
     */
    public int length() {
        return isString() ? name.length() + 2 : name.length();
    }

    /**
     * Returns {@code true} if this reference was unquoted name.
     */
    public boolean isName() {
        return !isString();
    }

    /**
     * Returns {@code true} if this reference was an attribute
     * of some other node.
     */
    public boolean isAttribute() {
        return (flags & ATTRIBUTE) != 0;
    }

    public void markAsAttribute() {
        flags |= ATTRIBUTE;
    }

    /**
     * Returns {@code true} if this reference was a quoted name.
     * If so, the {@link #start} and {@link #length} include the positions
     * of the opening and closing quotes, but {@link #isName} returns the
     * text within the quotes.
     */
    public boolean isString() {
        return (flags & STRING) != 0;
    }

    public void markAsString() {
        flags |= STRING;
    }

    /**
     * Returns {@code true} if this reference is a function or method call.
     */
    public boolean isCall() {
        return (flags & CALL) != 0;
    }

    /**
     * Returns {@code true} if this reference is a class instantiation.
     */
    public void markAsCall() {
        flags |= CALL;
        flags &= ~NEW;
    }

    public boolean isNew() {
        return (flags & NEW) != 0;
    }

    public void markAsNew() {
        flags |= NEW;
        flags &= ~CALL;
    }

    public boolean isRef() {
        return !(isCall() || isNew());
    }

    @Override
    public String toString() {
        return "<Ref:" + file + ":" + name + ":" + start + ">";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Ref)) {
            return false;
        }
        Ref ref = (Ref)obj;
        if (start != ref.start) {
            return false;
        }
        if (name != null) {
            if (!name.equals(ref.name)) {
                return false;
            }
        } else {
            if (ref.name != null) {
                return false;
            }
        }
        if (file != null) {
            if (!file.equals(ref.file)) {
                return false;
            }
        } else {
            if (ref.file != null) {
                return false;
            }
        }
        // This should never happen, but checking it here can help surface bugs.
        if (flags != ref.flags) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return ("" + file + name + start).hashCode();
    }
}
