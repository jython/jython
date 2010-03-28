/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer.demos;

import org.python.indexer.Indexer;
import org.python.indexer.NBinding;
import org.python.indexer.StyleRun;
import org.python.indexer.Util;

import java.io.File;
import java.util.List;

/**
 * Simple proof-of-concept demo app for the indexer.  Generates a static-html
 * cross-referenced view of the code in a file or directory, using the index to
 * create links and outlines.  <p>
 *
 * The demo not attempt to show general cross references (declarations and uses
 * of a symbol) from the index, nor does it display the inferred type
 * information or generated error/warning diagnostics.  It could be made to do
 * these things, as well as be made more configurable and generally useful, with
 * additional effort.<p>
 *
 * Run it from jython source tree root dir with; e.g., to index <code>/usr/lib/python2.4/email</code>
 * <pre>
 * ant jar &amp;&amp; java -classpath ./dist/jython.jar org.python.indexer.demos.HtmlDemo /usr/lib/python2.4 /usr/lib/python2.4/email
 * </pre>
 *
 * Fully indexing the Python standard library may require a more complete build to pick up all the dependencies:
 * <pre>
 * rm -rf ./html/ &amp;&amp; ant clean &amp;&amp; ant jar &amp;&amp; ant jar-complete &amp;&amp; java -classpath ./dist/jython.jar org.python.indexer.demos.HtmlDemo /usr/lib/python2.4 /usr/lib/python2.4
 * </pre>
 *
 * You can alternately use Jython's version of the Python library.
 * The following command will index the whole thing:
 * <pre>
 * ant jar-complete &amp;&amp; java -classpath ./dist/jython.jar org.python.indexer.demos.HtmlDemo ./CPythonLib ./CPythonLib
 * </pre>
 */
public class HtmlDemo {

    private static final File OUTPUT_DIR =
            new File(new File("./html").getAbsolutePath());

    private static final String CSS =
            ".builtin {color: #5b4eaf;}\n" +
            ".comment, .block-comment {color: #005000; font-style: italic;}\n" +
            ".constant {color: #888888;}\n" +
            ".decorator {color: #778899;}\n" +
            ".doc-string {color: #005000;}\n" +
            ".error {border-bottom: 1px solid red;}\n" +
            ".field-name {color: #2e8b57;}\n" +
            ".function {color: #880000;}\n" +
            ".identifier {color: #8b7765;}\n" +
            ".info {border-bottom: 1px dotted RoyalBlue;}\n" +
            ".keyword {color: #0000cd;}\n" +
            ".lineno {color: #aaaaaa;}\n" +
            ".number {color: #483d8b;}\n" +
            ".parameter {color: #2e8b57;}\n" +
            ".string {color: #4169e1;}\n" +
            ".type-name {color: #4682b4;}\n" +
            ".warning {border-bottom: 1px dotted orange;}\n";

    private Indexer indexer;
    private File rootDir;
    private String rootPath;
    private Linker linker;

    private void makeOutputDir() throws Exception {
        if (!OUTPUT_DIR.exists()) {
            OUTPUT_DIR.mkdirs();
            info("created directory: " + OUTPUT_DIR.getAbsolutePath());
        }
    }

    private void start(File stdlib, File fileOrDir) throws Exception {
        rootDir = fileOrDir.isFile() ? fileOrDir.getParentFile() : fileOrDir;
        rootPath = rootDir.getCanonicalPath();

        indexer = new Indexer();
        indexer.addPath(stdlib.getCanonicalPath());
        info("building index...");
        indexer.loadFileRecursive(fileOrDir.getCanonicalPath());
        indexer.ready();

        info(indexer.getStatusReport());
        generateHtml();
    }

    private void generateHtml() throws Exception {
        info("generating html...");
        makeOutputDir();
        linker = new Linker(rootPath, OUTPUT_DIR);
        linker.findLinks(indexer);

        int rootLength = rootPath.length();
        for (String path : indexer.getLoadedFiles()) {
            if (!path.startsWith(rootPath)) {
                continue;
            }
            File destFile = Util.joinPath(OUTPUT_DIR, path.substring(rootLength));
            destFile.getParentFile().mkdirs();
            String destPath = destFile.getAbsolutePath() + ".html";
            String html = markup(path);
            Util.writeFile(destPath, html);
        }

        info("wrote " + indexer.getLoadedFiles().size() + " files to " + OUTPUT_DIR);
    }

    private String markup(String path) throws Exception {
        String source = Util.readFile(path);

        List<StyleRun> styles = new Styler(indexer, linker).addStyles(path, source);
        styles.addAll(linker.getStyles(path));

        source = new StyleApplier(path, source, styles).apply();

        String outline = new HtmlOutline(indexer).generate(path);

        return "<html><head title=\"" + path + "\">"
                + "<style type='text/css'>\n" + CSS + "</style>\n"
                + "</head>\n<body>\n"
                + "<table width=100% border='1px solid gray'><tr><td valign='top'>"
                + outline
                + "</td><td>"
                + "<pre>" + addLineNumbers(source) + "</pre>"
                + "</td></tr></table></body></html>";
    }

    private String addLineNumbers(String source) {
        StringBuilder result = new StringBuilder((int)(source.length() * 1.2));
        int count = 1;
        for (String line : source.split("\n")) {
            result.append("<span class='lineno'>");
            result.append(count++);
            result.append("</span> ");
            result.append(line);
            result.append("\n");
        }
        return result.toString();
    }

    private static void abort(String msg) {
        System.err.println(msg);
        System.exit(1);
    }

    private static void info(Object msg) {
        System.out.println(msg);
    }

    private static void usage() {
        info("Usage:  java org.python.indexer.HtmlDemo <python-stdlib> <file-or-dir>");
        info("  first arg specifies the root of the python standard library");
        info("  second arg specifies file or directory for which to generate the index");
        info("Example that generates an index for just the email libraries:");
        info(" java org.python.indexer.HtmlDemo ./CPythonLib ./CPythonLib/email");
        System.exit(0);
    }

    private static File checkFile(String path) {
        File f = new File(path);
        if (!f.canRead()) {
            abort("Path not found or not readable: " + path);
        }
        return f;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            usage();
        }

        File fileOrDir = checkFile(args[1]);
        File stdlib = checkFile(args[0]);
        if (!stdlib.isDirectory()) {
            abort("Not a directory: " + stdlib);
        }

        new HtmlDemo().start(stdlib, fileOrDir);
    }
}
