/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer;

import org.python.indexer.ast.NNode;
import org.python.indexer.ast.NModule;
import org.python.indexer.ast.NName;
import org.python.indexer.ast.NUrl;
import org.python.indexer.types.NModuleType;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnknownType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Indexes a set of Python files and builds a code graph. <p>
 * This class is not thread-safe.
 */
public class Indexer {

    /**
     * The global indexer instance.  Provides convenient access to global
     * resources, as well as easy cleanup of resources after the index is built.
     */
    public static Indexer idx;

    /**
     * A scope containing bindings for all modules currently loaded by the indexer.
     */
    public Scope moduleTable = new Scope(null, Scope.Type.GLOBAL);

    /**
     * The top-level (builtin) scope.
     */
    public Scope globaltable = new Scope(null, Scope.Type.GLOBAL);

    /**
     * A map of all bindings created, keyed on their qnames.
     */
    private Map<String, NBinding> allBindings = new HashMap<String, NBinding>();

    /**
     * A map of references to their referenced bindings.  Most nodes will refer
     * to a single binding, and few ever refer to more than two.  One situation
     * in which a multiple reference can occur is the an attribute of a union
     * type.  For instance:
     *
     * <pre>
     *   class A:
     *     def foo(self): pass
     *   class B:
     *     def foo(self): pass
     *   if some_condition:
     *     var = A()
     *   else
     *     var = B()
     *   var.foo()  # foo here refers to both A.foo and B.foo
     * <pre>
     */
    private Map<Ref, List<NBinding>> locations = new HashMap<Ref, List<NBinding>>();

    /**
     * Diagnostics.
     */
    public Map<String, List<Diagnostic>> problems = new HashMap<String, List<Diagnostic>>();
    public Map<String, List<Diagnostic>> parseErrs = new HashMap<String, List<Diagnostic>>();

    public String currentFile = null;
    public String projDir = null;

    public List<String> path = new ArrayList<String>();

    /**
     * Manages a store of serialized ASTs.  ANTLR parsing is one of the slower and
     * more expensive phases of indexing; reusing parse trees can help with resource
     * utilization when indexing several projects (or re-indexing one project).
     */
    private AstCache astCache;

    /**
     * When resolving imports we look in various possible locations.
     * This set keeps track of modules we attempted but didn't find.
     */
    public Set<String> failedModules = new HashSet<String>();

    /**
     * This set tracks module imports that could not be resolved.
     */
    private Map<String, Set<String>> unresolvedModules = new TreeMap<String, Set<String>>();

    /**
     * Manages the built-in modules -- that is, modules from the standard Python
     * library that are implemented in C and consequently have no Python source.
     */
    public Builtins builtins;

    private boolean aggressiveAssertions;

    // stats counters
    private int nloc = 0;
    private int nunbound = 0;
    private int nunknown = 0;
    private int nprob = 0;
    private int nparsing = 0;
    private int loadedFiles = 0;

    private Logger logger = Logger.getLogger(Indexer.class.getCanonicalName());

    public Indexer() {
        idx = this;
        builtins = new Builtins(globaltable, moduleTable);
        builtins.init();
    }

    public void setLogger(Logger logger) {
        if (logger == null) {
            throw new IllegalArgumentException("null logger param");
        }
        logger = logger;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setProjectDir(String cd) throws IOException {
        projDir = Util.canonicalize(cd);
    }

    /**
     * Configures whether the indexer should abort with an exception when it
     * encounters an internal error or unexpected program state.  Normally the
     * indexer attempts to continue indexing, on the assumption that having an
     * index with mostly good data is better than having no index at all.
     * Enabling aggressive assertions is useful for debugging the indexer.
     */
    public void enableAggressiveAssertions(boolean enable) {
        aggressiveAssertions = enable;
    }

    public boolean aggressiveAssertionsEnabled() {
        return aggressiveAssertions;
    }

    /**
     * If aggressive assertions are enabled, propages the passed
     * {@link Throwable}, wrapped in an {@link IndexingException}.
     * @param msg descriptive message; ok to be {@code null}
     * @throws IndexingException
     */
    public void handleException(String msg, Throwable cause) {
        // Stack overflows are still fairly common due to cyclic
        // types, and they take up an awful lot of log space, so we
        // don't log the whole trace by default.
        if (cause instanceof StackOverflowError) {
            logger.log(Level.WARNING, msg, cause);
            return;
        }

        if (aggressiveAssertionsEnabled()) {
            if (msg != null) {
                throw new IndexingException(msg, cause);
            }
            throw new IndexingException(cause);
        }
        if (msg == null)
            msg = "<null msg>";
        if (cause == null)
            cause = new Exception();
        logger.log(Level.WARNING, msg, cause);
    }

    /**
     * Signals a failed assertion about the state of the indexer or index.
     * If aggressive assertions are enabled, throws an {@code IndexingException}.
     * Otherwise the message is logged as a warning, and indexing continues.
     * @param msg a descriptive message about the problem
     * @see enableAggressiveAssertions
     * @throws IndexingException
     */
    public void reportFailedAssertion(String msg) {
        if (aggressiveAssertionsEnabled()) {
            throw new IndexingException(msg, new Exception());  // capture stack
        }
        // Need more configuration control here.
        // Currently getting a hillion jillion of these in large clients.
        if (false) {
            logger.log(Level.WARNING, msg);
        }
    }

    /**
     * Adds the specified absolute paths to the module search path.
     */
    public void addPaths(List<String> p) throws IOException {
        for (String s : p) {
            addPath(s);
        }
    }

    /**
     * Adds the specified absolute path to the module search path.
     */
    public void addPath(String p) throws IOException {
        path.add(Util.canonicalize(p));
    }

    /**
     * Sets the module search path to the specified list of absolute paths.
     */
    public void setPath(List<String> path) throws IOException {
        this.path = new ArrayList<String>(path.size());
        addPaths(path);
    }

    /**
     * Returns the module search path -- the project directory followed by any
     * paths that were added by {@link addPath}.
     */
    public List<String> getLoadPath() {
        List<String> loadPath = new ArrayList<String>();
        if (projDir != null) {
            loadPath.add(projDir);
        }
        loadPath.addAll(path);
        return loadPath;
    }

    public boolean isLibFile(String file) {
        if (file.startsWith("/")) {
            return true;
        }
        if (path != null) {
            for (String p : path) {
                if (file.startsWith(p)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the mutable set of all bindings collected, keyed on their qnames.
     */
    public Map<String, NBinding> getBindings() {
        return allBindings;
    }

    /**
     * Return the binding for {@code qname}, or {@code null} if not known.
     */
    public NBinding lookupQname(String qname) {
        return allBindings.get(qname);
    }

    /**
     * Return the type for {@code qname}, or {@code null} if not known.
     * @throws IllegalStateException if {@link #ready} has not been called.
     */
    public NType lookupQnameType(String qname) {
        NBinding b = lookupQname(qname);
        if (b != null) {
            return b.followType();
        }
        return null;
    }

    NModuleType getCachedModule(String file) {
        return (NModuleType)moduleTable.lookupType(file);
    }

    /**
     * Returns (loading/resolving if necessary) the module for a given source path.
     * @param file absolute file path
     */
    public NModuleType getModuleForFile(String file) throws Exception {
        if (failedModules.contains(file)) {
            return null;
        }
        NModuleType m = getCachedModule(file);
        if (m != null) {
            return m;
        }
        return loadFile(file);
    }

    /**
     * Returns the list, possibly empty but never {@code null}, of
     * errors and warnings generated in the file.
     */
    public List<Diagnostic> getDiagnosticsForFile(String file) {
        List<Diagnostic> errs = problems.get(file);
        if (errs != null) {
            return errs;
        }
        return new ArrayList<Diagnostic>();
    }

    /**
     * Create an outline for a file in the index.
     * @param path the file for which to build the outline
     * @return a list of entries constituting the file outline.
     * Returns an empty list if the indexer hasn't indexed that path.
     */
    public List<Outliner.Entry> generateOutline(String file) throws Exception {
        return new Outliner().generate(this, file);
    }

    /**
     * Add a reference to binding {@code b} at AST node {@code node}.
     * @param node a node referring to a name binding.  Typically a
     * {@link NName}, {@link NStr} or {@link NUrl}.
     */
    public void putLocation(NNode node, NBinding b) {
        if (node == null) {
            return;
        }
        putLocation(new Ref(node), b);
    }

    public void putLocation(Ref ref, NBinding b) {
        if (ref == null) {
            return;
        }
        List<NBinding> bindings = locations.get(ref);
        if (bindings == null) {
            // The indexer is heavily memory-constrained, so we need small overhead.
            // Empirically using a capacity-1 ArrayList for the binding set
            // uses about 1/2 the memory of a LinkedList, and 1/4 the memory
            // of a default HashSet.
            bindings = new ArrayList<NBinding>(1);
            locations.put(ref, bindings);
        }
        if (!bindings.contains(b)) {
            bindings.add(b);
            // Having > 1 is often an indicator of an indexer bug:
            // if (bindings.size() > 1) {
            //     info("now have " + bindings.size() + " bindings for " + ref + " in " +
            //          ref.getFile() + ": " + bindings);
            // }
        }
        b.addRef(ref);
    }

    /**
     * Add {@code node} as a reference to binding {@code b}, removing
     * {@code node} from any other binding ref-lists that it may have occupied.
     * Currently only used in retargeting attribute references from provisional
     * bindings once the actual binding is determined.
     */
    public void updateLocation(Ref node, NBinding b) {
        if (node == null) {
            return;
        }
        List<NBinding> bindings = locations.get(node);
        if (bindings == null) {
            bindings = new ArrayList<NBinding>(1);
            locations.put(node, bindings);
        } else {
            for (NBinding oldb : bindings) {
                oldb.removeRef(node);
            }
            bindings.clear();
        }
        if (!bindings.contains(b)) {
            bindings.add(b);
        }
        b.addRef(node);
    }

    public void removeBinding(NBinding b) {
        allBindings.remove(b);
    }

    public NBinding putBinding(NBinding b) {
        if (b == null) {
            throw new IllegalArgumentException("null binding arg");
        }
        String qname = b.getQname();
        if (qname == null || qname.length() == 0) {
            throw new IllegalArgumentException("Null/empty qname: " + b);
        }

        NBinding existing = allBindings.get(qname);
        if (existing == b) {
            return b;
        }
        if (existing != null) {
            duplicateBindingFailure(b, existing);

            // A bad edge case was triggered by an __init__.py that defined a
            // "Parser" binding (type unknown), and there was a Parser.py in the
            // same directory.  Loading Parser.py resulted in infinite recursion.
            //
            // XXX: need to revisit this logic.  It seems that bindings made in
            // __init__.py probably (?) ought to have __init__ in their qnames
            // to avoid dup-binding conflicts.  The Indexer module table also
            // probably ought not be a normal scope -- it's different enough that
            // overloading it to handle modules is making the logic rather twisty.
            if (b.getKind() == NBinding.Kind.MODULE) {
                return b;
            }

            return existing;
        }
        allBindings.put(qname, b);
        return b;
    }

    private void duplicateBindingFailure(NBinding newb, NBinding oldb) {
        // XXX:  this seems to happen only (and always) for duplicated
        // class or function defs in the same scope.  Need to figure out
        // what the right thing is for this scenario.
        if (true) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Error creating binding ");
        sb.append(newb);
        sb.append(" in file ");
        sb.append(newb.getFirstFile());
        sb.append(": qname already bound to ");
        sb.append(oldb);
        sb.append(" in file ");
        sb.append(oldb.getFirstFile());
        reportFailedAssertion(sb.toString());
    }

    public void putProblem(NNode loc, String msg) {
        String file;
        if (loc != null && ((file = loc.getFile()) != null)) {
            addFileErr(file, loc.start(), loc.end(), msg);
        }
    }

    public void putProblem(String file, int beg, int end, String msg) {
        if (file != null) {
            addFileErr(file, beg, end, msg);
        }
    }

    void addFileErr(String file, int beg, int end, String msg) {
        List<Diagnostic> msgs = getFileErrs(file, problems);
        msgs.add(new Diagnostic(file, Diagnostic.Type.ERROR, beg, end, msg));
    }

    List<Diagnostic> getParseErrs(String file) {
        return getFileErrs(file, parseErrs);
    }

    List<Diagnostic> getFileErrs(String file, Map<String, List<Diagnostic>> map) {
        List<Diagnostic> msgs = map.get(file);
        if (msgs == null) {
            msgs = new ArrayList<Diagnostic>();
            map.put(file, msgs);
        }
        return msgs;
    }

    /**
     * Loads a file and all its ancestor packages.
     * @see #loadFile(String,boolean)
     */
    public NModuleType loadFile(String path) throws Exception {
        return loadFile(path, false);
    }

    /**
     * Loads a module from a string containing the module contents.
     * Idempotent:  looks in the module cache first. Used for simple unit tests.
     * @param path a path for reporting/caching purposes.  The filename
     *        component is used to construct the module qualified name.
     */
    public NModuleType loadString(String path, String contents) throws Exception {
        NModuleType module = getCachedModule(path);
        if (module != null) {
            finer("\nusing cached module " + path + " [succeeded]");
            return module;
        }
        return parseAndResolve(path, contents);
    }

    /**
     * Load, parse and analyze a source file given its absolute path.
     * By default, loads the entire ancestor package chain.
     *
     * @param path the absolute path to the file or directory.
     *        If it is a directory, it is suffixed with "__init__.py", and
     *        only that file is loaded from the directory.
     *
     * @param noparents {@code true} to skip loading ancestor packages
     *
     * @return {@code null} if {@code path} could not be loaded
     */
    public NModuleType loadFile(String path, boolean skipChain) throws Exception {
        File f = new File(path);
        if (f.isDirectory()) {
            finer("\n    loading init file from directory: " + path);
            f = Util.joinPath(path, "__init__.py");
            path = f.getAbsolutePath();
        }

        if (!f.canRead()) {
            finer("\nfile not not found or cannot be read: " + path);
            return null;
        }

        NModuleType module = getCachedModule(path);
        if (module != null) {
            finer("\nusing cached module " + path + " [succeeded]");
            return module;
        }

        if (!skipChain) {
            loadParentPackage(path);
        }
        try {
            return parseAndResolve(path);
        } catch (StackOverflowError soe) {
            handleException("Error loading " + path, soe);
            return null;
        }
    }

    /**
     * When we load a module, load all its parent packages, top-down.
     * This is in part because Python does it anyway, and in part so that you
     * can click on all parent package components in import statements.
     * We load whole ancestor chain top-down, as does Python.
     */
    private void loadParentPackage(String file) throws Exception {
        File f = new File(file);
        File parent = f.getParentFile();
        if (parent == null || isInLoadPath(parent)) {
            return;
        }
        // the parent package of an __init__.py file is the grandparent dir
        if (parent != null && f.isFile() && "__init__.py".equals(f.getName())) {
            parent = parent.getParentFile();
        }
        if (parent == null || isInLoadPath(parent)) {
            return;
        }
        File initpy = Util.joinPath(parent, "__init__.py");
        if (!(initpy.isFile() && initpy.canRead())) {
            return;
        }
        loadFile(initpy.getPath());
    }

    private boolean isInLoadPath(File dir) {
        for (String s : getLoadPath()) {
            if (new File(s).equals(dir)) {
                return true;
            }
        }
        return false;
    }

    private NModuleType parseAndResolve(String file) throws Exception {
        return parseAndResolve(file, null);
    }

    /**
     * Parse a file or string and return its module parse tree.
     * @param file the filename
     * @param contents optional file contents.  If {@code null}, loads the
     *        file contents from disk.
     */
    @SuppressWarnings("unchecked")
    private NModuleType parseAndResolve(String file, String contents) throws Exception {
        // Avoid infinite recursion if any caller forgets this check.  (Has happened.)
        NModuleType nmt = (NModuleType)moduleTable.lookupType(file);
        if (nmt != null) {
            return nmt;
        }

        // Put it in the cache now to prevent circular import from recursing.
        NModuleType mod = new NModuleType(Util.moduleNameFor(file), file, globaltable);
        moduleTable.put(file, new NUrl("file://" + file), mod, NBinding.Kind.MODULE);

        try {
            NModule ast = null;
            if (contents != null) {
                ast = getAstForFile(file, contents);
            } else {
                ast = getAstForFile(file);
            }
            if (ast == null) {
                return null;
            }

            finer("resolving: " + file);
            ast.resolve(globaltable);
            finer("[success]");
            loadedFiles++;
            return mod;
        } catch (OutOfMemoryError e) {
            if (astCache != null) {
                astCache.clear();
            }
            System.gc();
            return null;
        }
    }

    private AstCache getAstCache() throws Exception {
        if (astCache == null) {
            astCache = AstCache.get();
        }
        return astCache;
    }

    /**
     * Returns the syntax tree for {@code file}. <p>
     */
    public NModule getAstForFile(String file) throws Exception {
        return getAstCache().getAST(file);
    }

    /**
     * Returns the syntax tree for {@code file}. <p>
     */
    public NModule getAstForFile(String file, String contents) throws Exception {
        return getAstCache().getAST(file, contents);
    }

    public NModuleType getBuiltinModule(String qname) throws Exception {
        return builtins.get(qname);
    }

    /**
     * This method searches the module path for the module {@code modname}.
     * If found, it is passed to {@link #loadFile}.
     *
     * <p>The mechanisms for importing modules are in general statically
     * undecidable.  We make a reasonable effort to follow the most common
     * lookup rules.
     *
     * @param modname a module name.   Can be a relative path to a directory
     *        or a file (without the extension) or a possibly-qualified
     *        module name.  If it is a module name, cannot contain leading dots.
     *
     * @see http://docs.python.org/reference/simple_stmts.html#the-import-statement
     */
    public NModuleType loadModule(String modname) throws Exception {
        if (failedModules.contains(modname)) {
            return null;
        }

        NModuleType cached = getCachedModule(modname); // builtin file-less modules
        if (cached != null) {
            finer("\nusing cached module " + modname);
            return cached;
        }

        NModuleType mt = getBuiltinModule(modname);
        if (mt != null) {
            return mt;
        }

        finer("looking for module " + modname);

        if (modname.endsWith(".py")) {
            modname = modname.substring(0, modname.length() - 3);
        }
        String modpath = modname.replace('.', '/');

        // A nasty hack to avoid e.g. python2.5 becoming python2/5.
        // Should generalize this for directory components containing '.'.
        modpath = modpath.replaceFirst("(/python[23])/([0-9]/)", "$1.$2");

        List<String> loadPath = getLoadPath();

        for (String p : loadPath) {
            String dirname = p + modpath;
            String pyname = dirname + ".py";
            String initname = Util.joinPath(dirname, "__init__.py").getAbsolutePath();
            String name;

            // foo/bar has priority over foo/bar.py
            // http://www.python.org/doc/essays/packages.html
            if (Util.isReadableFile(initname)) {
                name = initname;
            } else if (Util.isReadableFile(pyname)) {
                name = pyname;
            } else {
                continue;
            }

            name = Util.canonicalize(name);
            NModuleType m = loadFile(name);
            if (m != null) {
                finer("load of module " + modname + "[succeeded]");
                return m;
            }
        }
        finer("failed to find module " + modname + " in load path");
        failedModules.add(modname);
        return null;
    }

    /**
     * Load all Python source files recursively if the given fullname is a
     * directory; otherwise just load a file.  Looks at file extension to
     * determine whether to load a given file.
     */
    public void loadFileRecursive(String fullname) throws Exception {
        File file_or_dir = new File(fullname);
        if (file_or_dir.isDirectory()) {
            for (File file : file_or_dir.listFiles()) {
                loadFileRecursive(file.getAbsolutePath());
            }
        } else {
            if (file_or_dir.getAbsolutePath().endsWith(".py")) {
                loadFile(file_or_dir.getAbsolutePath());
            }
        }
    }

    /**
     * Performs final indexing-building passes, including marking references to
     * undeclared variables. Caller should invoke this method after loading all
     * files.
     */
    public void ready() {
        fine("Checking for undeclared variables");

        for (Entry<Ref, List<NBinding>> ent : locations.entrySet()) {
            Ref ref = ent.getKey();
            List<NBinding> bindings = ent.getValue();

            convertCallToNew(ref, bindings);

            if (countDefs(bindings) == 0) {
                // XXX:  fix me:
                // if (ref instanceof NName && ((NName)ref).isAttribute()) {
                //     nunknown++;  // not so serious
                // } else {
                //     nunbound++;  // more serious
                //     putProblem(ref, "variable may not be bound: " + ref);
                // }
            } else {
                nloc++;
            }
        }

        nprob = problems.size();
        nparsing = parseErrs.size();

        Set<String> removals = new HashSet<String>();
        for (Entry<String, NBinding> e : allBindings.entrySet()) {
            NBinding nb = e.getValue();
            if (nb.isProvisional() || nb.getNumDefs() == 0) {
                removals.add(e.getKey());
            }
        }
        for (String s : removals) {
            allBindings.remove(s);
        }

        locations.clear();
    }

    private void convertCallToNew(Ref ref, List<NBinding> bindings) {
        if (ref.isRef()) {
            return;
        }
        if (bindings.isEmpty()) {
            return;
        }
        NBinding nb = bindings.get(0);
        NType t = nb.followType();
        if (t.isUnionType()) {
            t = t.asUnionType().firstKnownNonNullAlternate();
            if (t == null) {
                return;
            }
        }
        NType tt = t.follow();
        if (!tt.isUnknownType() && !tt.isFuncType()) {
            ref.markAsNew();
        }
    }

    /**
     * Clears the AST cache (to free up memory).  Subsequent calls to
     * {@link #getAstForFile} will either fetch the serialized AST from a
     * disk cache or re-parse the file from scratch.
     */
    public void clearAstCache() {
        if (astCache != null) {
            astCache.clear();
        }
    }

    /**
     * Clears the module table, discarding all resolved ASTs (modules)
     * and their scope information.
     */
    public void clearModuleTable() {
        moduleTable.clear();
        moduleTable = new Scope(null, Scope.Type.GLOBAL);
        clearAstCache();
    }

    private int countDefs(List<NBinding> bindings) {
        int count = 0;
        for (NBinding b : bindings) {
            count += b.getNumDefs();
        }
        return count;
    }

    private String printBindings() {
        StringBuilder sb = new StringBuilder();
        Set<String> sorter = new TreeSet<String>();
        sorter.addAll(allBindings.keySet());
        for (String key : sorter) {
            NBinding b = allBindings.get(key);
            sb.append(b.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Reports a failed module or submodule resolution.
     * @param qname module qname, e.g. "org.foo.bar"
     * @param file the file where the unresolved import occurred
     */
    public void recordUnresolvedModule(String qname, String file) {
        Set<String> importers = unresolvedModules.get(qname);
        if (importers == null) {
            importers = new TreeSet<String>();
            unresolvedModules.put(qname, importers);
        }
        importers.add(file);
    }

    /**
     * Report resolution rate and other statistics data.
     */
    public String getStatusReport() {
        int total = nloc + nunbound + nunknown;
        if (total == 0) {
            total = 1;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Summary: \n")
                .append("- modules loaded: ")
                .append(loadedFiles)
                .append("\n- unresolved modules: ")
                .append(unresolvedModules.size())
                .append("\n");

        for (String s : unresolvedModules.keySet()) {
            sb.append(s).append(": ");
            Set<String> importers = unresolvedModules.get(s);
            if (importers.size() > 5) {
                sb.append(importers.iterator().next());
                sb.append(" and " );
                sb.append(importers.size());
                sb.append(" more");
            } else {
                String files = importers.toString();
                sb.append(files.substring(1, files.length() - 1));
            }
            sb.append("\n");
        }

        // XXX: these are no longer accurate, and need to be fixed.
        // .append("\nnames resolved: " .append(percent(nloc, total)
        // .append("\nunbound: " .append(percent(nunbound, total)
        // .append("\nunknown: " .append(percent(nunknown, total)

        sb.append("\nsemantics problems: ").append(nprob);
        sb.append("\nparsing problems: ").append(nparsing);
        return sb.toString();
    }

    private String percent(int num, int total) {
        double pct = (num * 1.0) / total;
        pct = Math.round(pct * 10000) / 100.0;
        return num + "/" + total + " = " + pct + "%";
    }

    public int numFilesLoaded() {
        return loadedFiles;
    }

    public List<String> getLoadedFiles() {
        List<String> files = new ArrayList<String>();
        for (String file : moduleTable.keySet()) {
            if (file.startsWith("/")) {
                files.add(file);
            }
        }
        return files;
    }

    public void log(Level level, String msg) {
        if (logger.isLoggable(level)) {
            logger.log(level, msg);
        }
    }

    public void severe(String msg) {
        log(Level.SEVERE, msg);
    }

    public void warn(String msg) {
        log(Level.WARNING, msg);
    }

    public void info(String msg) {
        log(Level.INFO, msg);
    }

    public void fine(String msg) {
        log(Level.FINE, msg);
    }

    public void finer(String msg) {
        log(Level.FINER, msg);
    }

    /**
     * Releases all resources for the current indexer.
     */
    public void release() {
        // Null things out to catch anyone who might still be referencing them.
        moduleTable = globaltable = null;
        clearAstCache();
        astCache = null;
        locations = null;
        problems.clear();
        problems = null;
        parseErrs.clear();
        parseErrs = null;
        path.clear();
        path = null;
        failedModules.clear();
        failedModules = null;
        unresolvedModules.clear();
        unresolvedModules = null;
        builtins = null;
        allBindings.clear();
        allBindings = null;

        // Technically this is all that's needed for the garbage collector.
        idx = null;
    }

    @Override
    public String toString() {
        return "<Indexer:locs=" + locations.size() + ":unbound=" + nunbound + ":probs="
                + problems.size() + ":files=" + loadedFiles + ">";
    }
}
