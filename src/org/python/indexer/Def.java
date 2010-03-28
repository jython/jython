/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer;

import org.python.indexer.ast.NName;
import org.python.indexer.ast.NNode;
import org.python.indexer.ast.NUrl;

/**
 * Encapsulates information about a binding definition site.
 */
public class Def {

    // Being frugal with fields here is good for memory usage.
    private int start;
    private int end;
    private NBinding binding;
    private String fileOrUrl;
    private String name;

    public Def(NNode node) {
        this(node, null);
    }

    public Def(NNode node, NBinding b) {
        if (node == null) {
            throw new IllegalArgumentException("null 'node' param");
        }
        binding = b;
        if (node instanceof NUrl) {
            String url = ((NUrl)node).getURL();
            if (url.startsWith("file://")) {
                fileOrUrl = url.substring("file://".length());
            } else {
                fileOrUrl = url;
            }
            return;
        }

        // start/end offsets are invalid/bogus for NUrls
        start = node.start();
        end = node.end();
        fileOrUrl = node.getFile();
        if (fileOrUrl == null) {
            throw new IllegalArgumentException("Non-URL nodes must have a non-null file");
        }
        if (node instanceof NName) {
            name = ((NName)node).id;
        }
    }

    /**
     * Returns the name of the node.  Only applies if the definition coincides
     * with a {@link NName} node.
     * @return the name, or null
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the file if this node is from a source file, else {@code null}.
     */
    public String getFile() {
        return isURL() ? null : fileOrUrl;
    }

    /**
     * Returns the URL if this node is from a URL, else {@code null}.
     */
    public String getURL() {
        return isURL() ? fileOrUrl : null;
    }

    /**
     * Returns the file if from a source file, else the URL.
     */
    public String getFileOrUrl() {
        return fileOrUrl;
    }

    /**
     * Returns {@code true} if this node is from a URL.
     */
    public boolean isURL() {
        return fileOrUrl.startsWith("http://");
    }

    public boolean isModule() {
        return binding != null && binding.kind == NBinding.Kind.MODULE;
    }

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }

    public int length() {
        return end - start;
    }

    public boolean isName() {
        return name != null;
    }

    void setBinding(NBinding b) {
        binding = b;
    }

    public NBinding getBinding() {
        return binding;
    }

    @Override
    public String toString() {
        return "<Def:" + (name == null ? "" : name) +
                ":" + start + ":" + fileOrUrl + ">";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Def)) {
            return false;
        }
        Def def = (Def)obj;
        if (start != def.start) {
            return false;
        }
        if (end != def.end) {
            return false;
        }
        if (name != null) {
            if (!name.equals(def.name)) {
                return false;
            }
        } else {
            if (def.name != null) {
                return false;
            }
        }
        if (fileOrUrl != null) {
            if (!fileOrUrl.equals(def.fileOrUrl)) {
                return false;
            }
        } else {
            if (def.fileOrUrl != null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return ("" + fileOrUrl + name + start + end).hashCode();
    }
}
