/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.demos;

import org.python.indexer.Def;
import org.python.indexer.Ref;
import org.python.indexer.Indexer;
import org.python.indexer.NBinding;
import org.python.indexer.StyleRun;
import org.python.indexer.Util;
import org.python.indexer.types.NModuleType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collects per-file hyperlinks, as well as styles that require the
 * symbol table to resolve properly.
 */
class Linker {

    private static final Pattern CONSTANT = Pattern.compile("[A-Z_][A-Z0-9_]*");

    // Map of file-path to semantic styles & links for that path.
    private Map<String, List<StyleRun>> fileStyles = new HashMap<String, List<StyleRun>>();

    private File outDir;  // where we're generating the output html
    private String rootPath;

    /**
     * Constructor.
     * @param root the root of the directory tree being indexed
     * @param outdir the html output directory
     */
    public Linker(String root, File outdir) {
        rootPath = root;
        outDir = outdir;
    }

    /**
     * Process all bindings across all files and record per-file semantic styles.
     * Should be called once per index.
     */
    public void findLinks(Indexer indexer) {
        for (NBinding nb : indexer.getBindings().values()) {
            addSemanticStyles(nb);
            processDefs(nb);
            processRefs(nb);
        }
    }

    /**
     * Returns the styles (links and extra styles) generated for a given file.
     * @param path an absolute source path
     * @return a possibly-empty list of styles for that path
     */
    public List<StyleRun> getStyles(String path) {
        return stylesForFile(path);
    }

    private List<StyleRun> stylesForFile(String path) {
        List<StyleRun> styles = fileStyles.get(path);
        if (styles == null) {
            styles = new ArrayList<StyleRun>();
            fileStyles.put(path, styles);
        }
        return styles;
    }

    private void addFileStyle(String path, StyleRun style) {
        stylesForFile(path).add(style);
    }

    /**
     * Add additional highlighting styles based on information not evident from
     * the AST.
     * @param def the binding's main definition node
     * @param b the binding
     */
    private void addSemanticStyles(NBinding nb) {
        Def def = nb.getSignatureNode();
        if (def == null || !def.isName()) {
            return;
        }

        boolean isConst = CONSTANT.matcher(def.getName()).matches();
        switch (nb.getKind()) {
            case SCOPE:
                if (isConst) {
                    addSemanticStyle(def, StyleRun.Type.CONSTANT);
                }
                break;
            case VARIABLE:
                addSemanticStyle(def, isConst ? StyleRun.Type.CONSTANT : StyleRun.Type.IDENTIFIER);
                break;
            case PARAMETER:
                addSemanticStyle(def, StyleRun.Type.PARAMETER);
                break;
            case CLASS:
                addSemanticStyle(def, StyleRun.Type.TYPE_NAME);
                break;
        }
    }

    private void addSemanticStyle(Def def, StyleRun.Type type) {
        String path = def.getFile();
        if (path != null) {
            addFileStyle(path, new StyleRun(type, def.start(), def.length()));
        }
    }

    /**
     * Create name anchors for definition sites.
     */
    private void processDefs(NBinding nb) {
        Def def = nb.getSignatureNode();
        if (def == null || def.isURL()) {
            return;
        }
        StyleRun style = new StyleRun(StyleRun.Type.ANCHOR, def.start(), def.length());
        style.message = nb.getQname();
        style.url = nb.getQname();
        addFileStyle(def.getFile(), style);
    }

    /**
     * Collect cross-reference links for every file.
     */
    private void processRefs(NBinding nb) {
        if (nb.hasRefs()) {  // avoid lazy ref-list instantiation
            for (Ref ref : nb.getRefs()) {
                processRef(ref, nb);
            }
        }
    }

    /**
     * Adds a hyperlink for a single reference.
     */
    void processRef(Ref ref, NBinding nb) {
        String path = ref.getFile();
        StyleRun link = new StyleRun(StyleRun.Type.LINK, ref.start(), ref.length());
        link.message = nb.getQname();
        link.url = toURL(nb, path);
        if (link.url != null) {
            addFileStyle(path, link);
        }
    }

    /**
     * Generate a URL for a reference to a binding.
     * @param nb the referenced binding
     * @param path the path containing the reference, or null if there was an error
     */
    private String toURL(NBinding nb, String path) {
        Def def = nb.getSignatureNode();
        if (def == null) {
            return null;
        }
        if (nb.isBuiltin()) {
            return def.getURL();
        }

        if (def.isModule()) {
            return toModuleUrl(nb);
        }

        String anchor = "#" + nb.getQname();
        if (nb.getFirstFile().equals(path)) {
            return anchor;
        }

        String destPath = def.getFile();
        try {
            String relpath = destPath.substring(rootPath.length());
            return Util.joinPath(outDir.getAbsolutePath(), relpath) + ".html" + anchor;
        } catch (Exception x) {
            System.err.println("path problem:  dest=" + destPath + ", root=" + rootPath + ": " + x);
            return null;
        }
    }

    /**
     * Generate an anchorless URL linking to another file in the index.
     */
    private String toModuleUrl(NBinding nb) {
        NModuleType mtype = nb.getType().follow().asModuleType();
        String path = mtype.getFile();
        if (!path.startsWith(rootPath)) {
            return "file://" + path;  // can't find file => punt & load it directly
        }
        String relpath = path.substring(rootPath.length());
        return Util.joinPath(outDir.getAbsolutePath(), relpath) + ".html";
    }
}
